#!/usr/bin/env python3
"""Compare exported RL driver profiles by lap time on every map."""

from __future__ import annotations

import argparse
import configparser
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
DEFAULT_CAR_PROPERTIES_ROOT = REPO_ROOT / "assets" / "theme" / "gt3" / "cars"
POLICY_FILE_NAME = "rl_enemy_policy.json"
PHYSICS_STEP_SECONDS = 1.0 / 60.0
jpype = None


@dataclass
class TimedRun:
    map_id: str
    profile: str
    car: str
    fastest_lap: float | None
    avg_lap: float | None
    total_time: float | None
    completed_laps: int
    expected_laps: int
    error: str = ""
    is_car_average: bool = False

    @property
    def complete(self) -> bool:
        return not self.error and self.completed_laps >= self.expected_laps


@dataclass
class BestTimes:
    map_values: dict[str, float]
    worst_map_values: dict[str, float]
    profile_values: dict[tuple[str, str], float]


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
    parser.add_argument(
        "--cars",
        default="default",
        help="all, default, or comma-separated one-based car indices",
    )
    parser.add_argument(
        "--car-properties-root",
        default=os.fspath(DEFAULT_CAR_PROPERTIES_ROOT),
        help="directory containing 00.properties, 01.properties, and so on",
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


def load_car_names(properties_root: Path, car_count: int) -> list[str]:
    names: list[str] = []
    for index in range(car_count):
        fallback = str(index + 1)
        path = properties_root / f"{index:02d}.properties"
        try:
            content = path.read_text(encoding="utf-8")
            parser = configparser.ConfigParser(interpolation=None)
            parser.read_string(f"[car]\n{content}")
            name = parser.get("car", "name", fallback=fallback).strip()
        except (OSError, configparser.Error):
            name = fallback
        names.append(name or fallback)
    return names


def selected_cars(
    raw_cars: str,
    car_count: int,
    car_names: list[str] | None = None,
) -> list[tuple[str, int | None]]:
    def label(index: int) -> str:
        if car_names is not None and index < len(car_names):
            return car_names[index]
        return str(index + 1)

    requested = raw_cars.strip().lower()
    if requested in ("", "default"):
        return [("default", None)]
    if requested == "all":
        return [(label(index), index) for index in range(car_count)]

    cars: list[tuple[str, int | None]] = []
    seen: set[int] = set()
    for value in raw_cars.split(","):
        value = value.strip()
        if not value:
            continue
        car_number = int(value)
        if car_number < 1 or car_number > car_count:
            raise ValueError(f"car index must be between 1 and {car_count}: {car_number}")
        car_index = car_number - 1
        if car_index not in seen:
            seen.add(car_index)
            cars.append((label(car_index), car_index))
    if not cars:
        raise ValueError("--cars did not contain a car index")
    return cars


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


def make_environment(
    args: argparse.Namespace,
    ratass_game,
    arena_map,
    route_target: int,
    car_index: int | None,
):
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
    if car_index is not None:
        config.withCarPerformanceIndex(car_index)
    config.addMap(arena_map)
    return ratass_game.RlTrainingEnvironment(config)


def run_lap_timing(
    args: argparse.Namespace,
    arena_map,
    profile: str,
    car: str,
    car_index: int | None,
    policy,
) -> TimedRun:
    ratass_game = jpype.JClass("com.github.jbescos.RatassGame")
    ai_control_decision = jpype.JClass("com.github.jbescos.ai.AiControlDecision")
    java_float_array = jpype.JArray(jpype.JFloat)

    map_id = str(arena_map.getId())
    route_target = max(1, args.laps)
    env = make_environment(args, ratass_game, arena_map, route_target, car_index)
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
                car,
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
                    car,
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
        return TimedRun(map_id, profile, car, None, None, None, 0, args.laps)
    fastest = min(lap_times)
    average = sum(lap_times) / len(lap_times)
    completed_total = sum(lap_times) if len(lap_times) >= args.laps else None
    return TimedRun(
        map_id,
        profile,
        car,
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


def best_times(rows: Iterable[TimedRun], metric: str) -> BestTimes:
    map_values: dict[str, float] = {}
    worst_map_values: dict[str, float] = {}
    profile_values: dict[tuple[str, str], float] = {}
    for row in rows:
        if not row.complete:
            continue
        value = getattr(row, metric)
        if value is None:
            continue
        current = map_values.get(row.map_id)
        if current is None or value < current:
            map_values[row.map_id] = value
        current = worst_map_values.get(row.map_id)
        if current is None or value > current:
            worst_map_values[row.map_id] = value
        if row.is_car_average:
            continue
        profile_key = (row.map_id, row.profile)
        current = profile_values.get(profile_key)
        if current is None or value < current:
            profile_values[profile_key] = value
    return BestTimes(map_values, worst_map_values, profile_values)


def highlight(
    text: str,
    row: TimedRun,
    metric: str,
    best: BestTimes,
) -> str:
    if not row.complete:
        return text
    value = getattr(row, metric)
    if value is None:
        return text
    if row.is_car_average:
        best_value = best.map_values.get(row.map_id)
        if best_value is not None and abs(value - best_value) <= 0.0005:
            return f"**{text}**"
        return text

    map_best = best.map_values.get(row.map_id)
    map_worst = best.worst_map_values.get(row.map_id)
    profile_best = best.profile_values.get((row.map_id, row.profile))
    is_map_best = map_best is not None and abs(value - map_best) <= 0.0005
    is_map_worst = map_worst is not None and abs(value - map_worst) <= 0.0005
    is_profile_best = profile_best is not None and abs(value - profile_best) <= 0.0005
    if row.car == "default":
        if is_map_best:
            return f"**{text}**"
        if is_map_worst:
            return f"--{text}--"
        return text
    if is_map_best and is_profile_best:
        return f"**++{text}++**"
    if is_map_best:
        return f"**{text}**"
    if is_profile_best:
        return f"++{text}++"
    return text


def format_cell(
    row: TimedRun,
    metric: str,
    best: BestTimes,
) -> str:
    if row.is_car_average:
        value = getattr(row, metric)
        if not row.complete or value is None:
            return f"DNF {row.completed_laps}/{row.expected_laps}"
        return highlight(format_duration(value), row, metric, best)
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
    fastest_best: BestTimes,
    average_best: BestTimes,
    total_best: BestTimes,
    include_car: bool,
) -> list[str]:
    cells = [
        row.map_id,
        row.profile,
    ]
    if include_car:
        cells.append(row.car)
    cells.extend(
        [
            format_cell(row, "fastest_lap", fastest_best),
            format_cell(row, "avg_lap", average_best),
            format_cell(row, "total_time", total_best),
        ]
    )
    return cells


def print_table_rows(table_rows: list[list[str]], include_car: bool) -> None:
    headers = ["map", "profile"]
    if include_car:
        headers.append("car")
    headers.extend(["fastest lap", "avg lap", "total time"])
    first_metric = 3 if include_car else 2
    print_markdown_table(
        headers,
        table_rows,
        {first_metric, first_metric + 1, first_metric + 2},
    )


def car_average_row(map_id: str, profile: str, car_rows: list[TimedRun]) -> TimedRun:
    completed = [
        row
        for row in car_rows
        if row.complete
        and row.fastest_lap is not None
        and row.avg_lap is not None
        and row.total_time is not None
        and not row.is_car_average
    ]
    fastest_lap = None
    avg_lap = None
    total_time = None
    if len(completed) == len(car_rows) and completed:
        fastest_lap = sum(float(row.fastest_lap) for row in completed) / len(completed)
        avg_lap = sum(float(row.avg_lap) for row in completed) / len(completed)
        total_time = sum(float(row.total_time) for row in completed) / len(completed)
    return TimedRun(
        map_id=map_id,
        profile=profile,
        car="avg",
        fastest_lap=fastest_lap,
        avg_lap=avg_lap,
        total_time=total_time,
        completed_laps=len(completed),
        expected_laps=len(car_rows),
        is_car_average=True,
    )


def print_table(rows: list[TimedRun], group_by_map: bool) -> None:
    fastest_best = best_times(rows, "fastest_lap")
    average_best = best_times(rows, "avg_lap")
    total_best = best_times(rows, "total_time")
    include_car = any(row.car != "default" for row in rows)
    if group_by_map:
        current_map = None
        table_rows: list[list[str]] = []
        for row in rows:
            if current_map is None:
                current_map = row.map_id
            if row.map_id != current_map:
                print(f"map={current_map}")
                print_table_rows(table_rows, include_car)
                print()
                current_map = row.map_id
                table_rows = []
            table_rows.append(
                lap_time_row(row, fastest_best, average_best, total_best, include_car)
            )
        if current_map is not None:
            print(f"map={current_map}")
            print_table_rows(table_rows, include_car)
        return

    table_rows: list[list[str]] = []
    for row in rows:
        table_rows.append(lap_time_row(row, fastest_best, average_best, total_best, include_car))
    print_table_rows(table_rows, include_car)


def main() -> None:
    args = parse_args()
    if args.laps <= 0:
        raise ValueError("--laps must be positive")

    start_jvm(Path(args.jar).resolve())
    ratass_game = jpype.JClass("com.github.jbescos.RatassGame")
    profiles = profile_ids(Path(args.profile_root), args.profiles)
    maps = selected_maps(args.map_source, args.map_ids)
    car_count = int(ratass_game.getCarPerformanceCount())
    car_names = load_car_names(Path(args.car_properties_root), car_count)
    cars = selected_cars(args.cars, car_count, car_names)
    policy_root = Path(args.policy_root)
    rows: list[TimedRun] = []

    for arena_map in maps:
        map_id = str(arena_map.getId())
        map_rows: list[TimedRun] = []
        for profile in profiles:
            profile_rows: list[TimedRun] = []
            policy_path = policy_root / profile / POLICY_FILE_NAME
            if not policy_path.exists():
                message = f"missing_policy profile={profile} path={policy_path}"
                print(message, file=sys.stderr)
                if args.include_missing:
                    for car, _ in cars:
                        profile_rows.append(
                            TimedRun(
                                map_id,
                                profile,
                                car,
                                None,
                                None,
                                None,
                                0,
                                args.laps,
                                "missing policy",
                            )
                        )
            else:
                try:
                    policy = load_policy(policy_path)
                except Exception as exc:  # noqa: BLE001 - report and continue the matrix.
                    for car, _ in cars:
                        profile_rows.append(
                            TimedRun(
                                map_id,
                                profile,
                                car,
                                None,
                                None,
                                None,
                                0,
                                args.laps,
                                str(exc),
                            )
                        )
                else:
                    for car, car_index in cars:
                        if not args.quiet:
                            print(
                                f"evaluating map={map_id} profile={profile} car={car}",
                                file=sys.stderr,
                                flush=True,
                            )
                        try:
                            profile_rows.append(
                                run_lap_timing(
                                    args,
                                    arena_map,
                                    profile,
                                    car,
                                    car_index,
                                    policy,
                                )
                            )
                        except Exception as exc:  # noqa: BLE001 - continue the full matrix.
                            profile_rows.append(
                                TimedRun(
                                    map_id,
                                    profile,
                                    car,
                                    None,
                                    None,
                                    None,
                                    0,
                                    args.laps,
                                    str(exc),
                                )
                            )
            if len(cars) > 1 and profile_rows:
                profile_rows.append(car_average_row(map_id, profile, profile_rows))
            map_rows.extend(profile_rows)
        rows.extend(map_rows)
        if args.stream_map_tables:
            print_table(map_rows, True)
            print()
            sys.stdout.flush()

    if not args.stream_map_tables:
        print_table(rows, args.group_by_map)


if __name__ == "__main__":
    main()
