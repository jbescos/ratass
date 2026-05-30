#!/usr/bin/env python3
"""Load synthetic training maps once so their .ser caches are generated."""

from __future__ import annotations

import argparse
import os
from pathlib import Path

import jpype


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"


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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    start_jvm(Path(args.jar).resolve())
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createHeadlessTrainingSet()
    ids = [str(maps.get(index).getId()) for index in range(int(maps.size))]
    print(f"training_map_cache_ready count={len(ids)} maps={','.join(ids)}")


if __name__ == "__main__":
    main()
