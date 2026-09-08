# Nebrel Client

A modular Minecraft client for Fabric. Module system, in-game menu, themeable
interface, drag-and-drop HUD editor, and 32 fair-play visual and
quality-of-life modules.

- **Minecraft** 1.21.1 · **Fabric Loader** 0.16.14 · **Fabric API** 0.115.6 ·
  **Java** 21 · **Mappings** Yarn
- **Mod id** `nebrelclient` · **Entry point** `de.nebrel.client.NebrelClientMod`
- **Licence** MIT

Press **Right Shift** in game to open the menu.

---

## Build status, stated plainly

`./gradlew build` **has not been run.** The environment this was written in
blocks `maven.fabricmc.net`, `libraries.minecraft.net` and
`piston-meta.mojang.com` at the network policy level, so Fabric Loom cannot
resolve Minecraft, Yarn or the Fabric API:

```
$ curl -sS http://127.0.0.1:40819/__agentproxy/status
"detail": "gateway answered 403 to CONNECT (policy denial or upstream failure)",
"host": "maven.fabricmc.net:443"
```

On a machine with normal network access, `./gradlew build` should work
unchanged. **Compile it and launch it before trusting it.**

Two verification passes were built to cover what could be covered without a
compiler. Both currently pass:

```
$ ./tools/verify-core.sh
Compiling 32 source files ...
PASS  278 checks

$ ./tools/check-mappings.py
Loaded 6896 classes and 35816 member names
Checking 132 source files
OK  every Minecraft type and member name resolves against the mappings
```

- **`tools/verify-core.sh`** really compiles the Minecraft-independent half —
  settings, config, module registry, theme, animation, HUD geometry — with a
  plain JDK and runs 278 assertions against it. No Minecraft, no Loom.
- **`tools/check-mappings.py`** checks every Minecraft type and member name in
  all 132 source files against the official Yarn 1.21.1 mappings, including
  mixin targets and injector method names.

Between them they caught four real bugs while this was being written; see
[`docs/REFERENCE_MAP.md` §4](docs/REFERENCE_MAP.md) for what they were.

**What this does not prove:** the mapping check is name-level. It cannot verify
argument types, generic bounds, or that a mixin injection point resolves at load
time. The ten mixins are the highest-risk part of the codebase and the first
thing to check on a real build.

---

## Architecture

```
de.nebrel.client
├── NebrelClientMod        entry point; wires four Fabric events
├── core/                  composition root, shared stats, mixin state holder
├── event/                 hook interfaces + dispatcher
├── module/                Module, ModuleCategory, ModuleManager
│   └── impl/{hud,visual,player,utility,world,render}
├── setting/               8 setting types with conditional visibility
├── gui/                   screens, components, theme, layout
├── hud/                   HudManager, widgets, anchors
├── render/                DrawContext primitives, world-space drawing, animation
├── config/                versioned, atomic, debounced JSON persistence
├── keybind/               polled input snapshot
├── notification/          toasts
└── mixin/                 10 mixins, each verified against the mappings
```

### The split that makes this testable

Half the client imports no Minecraft class at all: settings, config, the module
registry, themes, the animation engine and HUD geometry. That is not incidental
— it is what lets `verify-core.sh` compile and exercise that half for real,
which matters a great deal when the other half cannot be compiled at all.

`Module` itself is Minecraft-free. A module that needs to tick or draw says so
by implementing `ClientTickHook`, `HudRenderHook` or `WorldRenderHook`.

### Dispatch costs nothing for modules that do nothing

`ModuleDispatcher` keeps one list per hook holding only the modules that both
implement it and are currently enabled. The lists are rebuilt when module state
changes, not walked per frame. A module that is off is not visited; a module
with no render hook is never asked to render.

A module that throws is disabled and reported, rather than allowed to spam the
log every frame or take the game down.

### Mixins talk through a field holder, not a registry

`NebrelState` holds plain fields. Modules write to it when something changes;
mixins read it. An injected method costs a field read and a branch instead of a
map lookup on a hot path. Every module clears its own entries in `onDisable`,
and everything is reset on shutdown.

Three features that looked like they needed a mixin turned out not to, which is
the better outcome each time:

- **Time / Weather Changer** — `ClientWorld.setTimeOfDay` and
  `World.setRainGradient` are public.
- **Wavey Capes** — nudges the trailing cape point vanilla already integrates,
  so the cape keeps reacting to sprinting, turning and falling.
- **3D Skin Layers** — vanilla re-angles the outer model parts each frame but
  does not re-scale them.

### Rendering uses no textures and no shaders

Every pixel of the interface is `DrawContext.fill` and the vanilla font. No
texture atlas, no custom shader: nothing to bind, cache or restore, no render
state for a module to leak, and no image assets to ship. Rounded corners are
drawn as horizontal spans with an alpha-blended boundary pixel — O(radius) per
corner, not O(radius²).

### Config never touches disk per frame

Changing anything sets a dirty flag. Writes happen at most every three seconds,
or immediately when the menu closes. Files are written to a temp sibling and
moved into place, so a crash mid-write cannot truncate a config. A corrupt file
is renamed `.broken` and defaults are used rather than failing startup. Every
file carries a format version with a migration path.

---

## Modules

**HUD** — Nebrel HUD · Keystrokes · Food Details · TNT Timer · Vanilla HUD
**Visual** — Crosshair · Full Bright · FOV Changer · Damage Tint · Animations · Arrow Trail
**Player** — Toggle Sprint · Free Look
**World** — Time Changer · Weather Changer
**Render** — No Fog · Screen Dim · Glint Colorizer · Item Model · Custom Nametags · Health Indicators · Hitbox · Item Highlighter · Overflow Particles · 3D Skin Layers · Wavey Capes
**Utility** — Auto Text · Shulker Tooltip · Pack Tweaks · Borderless Window · Tiers · Quests

Three carry names that promise more than they do, so they were renamed and
documented rather than left to imply otherwise:

- **Screen Dim** (asked for as "Blur") is a themed scrim, not a Gaussian blur. A
  real blur needs a framebuffer post-process pass, which would mean owning
  shader lifetime, resize handling and shader-mod compatibility. The scrim gets
  most of the readability benefit for none of that risk.
- **3D Skin Layers** scales the outer model parts so hats and jackets stand off
  the body. It does not rebuild the geometry as solid boxes.
- **Food Details** (asked for as "AppleSkin") is an independent implementation
  against the game's food component. It does not depend on the AppleSkin mod. If
  you run both, turn one off.

### The HUD

Thirteen widgets — frame rate, ping, coordinates, facing, speed, click rate,
server, memory, clock, biome, armour, potion effects, player info — each with
its own anchor, scale, padding, colours, border and background.

The editor supports dragging with snap guides against screen edges, centre lines
and neighbouring widgets; scroll-to-scale; arrow-key nudging; and right-click to
toggle a widget in place. Positions are anchor-relative pixel offsets, so a
widget 10 px from a corner stays 10 px from it at any resolution.

---

## Fair play

Not a cheat client, and the boundary is enforced in code:

- **Free Look** moves the camera only; player rotation is saved and restored.
- **Toggle Sprint** holds the sprint key, exactly as vanilla's own toggle does.
- **Auto Text** is edge-triggered with a cooldown. No timer, no repeat.
- **Hitbox**, **Health Indicators** and **Item Highlighter** draw through the
  depth-tested lines layer, so nothing renders through terrain.
- **Time** and **Weather Changer** change the client's own copy of world state.
- **Overflow Particles** can only lower the particle ceiling, never raise it.
- **CPS** counts clicks the player made.

Not implemented and not planned: kill aura, reach, aim assist, velocity, fly,
speed, packet exploits, anti-cheat bypass, X-ray, wall ESP, auto clicker, combat
automation.

---

## Attribution

Nothing in this repository is copied from another project. The NoRisk client
source is not public and was not read; the two public NoRisk repositories carry
no LICENSE file and were treated as reference-only. Several modules share a name
with an existing community mod but share no code with it.

The full accounting — what was examined, what each licence permits, and what was
actually taken — is in [`docs/REFERENCE_MAP.md`](docs/REFERENCE_MAP.md).
