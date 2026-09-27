#!/usr/bin/env python3
"""Restore the Gradle wrapper from the pinned official NeoForge 1.21.1 MDK."""
from __future__ import annotations
from pathlib import Path
import hashlib, os, shutil, tempfile, urllib.request, zipfile

ROOT = Path(__file__).resolve().parent
MDK_COMMIT = "30cafee9cd8d7f46427ec88fa8579d49c146df9a"
MDK_URL = f"https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/archive/{MDK_COMMIT}.zip"
WRAPPER_SHA256 = "7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172"
GRADLE_URL_LINE = r"distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip"
GRADLE_SHA256 = "72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f"

def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def main() -> None:
    print(f"Downloading official NeoForge 1.21.1 ModDevGradle MDK at {MDK_COMMIT}...")
    with tempfile.TemporaryDirectory(prefix="mystcraft-neoforge-mdk-") as td:
        td = Path(td)
        archive = td / "mdk.zip"
        urllib.request.urlretrieve(MDK_URL, archive)
        extract = td / "extract"
        with zipfile.ZipFile(archive) as z:
            z.extractall(extract)
        dirs = [p for p in extract.iterdir() if p.is_dir()]
        if len(dirs) != 1:
            raise RuntimeError("Could not uniquely locate the extracted NeoForge MDK directory.")
        mdk = dirs[0]
        wrapper_jar = mdk / "gradle/wrapper/gradle-wrapper.jar"
        wrapper_props = mdk / "gradle/wrapper/gradle-wrapper.properties"
        for req in (mdk/"gradlew", mdk/"gradlew.bat", wrapper_jar, wrapper_props):
            if not req.is_file():
                raise RuntimeError(f"Pinned NeoForge MDK is missing expected file: {req.relative_to(mdk)}")
        actual = sha256(wrapper_jar)
        if actual != WRAPPER_SHA256:
            raise RuntimeError(f"Unexpected Gradle wrapper JAR SHA-256: {actual}")
        props = wrapper_props.read_text(encoding="utf-8")
        if GRADLE_URL_LINE not in props:
            raise RuntimeError("Pinned NeoForge MDK no longer supplies the expected Gradle 9.2.1 distribution URL.")
        sha_line = f"distributionSha256Sum={GRADLE_SHA256}"
        if re_search := __import__('re').search(r"(?m)^distributionSha256Sum=.*$", props):
            props = props[:re_search.start()] + sha_line + props[re_search.end():]
        else:
            props = props.replace(GRADLE_URL_LINE, GRADLE_URL_LINE + "\n" + sha_line)
        (ROOT/"gradle/wrapper").mkdir(parents=True, exist_ok=True)
        shutil.copy2(mdk/"gradlew", ROOT/"gradlew")
        shutil.copy2(mdk/"gradlew.bat", ROOT/"gradlew.bat")
        shutil.copy2(wrapper_jar, ROOT/"gradle/wrapper/gradle-wrapper.jar")
        (ROOT/"gradle/wrapper/gradle-wrapper.properties").write_text(props, encoding="utf-8", newline="\n")
        if os.name != "nt":
            (ROOT/"gradlew").chmod((ROOT/"gradlew").stat().st_mode | 0o111)
    print("Gradle wrapper installed from the pinned official NeoForge MDK.")

if __name__ == "__main__":
    main()
