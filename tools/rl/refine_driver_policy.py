#!/usr/bin/env python3
"""Refine exported driver weights against real lap times without changing runtime controls."""

from __future__ import annotations

import argparse
import copy
import json
from pathlib import Path
import subprocess
import time

import jpype
import numpy as np

ROOT = Path(__file__).resolve().parents[2]


def passes_regression_limits(rows: np.ndarray, baseline: np.ndarray,
                             mean_slack: float = float("inf"),
                             allow_existing_failures: bool = False) -> bool:
    required = baseline[:, 0] == 1 if allow_existing_failures else np.ones(len(rows), dtype=bool)
    if not required.any() or not np.isfinite(rows).all():
        return False
    # A pre-existing failure must not hide a newly failing map or distort the time comparison.
    checked, reference = rows[required], baseline[required]
    return bool(np.all(checked[:, 0] == 1)
                and np.max(checked[:, 1] - reference[:, 1]) <= 2.0
                and checked[:, 1].mean() <= reference[:, 1].mean() + mean_slack
                and checked[:, 2].mean() <= reference[:, 2].mean() * 1.15 + 0.002)


def calibrated_policy(source: dict, parameters: np.ndarray) -> dict:
    """Fold gains and offsets into the final linear layer, before action clipping."""
    result = copy.deepcopy(source)
    layer = result["layers"][-1]
    if layer["activation"] != "linear" or result["actionSize"] != 2:
        raise ValueError("Expected two driving actions and a linear output layer")
    weights = np.asarray(layer["weights"]).reshape(layer["outputSize"], layer["inputSize"])
    bias = np.asarray(layer["bias"])
    for i in range(2):
        gain = np.exp(parameters[i])
        weights[i] *= gain
        bias[i] = bias[i] * gain + parameters[i + 2]
    layer["weights"] = weights.flatten().tolist()
    layer["bias"] = bias.tolist()
    return result


def adjusted_hidden_unit(source: dict, layer_index: int, unit: int,
                         log_gain: float = 0.0, offset: float = 0.0) -> dict:
    """Adjust one neuron's preactivation, leaving architecture and other neurons intact."""
    if not 0 <= layer_index < len(source["layers"]) - 1:
        raise ValueError("Expected a hidden layer")
    if not np.isfinite([log_gain, offset]).all():
        raise ValueError("Neuron adjustments must be finite")
    result = copy.deepcopy(source)
    layer = result["layers"][layer_index]
    if not 0 <= unit < layer["outputSize"]:
        raise ValueError("Invalid hidden unit")
    weights = np.asarray(layer["weights"], dtype=float).reshape(
        layer["outputSize"], layer["inputSize"])
    gain = np.exp(log_gain)
    weights[unit] *= gain
    layer["weights"] = weights.flatten().tolist()
    layer["bias"][unit] = layer["bias"][unit] * gain + offset
    return result


def blended_policy(source: dict, other: dict, fraction: float) -> dict:
    if not 0 <= fraction <= 1:
        raise ValueError("Blend fraction must be between zero and one")
    if any(source[key] != other[key] for key in ("observationSize", "actionSize")):
        raise ValueError("Policies have different observation/action contracts")
    if len(source["layers"]) != len(other["layers"]):
        raise ValueError("Policies have different architectures")
    result = copy.deepcopy(source)
    for layer, target in zip(result["layers"], other["layers"]):
        if any(layer[key] != target[key] for key in ("inputSize", "outputSize", "activation")):
            raise ValueError("Policies have different architectures")
        for key in ("weights", "bias"):
            layer[key] = ((1 - fraction) * np.asarray(layer[key])
                          + fraction * np.asarray(target[key])).tolist()
    return result


def start_benchmark():
    classes = ROOT / "target" / "driver-policy-benchmark"
    classes.mkdir(parents=True, exist_ok=True)
    jar = ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
    subprocess.run(["javac", "-cp", str(jar), "-d", str(classes),
                    str(Path(__file__).with_name("DriverPolicyBenchmark.java"))], check=True)
    jpype.startJVM("-Xmx768m", "--enable-native-access=ALL-UNNAMED",
                   classpath=[str(classes), str(jar)])
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    jpype.JClass("com.badlogic.gdx.Gdx").files = jpype.JClass(
        "com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()
    return jpype.JClass("DriverPolicyBenchmark")()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--repeat", type=int, default=4)
    parser.add_argument("--rounds", type=int, default=10)
    parser.add_argument("--weight-rounds", type=int, default=0,
                        help="coordinate-search passes over the learned output weights")
    parser.add_argument("--weight-step", type=float, default=0.01)
    parser.add_argument("--input-rounds", type=int, default=0,
                        help="coordinate-search passes over input-layer column gains")
    parser.add_argument("--hidden-rounds", type=int, default=0,
                        help="coordinate-search passes over hidden-neuron gains and biases")
    parser.add_argument("--hidden-step", type=float, default=0.02)
    parser.add_argument("--blend-with", type=Path, nargs="*", default=[],
                        help="compatible exported policies to blend with the original source")
    parser.add_argument("--blend-fractions", default="0.01,0.02,0.05,0.1,0.2,0.35,0.5",
                        help="comma-separated proportions of the other policy")
    parser.add_argument("--seed", type=int, default=20260531)
    parser.add_argument("--laps", type=int, default=3)
    parser.add_argument("--guard-laps", type=int, default=5,
                        help="also require potential improvements to finish longer races")
    parser.add_argument("--guard-tuning", default=(
        "AGILE_CHASSIS,CARBON_MONOCOQUE,CARBON_PROTOTYPE,"
        "HYPERCAR_CORE,CHAMPIONSHIP_TUNE,GROUND_EFFECT"),
                        help="comma-separated tuning builds checked before accepting improvements")
    parser.add_argument("--guard-amplified-tuning", default="CARBON_PROTOTYPE,HYPERCAR_CORE,GROUND_EFFECT",
                        help="additional tuning builds checked at x3 before accepting improvements")
    parser.add_argument("--random-spawns", action="store_true")
    parser.add_argument("--car-index", type=int, default=-1)
    parser.add_argument("--evaluate-only", action="store_true")
    parser.add_argument("--validate-against", type=Path,
                        help="compare two policies across cars, longer races and random starts")
    args = parser.parse_args()
    if min(args.repeat, args.laps, args.guard_laps) < 1 or min(
            args.rounds, args.weight_rounds, args.input_rounds, args.hidden_rounds) < 0 or min(
                args.weight_step, args.hidden_step) <= 0:
        parser.error("repeat/laps must be positive and rounds nonnegative")
    args.output_dir.mkdir(parents=True, exist_ok=True)
    benchmark = start_benchmark()
    source = json.loads(args.policy.read_text())
    if args.validate_against:
        reference = args.validate_against.read_text()
        cases = [("grid", 3, args.seed, False, -1, ""),
                 ("five-laps", 5, args.seed, False, -1, ""),
                 ("ten-laps", 10, args.seed, False, -1, ""),
                 ("random-a", 3, 20260910, True, -1, ""),
                 ("random-b", 3, 20260911, True, -1, "")]
        cases += [(f"car-{car:02d}", 3, args.seed, False, car, "") for car in range(10)]
        cases += [(card, 5, args.seed, False, -1, card) for card in (
            "AGILE_CHASSIS", "CARBON_MONOCOQUE", "CARBON_PROTOTYPE",
            "HYPERCAR_CORE", "CHAMPIONSHIP_TUNE", "GROUND_EFFECT")]
        comparisons = []
        cases = [(*case, 1.0) for case in cases]
        cases += [(f"{card}-x3", 5, args.seed, False, -1, card, 3.0) for card in (
            "CARBON_PROTOTYPE", "HYPERCAR_CORE", "GROUND_EFFECT")]
        for name, laps, seed, random_spawns, car, card, multiplier in cases:
            reference_rows = np.asarray(benchmark.evaluate(
                reference, args.repeat, laps, seed, random_spawns, car, card, multiplier))
            candidate_rows = np.asarray(benchmark.evaluate(
                json.dumps(source), args.repeat, laps, seed, random_spawns, car, card, multiplier))
            comparison = {"case": name,
                          "reference_average": float(reference_rows[:, 1].mean()),
                          "candidate_average": float(candidate_rows[:, 1].mean()),
                          "reference_completed": int(reference_rows[:, 0].sum()),
                          "candidate_completed": int(candidate_rows[:, 0].sum()),
                          "reference_offroad": float(reference_rows[:, 2].mean()),
                          "candidate_offroad": float(candidate_rows[:, 2].mean()),
                          "reference_rows": reference_rows.tolist(),
                          "candidate_rows": candidate_rows.tolist()}
            comparisons.append(comparison)
            (args.output_dir / "validation.json").write_text(
                json.dumps(comparisons, indent=2) + "\n")
            print(json.dumps({k: v for k, v in comparison.items()
                              if not k.endswith("_rows")}), flush=True)
        jpype.shutdownJVM()
        return
    history = []
    best_score = float("inf")
    best_parameters = np.zeros(4)
    best_policy = copy.deepcopy(source)
    baseline = None
    guard_baseline = np.asarray(benchmark.evaluate(json.dumps(source), args.repeat,
                               args.guard_laps, args.seed, False, args.car_index))
    if not args.evaluate_only and not np.all(guard_baseline[:, 0] == 1):
        raise ValueError("Starting policy must finish all longer-race validation maps")
    tuning_baselines = {}
    if not args.evaluate_only:
        builds = [(value.strip(), multiplier)
                  for values, multiplier in ((args.guard_tuning, 1.0),
                                             (args.guard_amplified_tuning, 3.0))
                  for value in values.split(",") if value.strip()]
        for card, multiplier in builds:
            tuning_baselines[(card, multiplier)] = np.asarray(benchmark.evaluate(json.dumps(source),
                args.repeat, args.guard_laps, args.seed, False, args.car_index, card, multiplier))
            if not np.any(tuning_baselines[(card, multiplier)][:, 0] == 1):
                raise ValueError(f"Starting policy does not finish any map with {card} x{multiplier:g}")

    def evaluate(parameters, candidate=None):
        nonlocal best_score, best_parameters, best_policy, baseline
        if candidate is None:
            candidate = calibrated_policy(source, parameters)
        started = time.monotonic()
        rows = np.asarray(benchmark.evaluate(json.dumps(candidate), args.repeat,
                          args.laps, args.seed, args.random_spawns, args.car_index))
        if baseline is None:
            baseline = rows.copy()
        complete = bool(np.all(rows[:, 0] == 1))
        # Require every map to finish, with no severe map-time or off-road regression.
        safe = passes_regression_limits(rows, baseline)
        score = float(rows[:, 1].mean()) if safe else float("inf")
        guard_rows = None
        tuning_guards = {}
        if score < best_score:
            guard_rows = np.asarray(benchmark.evaluate(json.dumps(candidate), args.repeat,
                                   args.guard_laps, args.seed, False, args.car_index))
            safe = passes_regression_limits(guard_rows, guard_baseline, 0.05)
            if not safe:
                score = float("inf")
        if score < best_score:
            for (card, multiplier), reference_rows in tuning_baselines.items():
                tuning_rows = np.asarray(benchmark.evaluate(json.dumps(candidate),
                    args.repeat, args.guard_laps, args.seed, False, args.car_index, card, multiplier))
                tuning_guards[f"{card}-x{multiplier:g}"] = tuning_rows.tolist()
                if not passes_regression_limits(tuning_rows, reference_rows, 0.05,
                                                allow_existing_failures=True):
                    safe = False
                    score = float("inf")
                    break
        record = {"evaluation": len(history), "parameters": parameters.tolist(),
                  "average_lap": float(rows[:, 1].mean()), "complete": complete,
                  "safe": bool(safe), "off_road_fraction": float(rows[:, 2].mean()),
                  "accepted": score < best_score,
                  "rows": rows.tolist(), "wall_seconds": time.monotonic() - started}
        if guard_rows is not None:
            record["guard_rows"] = guard_rows.tolist()
        if tuning_guards:
            record["tuning_guard_rows"] = tuning_guards
        history.append(record)
        if score < best_score:
            best_score = score
            best_parameters = parameters.copy()
            best_policy = copy.deepcopy(candidate)
            archive = args.output_dir / "accepted"
            archive.mkdir(exist_ok=True)
            (archive / f"policy-{record['evaluation']:06d}.json").write_text(json.dumps(candidate) + "\n")
            (args.output_dir / "rl_enemy_policy.json").write_text(json.dumps(candidate) + "\n")
            (args.output_dir / "best.json").write_text(json.dumps(record, indent=2) + "\n")
        (args.output_dir / "history.json").write_text(json.dumps(history, indent=2) + "\n")
        print(json.dumps({k: v for k, v in record.items() if not k.endswith("rows")}), flush=True)
        return score

    evaluate(best_parameters)
    if not args.evaluate_only:
        for index, path in enumerate(args.blend_with):
            other = json.loads(path.read_text())
            for fraction in (float(value) for value in args.blend_fractions.split(",")):
                evaluate(np.array([index, fraction]), blended_policy(source, other, fraction))
        if args.blend_with:
            source = copy.deepcopy(best_policy)
            best_parameters = np.zeros(4)
        steps = np.array([0.08, 0.08, 0.05, 0.025])
        for _ in range(args.rounds):
            before = best_score
            for dimension in range(4):
                center = best_parameters.copy()
                for direction in (-1, 1):
                    parameters = center.copy()
                    parameters[dimension] += direction * steps[dimension]
                    evaluate(parameters)
            if best_score >= before - 1e-8:
                steps *= 0.5
        rng = np.random.default_rng(args.seed)
        hidden_units = [(layer_index, unit, mode)
                        for layer_index, layer in enumerate(source["layers"][:-1])
                        for unit in range(layer["outputSize"]) for mode in (0, 1)]
        for round_index in range(args.hidden_rounds):
            step = args.hidden_step * (0.5 ** round_index)
            for index in rng.permutation(len(hidden_units)):
                layer_index, unit, mode = hidden_units[index]
                center = copy.deepcopy(best_policy)
                for direction in (-1, 1):
                    candidate = adjusted_hidden_unit(center, layer_index, unit,
                        log_gain=direction * step if mode == 0 else 0.0,
                        offset=direction * step if mode == 1 else 0.0)
                    evaluate(np.array([round_index, layer_index, unit, mode, direction, step]), candidate)
        for round_index in range(args.input_rounds):
            step = 0.08 * (0.5 ** round_index)
            for index in rng.permutation(source["observationSize"]):
                center = copy.deepcopy(best_policy)
                for direction in (-1, 1):
                    candidate = copy.deepcopy(center)
                    layer = candidate["layers"][0]
                    weights = np.asarray(layer["weights"]).reshape(
                        layer["outputSize"], layer["inputSize"])
                    weights[:, index] *= np.exp(direction * step)
                    layer["weights"] = weights.flatten().tolist()
                    evaluate(np.array([round_index, int(index), direction, step]), candidate)
        width = best_policy["layers"][-1]["inputSize"]
        for round_index in range(args.weight_rounds):
            step = args.weight_step * (0.5 ** round_index)
            for index in rng.permutation(2 * width):
                center = copy.deepcopy(best_policy)
                for direction in (-1, 1):
                    candidate = copy.deepcopy(center)
                    candidate["layers"][-1]["weights"][int(index)] += direction * step
                    evaluate(np.array([round_index, int(index), direction, step]), candidate)
    print(f"best_average_lap={best_score:.6f} parameters={best_parameters.tolist()}", flush=True)
    jpype.shutdownJVM()


if __name__ == "__main__":
    main()
