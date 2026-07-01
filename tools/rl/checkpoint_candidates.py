#!/usr/bin/env python3

"""Select a checkpoint candidate from recent RL training iterations."""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Sequence


CHECKPOINT_CANDIDATE_WINDOW = 10


@dataclass(frozen=True)
class CheckpointCandidate:
    iteration: int
    reward_mean: float
    episode_len_mean: float
    episodes: float
    checkpoint_path: str

    def has_reward_mean(self) -> bool:
        return self.episodes > 0 and math.isfinite(self.reward_mean)


@dataclass(frozen=True)
class CheckpointCandidateSelection:
    candidate: CheckpointCandidate
    eligible_count: int
    reason: str


def should_capture_checkpoint_candidate(
    iteration: int,
    total_iterations: int,
    checkpoint_every: int,
    window_size: int = CHECKPOINT_CANDIDATE_WINDOW,
) -> bool:
    """Return whether this iteration belongs to the next checkpoint window."""
    if iteration <= 0 or total_iterations <= 0 or window_size <= 0:
        return False

    if checkpoint_every > 0:
        next_checkpoint = (
            (iteration + checkpoint_every - 1) // checkpoint_every
        ) * checkpoint_every
        next_checkpoint = min(next_checkpoint, total_iterations)
    else:
        next_checkpoint = total_iterations
    return 0 <= next_checkpoint - iteration < window_size


def select_checkpoint_candidate(
    candidates: Sequence[CheckpointCandidate],
) -> CheckpointCandidateSelection:
    """Select the recent iteration with the highest mean episode reward."""
    if not candidates:
        raise ValueError("At least one checkpoint candidate is required")

    eligible = [candidate for candidate in candidates if candidate.has_reward_mean()]
    if not eligible:
        return CheckpointCandidateSelection(
            candidate=max(candidates, key=lambda candidate: candidate.iteration),
            eligible_count=0,
            reason="no_completed_episodes",
        )

    selected = max(
        eligible,
        key=lambda candidate: (candidate.reward_mean, candidate.iteration),
    )
    return CheckpointCandidateSelection(
        candidate=selected,
        eligible_count=len(eligible),
        reason="highest_reward_mean",
    )
