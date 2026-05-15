#!/usr/bin/env python3
"""Export a Ray RLlib PPO checkpoint into the small JSON policy used by the game."""

from __future__ import annotations

import argparse
import json
import os
import pickle
from pathlib import Path
from typing import Dict, Iterable

import numpy as np


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CHECKPOINT = REPO_ROOT / "rl-checkpoints-target-circle-cars-768-v1"
DEFAULT_OUTPUT = REPO_ROOT / "assets" / "ai" / "rl_enemy_policy.json"
OBSERVATION_SIZE = 44
ACTION_SIZE = 2

ACTOR_LAYERS = (
    (("encoder.encoder.net.mlp.0", "encoder.actor_encoder.net.mlp.0"), "tanh"),
    (("encoder.encoder.net.mlp.2", "encoder.actor_encoder.net.mlp.2"), "tanh"),
    (("pi.net.mlp.0",), "linear"),
)


def flatten(values: np.ndarray) -> Iterable[float]:
    return (round(float(value), 6) for value in values.reshape(-1))


def resolve_layer_prefix(state: Dict[str, np.ndarray], prefixes: Iterable[str]) -> str:
    for prefix in prefixes:
        if f"{prefix}.weight" in state and f"{prefix}.bias" in state:
            return prefix
    expected = ", ".join(prefixes)
    raise KeyError(f"None of the expected layer prefixes were found: {expected}")


def layer_from_state(state: Dict[str, np.ndarray], prefixes: Iterable[str], activation: str) -> Dict:
    prefix = resolve_layer_prefix(state, prefixes)
    weights = state[f"{prefix}.weight"]
    bias = state[f"{prefix}.bias"]
    if len(weights.shape) != 2:
        raise ValueError(f"{prefix}.weight is not a matrix: {weights.shape}")
    if prefix in ACTOR_LAYERS[0][0] and weights.shape[1] != OBSERVATION_SIZE:
        raise ValueError(
            f"unsupported actor input size {weights.shape[1]}; expected {OBSERVATION_SIZE}"
        )

    output_size, input_size = weights.shape
    return {
        "inputSize": int(input_size),
        "outputSize": int(output_size),
        "activation": activation,
        "weights": list(flatten(weights)),
        "bias": list(flatten(bias)),
    }


def load_policy_state(checkpoint_dir: Path) -> Dict[str, np.ndarray]:
    state_file = (
        checkpoint_dir
        / "learner_group"
        / "learner"
        / "rl_module"
        / "shared_policy"
        / "module_state.pkl"
    )
    if not state_file.exists():
        raise FileNotFoundError(
            f"{state_file} does not exist. Run tools/rl/train_rllib.py first."
        )

    with state_file.open("rb") as handle:
        return pickle.load(handle)


def export_policy(checkpoint_dir: Path, output_file: Path, objective: str) -> None:
    state = load_policy_state(checkpoint_dir)
    layers = [
        layer_from_state(state, prefix, activation)
        for prefix, activation in ACTOR_LAYERS
    ]

    output_file.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "format": "ratass-rl-policy-v3",
        "source": "ray-rllib-ppo",
        "objective": objective,
        "observationSize": OBSERVATION_SIZE,
        "actionSize": ACTION_SIZE,
        "output": "first actionSize outputs are deterministic direct throttle and turn controls",
        "layers": layers,
    }
    with output_file.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, separators=(",", ":"))
        handle.write("\n")

    print(f"exported={output_file}")
    print(f"layers={len(layers)}")
    print(f"bytes={output_file.stat().st_size}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint-dir", default=os.fspath(DEFAULT_CHECKPOINT))
    parser.add_argument("--output", default=os.fspath(DEFAULT_OUTPUT))
    parser.add_argument("--objective", default="target-circle-v1")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    export_policy(Path(args.checkpoint_dir), Path(args.output), args.objective)


if __name__ == "__main__":
    main()
