#!/usr/bin/env python3
"""Reproducible all-circuit stat sweeps with no recovery or passing assistance."""

import argparse
from dataclasses import asdict
import hashlib
import itertools
import json
from pathlib import Path
import random
import subprocess
import sys
from types import SimpleNamespace

import evaluate_lap_times as laps


def scenarios():
    result = {"baseline": [1.0] * 4}
    names = ("power", "grip", "aero", "mass")
    for index, name in enumerate(names):
        for value in (0.1, 0.2, 0.4, 0.6, 1.4, 1.8, 2.2, 3.0, 4.0, 5.0):
            stats = [1.0] * 4
            stats[index] = value
            result[f"{name}-{value:g}"] = stats
    for first, second in itertools.combinations(range(4), 2):
        for a, b in itertools.product((0.1, 2.2, 5.0), repeat=2):
            stats = [1.0] * 4
            stats[first], stats[second] = a, b
            result[f"pair-{names[first]}-{a:g}-{names[second]}-{b:g}"] = stats
    for index, values in enumerate(itertools.product((0.1, 5.0), repeat=4)):
        result[f"extreme-{index:02d}"] = list(values)
    for value in (1.4, 2.2, 3.0, 5.0):
        result[f"boost-{value:g}"] = [value, value, value, max(0.1, 2 - value)]
    for prefix, seed in (("mixed", 20260912), ("heldout", 20260913)):
        rng = random.Random(seed)
        for index in range(20):
            result[f"{prefix}-{index:02d}"] = [round(rng.uniform(0.1, 5), 2) for _ in names]
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=str(laps.DEFAULT_JAR))
    parser.add_argument("--profile", default="profile10")
    parser.add_argument("--output", required=True)
    parser.add_argument("--cases", default="")
    parser.add_argument("--maps", default="")
    parser.add_argument("--steps", type=int, default=9000)
    parser.add_argument("--laps", type=int, default=3)
    parser.add_argument("--workers", type=int, default=1)
    parser.add_argument("--retry-incomplete", nargs="+", default=(),
                        help="rerun only incomplete case/map pairs from previous JSONL reports")
    args = parser.parse_args()
    if args.workers < 1:
        parser.error("--workers must be positive")
    if args.steps < 1 or args.laps < 1:
        parser.error("--steps and --laps must be positive")
    if args.cases:
        unknown = set(args.cases.split(',')) - scenarios().keys()
        if unknown:
            parser.error(f"Unknown stat cases: {sorted(unknown)}")
    retry = None
    if args.retry_incomplete:
        retry = set()
        for report in args.retry_incomplete:
            with Path(report).open() as source:
                rows = [json.loads(line) for line in source if line.strip()]
            retry.update((row['case'], row['map_id']) for row in rows
                         if row['completed_laps'] < row['expected_laps'] or row['error'])
    if args.workers > 1:
        names = args.cases.split(",") if args.cases else list(scenarios())
        if retry is not None:
            names = [name for name in names if any(case == name for case, _ in retry)]
        processes = []
        paths = []
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        for index in range(min(args.workers, len(names))):
            path = output.with_name(output.name + f".part{index}")
            paths.append(path)
            command = [sys.executable, __file__, "--jar", args.jar, "--profile", args.profile,
                       "--output", str(path), "--cases", ",".join(names[index::args.workers]),
                       "--steps", str(args.steps), "--laps", str(args.laps)]
            if args.maps:
                command.extend(["--maps", args.maps])
            if args.retry_incomplete:
                command.extend(["--retry-incomplete", *args.retry_incomplete])
            processes.append(subprocess.Popen(command))
        failed = [process.wait() for process in processes]
        if any(failed):
            raise RuntimeError(f"Stat sweep workers failed: {failed}")
        with output.open("w") as destination:
            for path in paths:
                destination.write(path.read_text())
        return
    laps.start_jvm(Path(args.jar))
    policy_path = laps.DEFAULT_POLICY_ROOT / args.profile / laps.POLICY_FILE_NAME
    policy_hash = hashlib.sha256(policy_path.read_bytes()).hexdigest()
    policy = laps.load_policy(policy_path)
    maps = laps.selected_maps("game", args.maps)
    selected = set(args.cases.split(",")) if args.cases else None
    destination = Path(args.output)
    destination.parent.mkdir(parents=True, exist_ok=True)
    run = SimpleNamespace(laps=args.laps, steps=args.steps, timeout_seconds=0,
                          action_repeat=0, driver_action_repeat=0,
                          random_race_spawns=False, seed=20260531,
                          write_driver_metadata=False)
    run = laps.profile_timing_args(run, laps.DEFAULT_PROFILE_ROOT, args.profile)
    with destination.open("w") as output:
        for name, stats in scenarios().items():
            if selected and name not in selected:
                continue
            run.benchmark_stats = stats
            for arena_map in maps:
                if retry is not None and (name, str(arena_map.getId())) not in retry:
                    continue
                row = laps.run_lap_timing(run, arena_map, args.profile, "default", None, policy)
                output.write(json.dumps({"case": name, "stats": stats,
                                         "action_repeat": run.action_repeat,
                                         "step_limit": args.steps,
                                         "policy_sha256": policy_hash,
                                         **asdict(row)}) + "\n")
                output.flush()


if __name__ == "__main__":
    main()
