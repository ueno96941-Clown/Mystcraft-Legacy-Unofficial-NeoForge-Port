# Mystcraft Legacy — Unofficial Minecraft 1.21.1 / NeoForge Port

A compatibility-focused community port of **Mystcraft Legacy 0.13.7.06** to **Minecraft 1.21.1**, **NeoForge**, and **Java 21**.

**Release version:** `0.13.7.06-port.1.0.0-Ueno`

The `Ueno` suffix is only a maintainer/build identifier for this unofficial port. The project remains **Mystcraft Legacy**; the suffix does not claim ownership of Mystcraft and does not imply endorsement by the original developers.

## What this port brings back

The goal is to make the legacy Mystcraft play loop usable on a modern Minecraft version while preserving the original visible/gameplay behavior where Minecraft 1.21.1 permits it.

- Create and explore custom **Ages** from Mystcraft Pages/Symbols.
- Use **Descriptive Books** and **Linking Books** for Age travel.
- Use legacy-style Mystcraft furniture and book infrastructure, including the **Writing Desk**, **Book Binder**, **Ink Mixer**, **Lectern**, **Book Receptacle**, folders/portfolios, and related book handling.
- Build and use **Crystal Portals** with cross-dimension entity, vehicle, passenger, and momentum handling.
- Use **Star Fissures** and the restored Age travel/lifecycle systems.
- Play with the active legacy **Instability** families, including potion effects, Burning, Lightning, Explosions, Crumble, Meteors, and Red/Blue/Purple/White Decay.
- Use **Dense Ores**, including this port's compatibility extension for ores supplied by other mods.
- Generate runtime Symbols from compatible registered **biomes** and **fluids**, allowing heavily modded environments to participate in Age construction.
- Optional **JEI** integration exposes the payload-aware Linking Book crafting recipe through the normal crafting category.

This release does **not** grant the old port-only starter/tutorial Folder on first login. The release-facing Test Pages and manual Decay QA helper items have also been removed.

## Requirements

- Minecraft **1.21.1**
- NeoForge **21.1.250 or newer** for Minecraft 1.21.1
- Java **21**
- JEI **19.16+** is optional and only required for JEI recipe display integration

## Installation

1. Install Minecraft 1.21.1 with a compatible NeoForge 21.1.x loader.
2. Place `mystcraft-0.13.7.06-port.1.0.0-Ueno.jar` in the instance's `mods` folder.
3. Start the game.

No additional Mystcraft library mod is required by this port. If JEI is installed, Mystcraft's JEI compatibility loads as an optional client-side integration.

For an existing world, make a normal backup before changing a mod list. Mystcraft creates and persists custom dimensions, so ordinary world-backup practice is strongly recommended before upgrades or pack changes.

## Important gameplay notes

### Ages and unloading

Mystcraft Ages are installed lazily and can unload when no player is using them. Linking data keeps enough Age identity to restore an unloaded destination when it is needed again.

### Linking Books

Normal Linking Books do not allow same-dimension travel unless the legacy **Intra Linking** property is present. **Intra Linking Only** restricts a link to same-dimension travel.

### PvP

PvP is enabled by default in Mystcraft Ages. The legacy **Anti-PvP / PvPOff** Symbol disables player-vs-player damage for the Age that contains it.

### Dense Ores

Dense Ores uses a bounded legacy-style multiplier while also recognizing compatible ores from other mods:

| Effective Dense Ores Pages | Approx. total ore density | Instability contribution |
| ---: | ---: | ---: |
| 0 | 1x | 0 |
| 1 | 2x | 500 |
| 2 | 3x | 2100 |
| 3+ | 5x | 20000 |

Four or more authored Dense Ores Pages are treated as the 3-page tier.

### Instability

Instability is active in normal gameplay. Extremely unstable Ages are intentionally dangerous and can also become computationally expensive, especially while generating new chunks in a large modpack. If an Age produces sustained server-lag warnings, leave the Age and include its Page/Symbol list in a bug report.

## Compatibility scope

This is a port, not a byte-for-byte recreation of the Minecraft 1.12.2 engine.

The port preserves legacy intent and visible behavior where practical, but modern Minecraft world generation is used underneath. Exact 1.12.2 seed-for-seed or block-for-block parity is therefore **not** claimed for terrain, biome distribution, vanilla structures, or every colorizer result.

The bundled Mystcraft gameplay systems are separate from old third-party add-on compatibility. API-v1 compatibility surfaces exist, but **1.12.2 Mystcraft add-ons are not expected to run unchanged on 1.21.1**, and the old external Instability provider API is not fully wired into the built-in production Deck runtime.

See **`KNOWN_ISSUES.md`** for the current disclosed runtime issues, modern-engine approximations, benign diagnostic warnings, and add-on API limits.

## Bug reports

Please include enough information to reconstruct what happened. At minimum provide:

1. `latest.log`
2. `debug.log`
3. Minecraft, NeoForge, and Mystcraft version numbers
4. Mod list or modpack name/version
5. The action immediately before the problem
6. For Age/worldgen/Instability problems, the Page/Symbol list used to create the Age

Attach the crash report as well when Minecraft generates one. Full logs are much more useful than only the last few lines because the original cause can occur well before the visible failure.

**Maintainer:** `ueno969`  
**Direct contact:** `ueno96941@gmail.com`

## Building from source

Build requirements:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.250
- Parchment 2024.11.17 for Minecraft 1.21.1
- Gradle 9.2.1 through the Gradle Wrapper

The normal build command is:

```text
./gradlew clean build
```

`BUILD.bat` provides the clean release build path on Windows. If `gradle/wrapper/gradle-wrapper.jar` is absent, restore it from the pinned official NeoForge 1.21.1 ModDevGradle MDK with:

```text
python bootstrap_wrapper.py
```

Windows users may alternatively run `BOOTSTRAP_WINDOWS.bat` / `bootstrap_wrapper.ps1`. Both bootstrap paths verify the pinned wrapper JAR checksum and pin the Gradle 9.2.1 distribution checksum.

The built mod JAR is written under `build/libs/`.

### Public source package

Development checkpoint archives contain internal QA/history material and are **not** intended to be uploaded as the public source package.

Generate and audit the source-only public package with:

```text
python tools/make_public_source_export.py <output.zip>
python tools/audit_public_source_export.py <output.zip>
```

The exact matching public source ZIP should be published alongside the binary release.

## Upstream and licensing

The porting baseline is the official open-source Mystcraft Legacy repository:

- Repository: `Mystcraft/Mystcraft-Legacy`
- Branch: `1.12/develop`
- Pinned commit: `9bc8ddc061845df0cd5ea47f4fdef773cff786ae`
- Legacy behavior target: `0.13.7.06`
- Repository license at the pinned source baseline: **GNU LGPL v3**

The historical CurseForge binary is used only as a private comparison/reference input and is **not redistributed** by this project. Shipped Mystcraft-derived material is tied to files verifiably present in the official LGPLv3 source repository; provenance records are included with the source package.

For redistribution details, see:

- `COMPLIANCE_AND_LICENSE_NOTICE.md`
- `COMPLIANCE_AUDIT_REPORT.md`
- `COMPLIANCE_SUMMARY_JA.md`
- `ATTRIBUTION.md`
- `MODIFICATION_NOTICE.md`
- `SOURCE_PROVENANCE.md`
- `ASSET_PROVENANCE_SHA256.tsv`
- `THIRD_PARTY_NOTICES.md`
- `RELEASE_DISTRIBUTION_CHECKLIST.md`
- `RELEASE_LISTING_TEMPLATE.md`
- `KNOWN_ISSUES.md`
- `CHANGELOG.md`
- `LICENSES/`

The project's primary Mystcraft-derived license declaration is `LGPL-3.0-only`. Separate Minecraft/Mojang/Microsoft and Cyan policies apply to their respective names, brands, and content; they do not alter the LGPL terms covering Mystcraft-derived code/assets.

## Required unofficial-project notices

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

**This product contains trademarks and/or copyrighted works of Cyan. All rights reserved by Cyan. This product is not official and is not endorsed by Cyan.**

This is also **not an official Mystcraft release** and is not endorsed by Mystcraft Org, XCompWiz, or the original Mystcraft development team.
