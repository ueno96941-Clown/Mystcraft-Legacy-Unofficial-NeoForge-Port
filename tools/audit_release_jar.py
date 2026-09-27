#!/usr/bin/env python3
"""Audit the exact Mystcraft release JAR before upload."""
from pathlib import Path
import csv, hashlib, io, json, sys, zipfile

VERSION = "0.13.7.06-port.1.0.0-Ueno"
EXPECTED_NAME = f"mystcraft-{VERSION}.jar"
MC = "NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT."
CYAN = "This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan."
REQUIRED_META = {
    "META-INF/LICENSE-LGPL-3.0-mystcraft-port.txt", "META-INF/LICENSE-GPL-3.0.txt",
    "META-INF/ATTRIBUTION.md", "META-INF/MODIFICATION_NOTICE.md", "META-INF/SOURCE_PROVENANCE.md",
    "META-INF/ASSET_PROVENANCE_SHA256.tsv", "META-INF/COMPLIANCE_AND_LICENSE_NOTICE.md",
    "META-INF/COMPLIANCE_AUDIT_REPORT.md", "META-INF/COMPLIANCE_SUMMARY_JA.md",
    "META-INF/THIRD_PARTY_NOTICES.md", "META-INF/KNOWN_ISSUES.md", "META-INF/neoforge.mods.toml",
}

def fail(msgs):
    for m in msgs: print("FAIL:", m)
    raise SystemExit(1)

def main():
    if len(sys.argv) != 2: raise SystemExit("usage: audit_release_jar.py <release.jar>")
    p=Path(sys.argv[1]); problems=[]
    if not p.is_file(): fail([f"not found: {p}"])
    if p.name != EXPECTED_NAME: problems.append(f"unexpected JAR filename: {p.name} (expected {EXPECTED_NAME})")
    with zipfile.ZipFile(p) as z:
        names=z.namelist(); ns=set(names)
        missing=sorted(REQUIRED_META-ns)
        if missing: problems.append("missing required JAR entries: "+", ".join(missing))
        for n in names:
            low=n.lower()
            if n.startswith(("src/","docs/","tools/")) or low.endswith((".java",".bat",".ps1",".py",".log",".zip",".patch")):
                problems.append(f"development/source artifact in JAR: {n}")
            if n.startswith("net/minecraft/") or n.startswith("com/mojang/"):
                problems.append(f"Minecraft/Mojang project class namespace in JAR: {n}")
            if n.startswith("assets/minecraft/") or n.startswith("data/minecraft/"):
                problems.append(f"project resource in minecraft namespace: {n}")
            if low.endswith((".json",".mcmeta")):
                try: json.loads(z.read(n).decode("utf-8-sig"))
                except Exception as e: problems.append(f"invalid JSON/mcmeta {n}: {e}")
        def text(n):
            try: return z.read(n).decode("utf-8-sig")
            except Exception: return ""
        toml=text("META-INF/neoforge.mods.toml")
        for token in [f'version="{VERSION}"','displayName="Mystcraft Legacy (Unofficial 1.21.1 Port)"','ueno969','ueno96941@gmail.com',MC,CYAN]:
            if token not in toml: problems.append(f"neoforge.mods.toml missing: {token}")
        prov=text("META-INF/ASSET_PROVENANCE_SHA256.tsv")
        try: rows=list(csv.DictReader(io.StringIO(prov), delimiter="\t"))
        except Exception as e: rows=[]; problems.append(f"asset provenance parse failure: {e}")
        by={r.get("current_path",""):r for r in rows}
        prefix="assets/mystcraft/"
        assets=sorted(n for n in names if n.startswith(prefix) and n.lower().endswith((".png",".ogg")))
        rels=[n[len(prefix):] for n in assets]
        if len(assets)!=120: problems.append(f"expected 120 PNG/OGG assets, found {len(assets)}")
        if set(by)!=set(rels):
            missingp=sorted(set(rels)-set(by)); extrap=sorted(set(by)-set(rels))
            if missingp: problems.append("provenance missing: "+", ".join(missingp[:10]))
            if extrap: problems.append("provenance extra: "+", ".join(extrap[:10]))
        for n in assets:
            rel=n[len(prefix):]; row=by.get(rel)
            if row and hashlib.sha256(z.read(n)).hexdigest()!=row.get("sha256"):
                problems.append(f"asset hash mismatch: {rel}")
        if sum(1 for r in rows if r.get("origin")=="port-added")!=1 or by.get("textures/item/white.png",{}).get("origin")!="port-added":
            problems.append("expected exactly one port-added PNG/OGG: textures/item/white.png")
    if problems: fail(problems)
    print(f"PASS release JAR audit: {p.name}; {len(names)} entries; 120 PNG/OGG assets")
if __name__=="__main__": main()
