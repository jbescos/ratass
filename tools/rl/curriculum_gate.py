"""Only advance a curriculum from an evaluated, archived successful policy."""

import argparse
import json
import math
from pathlib import Path


def stage_passed(state_path: Path, minimum_targets: float) -> bool:
    try:
        state = json.loads(state_path.read_text(encoding="utf-8"))
        metrics = state["metrics"]
        success = float(metrics["success_rate"])
        targets = float(metrics["avg_targets"])
        return (
            math.isfinite(success) and success >= 1.0
            and math.isfinite(targets) and targets >= minimum_targets
            and Path(state["archived_policy"]).is_file()
            and (Path(state["best_rllib_checkpoint"]) / "rllib_checkpoint.json").is_file()
        )
    except (OSError, ValueError, TypeError, KeyError):
        return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("state", type=Path)
    parser.add_argument("minimum_targets", type=float)
    args = parser.parse_args()
    passed = stage_passed(args.state, args.minimum_targets)
    print(f"curriculum_gate passed={int(passed)} state={args.state}", flush=True)
    raise SystemExit(0 if passed else 1)


if __name__ == "__main__":
    main()
