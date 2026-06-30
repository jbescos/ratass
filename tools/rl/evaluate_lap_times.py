#!/usr/bin/env python3
"""Compare exported RL driver profiles by lap time on every map."""

from __future__ import annotations

import argparse
import os
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_POLICY_ROOT = REPO_ROOT / "assets" / "ai" / "policies"
DEFAULT_PROFILE_ROOT = REPO_ROOT / "tools" / "rl" / "policies"
POLICY_FILE_NAME = "rl_enemy_policy.json"
PHYSICS_STEP_SECONDS = 1.0 / 60.0
jpype = None


@dataclass
class TimedRun:
    map_id: str
    profile: str
    fastest_lap: float | None
    avg_lap: float | None
    total_time: float | None
    completed_laps: int
    expected_laps: int
    error: str = ""

    @property
    def complete(self) -> bool:
        return not self.error and self.completed_laps >= self.expected_laps


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--policy-root", default=os.fspath(DEFAULT_POLICY_ROOT))
    parser.add_argument("--profile-root", default=os.fspath(DEFAULT_PROFILE_ROOT))
    parser.add_argument(
        "--profiles",
        default="all",
        help="comma-separated profile ids; default is all profile folders",
    )
    parser.add_argument(
        "--map-source",
        choices=("all", "game", "training"),
        default="all",
        help="all includes normal game maps plus training/test maps",
    )
    parser.add_argument(
        "--map-ids",
        default="",
        help="comma-separated map ids; empty means every selected map",
    )
    parser.add_argument("--laps", type=int, default=5)
    parser.add_argument(
        "--steps",
        type=int,
        default=6400,
        help="maximum RL actions per map/profile; 0 means no practical step limit",
    )
    parser.add_argument("--action-repeat", type=int, default=4)
    parser.add_argument("--seed", type=int, default=20260531)
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=0.0,
        help="wall-clock timeout per map/profile; 0 disables the timeout",
    )
    parser.add_argument(
        "--random-race-spawns",
        action="store_true",
        help="use random race spawns instead of the fixed starting grid",
    )
    parser.add_argument(
        "--include-missing",
        action="store_true",
        help="include missing policy models in the table instead of only warning",
    )
    parser.add_argument(
        "--group-by-map",
        action="store_true",
        help="print a separate table for each map",
    )
    parser.add_argument(
        "--stream-map-tables",
        action="store_true",
        help="print each map table as soon as all profiles for that map finish",
    )
    parser.add_argument("--quiet", action="store_true")
    return parser.parse_args()


def start_jvm(jar_path: Path) -> None:
    ensure_jpype()
    if jpype.isJVMStarted():
        return
    if not jar_path.exists():
        raise FileNotFoundError(f"{jar_path} does not exist. Run `mvn -pl desktop -am package` first.")
    max_heap = os.environ.get("RATASS_RL_JVM_MAX_HEAP", "512m").strip()
    jvm_args = ["-Xrs"]
    if max_heap:
        jvm_args.append(f"-Xmx{max_heap}")
    jpype.startJVM(jpype.getDefaultJVMPath(), *jvm_args, classpath=[str(jar_path)])
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


def ensure_jpype() -> None:
    global jpype
    if jpype is not None:
        return
    try:
        import jpype as jpype_module
    except ModuleNotFoundError as exc:
        venv_python = REPO_ROOT / ".venv-rl" / "bin" / "python"
        if venv_python.exists() and Path(sys.executable) != venv_python:
            os.execv(os.fspath(venv_python), [os.fspath(venv_python), __file__, *sys.argv[1:]])
        raise ModuleNotFoundError(
            "jpype is not available. Run with `.venv-rl/bin/python "
            "tools/rl/evaluate_lap_times.py ...`."
        ) from exc
    jpype = jpype_module


def profile_ids(profile_root: Path, raw_profiles: str) -> list[str]:
    if raw_profiles.strip() and raw_profiles.strip().lower() != "all":
        return [value.strip() for value in raw_profiles.split(",") if value.strip()]
    return sorted(path.name for path in profile_root.iterdir() if path.is_dir())


def add_maps(target: list, source, seen: set[str]) -> None:
    for index in range(int(source.size)):
        arena_map = source.get(index)
        map_id = str(arena_map.getId())
        if map_id in seen:
            continue
        seen.add(map_id)
        target.append(arena_map)


def selected_maps(map_source: str, map_ids: str):
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    requested = [value.strip() for value in map_ids.split(",") if value.strip()]
    if requested:
        selected = arena_maps.createSelectedSet(",".join(requested))
        by_id = {
            str(selected.get(index).getId()): selected.get(index)
            for index in range(int(selected.size))
        }
        return [by_id[map_id] for map_id in requested]

    maps = []
    seen: set[str] = set()
    if map_source in ("all", "game"):
        add_maps(maps, arena_maps.createDefaultSet(), seen)
    if map_source in ("all", "training"):
        add_maps(maps, arena_maps.createHeadlessTrainingSet(), seen)

    return maps


def load_policy(policy_path: Path):
    rl_policy = jpype.JClass("com.github.jbescos.ai.rl.RlPolicy")
    return rl_policy.fromJson(policy_path.read_text(encoding="utf-8"))


def make_environment(args: argparse.Namespace, ratass_game, arena_map, route_target: int):
    max_action_steps = args.steps if args.steps > 0 else 2_147_483_647
    config = (
        ratass_game.RlTrainingConfig()
        .withControlledAgentCount(1)
        .withFieldSize(1)
        .withActionRepeat(args.action_repeat)
        .withMaxActionSteps(max_action_steps)
        .withRouteTargets(route_target)
        .withRaceMode(True)
        .withRandomRaceSpawns(bool(args.random_race_spawns))
        .withRewardBreakdownEnabled(False)
        .withStepDetailsEnabled(True)
        .withDebugTraceEnabled(False)
        .withSeed(args.seed)
    )
    config.addMap(arena_map)
    return ratass_game.RlTrainingEnvironment(config)


def run_lap_timing(args: argparse.Namespace, arena_map, profile: str, policy) -> TimedRun:
    ratass_game = jpype.JClass("com.github.jbescos.RatassGame")
    ai_control_decision = jpype.JClass("com.github.jbescos.ai.AiControlDecision")
    java_float_array = jpype.JArray(jpype.JFloat)

    map_id = str(arena_map.getId())
    route_target = max(1, args.laps)
    env = make_environment(args, ratass_game, arena_map, route_target)
    lap_times: list[float] = []
    total_time = 0.0
    last_lap_time = 0.0
    try:
        result = env.reset()
        started_at = time.monotonic()
        env_observation_size = int(env.getObservationSize())
        policy_observation_size = int(policy.getObservationSize())
        scratch_size = int(policy.getScratchSize())
        if policy_observation_size != env_observation_size:
            return TimedRun(
                map_id,
                profile,
                None,
                None,
                None,
                0,
                args.laps,
                f"policy observation size {policy_observation_size} != env {env_observation_size}",
            )

        observation = java_float_array(env_observation_size)
        scratch_a = java_float_array(scratch_size)
        scratch_b = java_float_array(scratch_size)
        decision_buffer = ai_control_decision()
        action_buffer = java_float_array(2)
        while not result.episodeDone and (args.steps <= 0 or int(result.actionStep) < args.steps):
            if args.timeout_seconds > 0 and time.monotonic() - started_at >= args.timeout_seconds:
                return TimedRun(
                    map_id,
                    profile,
                    None,
                    None,
                    None,
                    len(lap_times),
                    args.laps,
                    "timedout",
                )
            for observation_index in range(env_observation_size):
                observation[observation_index] = result.observations[observation_index]
            decision = policy.computeAction(
                observation,
                scratch_a,
                scratch_b,
                decision_buffer,
            )
            action_buffer[0] = decision.throttle
            action_buffer[1] = decision.turn
            result = env.step(action_buffer)
            total_time = int(result.actionStep) * max(1, args.action_repeat) * PHYSICS_STEP_SECONDS
            reached = int(result.routeTargetsReached[0]) if len(result.routeTargetsReached) > 0 else 0
            while len(lap_times) < args.laps and reached >= (len(lap_times) + 1):
                lap_times.append(total_time - last_lap_time)
                last_lap_time = total_time
    finally:
        env.close()

    if not lap_times:
        return TimedRun(map_id, profile, None, None, None, 0, args.laps)
    fastest = min(lap_times)
    average = sum(lap_times) / len(lap_times)
    completed_total = sum(lap_times) if len(lap_times) >= args.laps else None
    return TimedRun(
        map_id,
        profile,
        fastest,
        average,
        completed_total,
        len(lap_times),
        args.laps,
    )


def format_duration(value: float | None) -> str:
    if value is None:
        return "-"
    minutes = int(value // 60)
    seconds = value - minutes * 60
    if minutes > 0:
        return f"{minutes}:{seconds:06.3f}"
    return f"{seconds:.3f}"


def best_values_by_map(rows: Iterable[TimedRun], metric: str) -> dict[str, float]:
    best: dict[str, float] = {}
    for row in rows:
        if not row.complete:
            continue
        value = getattr(row, metric)
        if value is None:
            continue
        current = best.get(row.map_id)
        if current is None or value < current:
            best[row.map_id] = value
    return best


def highlight(text: str, row: TimedRun, metric: str, best: dict[str, float]) -> str:
    if not row.complete:
        return text
    value = getattr(row, metric)
    if value is None:
        return text
    best_value = best.get(row.map_id)
    if best_value is not None and abs(value - best_value) <= 0.0005:
        return f"**{text}**"
    return text


def format_cell(row: TimedRun, metric: str, best: dict[str, float]) -> str:
    if row.error:
        return row.error
    value = getattr(row, metric)
    if metric == "total_time" and not row.complete:
        return f"DNF {row.completed_laps}/{row.expected_laps}"
    return highlight(format_duration(value), row, metric, best)


def print_markdown_table(headers: list[str], rows: list[list[str]], right_aligned: set[int]) -> None:
    widths = [len(header) for header in headers]
    for row in rows:
        for index, cell in enumerate(row):
            widths[index] = max(widths[index], len(cell))

    def format_row(cells: list[str]) -> str:
        formatted = []
        for index, cell in enumerate(cells):
            if index in right_aligned:
                formatted.append(cell.rjust(widths[index]))
            else:
                formatted.append(cell.ljust(widths[index]))
        return "| " + " | ".join(formatted) + " |"

    separators = []
    for index, width in enumerate(widths):
        marker_width = max(3, width)
        if index in right_aligned:
            separators.append("-" * (marker_width - 1) + ":")
        else:
            separators.append("-" * marker_width)

    print(format_row(headers))
    print("| " + " | ".join(separators) + " |")
    for row in rows:
        print(format_row(row))


def lap_time_row(
    row: TimedRun,
    fastest_best: dict[str, float],
    average_best: dict[str, float],
    total_best: dict[str, float],
) -> list[str]:
    return [
        row.map_id,
        row.profile,
        format_cell(row, "fastest_lap", fastest_best),
        format_cell(row, "avg_lap", average_best),
        format_cell(row, "total_time", total_best),
    ]


def print_table_rows(table_rows: list[list[str]]) -> None:
    print_markdown_table(
        ["map", "profile", "fastest lap", "avg lap", "total time"],
        table_rows,
        {2, 3, 4},
    )


def print_table(rows: list[TimedRun], group_by_map: bool) -> None:
    fastest_best = best_values_by_map(rows, "fastest_lap")
    average_best = best_values_by_map(rows, "avg_lap")
    total_best = best_values_by_map(rows, "total_time")
    if group_by_map:
        current_map = None
        table_rows: list[list[str]] = []
        for row in rows:
            if current_map is None:
                current_map = row.map_id
            if row.map_id != current_map:
                print(f"map={current_map}")
                print_table_rows(table_rows)
                print()
                current_map = row.map_id
                table_rows = []
            table_rows.append(lap_time_row(row, fastest_best, average_best, total_best))
        if current_map is not None:
            print(f"map={current_map}")
            print_table_rows(table_rows)
        return

    table_rows: list[list[str]] = []
    for row in rows:
        table_rows.append(lap_time_row(row, fastest_best, average_best, total_best))
    print_table_rows(table_rows)


def main() -> None:
    args = parse_args()
    if args.laps <= 0:
        raise ValueError("--laps must be positive")

    start_jvm(Path(args.jar).resolve())
    profiles = profile_ids(Path(args.profile_root), args.profiles)
    maps = selected_maps(args.map_source, args.map_ids)
    policy_root = Path(args.policy_root)
    rows: list[TimedRun] = []

    for arena_map in maps:
        map_id = str(arena_map.getId())
        map_rows: list[TimedRun] = []
        for profile in profiles:
            policy_path = policy_root / profile / POLICY_FILE_NAME
            if not policy_path.exists():
                message = f"missing_policy profile={profile} path={policy_path}"
                print(message, file=sys.stderr)
                if args.include_missing:
                    map_rows.append(
                        TimedRun(map_id, profile, None, None, None, 0, args.laps, "missing policy")
                    )
                continue
            if not args.quiet:
                print(f"evaluating map={map_id} profile={profile}", file=sys.stderr, flush=True)
            try:
                policy = load_policy(policy_path)
                map_rows.append(run_lap_timing(args, arena_map, profile, policy))
            except Exception as exc:  # noqa: BLE001 - report and continue the full matrix.
                map_rows.append(TimedRun(map_id, profile, None, None, None, 0, args.laps, str(exc)))
        rows.extend(map_rows)
        if args.stream_map_tables:
            print_table(map_rows, True)
            print()
            sys.stdout.flush()

    if not args.stream_map_tables:
        print_table(rows, args.group_by_map)


if __name__ == "__main__":
    main()
