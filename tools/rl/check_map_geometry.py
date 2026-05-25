#!/usr/bin/env python3
"""Check whether race checkpoints and route targets sit on playable road."""

from __future__ import annotations

import argparse
import math
import os
from pathlib import Path

import jpype


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_ROUTE_MARGIN = 0.75


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--map-id", default="")
    parser.add_argument("--min-clearance", type=float, default=0.50)
    parser.add_argument("--route-margin", type=float, default=DEFAULT_ROUTE_MARGIN)
    return parser.parse_args()


def start_jvm(jar_path: Path) -> None:
    if jpype.isJVMStarted():
        return
    jpype.startJVM(classpath=[str(jar_path)])
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


def fmt(value: float) -> str:
    return f"{value:.3f}"


def gate_supported_fraction(arena_map, checkpoint) -> float:
    if not bool(checkpoint.hasGate):
        return 1.0
    samples = 15
    supported = 0
    for index in range(samples):
        alpha = index / (samples - 1)
        x = checkpoint.gateStartX + (checkpoint.gateEndX - checkpoint.gateStartX) * alpha
        y = checkpoint.gateStartY + (checkpoint.gateEndY - checkpoint.gateStartY) * alpha
        if bool(arena_map.supports(float(x), float(y))):
            supported += 1
    return supported / samples


def checkpoint_route(arena_map, checkpoint, next_checkpoint, route_margin: float):
    vector2 = jpype.JClass("com.badlogic.gdx.math.Vector2")
    from_point = vector2(float(checkpoint.x), float(checkpoint.y))
    target = vector2(float(next_checkpoint.x), float(next_checkpoint.y))
    route_target = vector2()
    arena_map.findDriveTarget(from_point, target, float(route_margin), route_target)
    route_distance = float(arena_map.estimateDriveDistance(from_point, target, float(route_margin)))
    direct_distance = math.hypot(
        float(next_checkpoint.x - checkpoint.x),
        float(next_checkpoint.y - checkpoint.y),
    )
    return route_target, route_distance, direct_distance


def diagnose_map(arena_map, min_clearance: float, route_margin: float) -> int:
    checkpoint_count = int(arena_map.getCheckpointCount())
    spawn_count = int(arena_map.getSpawnCount())
    issues = 0
    print(
        f"map={arena_map.getId()} checkpoints={checkpoint_count} spawns={spawn_count}",
        flush=True,
    )
    for index in range(checkpoint_count):
        checkpoint = arena_map.getCheckpoint(index)
        next_checkpoint = arena_map.getCheckpoint((index + 1) % checkpoint_count)
        center_supported = bool(arena_map.supports(float(checkpoint.x), float(checkpoint.y)))
        center_clearance = float(
            arena_map.approximateDistanceToHazard(float(checkpoint.x), float(checkpoint.y))
        )
        gate_fraction = gate_supported_fraction(arena_map, checkpoint)
        route_target, route_distance, direct_distance = checkpoint_route(
            arena_map,
            checkpoint,
            next_checkpoint,
            route_margin,
        )
        route_supported = bool(arena_map.supports(route_target))
        route_clearance = float(arena_map.approximateDistanceToHazard(route_target))
        ratio = route_distance / max(0.001, direct_distance)
        checkpoint_issues = []
        if not center_supported:
            checkpoint_issues.append("center_off_road")
        if center_clearance < min_clearance:
            checkpoint_issues.append("low_center_clearance")
        if bool(checkpoint.hasGate) and gate_fraction < 0.45:
            checkpoint_issues.append("gate_mostly_off_road")
        if not route_supported:
            checkpoint_issues.append("route_target_off_road")
        if route_clearance < min_clearance:
            checkpoint_issues.append("low_route_clearance")
        issues += len(checkpoint_issues)
        print(
            "  "
            f"checkpoint={index} "
            f"x={fmt(float(checkpoint.x))} y={fmt(float(checkpoint.y))} "
            f"center_supported={int(center_supported)} "
            f"center_clearance={fmt(center_clearance)} "
            f"gate_fraction={fmt(gate_fraction)} "
            f"route_target_x={fmt(float(route_target.x))} "
            f"route_target_y={fmt(float(route_target.y))} "
            f"route_supported={int(route_supported)} "
            f"route_clearance={fmt(route_clearance)} "
            f"route_distance={fmt(route_distance)} "
            f"direct_distance={fmt(direct_distance)} "
            f"route_direct_ratio={fmt(ratio)} "
            f"issues={','.join(checkpoint_issues) if checkpoint_issues else 'none'}",
            flush=True,
        )
    print(f"map={arena_map.getId()} issue_count={issues}", flush=True)
    return issues


def main() -> None:
    args = parse_args()
    start_jvm(Path(args.jar))
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createDefaultSet()
    total_issues = 0
    checked = 0
    for index in range(int(maps.size)):
        arena_map = maps.get(index)
        if args.map_id and str(arena_map.getId()) != args.map_id:
            continue
        checked += 1
        total_issues += diagnose_map(arena_map, args.min_clearance, args.route_margin)
    if checked == 0:
        raise SystemExit(f"Unknown map id: {args.map_id}")
    print(f"summary maps={checked} issue_count={total_issues}", flush=True)
    raise SystemExit(1 if total_issues else 0)


if __name__ == "__main__":
    main()
