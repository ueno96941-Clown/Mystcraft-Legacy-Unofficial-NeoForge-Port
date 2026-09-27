# Known Issues and Compatibility Limitations

Applies to **Mystcraft Legacy — Unofficial Minecraft 1.21.1 / NeoForge Port** release `0.13.7.06-port.1.0.0-Ueno`.

This file distinguishes actual known runtime issues from intentional modern-engine approximations. A difference from Minecraft 1.12.2 is not automatically a bug: this port targets the visible/gameplay behavior of Mystcraft Legacy 0.13.7.06 where the Minecraft 1.21.1 engine permits it.

## User-facing known issues

### 1. Rare vanilla passenger warning during very rapid ridden Crystal Portal travel

During unusually rapid repeated **cross-dimension** Crystal Portal travel while riding a vehicle, the client can occasionally log:

```text
Received passengers for unknown entity
```

The portal transfer code already delays riding-tree reconstruction and keeps the destination Age alive while the client catches up. In the completed endurance tests this warning was **not** associated with server-side transfer failure, entity duplication, transaction expiry, or a persistent forced dismount.

If a player sees an actual gameplay failure together with this warning, report it with both `latest.log` and `debug.log` rather than treating the warning alone as proof of corruption.

### 2. A structure start can be skipped at a modern WorldGenRegion boundary

The legacy structure bridge may log either of these warnings while processing structure Symbols:

```text
Skipped unavailable worldgen structure-reference chunk
Skipped out-of-region structure start during FEATURES
```

Minecraft 1.21.1 can expose a valid structure reference whose start chunk is not resident in the current `WorldGenRegion`. The port deliberately **skips that placement instead of recursively requesting out-of-region world generation**, because the latter can recurse into worldgen and crash the server.

Practical effect: an occasional Mineshaft, Stronghold, Village, or Nether Fortress placement near a generation-region boundary can be absent or incomplete in that location. This is a safety tradeoff, not save corruption.

### 3. Extreme Instability Ages can cause severe server tick load

Very high Instability is intentionally destructive, but pathological combinations can also become computationally expensive. This is most noticeable when several live Instability providers are active at once in a heavily modded pack or while new chunks are generating.

Normal Ages and the calibrated low/mid Instability ranges are the intended play path. If an Age produces sustained `Can't keep up!` messages, leave the Age and include its Page/Symbol list with the logs when reporting the case.

## Intentional legacy-to-modern world-generation approximations

These are known compatibility limits rather than unfinished basic functionality:

- **Native biome distribution (`BioConNative`)** — the original 1.12.2 implementation used the old `BiomeProvider` / GenLayer stack. The live 1.21.1 port uses the modern Overworld MultiNoise biome source contract.
- **Mineshafts, Strongholds, Villages and Nether Fortresses** — legacy placement intent is preserved where practical, but the actual structure pieces/layouts are Minecraft 1.21.1 structures rather than the 1.12.2 vanilla implementations.
- **Terrain Normal / Amplified / Nether / End** — the recovered legacy density/noise behavior is carried through the port's worldgen planning, but the final live chunk-generation adapter is a Minecraft 1.21.1 implementation. Seed-for-seed or block-for-block identity with a 1.12.2 Age is not guaranteed.
- **Natural grass / foliage / water coloring** — legacy color data is preserved, but the current client bridge can still resolve through modern biome tint paths. Exact 1.12.2 colorizer output is not guaranteed at every position.

These limitations do **not** mean the Symbols are unavailable; they mean exact 1.12.2 engine parity is not claimed.

## Third-party Mystcraft API compatibility limits

This port contains substantial API-v1 compatibility surfaces, but it is **not** a promise that every 1.12.2 Mystcraft add-on can be dropped into Minecraft 1.21.1 unchanged.

The largest known gap is the external Instability API:

- external Instability provider registration is discoverable but is not integrated into the built-in production Deck runtime;
- legacy `addCards` compatibility is a no-op;
- the reflective Instability factory/effect path remains a compatibility stub.

Other API areas such as legacy rendering hooks, generated-symbol factory metadata, and trade-value semantics are adapted to modern runtime contracts rather than guaranteed exact 1.12.2 behavior. These limits affect **third-party add-on authors**, not the built-in Mystcraft 0.13.7.06 gameplay providers used by this port.

## Benign or diagnostic warnings

The following messages are useful diagnostics but are not, by themselves, release-blocking failures:

- `Texture mystcraft:item/white with size 1x1 limits mip level ...` — the port-added one-pixel white utility texture cannot produce normal mip levels.
- `Failed to reserve Archivist POI ...` — the Writing Desk could not claim its Archivist job-site ticket on that attempt. The shop repair/binding code has retry and migration paths. Report it if the Archivist is actually missing or changes profession persistently.
- `Removed stale destination entity before Crystal Portal transfer ...` — a defensive duplicate-UUID guard removed a stale destination-side entity before completing the authoritative transfer.

## What to include in a bug report

For a runtime problem, include at minimum:

1. `latest.log`;
2. `debug.log`;
3. Minecraft, NeoForge and Mystcraft version numbers;
4. the mod list or modpack name/version;
5. the action immediately before the failure;
6. for Age/worldgen/Instability problems, the Page/Symbol list used to create the Age.

A crash report should be attached in addition to the two logs when one exists. Do not trim the logs to only the last few lines; the useful cause is often earlier than the visible failure.
