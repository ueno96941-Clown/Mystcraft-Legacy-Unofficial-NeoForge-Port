# Modification Notice

**Release baseline:** `0.13.7.06-port.1.0.0-Ueno` / 2026-09-26

This repository is an unofficial, substantially modified port of Mystcraft / Mystcraft Legacy.

The upstream project targeted Minecraft 1.12.2 / Forge. This port adapts it to Minecraft 1.21.1 / NeoForge and Java 21. Major modification areas include dimension lifecycle, Age generation bridges, rendering, networking, data components, GUI/block-entity integration, world-generation compatibility, dynamic Symbol resolution, Crystal Portal vehicle/passenger/projectile transfer, lazy Age load/unload handling, legacy Linking Book behavior, JEI compatibility, Instability restoration/calibration, modded Dense Ores compatibility, ModernFix stronghold-cache integration, startup/runtime cleanup, and build/release tooling.

The port preserves the active legacy Instability gameplay families while adapting implementation details to the modern engine. Dense Ores retains a bounded compatibility extension for modded ores. Port-only QA Symbol Pages, manual Decay helper items, release-disabled telemetry, and the port-only first-login starter/tutorial Folder were removed from the release-facing build. Normal Decay and other Instability gameplay remain available.

Source-level regression tests and maintenance harnesses may exist in the development archive, but they are not runtime JAR content and development-only harnesses are excluded from the public source export. The block-instability profiler remains because its non-ore result participates in live legacy Instability scoring.

The public release identifier is `0.13.7.06-port.1.0.0-Ueno`. `Ueno` is only a compact maintainer/build identifier so this unofficial port can be distinguished from other builds. The displayed project name remains **Mystcraft Legacy (Unofficial 1.21.1 Port)**; the suffix does not imply ownership of Mystcraft or endorsement by the upstream project.

This is **not an official upstream Mystcraft release**. No endorsement by Mystcraft Org, XCompWiz, Mojang/Microsoft, NeoForged, or Cyan is implied.

See `ATTRIBUTION.md`, `LICENSE`, `LICENSES/`, `SOURCE_PROVENANCE.md`, and `COMPLIANCE_AND_LICENSE_NOTICE.md` for attribution, license, provenance, and distribution-boundary information.
