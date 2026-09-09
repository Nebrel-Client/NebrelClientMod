#!/usr/bin/env python3
"""
Validate Nebrel's Minecraft API usage against the Yarn 1.21.1 mappings.

IMPORTANT - what this script can and cannot tell you as of the 26.x multi-
version build (see versions/*/gradle.properties, docs/MULTI_VERSION.md):

Minecraft 26.1 and later ship unobfuscated, with Mojang's own names built in,
and Fabric no longer publishes Yarn mappings for them at all - there is
nothing for this script to check the 26.x targets against. It still clones
and checks against Yarn 1.21.1 (hardcoded fallback below, since
`minecraft_version` no longer lives in the root gradle.properties), which
only tells you whether the source is still internally consistent with the
*original* API it was written against. It is NOT evidence that the code
compiles against 26.1, 26.1.1, 26.1.2 or 26.2's real (Mojang-mapped) API -
those have real, unverified porting risk from ~a year of Minecraft updates
between 1.21.1 and 26.x. Real compilation on a machine with Fabric/Mojang
network access remains the only source of truth for the 26.x targets.

On a machine that cannot reach maven.fabricmc.net there is no way to compile
the game-facing half of the client, so for the 1.21.1-era baseline this script
provided the next best thing: it checks every Minecraft type and member name
the source refers to against the official Yarn mapping files.

It verifies three things:

  1. Every ``import net.minecraft...`` names a class that exists in the mappings.
  2. Every ``@Mixin(Type.class)`` target exists.
  3. Every ``method = "name"`` in a mixin injector exists on the targeted class
     or somewhere in its package's mapping, and every method called on an
     obviously-Minecraft receiver is a name Yarn actually defines.

It is a name-level check, not a type check: it catches invented and misspelled
API, which is the failure mode that matters here. It cannot catch a wrong
argument type. Real compilation remains the source of truth.

Usage:
    tools/check-mappings.py [path/to/yarn/mappings]

If the mappings path is omitted the script clones FabricMC/yarn's 1.21.1
branch into build/yarn.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src" / "main" / "java"

# Method names that come from the JDK, Java itself, Gson, SLF4J, LWJGL, the
# Fabric API or Nebrel's own code. They are not expected in Yarn.
NON_YARN_PREFIXES = (
    "de.nebrel.",
    "java.",
    "javax.",
    "org.slf4j.",
    "org.lwjgl.",
    "org.spongepowered.",
    "com.google.gson.",
    "com.mojang.blaze3d.",
    "net.fabricmc.",
)


def ensure_mappings(argv: list[str]) -> Path:
    if len(argv) > 1:
        path = Path(argv[1]).resolve()
        if not path.is_dir():
            sys.exit(f"mappings directory not found: {path}")
        return path

    # Hardcoded rather than read from gradle.properties: none of the four
    # 26.x version subprojects declare a `minecraft_version` there any more
    # (it now lives per-subproject in versions/*/gradle.properties, and none
    # of those versions have Yarn mappings to check against anyway). 1.21.1
    # is what the source was actually written and last verified against.
    version = "1.21.1"
    target = ROOT / "build" / "yarn"
    mappings = target / "mappings"
    if not mappings.is_dir():
        target.parent.mkdir(parents=True, exist_ok=True)
        print(f"Cloning FabricMC/yarn branch {version} ...")
        result = subprocess.run(
            ["git", "clone", "--depth", "1", "--branch", version,
             "https://github.com/FabricMC/yarn.git", str(target)],
            capture_output=True, text=True)
        if result.returncode != 0:
            sys.exit("could not clone the Yarn mappings:\n" + result.stderr)
    return mappings


def load_mappings(mappings: Path):
    """Returns (class simple names, all member names, per-class member names)."""
    classes: set[str] = set()
    members: set[str] = set()
    per_class: dict[str, set[str]] = defaultdict(set)

    class_pattern = re.compile(r"^(\t*)CLASS\s+\S+(?:\s+(\S+))?")
    member_pattern = re.compile(r"^(\t*)(METHOD|FIELD)\s+\S+\s+(\S+)")

    for path in mappings.rglob("*.mapping"):
        current: list[str] = []
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            class_match = class_pattern.match(line)
            if class_match:
                depth = len(class_match.group(1))
                name = class_match.group(2)
                if name is None:
                    # Unmapped inner class; keep the nesting level consistent.
                    name = "?"
                # A top-level entry maps to a full path
                # ("net/minecraft/client/MinecraftClient"); nested entries map
                # to a bare simple name. Only the simple name is compared.
                simple = name.rsplit("/", 1)[-1]
                current = current[:depth] + [simple]
                classes.add(simple)
                continue

            member_match = member_pattern.match(line)
            if member_match and current:
                name = member_match.group(3)
                if name.startswith("<"):
                    continue
                members.add(name)
                per_class[current[-1]].add(name)

    return classes, members, per_class


def java_sources() -> list[Path]:
    return sorted(SRC.rglob("*.java"))


def check_imports(sources, classes) -> list[str]:
    problems = []
    pattern = re.compile(r"^import\s+(net\.minecraft\.[A-Za-z0-9_.]+);", re.MULTILINE)
    for path in sources:
        text = path.read_text(encoding="utf-8")
        for match in pattern.finditer(text):
            fqn = match.group(1)
            simple = fqn.rsplit(".", 1)[-1]
            if simple not in classes:
                problems.append(f"{rel(path)}: unknown Minecraft class '{fqn}'")
    return problems


def check_mixin_targets(sources, classes, per_class) -> list[str]:
    problems = []
    mixin_pattern = re.compile(r"@Mixin\(\s*([A-Za-z0-9_.]+)\.class\s*\)")
    method_pattern = re.compile(r"method\s*=\s*\"([A-Za-z0-9_$]+)")
    accessor_pattern = re.compile(r"@(?:Accessor|Invoker)\(\s*\"([A-Za-z0-9_]+)\"\s*\)")

    for path in sources:
        if "/mixin/" not in path.as_posix():
            continue
        text = path.read_text(encoding="utf-8")

        targets = [m.group(1).split(".")[-1] for m in mixin_pattern.finditer(text)]
        for target in targets:
            if target not in classes:
                problems.append(f"{rel(path)}: mixin targets unknown class '{target}'")

        allowed: set[str] = set()
        for target in targets:
            allowed |= per_class.get(target, set())
            # Inner classes are recorded under their simple name too.
            for key, names in per_class.items():
                if key == target:
                    allowed |= names

        for match in method_pattern.finditer(text):
            name = match.group(1)
            if not allowed:
                continue
            if name not in allowed:
                problems.append(
                    f"{rel(path)}: injector target '{name}' is not declared on "
                    f"{', '.join(targets)} (it may be inherited; verify by hand)")

        for match in accessor_pattern.finditer(text):
            name = match.group(1)
            if allowed and name not in allowed:
                problems.append(
                    f"{rel(path)}: accessor '{name}' not found on {', '.join(targets)}")

    return problems


def check_called_names(sources, members) -> list[str]:
    """
    Flags method names that look like Minecraft API but do not exist in Yarn.

    Only names called on a receiver are considered, and any name defined
    anywhere in Nebrel's own sources is excluded, as are the JDK names the
    client uses. The goal is to catch an invented method, not to type check.
    """
    problems = []

    own_names: set[str] = set()
    declaration = re.compile(
        r"^\s*(?:public|private|protected|static|final|abstract|synchronized|\s)*"
        r"[\w<>\[\],.?\s]+\s+([a-z][A-Za-z0-9_]*)\s*\(", re.MULTILINE)
    # Record accessors are implicit, so the declaration pattern never sees them.
    # Take the component names out of the header instead.
    record_header = re.compile(r"\brecord\s+\w+\s*\(([^)]*)\)", re.DOTALL)
    component = re.compile(r"([a-z][A-Za-z0-9_]*)\s*(?:,|$)")
    for path in sources:
        text = path.read_text(encoding="utf-8")
        for match in declaration.finditer(text):
            own_names.add(match.group(1))
        for match in record_header.finditer(text):
            for part in match.group(1).split(","):
                trailing = component.search(part.strip())
                if trailing:
                    own_names.add(trailing.group(1))

    # Members of libraries that are not obfuscated and so never appear in Yarn:
    # the JDK, Gson, SLF4J, LWJGL/GLFW, blaze3d, the Mixin API and Fabric Loader.
    library_names = {
        # Gson
        "setPrettyPrinting", "disableHtmlEscaping", "parseReader", "create",
        # java.lang.Runtime
        "getRuntime", "totalMemory", "freeMemory", "maxMemory",
        # GLFW
        "glfwGetMouseButton", "glfwGetMonitors", "glfwGetMonitorPos", "glfwGetPrimaryMonitor",
        "glfwGetVideoMode", "glfwGetWindowAttrib", "glfwSetWindowAttrib",
        "glfwGetWindowPos", "glfwSetWindowPos", "glfwGetWindowSize", "glfwSetWindowSize",
        # JOML
        "rotationX", "rotationY", "rotationZ", "rotateX", "rotateY", "rotateZ",
        # java.lang.Math and java.util
        "toRadians", "toDegrees", "hasNext", "next", "newSetFromMap",
        # blaze3d RenderSystem
        "setShaderFogStart", "setShaderFogEnd", "getShaderFogStart", "getShaderFogEnd",
        "setShaderColor", "enableBlend", "disableBlend", "defaultBlendFunc",
        "enableDepthTest", "disableDepthTest", "depthMask",
        # Mixin callbacks
        "setReturnValue", "cancel", "getReturnValue",
        # Fabric Loader and API, including WorldRenderContext accessors
        "getConfigDir", "getGameDir", "isModLoaded", "register", "registerKeyBinding",
        "consumers", "matrixStack", "camera", "tickCounter", "worldRenderer",
        # java.util.Comparator chaining
        "thenComparing", "thenComparingInt", "reversed",
        # Nebrel's own static registrar, resolved at compile time
        "registerAll",
    }

    jdk_names = library_names | {
        # java.lang / java.util / streams and the handful of library calls used.
        "abs", "accept", "add", "addAll", "addFirst", "addLast", "append", "apply",
        "asList", "ceil", "clear", "clone", "compare", "comparingInt", "computeIfAbsent",
        "contains", "containsKey", "copyOf", "copyOfRange", "cos", "createDirectories",
        "currentTimeMillis", "deleteIfExists", "endsWith", "equals", "exists", "floor",
        "floorMod", "forEach", "format", "get", "getAsBoolean", "getAsDouble", "getAsInt",
        "getAsJsonArray", "getAsJsonObject", "getAsJsonPrimitive", "getAsLong",
        "getAsString", "getAsFloat", "getBytes", "getClass", "getDeclaringClass",
        "getEnumConstants", "getInstance", "getKey", "getMessage", "getName",
        "getOrDefault", "getParent", "getSimpleName", "getValue", "hashCode", "indexOf",
        "info", "isBlank", "isDirectory", "isEmpty", "isFinite", "isInfinite", "isJsonArray",
        "isJsonObject", "isJsonPrimitive", "isNaN", "isNumber", "isPresent", "isRegularFile",
        "iterator", "join", "keySet", "lastIndexOf", "length", "log", "map", "max", "min",
        "move", "nanoTime", "newBufferedReader", "newBufferedWriter", "noneOf", "now",
        "of", "ofNullable", "ofPattern", "orElse", "parseInt", "parseLong", "pollFirst",
        "pow", "put", "putIfAbsent", "readString", "remove", "removeIf", "replace",
        "replaceAll", "resolve", "resolveSibling", "reset", "round", "run", "setLength",
        "signum", "sin", "size", "sort", "split", "sqrt", "startsWith", "stream",
        "substring", "test", "toArray", "toJson", "toLowerCase", "toString", "toUpperCase",
        "trim", "values", "valueOf", "walk", "warn", "error", "writeString", "printStackTrace",
        "charAt", "compareTo", "doubleValue", "floatValue", "intValue", "longValue",
        "matches", "entrySet", "sorted", "collect", "filter", "count", "anyMatch",
        "unmodifiableSet", "unmodifiableList", "addProperty", "toMillis", "setValue",
        "getAsFloat", "sleep", "arraycopy", "deepToString", "getDuration",
        "isISOControl", "isWhitespace", "ordinal", "repeat", "codePointAt",
        "computeIfPresent", "getOrCreate", "toUnmodifiableList",
        # java.net.http, java.time and Gson's JsonParser - RemoteEntitlementProvider
        "connectTimeout", "followRedirects", "sendAsync", "thenAccept",
        "exceptionally", "whenComplete", "ofSeconds", "parseString",
        "unmodifiableMap", "newBuilder", "timeout",
    }

    # Calls on a receiver: something.name(
    call_pattern = re.compile(r"\.\s*([a-z][A-Za-z0-9_]*)\s*\(")

    unknown: dict[str, set[str]] = defaultdict(set)
    for path in sources:
        text = path.read_text(encoding="utf-8")
        # Strip comments so prose does not produce findings.
        text = re.sub(r"//.*", "", text)
        text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
        for match in call_pattern.finditer(text):
            name = match.group(1)
            if name in members or name in own_names or name in jdk_names:
                continue
            unknown[name].add(rel(path))

    for name in sorted(unknown):
        where = ", ".join(sorted(unknown[name])[:3])
        problems.append(f"unrecognised method name '{name}' (used in {where})")
    return problems


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))


def main() -> int:
    mappings = ensure_mappings(sys.argv)
    classes, members, per_class = load_mappings(mappings)
    print(f"Loaded {len(classes)} classes and {len(members)} member names "
          f"from {mappings.relative_to(ROOT) if mappings.is_relative_to(ROOT) else mappings}")

    sources = java_sources()
    print(f"Checking {len(sources)} source files\n")

    problems: list[str] = []
    problems += check_imports(sources, classes)
    problems += check_mixin_targets(sources, classes, per_class)
    problems += check_called_names(sources, members)

    if not problems:
        print("OK  every Minecraft type and member name resolves against the mappings")
        return 0

    print(f"{len(problems)} finding(s):\n")
    for problem in problems:
        print(f"  {problem}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
