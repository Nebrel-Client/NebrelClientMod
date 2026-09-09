# Reference Map

What was examined before writing Nebrel Client, what its licence permits, and
what was actually taken from it.

**Summary: nothing was copied.** No third-party source file, asset, name or icon
is present in this repository. Everything below was read to understand a problem
or to check a fact; every line of Nebrel is written from scratch.

---

## 1. NoRisk Client

### The finding that decides everything else

`nrc-core` — the actual NoRisk client — **is not public.**

```
$ git ls-remote https://github.com/NoRiskClient/nrc-core.git HEAD
fatal: could not read Username for 'https://github.com': terminal prompts disabled
```

The same is true of `nrc-abstraction`, `nrc-compat`, `nrc-owolib` and
`nrc-cosmetics`. The reference in `nrc-configs/CLIENT_CONTRIBUTING.md` to
`NoRiskClient/nrc-core/tree/dev/mojang-mappings` points at a private repository.

So there was no NoRisk client source to read, and none was read. The client is
distributed as a compiled Maven artifact (`gg.norisk:nrc-client` on
`maven.norisk.gg`); decompiling it was not attempted and would not have been
appropriate.

### Repository classification

| Repository | Purpose | LICENSE file | May we copy code? | How it was used |
|---|---|---|---|---|
| `NoRiskClient/nrc-configs` | Modpack definitions, version configs, client translations, JSON schemas | **None** | **No** | Read to establish which Minecraft versions and Fabric loader versions NoRisk targets, and which third-party mods make up their client |
| `NoRiskClient/nrc-designer-docs` | Blockbench authoring guide for cosmetics and emotes | **None** | **No** | Skimmed; concerns cosmetic asset authoring, not the in-game module UI. Not used |
| `NoRiskClient/nrc-core` and the other `nrc-*` artifacts | The client itself | n/a — **private** | **No** | Not accessible; not read |

**No LICENSE file means all rights reserved.** Neither public repository grants
any licence, so both are reference-only. Nothing from either is reproduced here,
including configuration structure: Nebrel's config format was designed around
its own settings model.

### What the public configs established

From `nrc-configs/versions/prod/nrc-1.21.1.json`:

```json
{ "game_version": "1.21.1", "loader": "fabric", "loader_version": "0.16.14" }
```

This is the only place NoRisk influenced a Nebrel decision: it confirmed 1.21.1
with Fabric loader 0.16.14 as a well-supported, current target, which is what
`gradle.properties` pins. That is a factual observation about a version number,
not a copied artefact.

From `nrc-configs/packs/norisk-prod.json`, their Fabric client is assembled from
their own closed `nrc-*` artifacts plus third-party open-source mods: AppleSkin,
ShulkerBoxTooltip, WaveyCapes, 3DSkinLayers, Tiers, Sodium, Lithium,
ImmediatelyFast, EntityCulling, FerriteCore, ModMenu, Cloth Config and
Simple Voice Chat.

That composition explains the module list in the brief — several requested
"modules" are the names of separate community mods NoRisk bundles. Nebrel does
not bundle or depend on any of them.

### Branding

No NoRisk name, logo, icon, texture, sound, font, cosmetic model or server asset
appears in this repository. Nebrel has its own name, its own palette, and its
entire interface is drawn in code from rectangles and the vanilla font, so there
are no image assets to have copied in the first place.

The Nebrel+ badge is the letter **N**, for Nebrel, drawn in the vanilla font on a
rounded plate the client fills itself. It is not an image, not a glyph from
another client's font, and not a redraw of anyone else's mark. It sits before the
name, defaults to the purple accent, and follows the active theme.

---

## 2. The mods whose names appear in the module list

Several requested modules share a name with an existing community mod. Nebrel
implements the *feature*, from the game's own API, with no dependency on and no
code from any of them. None was cloned or read.

| Requested module | Same-named mod | That mod's licence | What Nebrel does |
|---|---|---|---|
| AppleSkin | AppleSkin | Unlicense (public domain) | Own implementation against `DataComponentTypes.FOOD` and `HungerManager`. Renamed **Food Details** in the UI |
| Shulker Box Tooltip | ShulkerBoxTooltip | LGPL-3.0 | Own implementation via Fabric's `ItemTooltipCallback` and the `CONTAINER` component |
| Wavey Capes | WaveyCapes | LGPL-3.0 | Own approach entirely: offsets the trailing cape point vanilla already integrates |
| 3D Skin Layers | 3dSkinLayers | LGPL-3.0 | Own approach: scales the outer model parts. Simpler than that mod's rebuilt geometry, and documented as such |
| Tiers | Tiers | unverified | Provider interface plus a local provider. No third-party API is contacted |
| Blur | PolyBlur | LGPL-3.0 | Not a blur. A themed scrim, named **Screen Dim** and documented as a scrim |
| Nebrel HUD | EvergreenHUD | LGPL-3.0 | Own widget and anchor system |
| Toggle Sprint | PolySprint | LGPL-3.0 | Own implementation, mirroring vanilla's own sprint-toggle option |
| Custom Nametags | PolyNametag | LGPL-3.0 | Own implementation via `renderLabelIfPresent` |
| Time / Weather Changer | PolyTime / PolyWeather | LGPL-3.0 | Own implementation; needs no mixin at all |

**On the LGPL entries:** the licences are noted for completeness only. Nothing
was copied or derived from any of them, so no copyleft obligation attaches to
Nebrel. Had any code been taken, that would have been raised before writing it,
as the brief requires.

Nebrel is MIT licensed and carries no inherited obligations.

---

## 3. What *was* used, and its licence

| Source | Licence | Use |
|---|---|---|
| **FabricMC/yarn**, branch `1.21.1` | CC0-1.0 (public domain) | Verifying that every Minecraft type and member Nebrel references actually exists. See §4 |
| Minecraft / Fabric API | Mojang EULA / Apache-2.0 | Compiled against, as any mod is |
| Gson | Apache-2.0 | Already on Minecraft's classpath; used for config, not bundled |

Yarn's mappings are the only external material this project depends on beyond
the platform itself, and CC0 places no restriction on that use.

---

## 4. Verification, and one thing this environment could not do

### `maven.fabricmc.net` is blocked here

```
$ curl -sS http://127.0.0.1:40819/__agentproxy/status
"detail": "gateway answered 403 to CONNECT (policy denial or upstream failure)",
"host": "maven.fabricmc.net:443"
```

`libraries.minecraft.net` and `piston-meta.mojang.com` are blocked too. Fabric
Loom cannot resolve Minecraft, Yarn or the Fabric API, so **`./gradlew build`
could not be run in this environment.** Not "was skipped" — it is not possible
here. On any machine with normal network access it should run unchanged.

Two verification passes were built to cover as much as possible without it.

### `tools/verify-core.sh` — real compilation, real assertions

The settings, config, module-registry, theme, animation and HUD-geometry layers
carry no Minecraft import, by design — and so does the whole Nebrel+ engine,
including `IdentityRenderer`, which draws through a `GlyphSink` interface rather
than a text renderer. That subset compiles with a plain JDK, and does:

```
Compiling 64 source files ...
PASS  420 checks
```

The 420 assertions cover clamping and quantisation, colour maths and HSB round
trips, conditional visibility, module lifecycle and duplicate-id rejection,
search, anchor geometry across resolution changes, config round trips, and
config resilience against corrupt, truncated, wrong-typed, stale and
version-skewed files.

Nebrel+ is covered by roughly 140 of them, and they are the answer to "is this
actually implemented or just a flag". A `RecordingSink` captures every draw, so
the badge assertions check what was *painted* — that the plate style emits a
plate and an `N`, that the bracket style emits `[N]`, that a disabled badge
draws nothing and reserves no width, that a name under an effect is drawn as six
separate glyphs that still spell the name, and that scaling a glyph does not
move its neighbours. A boolean `hasBadge` would pass none of them. The rest
cover entitlement merging and caching, provider isolation on failure, the badge
colour modes, `AdditionalNametag.sanitise`, effect stage ordering, the fact that
each effect actually changes its own output, determinism across repeated
evaluation, and that `plus.json` contains no token, password or payment field.

Three real bugs were caught here and fixed:

- `SmoothScroll` was not converging, because the test drove it in a tight loop
  while the class is deliberately wall-clock driven. The test was wrong; the
  behaviour is the frame-rate independence it is supposed to have.
- `HudAnchor` originally stored fractional positions. The round-trip test passed,
  but the margin test did not: a widget 10 px from the bottom-right corner became
  6.7 px from it at a lower resolution. Anchors now store a pixel offset from the
  anchor point, which is what makes an anchor worth having.
- Adding the Nebrel+ engine to this pass immediately failed to compile:
  `IdentityRenderer` was reaching for the Minecraft-facing `HudGlyphSink` through
  an `instanceof` to draw the outline badge style. `plateOutline` moved onto the
  `GlyphSink` interface with a no-op default, which is both the honest answer for
  a surface with no geometry and what lets the renderer stay Minecraft-free.

### `tools/check-mappings.py` — every Minecraft symbol, checked

For the game-facing half, the Yarn 1.21.1 mappings are the next best thing to a
compiler. The script checks every `import net.minecraft...`, every
`@Mixin(...)` target, every `method = "..."` injector target, every `@Accessor`
and `@Invoker`, and every method name called on a receiver:

```
Loaded 6900 classes and 35817 member names
Checking 172 source files
OK  every Minecraft type and member name resolves against the mappings
```

It caught three real errors:

- `DrawContext.drawStackOverlay` does not exist in 1.21.1. The correct name is
  `drawItemInSlot`.
- `ClientWorld.getPlayers()` is declared on `World` with a wildcard element
  type, so iterating it as `AbstractClientPlayerEntity` would not compile. Three
  modules now use `getEntities()` with an `instanceof` pattern.
- The checker itself was wrong once Nebrel+ introduced records: it only learned
  Nebrel's own method names from declarations, and a record's accessors are
  implicit, so `backgroundColor`, `opacity` and friends were reported as invented
  Minecraft API. It now reads record components out of the header too. Worth
  recording as a caught error rather than a silent fix — a checker that
  false-flags is a checker people stop reading.

**What this does not prove.** It is a name-level check. It cannot verify argument
types, generic bounds, or that a mixin injection point resolves at load time.
Compilation and a launch are still required, and are the first thing to do on a
machine that can reach the Fabric maven.

---

## 5. Design analysis

Screenshots and videos of the NoRisk in-game menu were not reachable from this
environment (`github.com` HTML, and general web browsing, are blocked by the same
egress policy). The design analysis below therefore rests on the structure the
public `nrc-configs` translation file exposes — roughly 750 `nrc.ui.*` keys —
plus the conventions this class of client shares.

**What this kind of client gets right**

- A category sidebar with a card grid. Modules are browsable rather than buried,
  and the shape is familiar enough to need no explanation.
- Settings inside the same window. Navigation without losing the frame.
- Conditional settings. The `nrc.ui.modules.value.*` keys imply many dependent
  options; hiding what does not apply keeps a long list readable.
- Restrained accent use for state.

**What is worth improving**

- Setting groups. A flat list of thirty options is hard to scan whatever the
  styling.
- Search that only matches module names. Users remember what a feature *does*,
  not what it is called.
- HUD positioning through numeric offset fields rather than direct manipulation.
- Resolution-dependent HUD layouts.

**How Nebrel implements it**

- Settings are grouped into sections, and a section whose settings are all
  currently hidden disappears entirely — heading included.
- Search covers module names, descriptions, ids, categories **and setting
  names**, so "rainbow" finds every module with a rainbow option. A non-empty
  query searches all categories, because someone typing "fog" wants No Fog
  wherever it lives.
- The HUD is edited by dragging, with snap guides against screen edges, centre
  lines and neighbouring widgets, scroll-to-scale, and arrow-key nudging.
- HUD positions are anchor-relative pixel offsets, so a layout survives a
  resolution change (§4).
- Every colour is a theme token. Runtime theme switching and a user-chosen
  accent needed no component changes, and the accent's hover and pressed states
  are derived from whatever colour is picked.
- The whole interface is drawn from rectangles and the vanilla font: no texture
  atlas, no custom shader, nothing to bind or restore, and no image assets.

---

## 6. Fair play

Nebrel is not a cheat client, and the boundary is enforced in the code rather
than stated in a README:

- **Free Look** moves the camera only. Player yaw and pitch are saved on
  activation and restored on release; nothing different is sent to the server.
- **Toggle Sprint** holds the sprint key down, which is exactly what vanilla's
  own "Sprint: Toggle" option does. No movement value is altered.
- **Auto Text** is edge-triggered with a cooldown, one message per deliberate
  key press. There is no timer and no repeat.
- **Hitbox**, **Health Indicators** and **Item Highlighter** draw through the
  depth-tested lines layer, so anything behind terrain stays hidden. They make
  what is already on screen easier to read; they reveal nothing.
- **Time** and **Weather Changer** alter the client's own copy of world state.
  Mob spawning, crop growth and daylight sensors keep following the server.
- **Overflow Particles** can only ever reduce the particle count below the
  player's own setting, never raise the ceiling.
- **CPS** counts clicks the player made. Nothing in Nebrel generates input.

Not implemented, and not planned: kill aura, reach, aim assist, velocity, fly,
speed, packet exploits, anti-cheat bypass, X-ray, wall ESP, auto clicker, combat
automation.
