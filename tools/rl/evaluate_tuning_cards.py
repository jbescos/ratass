#!/usr/bin/env python3
"""Benchmark every tuning card with one fixed exported driver policy."""

from __future__ import annotations

import argparse
from pathlib import Path
from types import SimpleNamespace

import evaluate_lap_times as lap_eval


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", default="profile04")
    parser.add_argument("--jar", default=str(lap_eval.DEFAULT_JAR))
    parser.add_argument("--policy-root", default=str(lap_eval.DEFAULT_POLICY_ROOT))
    parser.add_argument("--map-source", choices=("game", "training", "all"), default="game")
    parser.add_argument("--map-ids", default="")
    parser.add_argument("--laps", type=int, default=3)
    parser.add_argument("--steps", type=int, default=0)
    parser.add_argument("--action-repeat", type=int, default=4)
    parser.add_argument("--timeout-seconds", type=float, default=10.0)
    parser.add_argument("--seed", type=int, default=20260531)
    parser.add_argument(
        "--tier",
        type=int,
        choices=(1, 2, 3),
        help="benchmark one tier; default benchmarks all tiers",
    )
    parser.add_argument(
        "--card-id",
        default="",
        help="benchmark one tuning-card enum ID; default benchmarks the selected tier",
    )
    parser.add_argument("--exclude-baseline", action="store_true")
    parser.add_argument(
        "--tuning-effect-multiplier",
        type=float,
        default=1.0,
        help="headless-only multiplier applied to each Tuning deviation",
    )
    return parser.parse_args()


def tuning_cards(
    selected_tier: int | None,
    selected_card_id: str,
) -> list[tuple[str, str, int]]:
    catalog = lap_eval.jpype.JClass(
        "com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog"
    )
    cards: list[tuple[str, str, int]] = []
    for card in catalog.all():
        if str(card.getSlotType().name()) != "TUNING":
            continue
        tier = int(card.getTier())
        card_id = str(card.getId().name())
        if selected_card_id and card_id != selected_card_id:
            continue
        if selected_tier is None or tier == selected_tier:
            cards.append((card_id, str(card.getTitle()), tier))
    return cards


def main() -> None:
    args = parse_args()
    if args.laps <= 0:
        raise ValueError("--laps must be positive")

    lap_eval.start_jvm(Path(args.jar).resolve())
    policy_path = Path(args.policy_root) / args.profile / lap_eval.POLICY_FILE_NAME
    policy = lap_eval.load_policy(policy_path)
    maps = lap_eval.selected_maps(args.map_source, args.map_ids)
    selected_card_id = args.card_id.strip().upper()
    cards = tuning_cards(args.tier, selected_card_id)
    if selected_card_id and not cards:
        raise ValueError(f"Unknown tuning card for selected tier: {selected_card_id}")
    choices = [] if args.exclude_baseline else [("", "No tuning", 0)]
    choices.extend(cards)
    rows: list[lap_eval.TimedRun] = []

    run_args = SimpleNamespace(
        laps=args.laps,
        timeout_seconds=args.timeout_seconds,
        steps=args.steps,
        action_repeat=args.action_repeat,
        random_race_spawns=False,
        seed=args.seed,
        write_driver_metadata=False,
        tuning_card="",
        tuning_effect_multiplier=args.tuning_effect_multiplier,
    )
    for card_id, title, tier in choices:
        run_args.tuning_card = card_id
        label = f"T{tier} {title}" if tier > 0 else title
        for arena_map in maps:
            rows.append(
                lap_eval.run_lap_timing(
                    run_args,
                    arena_map,
                    label,
                    "default",
                    None,
                    policy,
                )
            )

    print(
        f"tuning_card_benchmark profile={args.profile} maps={len(maps)} "
        f"laps={args.laps} seed={args.seed} "
        f"effect_multiplier={args.tuning_effect_multiplier:g}"
    )
    lap_eval.print_overall_profile_averages(rows)


if __name__ == "__main__":
    main()
