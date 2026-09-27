# Final Distribution Compliance and Hygiene Audit

**Audit date:** 2026-09-26  
**Release:** `0.13.7.06-port.1.0.0-Ueno`  
**Scope:** source package, resources, build/release scripts, license/provenance documentation, public-export boundary, and current official policies reasonably relevant to this port.

This is a technical/documentary audit, not legal advice.

## 1. Release-boundary findings corrected in this audit

The final distribution audit did not simply accept the earlier source-export PASS. A stricter inspection found and corrected several release-boundary defects:

- internal checkpoint summaries and `docs/` audit/history material were still reaching the public source ZIP;
- development-only Java harnesses were still present in the public export;
- `VERIFY_ENVIRONMENT.bat` referenced a deleted `tools/prebuild_static_audit.py`, which could make an otherwise valid Windows build fail when Python was installed;
- `FIND_SMALL_LIBRARY.bat` referenced a deleted `tools/FindSmallLibrary.java` and was removed;
- `BUILD.bat` did not force a clean build and now runs `clean build`;
- release-facing compliance documents contained stale internal checkpoint labels and an obsolete public-export file count;
- the README used the phrase “effective Dense Ores Pages” for the authored 3+ cap; it now correctly says “authored”;
- a cross-platform `bootstrap_wrapper.py` and an independent release-JAR auditor were added.

These corrections affect packaging, build helpers and release documentation. `src/main/java` and `src/main/resources` remain unchanged from the previously clean-built and successfully launched runtime baseline.

## 2. Mystcraft redistribution/provenance basis

The source/provenance baseline remains the official `Mystcraft/Mystcraft-Legacy` repository at commit `9bc8ddc061845df0cd5ea47f4fdef773cff786ae`. Its license file is GNU LGPL version 3. The public source includes license texts, attribution, modification notice, source provenance and asset provenance.

The historical CurseForge binary is not part of either release artifact.

## 3. Asset audit

The release has **120** PNG/OGG files under `assets/mystcraft`:

- **119** are hash-identical to matching files in the pinned upstream Mystcraft-Legacy source baseline;
- **1** is port-added: `textures/item/white.png`.

`ASSET_PROVENANCE_SHA256.tsv` exactly covers those assets. Public-source and binary-JAR audit tools verify each shipped byte stream against the table.

No Myst/Riven game music, Cyan game binary, official marketing artwork, Minecraft game binary, or project-owned file under the `minecraft` asset/data namespace was identified in the audited release tree.

## 4. Minecraft policy boundary reviewed

The current Minecraft EULA/Usage Guidelines reviewed on the audit date allow distribution of mods as mods rather than modified game clients/servers, require independent products not to appear official, and require clear publisher/contact identification and a prominent unofficial-product disclaimer. This release provides `ueno969`, `ueno96941@gmail.com`, and the notice below in metadata and release-facing documents:

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

The public exporter also rejects project Java sources under `net/minecraft` or `com/mojang` and project resources under `assets/minecraft` or `data/minecraft`.

## 5. Cyan policy boundary reviewed

The current Cyan Fan-Made Content Policy reviewed on the audit date requires its fan-content disclaimer, prohibits implying endorsement, restricts commercial objectives/crowdfunding without separate permission, and states that Myst/Riven music requires separate authorization from the relevant music rightsholder. This release carries the required disclaimer:

**This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan.**

The package audit did not identify Myst/Riven game music or Cyan game binaries/official marketing artwork.

## 6. Build/dependency boundary

NeoForge/ModDevGradle, JEI API, Parchment and Gradle remain external dependencies. No NeoForge/Minecraft/JEI binary is bundled as a project release artifact. The Gradle wrapper JAR may be restored locally from the pinned official NeoForge MDK snapshot at commit `30cafee9cd8d7f46427ec88fa8579d49c146df9a`.

Pinned integrity values:

- MDK-carried wrapper JAR SHA-256: `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`
- Gradle 9.2.1 binary distribution SHA-256: `72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f`

Both PowerShell and Python bootstrap paths enforce these values.

## 7. Public source hygiene gate

The final public exporter excludes all internal `docs/`, root checkpoint notes, development harnesses, build/run/cache/log/crash output, compiled classes, ZIP/patch artifacts, embedded Gradle distributions, and non-wrapper JARs. The strengthened public-source auditor additionally checks required notices, exact release identity, disclaimers/contact, path hygiene, wrapper hashes, asset provenance, and the absence of stale release identifiers/checkpoint labels in public release documents.

The audited public source export contains **787 files** and exactly **120 PNG/OGG assets**.

## 8. Remaining release gates

This source/package audit is complete, but binary publication remains gated on:

1. clean-environment build from this exact baseline;
2. `python tools/audit_release_jar.py build/libs/mystcraft-0.13.7.06-port.1.0.0-Ueno.jar`;
3. fresh-player/fresh-world smoke test;
4. final ATM10 release-candidate run using the identical binary intended for upload.

A PASS here does not replace those runtime/binary gates.
