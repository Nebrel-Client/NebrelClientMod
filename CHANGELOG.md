# Changelog

All notable changes to Nebrel Client are recorded here. Versions follow
`major.minor.patch`; the mod version is templated from `gradle.properties`
(`mod_version`) into `fabric.mod.json` at build time.

## 26.2

- Nebrel+ page and nametag designer: live preview (World / Tab List / Chat
  tabs), badge style/colour controls, seven nametag effects, an additional
  nametag line, and a sidebar entry.
- Config, cache invalidation and the (currently unimplemented) server bridge
  wired into the Nebrel+ facade.
- Core self-test extended to 420 assertions; the whole Nebrel+ engine compiles
  and is tested headlessly (`tools/verify-core.sh`).

## 26.1.2

- Fixed the additional nametag line drifting sideways whenever its Scale
  setting was not 1, and fixed that same setting being silently ignored
  whenever nametag effects were applied to the line.
- Fixed `IdentityRenderer` reaching into the Minecraft-facing `HudGlyphSink`
  via `instanceof` to draw the outline badge style; `plateOutline` is now part
  of the `GlyphSink` interface with a safe no-op default.
- Fixed `tools/check-mappings.py` false-flagging Nebrel's own record accessors
  as invented Minecraft API.

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
