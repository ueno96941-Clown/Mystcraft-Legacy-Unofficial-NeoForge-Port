# Changelog

All notable player-facing changes for the **Mystcraft Legacy — Unofficial Minecraft 1.21.1 / NeoForge Port** are summarized here.

This changelog is intentionally release-oriented. It does not reproduce the hundreds of internal development checkpoints used while porting and testing the mod.

## 0.13.7.06-port.1.0.0-Ueno — 2026-09-26

Initial public release of the Minecraft 1.21.1 / NeoForge port, based on Mystcraft Legacy 0.13.7.06.

### Restored legacy gameplay

- Restored the core **Age creation and exploration** loop using Mystcraft Pages/Symbols and Descriptive Books.
- Restored **Linking Books**, including the legacy same-dimension rules for Intra Linking and Intra Linking Only.
- Restored the main Mystcraft book/furniture workflow, including the **Writing Desk, Book Binder, Ink Mixer, Lectern, Book Receptacle, folders/portfolios**, and related book handling.
- Restored **Crystal Portals**, including cross-dimension transfer for players, dropped items, projectiles, minecarts/boats, passengers, and preserved movement where supported by the modern engine.
- Restored **Star Fissures** and the modernized Age lifecycle needed to travel to, unload, restore, and revisit generated Ages.
- Restored the active legacy **Instability** families used by this port, including potion effects, Burning, Lightning, Explosions, Crumble, Meteors, and Red/Blue/Purple/White Decay.
- Restored **Dense Ores** with bounded legacy-style scaling.
- Restored Mystcraft's Archivist/shop-facing gameplay and modern villager/POI integration used by the Writing Desk flow.

### Modern Minecraft compatibility additions

- Ported the runtime to **Minecraft 1.21.1, NeoForge 21.1.x, and Java 21**.
- Added runtime Symbol discovery for compatible registered **biomes and fluids**, allowing modded content to participate in Age construction.
- Extended **Dense Ores** classification to compatible ores added by other mods.
- Added optional **JEI** integration for the payload-aware Linking Book crafting recipe without making JEI a hard dependency.
- Added a compatibility bridge for **ModernFix stronghold caching** so dynamically installed Ages can generate structures without the ModernFix executor-context crash encountered during pack testing.
- Adapted legacy structure intent to modern 1.21.1 world generation for Mineshafts, Strongholds, Villages, Nether Fortresses, and related structure Symbols.

### Reliability and lifecycle work

- Added lazy Age installation/restoration and automatic unloading for unused Ages so prepared Age metadata does not force every generated dimension to remain loaded.
- Added persistence/recovery handling so Linking data can restore unloaded Age destinations when they are used again.
- Added defensive Crystal Portal transfer handling for duplicate UUIDs, vehicle/passenger trees, projectiles, and destination-side synchronization.
- Added safeguards around modern `WorldGenRegion` boundaries to avoid recursive/out-of-region structure generation crashes.
- Fixed runtime failures found during practical testing, including removed-entity meteor packet access, empty height-range generation, same-dimension Linking Book behavior, and multiple Age/worldgen edge cases.
- Added dynamic Symbol caching/lazy rebuild work to reduce startup overhead in heavily modded environments.

### Release cleanup

- Removed port-only **Test Pages/Symbols** and manual Decay QA helper items from the release-facing registry.
- Removed the port-only first-login **starter/tutorial Folder**.
- Removed release-disabled QA telemetry, diagnostic spam, and development-only runtime traces while retaining useful WARN/ERROR diagnostics for bug reports.
- Changed detailed startup/Age-restore diagnostics to DEBUG logging while leaving important compatibility and failure diagnostics available.
- Added public source export/audit tooling, provenance records, licensing notices, and a release distribution checklist.
- Added `KNOWN_ISSUES.md` documenting current runtime warnings, compatibility limits, and bug-report requirements.

### Gameplay notes and intentional differences from 1.12.2

- **PvP is enabled by default** in Ages; the Anti-PvP / PvPOff Symbol disables it for that Age.
- Dense Ores currently uses the following effective tiers: approximately **2x / 3x / 5x** total ore density for 1 / 2 / 3+ effective Dense Ores Pages, with increasing Instability costs.
- Exact **seed-for-seed or block-for-block parity** with Minecraft 1.12.2 is not claimed for terrain, biome distribution, vanilla structure layouts, or every biome-color result because the port runs on the Minecraft 1.21.1 world-generation engine.
- Old third-party Mystcraft add-ons are **not expected to run unchanged** on 1.21.1. In particular, the old external Instability provider API is not fully integrated into the built-in production Deck runtime.
- Extremely unstable Ages can create substantial server tick load, especially while generating new chunks in large modpacks.

### Known issues

See **`KNOWN_ISSUES.md`** for the current release-specific issue list and diagnostic guidance. Important disclosed limitations include:

- a rare non-fatal vanilla passenger warning during extremely rapid ridden cross-dimension Crystal Portal travel;
- occasional structure placement skips at modern WorldGenRegion boundaries as a crash-prevention tradeoff;
- high server-tick cost from pathological Instability combinations;
- incomplete compatibility for third-party add-ons relying on the legacy external Instability API.

### Release identity

- Project display name: **Mystcraft Legacy (Unofficial 1.21.1 Port)**
- Release version: **`0.13.7.06-port.1.0.0-Ueno`**
- Expected binary name: **`mystcraft-0.13.7.06-port.1.0.0-Ueno.jar`**

`Ueno` is only a maintainer/build identifier for this unofficial port and does not imply ownership of Mystcraft or endorsement by the original developers.
