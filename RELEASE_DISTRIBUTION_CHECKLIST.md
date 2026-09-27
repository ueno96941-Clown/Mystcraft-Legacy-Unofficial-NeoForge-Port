# Release Distribution Checklist

Run this immediately before each upload. The goal is to make the release boundary obvious and reproducible.

- [ ] Build with `clean build` from the exact public-source baseline being released.
- [ ] Confirm the JAR filename/version is `mystcraft-0.13.7.06-port.1.0.0-Ueno.jar` / `0.13.7.06-port.1.0.0-Ueno`.
- [ ] Run `python tools/make_public_source_export.py <output.zip>` and `python tools/audit_public_source_export.py <output.zip>`; require PASS.
- [ ] Run `python tools/audit_release_jar.py build/libs/mystcraft-0.13.7.06-port.1.0.0-Ueno.jar`; require PASS.
- [ ] Confirm the public source contains no internal `docs/`, checkpoint notes, development harnesses, local paths, logs, caches, run data, crash reports or unrelated binaries.
- [ ] Confirm the JAR contains the required `META-INF` license, attribution, modification, provenance, third-party, compliance and known-issues notices.
- [ ] Attach the **matching public source ZIP** alongside the mod JAR; do not make a binary-only release.
- [ ] Never upload the historical Mystcraft CurseForge JAR, comparison source archive, Minecraft/NeoForge JARs, Gradle distributions/caches, run directories, logs or crash reports.
- [ ] Re-read `KNOWN_ISSUES.md` and `CHANGELOG.md` against the exact binary being uploaded.
- [ ] Re-read the current Minecraft EULA/Usage Guidelines and Cyan Fan-Made Content Policy on the actual release date.
- [ ] Put the Minecraft and Cyan disclaimers from `RELEASE_LISTING_TEMPLATE.md` on every download/listing/project page where required.
- [ ] Confirm public maintainer/publisher `ueno969` and direct contact `ueno96941@gmail.com` are current on listing pages.
- [ ] Keep the mod free/non-paywalled under the current Cyan fan-content policy unless separate written permission changes the applicable terms.
- [ ] Do not add Myst/Riven game music without separate authorization from the relevant music rightsholder.
- [ ] Do not use official Minecraft/Mojang/Microsoft/Cyan/Myst/Riven logos or marketing art as project branding without applicable permission.
- [ ] State clearly that this is an unofficial modified port and is not endorsed by Mystcraft Org/XCompWiz.
- [ ] If any PNG/OGG changes, regenerate and re-check `ASSET_PROVENANCE_SHA256.tsv`.
- [ ] If build-tool versions change, update/check third-party notices and pinned Gradle/wrapper checksums.
- [ ] Perform fresh-player/fresh-world smoke testing and the final ATM10 RC run using the **same JAR** intended for upload.
- [ ] Archive the exact released JAR, public-source ZIP and their SHA-256 hashes together with the release record.
