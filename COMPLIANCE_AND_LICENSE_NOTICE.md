# Compliance and License Notice

**Release baseline:** 2026-09-26 / `0.13.7.06-port.1.0.0-Ueno`

This document records the release packaging boundary used by this unofficial port. It is technical/documentary information, not legal advice.

## Project identity and contact

- Display name: **Mystcraft Legacy (Unofficial 1.21.1 Port)**
- Release version: `0.13.7.06-port.1.0.0-Ueno`
- Maintainer/publisher identifier: `ueno969`
- Direct contact: `ueno96941@gmail.com`
- `Ueno` is a maintainer/build suffix only and does not imply ownership of Mystcraft or official endorsement.

## Mystcraft-derived source and assets

This port uses the official `Mystcraft/Mystcraft-Legacy` repository at commit `9bc8ddc061845df0cd5ea47f4fdef773cff786ae` as its source/provenance baseline. The license file at that pinned baseline is GNU LGPL version 3. The public package includes the LGPLv3/GPLv3 texts, attribution, modification notice, source provenance, and per-file PNG/OGG SHA-256 provenance.

The historical Mystcraft CurseForge binary is not redistributed. Minecraft/NeoForge binaries, Gradle distributions/caches, private reference archives, run directories, logs, crash reports and unrelated third-party binaries are outside the release package.

## Minecraft/Mojang/Microsoft boundary

The release is distributed as a mod only, not as a modified Minecraft client/server. The project metadata and public materials identify the independent maintainer and direct contact and carry the required unofficial-project notice.

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

## Cyan boundary

The release carries Cyan's fan-content disclaimer and does not include Myst/Riven game music or Cyan game binaries/official marketing artwork identified by the package audit. Keep the release free/non-paywalled unless separate permission changes the applicable terms, and re-check Cyan's current policy before each publication.

**This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan.**

## Build dependencies and wrapper

NeoForge, ModDevGradle, JEI API, Parchment mappings, and Gradle are external development/runtime dependencies. They are resolved by the build system and are not redistributed as project binaries. The source package includes Gradle wrapper scripts/properties. If the wrapper JAR is absent, the bootstrap scripts restore it from the pinned official NeoForge MDK snapshot and verify the pinned wrapper JAR and Gradle distribution SHA-256 values.

## Asset inventory

This release contains **120 PNG/OGG Mystcraft assets** covered exactly by `ASSET_PROVENANCE_SHA256.tsv`: **119 upstream-identical** and one port-added compatibility asset (`textures/item/white.png`). The public-source and release-JAR audit tools verify coverage and hashes.

## Release controls

Before publication:

1. generate and audit the public source ZIP;
2. build from that exact release baseline in a clean environment;
3. audit the produced JAR with `tools/audit_release_jar.py`;
4. publish the matching public source ZIP alongside the JAR;
5. re-read the current Minecraft Usage Guidelines/EULA and Cyan Fan-Made Content Policy;
6. keep the listing disclaimers, maintainer identity and direct contact synchronized.

See `COMPLIANCE_AUDIT_REPORT.md`, `SOURCE_PROVENANCE.md`, `THIRD_PARTY_NOTICES.md`, and `RELEASE_DISTRIBUTION_CHECKLIST.md` for details.
