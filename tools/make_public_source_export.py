#!/usr/bin/env python3
"""Create the source-only public export without internal checkpoint/developer artifacts."""
from pathlib import Path
import re, sys, zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT.parent / "Mystcraft_Legacy_public_source.zip"
EXCLUDE_DIRS = {".gradle", "build", "run", "runs", "logs", "crash-reports", ".idea", ".vscode", "__pycache__"}
EXCLUDE_PATH_PREFIXES = {("docs",), ("tools", "harness")}
EXCLUDE_ROOT_DOC_PREFIXES = (
    "CHECKPOINT_", "PRE_AUDIT_", "STAGE1", "MYSTCRAFT_HANDOFF_",
    "I83_", "I89_", "I90_", "I92_", "I95_", "I96_", "I98_", "I99_", "I100_", "I104_", "I108_",
)
EXCLUDE_ROOT_FILES = {
    "STATIC_PROGRESS_JOURNAL.md", "STATIC_RUNTIME_BOUNDARY.md", "PORTING_STATUS.md",
    "ORIGINAL_SOURCE_AUDIT_MATRIX_I84.md", "LEGACY_SOURCE_NOTES.md",
    "RUNTIME_TEST_PLAN_CP208.md", "EMBEDDED_GRADLE_README.txt",
    "MYSTCRAFT_CP227_DEV_VISUAL_COLOR_SPLIT_JA.md",
}
EXCLUDE_TOOL_FILES = {
    "prebuild_static_audit.py", "cp226_log_audit.py", "FindSmallLibrary.java",
    "LegacyPopulationH18Harness.java", "CP223RegressionPlanHarness.java", "DenseOresPolicyHarness.java",
}

def developer_only_tool(rel: Path) -> bool:
    if len(rel.parts) != 2 or rel.parts[0] != "tools":
        return False
    name = rel.name
    return (
        name in EXCLUDE_TOOL_FILES
        or (name.startswith("audit_cp") and name.endswith(".py"))
        or bool(re.fullmatch(r"InstabilityCp\d+.*Harness\.java", name))
    )

def allowed(rel: Path) -> bool:
    if developer_only_tool(rel): return False
    if any(p in EXCLUDE_DIRS for p in rel.parts): return False
    if any(tuple(rel.parts[:len(prefix)]) == prefix for prefix in EXCLUDE_PATH_PREFIXES): return False
    if len(rel.parts) >= 2 and rel.parts[0] == "tools" and rel.parts[1].startswith("gradle-"): return False
    if rel.suffix.lower() in {".class", ".log", ".zip", ".patch"}: return False
    if rel.suffix.lower() == ".jar" and rel.as_posix() != "gradle/wrapper/gradle-wrapper.jar": return False
    if len(rel.parts) == 1:
        if rel.name in EXCLUDE_ROOT_FILES or rel.name.startswith(EXCLUDE_ROOT_DOC_PREFIXES): return False
        if re.match(r"^CP\d", rel.name, re.I): return False
    return True

files = [p for p in ROOT.rglob("*") if p.is_file() and allowed(p.relative_to(ROOT))]
OUT.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(OUT, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted(files):
        z.write(p, Path("Mystcraft-Legacy-1.21.1") / p.relative_to(ROOT))
print(f"wrote {OUT} ({len(files)} files)")
