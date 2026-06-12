#!/usr/bin/env python3
"""Regenerate map .json.gz metadata caches from the current mask PNG files."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

import jpype


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = Path("desktop") / "target" / "ratass-desktop-1.0.jar"
DEFAULT_GAME_MAP_DIR = Path("assets") / "maps"
DEFAULT_TRAINING_MAP_DIR = Path("tools") / "rl" / "trainingMaps"


def start_jvm(jar_path: Path) -> None:
    if jpype.isJVMStarted():
        return
    if not jar_path.exists():
        raise FileNotFoundError(f"{jar_path} does not exist. Run `mvn -pl desktop -am package` first.")
    jpype.startJVM(jpype.getDefaultJVMPath(), "-Xrs", classpath=[str(jar_path)])
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


def remove_cache_files(map_dir: Path) -> int:
    removed = 0
    for pattern in ("*.json.gz", "*.ser"):
        for path in sorted(map_dir.glob(pattern)):
            path.unlink()
            removed += 1
    return removed


def load_maps_from_directory(map_dir: Path):
    loader = jpype.JClass("com.github.jbescos.gameplay.maps.ImageArenaMapLoader")
    string_type = jpype.JClass("java.lang.String")
    float_type = jpype.JClass("java.lang.Float").TYPE
    boolean_type = jpype.JClass("java.lang.Boolean").TYPE
    method = loader.class_.getDeclaredMethod(
        "loadMapsFromDirectory",
        string_type.class_,
        float_type,
        boolean_type,
    )
    method.setAccessible(True)
    return method.invoke(None, repo_relative(map_dir), jpype.JFloat(8.0), True)


def repo_relative(path: Path) -> str:
    resolved = path.resolve()
    try:
        return os.fspath(resolved.relative_to(REPO_ROOT))
    except ValueError:
        return os.fspath(resolved)


def project_path(path: Path) -> Path:
    return path if path.is_absolute() else REPO_ROOT / path


def map_ids(maps) -> list[str]:
    return [str(maps.get(index).getId()) for index in range(int(maps.size))]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--game-map-dir", default=os.fspath(DEFAULT_GAME_MAP_DIR))
    parser.add_argument("--training-map-dir", default=os.fspath(DEFAULT_TRAINING_MAP_DIR))
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    jar_path = project_path(Path(args.jar)).resolve()
    game_map_dir = project_path(Path(args.game_map_dir)).resolve()
    training_map_dir = project_path(Path(args.training_map_dir)).resolve()

    removed_game = remove_cache_files(game_map_dir)
    removed_training = remove_cache_files(training_map_dir)
    print(
        "map_cache_regenerate_removed"
        f" game_dir={repo_relative(game_map_dir)} game_cache={removed_game}"
        f" training_dir={repo_relative(training_map_dir)} training_cache={removed_training}"
    )

    start_jvm(jar_path)
    game_maps = load_maps_from_directory(game_map_dir)
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    training_maps = arena_maps.createHeadlessTrainingSet()
    print(
        "map_cache_regenerate_ready"
        f" game_count={int(game_maps.size)} game_maps={','.join(map_ids(game_maps))}"
        f" training_count={int(training_maps.size)} training_maps={','.join(map_ids(training_maps))}"
    )


if __name__ == "__main__":
    main()
