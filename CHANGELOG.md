# Changelog

All notable changes to Nebrel Client are recorded here. Versions follow
`major.minor.patch`; the mod version is templated from `gradle.properties`
(`mod_version`) into `fabric.mod.json` at build time.

## 26.1

- Nebrel+ entitlement architecture: `EntitlementService`, `NebrelEntitlement`,
  `EntitlementProvider`, `LocalEntitlementProvider` (development-only, not a
  security boundary).
- Nebrel+ badge (`N`), badge rendering shared across world nametag, tab list
  and chat through one `IdentityRenderer` / `GlyphSink` seam.
- Nametag effect pipeline (Rainbow, Chromatic, Blinking, Shaking, Waving,
  Skewing, Growing) and the additional nametag line.
- `CustomNametagsModule` reworked to describe the nametag frame only; a single
  `NametagCoordinator` owns the actual draw.

## 0.1.0

- Initial client: module system, in-game menu, HUD system and editor, 32
  fair-play modules, theming, config persistence.
