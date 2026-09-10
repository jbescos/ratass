#!/usr/bin/env python3
"""Distill per-map expert driving into one exported driver, retaining rollout-validated candidates."""

import argparse
import copy
import json
from pathlib import Path
from types import SimpleNamespace

import jpype
import numpy as np
import torch
from torch import nn

from evaluate_lap_times import DEFAULT_PROFILE_ROOT, DEFAULT_POLICY_ROOT, profile_timing_args
from refine_driver_policy import passes_regression_limits, start_benchmark


class ExportedDriver(nn.Module):
    def __init__(self, payload):
        super().__init__()
        self.source = copy.deepcopy(payload)
        self.layers = nn.ModuleList()
        for layer in payload["layers"]:
            if layer["activation"] not in ("linear", "tanh", "relu"):
                raise ValueError("Unsupported policy activation")
            linear = nn.Linear(layer["inputSize"], layer["outputSize"])
            with torch.no_grad():
                linear.weight.copy_(torch.tensor(layer["weights"]).reshape_as(linear.weight))
                linear.bias.copy_(torch.tensor(layer["bias"]))
            self.layers.append(linear)

    def forward(self, values):
        for linear, definition in zip(self.layers, self.source["layers"]):
            values = linear(values)
            if definition["activation"] == "tanh":
                values = torch.tanh(values)
            elif definition["activation"] == "relu":
                values = torch.relu(values)
        return values[:, :self.source["actionSize"]].clamp(-1, 1)

    def payload(self):
        result = copy.deepcopy(self.source)
        for layer, linear in zip(result["layers"], self.layers):
            layer["weights"] = linear.weight.detach().cpu().flatten().tolist()
            layer["bias"] = linear.bias.detach().cpu().tolist()
        return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--teachers", default="profile07,profile08,profile09")
    parser.add_argument("--repeat", type=int, default=4)
    parser.add_argument("--epochs", type=int, default=60)
    parser.add_argument("--lr", type=float, default=3e-6)
    parser.add_argument("--seed", type=int, default=20260910)
    args = parser.parse_args()
    if args.repeat < 1 or args.epochs < 1 or args.lr <= 0:
        parser.error("repeat, epochs and learning rate must be positive")
    torch.set_num_threads(2)
    torch.manual_seed(args.seed)
    rng = np.random.default_rng(args.seed)
    benchmark = start_benchmark()
    source = json.loads(args.policy.read_text())
    if source["actionSize"] != 2:
        raise ValueError("Expected a driving policy")
    args.output_dir.mkdir(parents=True, exist_ok=True)
    archive = args.output_dir / "accepted"
    archive.mkdir(exist_ok=True)

    def evaluate(payload, laps=3, card="", multiplier=1.0, repeat=None):
        return np.asarray(benchmark.evaluate(json.dumps(payload), repeat or args.repeat,
            laps, args.seed, False, -1, card, multiplier))

    baseline = evaluate(source)
    if not np.all(baseline[:, 0] == 1):
        raise ValueError("Student baseline must finish all maps")
    experts = [("student", source, args.repeat, baseline)]
    for profile in filter(None, (v.strip() for v in args.teachers.split(","))):
        payload = json.loads((DEFAULT_POLICY_ROOT / profile / "rl_enemy_policy.json").read_text())
        if (payload["observationSize"], payload["actionSize"]) != (source["observationSize"], 2):
            raise ValueError(f"Incompatible teacher {profile}")
        repeat = profile_timing_args(SimpleNamespace(action_repeat=0, driver_action_repeat=0),
                                     DEFAULT_PROFILE_ROOT, profile).action_repeat
        experts.append((profile, payload, repeat, evaluate(payload, repeat=repeat)))
    choices, examples = [], []
    for map_index in range(len(baseline)):
        eligible = [expert for expert in experts if expert[3][map_index, 0] == 1]
        name, payload, repeat, scores = min(eligible, key=lambda e: e[3][map_index, 1])
        data = np.asarray(benchmark.demonstrations(json.dumps(payload), repeat, args.repeat,
                                                   3, args.seed, map_index), dtype=np.float32)
        examples.append(data[rng.choice(len(data), min(2000, len(data)), replace=False)])
        choices.append({"map": f"map{map_index:03d}", "teacher": name,
                        "baseline_lap": float(baseline[map_index, 1]),
                        "teacher_lap": float(scores[map_index, 1])})
        print(json.dumps(choices[-1]), flush=True)
    (args.output_dir / "teachers.json").write_text(json.dumps(choices, indent=2) + "\n")
    data = np.concatenate(examples)
    inputs = torch.from_numpy(data[:, :source["observationSize"]].copy())
    targets = torch.from_numpy(data[:, source["observationSize"]:].copy())
    model = ExportedDriver(source)
    with torch.no_grad():
        anchor = model(inputs).clone()
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr)
    guards = [(5, "", 1.0)] + [(5, card, 1.0) for card in (
        "AGILE_CHASSIS", "CARBON_MONOCOQUE", "CARBON_PROTOTYPE", "HYPERCAR_CORE",
        "CHAMPIONSHIP_TUNE", "GROUND_EFFECT")]
    guards += [(5, card, 3.0) for card in ("CARBON_PROTOTYPE", "HYPERCAR_CORE", "GROUND_EFFECT")]
    references = [evaluate(source, *guard) for guard in guards]
    best_average = float(baseline[:, 1].mean())
    (args.output_dir / "rl_enemy_policy.json").write_text(json.dumps(source) + "\n")
    history = []
    for epoch in range(1, args.epochs + 1):
        losses = []
        for indices in torch.randperm(len(inputs)).split(1024):
            prediction = model(inputs[indices])
            loss = (prediction - targets[indices]).square().mean()
            loss = loss + 0.1 * (prediction - anchor[indices]).square().mean()
            optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            losses.append(float(loss.detach()))
        if epoch % 2 and epoch != args.epochs:
            continue
        payload = model.payload()
        rows = evaluate(payload)
        average = float(rows[:, 1].mean())
        accepted = average < best_average and passes_regression_limits(rows, baseline)
        checked_guards = {}
        if accepted:
            for guard, reference in zip(guards, references):
                result = evaluate(payload, *guard)
                checked_guards[str(guard)] = result.tolist()
                if not passes_regression_limits(result, reference, 0.05, allow_existing_failures=True):
                    accepted = False
                    break
        record = {"epoch": epoch, "loss": float(np.mean(losses)), "average_lap": average,
                  "completed": int(rows[:, 0].sum()), "accepted": bool(accepted),
                  "rows": rows.tolist(), "guards": checked_guards}
        history.append(record)
        if accepted:
            best_average = average
            (archive / f"epoch-{epoch:04d}.json").write_text(json.dumps(payload) + "\n")
            (args.output_dir / "rl_enemy_policy.json").write_text(json.dumps(payload) + "\n")
        (args.output_dir / "history.json").write_text(json.dumps(history, indent=2) + "\n")
        print(json.dumps({k: v for k, v in record.items() if k not in ("rows", "guards")}), flush=True)
    print(f"best_average_lap={best_average:.6f}", flush=True)
    jpype.shutdownJVM()


if __name__ == "__main__":
    main()
