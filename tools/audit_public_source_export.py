#!/usr/bin/env python3
"""Audit the Mystcraft public source ZIP for release hygiene and reproducibility."""
from pathlib import Path
import csv, hashlib, io, json, re, sys, zipfile

p=Path(sys.argv[1]) if len(sys.argv)>1 else None
if not p or not p.is_file(): raise SystemExit("usage: audit_public_source_export.py <zip>")
ROOT="Mystcraft-Legacy-1.21.1/"
VERSION="0.13.7.06-port.1.0.0-Ueno"
MC="NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT."
CYAN="This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan."
GRADLE_SHA="72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f"
WRAPPER_SHA="7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172"
required_rel={
"LICENSE","README.md","ATTRIBUTION.md","MODIFICATION_NOTICE.md","SOURCE_PROVENANCE.md","ASSET_PROVENANCE_SHA256.tsv",
"COMPLIANCE_AND_LICENSE_NOTICE.md","COMPLIANCE_AUDIT_REPORT.md","COMPLIANCE_SUMMARY_JA.md","THIRD_PARTY_NOTICES.md",
"RELEASE_DISTRIBUTION_CHECKLIST.md","RELEASE_LISTING_TEMPLATE.md","KNOWN_ISSUES.md","CHANGELOG.md","PUBLIC_RELEASE_EXCLUDES.md",
"LICENSES/LGPL-3.0.txt","LICENSES/GPL-3.0.txt","LICENSES/Apache-2.0.txt","LICENSES/LGPL-2.1.txt","LICENSES/CC0-1.0.txt","LICENSES/README.md",
"build.gradle","gradle.properties","settings.gradle","gradlew","gradlew.bat","bootstrap_wrapper.ps1","bootstrap_wrapper.py","BUILD.bat","VERIFY_ENVIRONMENT.bat",
"tools/make_public_source_export.py","tools/audit_public_source_export.py","tools/audit_release_jar.py",
"gradle/wrapper/gradle-wrapper.properties","src/main/templates/META-INF/neoforge.mods.toml",
}
release_docs={"README.md","MODIFICATION_NOTICE.md","SOURCE_PROVENANCE.md","COMPLIANCE_AND_LICENSE_NOTICE.md","COMPLIANCE_AUDIT_REPORT.md","COMPLIANCE_SUMMARY_JA.md","THIRD_PARTY_NOTICES.md","RELEASE_DISTRIBUTION_CHECKLIST.md","RELEASE_LISTING_TEMPLATE.md","KNOWN_ISSUES.md","CHANGELOG.md","PUBLIC_RELEASE_EXCLUDES.md"}
secret_patterns=[re.compile(r"C:\\Users\\",re.I),re.compile(r"/Users/"),re.compile(r"/home/"),re.compile(r"/mnt/" + r"data"),re.compile(r"oai_" + r"shared",re.I),re.compile(r"AppData\\",re.I),re.compile(r"\.gradle[/\\]caches",re.I),re.compile(r"AKIA[0-9A-Z]{16}"),re.compile(r"gh[pousr]_[A-Za-z0-9_]{20,}"),re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")]
problems=[]
with zipfile.ZipFile(p) as z:
    names=z.namelist(); ns=set(names)
    missing=sorted(ROOT+x for x in required_rel if ROOT+x not in ns)
    if missing: problems += ["missing required: "+x for x in missing]
    # Zip path safety/collisions.
    seen_case={}
    for info in z.infolist():
        n=info.filename
        if not n.startswith(ROOT): problems.append(f"entry outside root: {n}"); continue
        rel=n[len(ROOT):]
        parts=Path(rel).parts
        if rel.startswith("/") or ".." in parts: problems.append(f"unsafe path: {n}")
        key=rel.casefold()
        if key in seen_case and seen_case[key]!=rel: problems.append(f"case-collision: {seen_case[key]} / {rel}")
        seen_case[key]=rel
        mode=(info.external_attr>>16)&0o170000
        if mode==0o120000: problems.append(f"symlink in public source: {n}")
        low=rel.lower()
        top="/" not in rel.strip("/")
        if rel.startswith("docs/"): problems.append(f"internal docs leaked: {rel}")
        if top and re.match(r"^CP\d",rel,re.I): problems.append(f"checkpoint file leaked: {rel}")
        if rel.startswith("tools/harness/") or re.fullmatch(r"tools/InstabilityCp\d+.*Harness\.java",rel): problems.append(f"development harness leaked: {rel}")
        if any(x in parts for x in (".gradle","build","run","runs","logs","crash-reports",".idea",".vscode","__pycache__")): problems.append(f"build/private artifact: {rel}")
        if low.endswith((".class",".log",".zip",".patch")): problems.append(f"forbidden artifact: {rel}")
        if low.endswith(".jar") and rel!="gradle/wrapper/gradle-wrapper.jar": problems.append(f"unexpected JAR: {rel}")
        if rel.startswith("src/main/java/net/minecraft/") or rel.startswith("src/main/java/com/mojang/"): problems.append(f"Minecraft/Mojang source namespace: {rel}")
        if rel.startswith("src/main/resources/assets/minecraft/") or rel.startswith("src/main/resources/data/minecraft/"): problems.append(f"minecraft resource namespace: {rel}")
        if low.endswith((".json",".mcmeta")):
            try: json.loads(z.read(n).decode("utf-8-sig"))
            except Exception as e: problems.append(f"invalid JSON/mcmeta {rel}: {e}")
        if low.endswith((".java",".kt",".groovy",".gradle",".properties",".toml",".json",".md",".txt",".bat",".ps1",".py",".yml",".yaml",".mcmeta")) and rel!="tools/audit_public_source_export.py":
            try: txt=z.read(n).decode("utf-8-sig")
            except Exception: txt=""
            for pat in secret_patterns:
                if pat.search(txt): problems.append(f"private/secret pattern in {rel}: {pat.pattern}")
    def text(rel):
        try:return z.read(ROOT+rel).decode("utf-8-sig")
        except Exception:return ""
    # Release identity/docs.
    props=text("gradle.properties"); toml=text("src/main/templates/META-INF/neoforge.mods.toml")
    if f"mod_version={VERSION}" not in props: problems.append("gradle.properties release version mismatch")
    for rel in ["README.md","RELEASE_LISTING_TEMPLATE.md","COMPLIANCE_AND_LICENSE_NOTICE.md"]:
        txt=text(rel)
        for token in [MC,CYAN,"ueno969","ueno96941@gmail.com"]:
            if token not in txt: problems.append(f"{rel} missing release identity/disclaimer token: {token}")
    for token in ["ueno969","ueno96941@gmail.com",MC,CYAN]:
        if token not in toml: problems.append(f"neoforge.mods.toml missing {token}")
    for rel in release_docs:
        txt=text(rel)
        if re.search(r"\bCP\d{2,4}[A-Z]?\b",txt): problems.append(f"internal checkpoint label leaked into public release document: {rel}")
        if ("0.13.7.06-port." + "1c12") in txt: problems.append(f"stale pre-release version in public document: {rel}")
    # Wrapper/bootstrap integrity declarations.
    wrapper=text("gradle/wrapper/gradle-wrapper.properties")
    if "gradle-9.2.1-bin.zip" not in wrapper or f"distributionSha256Sum={GRADLE_SHA}" not in wrapper: problems.append("wrapper properties pin/checksum mismatch")
    for rel in ["bootstrap_wrapper.ps1","bootstrap_wrapper.py"]:
        txt=text(rel)
        if WRAPPER_SHA not in txt or GRADLE_SHA not in txt or "30cafee9cd8d7f46427ec88fa8579d49c146df9a" not in txt: problems.append(f"{rel} missing pinned bootstrap identity/checksums")
    if "prebuild_static_audit.py" in text("VERIFY_ENVIRONMENT.bat"): problems.append("VERIFY_ENVIRONMENT.bat still references deleted prebuild audit")
    if " clean build --stacktrace" not in text("BUILD.bat"): problems.append("BUILD.bat is not a clean release build")
    # Asset provenance exactness.
    prefix=ROOT+"src/main/resources/assets/mystcraft/"
    assets=sorted(n for n in names if n.startswith(prefix) and n.lower().endswith((".png",".ogg")))
    try: rows=list(csv.DictReader(io.StringIO(text("ASSET_PROVENANCE_SHA256.tsv")),delimiter="\t"))
    except Exception as e: rows=[]; problems.append(f"asset provenance parse failure: {e}")
    by={r.get("current_path",""):r for r in rows}; rels=[n[len(prefix):] for n in assets]
    if len(assets)!=120: problems.append(f"expected 120 PNG/OGG assets, found {len(assets)}")
    if set(by)!=set(rels): problems.append("asset provenance paths do not exactly match public assets")
    for n in assets:
        rel=n[len(prefix):]; r=by.get(rel)
        if r and hashlib.sha256(z.read(n)).hexdigest()!=r.get("sha256"): problems.append(f"asset hash mismatch: {rel}")
    if sum(1 for r in rows if r.get("origin")=="port-added")!=1 or by.get("textures/item/white.png",{}).get("origin")!="port-added": problems.append("port-added asset classification mismatch")
if problems:
    for m in problems[:250]: print("FAIL:",m)
    raise SystemExit(1)
print(f"PASS public export compliance audit: {len(names)} files; {len(assets)} PNG/OGG assets")
