# Multi-version build (26.1 / 26.1.1 / 26.1.2 / 26.2)

## Why this exists

Minecraft moved to year-based versioning in spring 2026 (`YY.Drop.Hotfix`)
and, starting with 26.1, ships **unobfuscated** - Mojang's own class and
member names are what's in the jar, so there is no Yarn intermediary mapping
step any more. `gradlew build` now builds four targets in one invocation:
26.1, 26.1.1, 26.1.2, 26.2.

## Layout

```
build.gradle                    root: only the Minecraft-free compileCore check
settings.gradle                 declares the four version subprojects
gradle.properties               shared: mod_version, maven_group, java_version
gradle/version-project.gradle   shared Loom/dependency config, applied by each subproject
versions/
  26.1/    { build.gradle, gradle.properties }
  26.1.1/  { build.gradle, gradle.properties }
  26.1.2/  { build.gradle, gradle.properties }
  26.2/    { build.gradle, gradle.properties }
src/main/java, src/main/resources   ONE shared source tree, all four compile it
```

Each subproject's `gradle.properties` holds that version's exact
`minecraft_version` / `fabric_version` / `loader_version`; each `build.gradle`
declares its own `fabric-loom` plugin version (a literal, since Gradle's
`plugins {}` block can't take a variable) and then applies the shared
`gradle/version-project.gradle` for everything else - the Loom `runs` block,
the dependency block (deliberately with **no** `mappings` line - see below),
`processResources` templating into `fabric.mod.json`, and the jar/sources-jar
setup.

**No mappings dependency at all.** The first attempt used
`mappings loom.officialMojangMappings()`, which failed with "Failed to find
official mojang mappings for 26.1" - that call fetches a per-version mapping
file from Mojang, and Mojang doesn't publish one for 26.x because there's
nothing to map (the jar already ships with real names). Fabric's own archived
26.1.2 porting guide confirms the fix is to remove the `mappings` line
entirely, not to call `officialMojangMappings()`.

| Target  | Loom    | Fabric Loader | Fabric API        |
|---------|---------|----------------|-------------------|
| 26.1    | 1.15.5  | 0.18.4         | 0.145.1+26.1      |
| 26.1.1  | 1.15.5  | 0.18.4         | 0.145.4+26.1.1    |
| 26.1.2  | 1.15.5  | 0.18.4         | 0.149.1+26.1.2    |
| 26.2    | 1.17.13 | 0.19.3         | 0.159.0+26.2      |

The Loom versions are confirmed against the real
`https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml`
(there is no bare "1.15" or "1.17" artifact - only patch releases exist;
these are the latest patch in each recommended line as of September 2026).
Fabric Loader and Fabric API are **not yet confirmed the same way** - see
below.

26.2 needs the newer Loom/Loader because Fabric Loader 0.19 + Loom 1.17
added enum-extension support that changed how mixins are applied at compile
time; the 26.1 family stays on the older, stable pair.

Gradle itself runs on **9.5.1** (`gradle/wrapper/gradle-wrapper.properties`),
the lowest version that satisfies both families' minimums (26.1 wants >=9.4.0,
26.2 wants >=9.5.1). Minecraft 26.x requires **Java 25** at runtime, which is
also now the Gradle launcher JVM requirement - see the root README/CLAUDE
notes on pointing `JAVA_HOME` or `org.gradle.java.home` at a JDK 25 install.

## What is, and is not, verified

- The Gradle project structure itself: `settings.gradle` correctly declares
  and discovers all four subprojects, and the root `build.gradle`'s
  `compileCore` task has been run for real in this environment (resolves a
  real Maven Central dependency, compiles for real) - see
  `tools/verify-core.sh`. That part is genuinely tested.
- **Nothing about the actual 26.x compile has been verified.** This
  environment has no network access to `maven.fabricmc.net` or Mojang's
  distribution servers, no Windows, and no display, so the `fabric-loom`
  plugin itself has never been resolved here, let alone run against a real
  Minecraft 26.x jar.
- The Loom versions in the table were wrong on the first pass (`1.15` and
  `1.17` do not exist as artifacts - Gradle failed with "Plugin ... was not
  found") and are now fixed against the real
  `maven-metadata.xml` fetched from a machine with actual network access.
  Fabric Loader and Fabric API were sourced the same unverified way (web
  search, since `meta.fabricmc.net` and `api.modrinth.com` are both blocked
  by this sandbox's egress proxy) and have **not** been re-checked against
  their real `maven-metadata.xml` yet - treat them with the same suspicion
  that turned out to be warranted for Loom, and fetch
  `https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml`
  and `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml`
  from a real machine before assuming the next build error is the last one.
- **The actual source (175 Minecraft-facing files) was written and last
  verified against Minecraft 1.21.1 via Yarn mappings** (`tools/check-mappings.py`,
  which still only checks that 1.21.1 baseline - Minecraft 26.x has no Yarn
  mappings to check against at all). Roughly a year of real Minecraft updates
  sits between 1.21.1 and 26.x. Class/method names, mixin targets, and
  render-pipeline APIs (`DrawContext`, `MatrixStack`, GUI scissor handling,
  etc.) may well have changed and have not been audited against Mojang's
  official 26.x names. The first real `gradlew.bat build` on a machine with
  JDK 25 and Fabric/Mojang network access is expected to surface genuine
  compile errors per version - that is the next step, not a sign something
  was done wrong here.
