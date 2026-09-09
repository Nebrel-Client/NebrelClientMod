import com.google.gson.JsonObject;
import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.config.ConfigFile;
import de.nebrel.client.config.ConfigManager;
import de.nebrel.client.config.KeybindConfigSection;
import de.nebrel.client.config.ModuleConfigSection;
import de.nebrel.client.config.ThemeConfigSection;
import de.nebrel.client.gui.theme.ThemeManager;
import de.nebrel.client.gui.theme.Themes;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.render.animation.SmoothScroll;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.RangeSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.StringSetting;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.plus.EntitlementProvider;
import de.nebrel.client.plus.EntitlementService;
import de.nebrel.client.plus.LocalEntitlementProvider;
import de.nebrel.client.plus.NebrelEntitlement;
import de.nebrel.client.plus.PlusSettings;
import de.nebrel.client.plus.RemoteEntitlementProvider;
import de.nebrel.client.render.icon.Icons;
import de.nebrel.client.render.icon.PixelIcon;
import de.nebrel.client.plus.badge.BadgeService;
import de.nebrel.client.plus.badge.NebrelBadge;
import de.nebrel.client.plus.nametag.AdditionalNametag;
import de.nebrel.client.plus.nametag.EffectStage;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagEffectPipeline;
import de.nebrel.client.plus.nametag.NametagProfile;
import de.nebrel.client.plus.nametag.NametagRenderContext;
import de.nebrel.client.plus.profile.NebrelPlayerProfile;
import de.nebrel.client.plus.profile.PlayerProfileCache;
import de.nebrel.client.plus.render.GlyphSink;
import de.nebrel.client.plus.render.IdentityRenderer;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless verification for the Minecraft-independent half of Nebrel Client.
 *
 * <p>Run it with {@code tools/verify-core.sh}. It exists because the settings,
 * config, theme and animation layers carry the behaviour that is easiest to get
 * subtly wrong (clamping, migration, conditional visibility, colour maths) and
 * hardest to notice in game.</p>
 */
public final class CoreSelfTest {

    private static int checks;
    private static int failures;

    // -- a throwaway module used as the subject under test --------------------

    enum Style { SIMPLE, CROSS, DOT_ONLY }

    enum Target { PLAYERS, MOBS, ANIMALS }

    static final class SampleModule extends Module {
        final BooleanSetting rainbow;
        final NumberSetting rainbowSpeed;
        final NumberSetting gap;
        final EnumSetting<Style> style;
        final ColorSetting color;
        final StringSetting label;
        final MultiSelectSetting<Target> targets;
        final RangeSetting distance;

        SampleModule() {
            super("sample", "Sample Crosshair", "A test module for the core self test",
                    ModuleCategory.VISUAL, "+");
            this.style = choice("style", "Style", "Crosshair shape", Style.SIMPLE);
            this.gap = number("gap", "Gap", "Space in the middle", 2.0D, 0.0D, 10.0D, 0.5D);
            this.rainbow = bool("rainbow", "Rainbow", "Cycle the colour", false);
            this.rainbowSpeed = number("rainbowSpeed", "Rainbow Speed", "Cycles per second",
                    0.5D, 0.05D, 3.0D, 0.05D);
            this.rainbowSpeed.visibleWhen(this.rainbow);
            this.color = color("color", "Color", "Main colour", 0xFFFFFFFF, true);
            this.label = text("label", "Label", "Text under the crosshair", "", 32);
            this.targets = multi("targets", "Targets", "Which entities count",
                    Target.class, Target.PLAYERS, Target.MOBS);
            this.distance = range("distance", "Distance", "Visible distance band",
                    4.0D, 32.0D, 0.0D, 64.0D, 1.0D);
        }
    }

    static final class OtherModule extends Module {
        OtherModule() {
            super("nofog", "No Fog", "Removes terrain fog", ModuleCategory.RENDER, "~");
            bool("terrain", "Terrain Fog", "Remove distance fog", true);
        }
    }

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("nebrel-core-test");
        try {
            testMath();
            testColor();
            testEasing();
            testAnimation();
            testScroll();
            testNumberSetting();
            testColorSetting();
            testEnumAndMultiSetting();
            testRangeSetting();
            testStringAndKeybind();
            testConditionalVisibility();
            testModuleLifecycle();
            testModuleManager();
            testSearch();
            testAnchors();
            testThemes();
            testConfigRoundTrip(temp);
            testConfigResilience(temp);
            testClientSettings();
            testEntitlements();
            testBadgeService();
            testAdditionalNametagSanitising();
            testEffectPipeline();
            testIdentityRendering();
            testPlusConfigRoundTrip(temp);
            testRemoteEntitlementProvider();
            testPixelIcons();
        } finally {
            deleteTree(temp);
        }

        System.out.println();
        System.out.println(failures == 0
                ? "PASS  " + checks + " checks"
                : "FAIL  " + failures + " of " + checks + " checks failed");
        System.exit(failures == 0 ? 0 : 1);
    }

    // -- individual areas ----------------------------------------------------

    static void testMath() {
        section("math");
        check("clamp low", NebrelMath.clamp(-5.0D, 0.0D, 10.0D) == 0.0D);
        check("clamp high", NebrelMath.clamp(50.0D, 0.0D, 10.0D) == 10.0D);
        check("lerp midpoint", NebrelMath.lerp(0.0D, 10.0D, 0.5D) == 5.0D);
        check("snap to step", NebrelMath.snap(2.3D, 0.5D) == 2.5D);
        check("map range", NebrelMath.map(5.0D, 0.0D, 10.0D, 0.0D, 100.0D) == 50.0D);
        check("inside true", NebrelMath.inside(5, 5, 0, 0, 10, 10));
        check("inside right edge exclusive", !NebrelMath.inside(10, 5, 0, 0, 10, 10));
        check("round 2dp", NebrelMath.round(1.23456D, 2) == 1.23D);
        // approach must converge and never overshoot
        float v = 0.0F;
        for (int i = 0; i < 200; i++) {
            v = NebrelMath.approach(v, 1.0F, 0.4F, 1.0F / 60.0F);
        }
        check("approach converges", Math.abs(v - 1.0F) < 0.001F);
        check("approach never overshoots", v <= 1.0F);
    }

    static void testColor() {
        section("color");
        int c = ColorUtil.argb(200, 10, 20, 30);
        check("alpha channel", ColorUtil.alpha(c) == 200);
        check("red channel", ColorUtil.red(c) == 10);
        check("green channel", ColorUtil.green(c) == 20);
        check("blue channel", ColorUtil.blue(c) == 30);
        check("withAlpha keeps rgb", ColorUtil.withAlpha(c, 50) == ColorUtil.argb(50, 10, 20, 30));
        check("fadeAlpha halves", ColorUtil.alpha(ColorUtil.fadeAlpha(c, 0.5F)) == 100);
        check("lerp endpoints", ColorUtil.lerp(0xFF000000, 0xFFFFFFFF, 1.0F) == 0xFFFFFFFF);
        check("lerp midpoint grey", ColorUtil.red(ColorUtil.lerp(0xFF000000, 0xFFFFFFFF, 0.5F)) == 128);

        // hsb round trip
        int original = 0xFF3B82F6;
        float[] hsb = ColorUtil.rgbToHsb(original);
        int back = ColorUtil.hsbToRgb(hsb[0], hsb[1], hsb[2]);
        check("hsb round trip red", Math.abs(ColorUtil.red(back) - ColorUtil.red(original)) <= 1);
        check("hsb round trip green", Math.abs(ColorUtil.green(back) - ColorUtil.green(original)) <= 1);
        check("hsb round trip blue", Math.abs(ColorUtil.blue(back) - ColorUtil.blue(original)) <= 1);

        check("parse #rrggbb", ColorUtil.parseHex("#3B82F6", 0) == 0xFF3B82F6);
        check("parse #aarrggbb", ColorUtil.parseHex("#803B82F6", 0) == 0x803B82F6);
        check("parse garbage falls back", ColorUtil.parseHex("not a colour", 0x1234) == 0x1234);
        check("parse null falls back", ColorUtil.parseHex(null, 7) == 7);
        check("toHex round trip", ColorUtil.parseHex(ColorUtil.toHex(0x803B82F6), 0) == 0x803B82F6);
        check("white is bright", ColorUtil.luminance(0xFFFFFFFF) > 0.99F);
        check("black is dark", ColorUtil.luminance(0xFF000000) < 0.01F);
        check("readable on white is dark", ColorUtil.luminance(ColorUtil.readableTextOn(0xFFFFFFFF)) < 0.3F);
        check("readable on black is light", ColorUtil.luminance(ColorUtil.readableTextOn(0xFF000000)) > 0.7F);
        // rainbow must stay in gamut and actually move
        int r1 = ColorUtil.rainbow(0L, 1.0F, 0.0F);
        int r2 = ColorUtil.rainbow(300L, 1.0F, 0.0F);
        check("rainbow advances", r1 != r2);
        check("rainbow is opaque", ColorUtil.alpha(r1) == 255);
    }

    static void testEasing() {
        section("easing");
        for (Easing easing : Easing.values()) {
            check(easing.name() + " starts at 0", Math.abs(easing.apply(0.0F)) < 0.001F);
            check(easing.name() + " ends at 1", Math.abs(easing.apply(1.0F) - 1.0F) < 0.001F);
        }
        check("smoothstep midpoint", Math.abs(Easing.SMOOTHSTEP.apply(0.5F) - 0.5F) < 0.001F);
        check("ease out is ahead of linear", Easing.EASE_OUT.apply(0.25F) > 0.25F);
    }

    static void testAnimation() throws Exception {
        section("animation");
        Animation animation = new Animation(0.0F, 60L, Easing.LINEAR);
        check("starts at initial", animation.value() == 0.0F);
        check("starts finished", animation.finished());
        animation.animateTo(1.0F);
        check("running after retarget", !animation.finished());
        check("target recorded", animation.target() == 1.0F);
        Thread.sleep(120L);
        check("reaches target", animation.value() == 1.0F);
        check("finished after duration", animation.finished());

        // Interrupting mid flight must continue from the current value.
        Animation interrupted = new Animation(0.0F, 400L, Easing.LINEAR);
        interrupted.animateTo(1.0F);
        Thread.sleep(80L);
        float mid = interrupted.value();
        check("mid flight is between", mid > 0.0F && mid < 1.0F);
        interrupted.animateTo(0.0F);
        check("reversal starts near current", Math.abs(interrupted.value() - mid) < 0.1F);

        Animation snapped = new Animation(0.0F, 200L, Easing.LINEAR);
        snapped.set(0.7F);
        check("set is immediate", snapped.value() == 0.7F && snapped.finished());
        snapped.animateTo(true);
        check("boolean retarget", snapped.target() == 1.0F);
    }

    static void testScroll() throws Exception {
        section("scroll");
        SmoothScroll scroll = new SmoothScroll();
        scroll.updateBounds(500.0F, 200.0F);
        check("max offset", scroll.maxOffset() == 300.0F);
        check("scrollable", scroll.scrollable());

        // The chase is driven by wall-clock time, so settling needs real time
        // to pass. A tight loop would not advance it at all, which is exactly
        // the frame-rate independence the class is supposed to have.
        scroll.scroll(-1.0D, 40.0F);
        settle(scroll);
        check("scrolled down by a notch", Math.abs(scroll.offset() - 40.0F) < 1.0F);
        check("progress reflects position",
                Math.abs(scroll.progress() - 40.0F / 300.0F) < 0.02F);

        // Scrolling past the end must clamp, not run away.
        for (int i = 0; i < 50; i++) {
            scroll.scroll(-1.0D, 40.0F);
        }
        settle(scroll);
        check("clamped at bottom", scroll.offset() <= 300.0F + 0.01F);
        check("progress at bottom", scroll.progress() > 0.99F);

        for (int i = 0; i < 100; i++) {
            scroll.scroll(1.0D, 40.0F);
        }
        settle(scroll);
        check("clamped at top", scroll.offset() >= -0.01F);

        // Many tiny deltas (a trackpad) must reach the same place as one big one.
        SmoothScroll trackpad = new SmoothScroll();
        trackpad.updateBounds(500.0F, 200.0F);
        for (int i = 0; i < 40; i++) {
            trackpad.scroll(-0.1D, 40.0F);
        }
        settle(trackpad);
        check("trackpad deltas accumulate", Math.abs(trackpad.offset() - 160.0F) < 1.0F);

        // Shrinking the content must pull the offset back into range.
        scroll.scroll(-20.0D, 40.0F);
        scroll.updateBounds(210.0F, 200.0F);
        check("bounds shrink clamps", scroll.offset() <= 10.0F + 0.01F);
        scroll.updateBounds(100.0F, 200.0F);
        check("content shorter than viewport", !scroll.scrollable() && scroll.maxOffset() == 0.0F);
        check("progress is zero when not scrollable", scroll.progress() == 0.0F);

        scroll.updateBounds(500.0F, 200.0F);
        scroll.scroll(-3.0D, 40.0F);
        scroll.reset();
        settle(scroll);
        check("reset returns to the top", scroll.offset() == 0.0F);

        // Non-smooth mode must report the target directly.
        SmoothScroll instant = new SmoothScroll();
        instant.setSmoothing(false);
        instant.updateBounds(500.0F, 200.0F);
        instant.scroll(-1.0D, 40.0F);
        check("smoothing off jumps immediately", instant.offset() == 40.0F);
        instant.scrollTo(1000.0F);
        check("scrollTo clamps to max", instant.offset() == 300.0F);
        instant.scrollTo(-50.0F);
        check("scrollTo clamps to zero", instant.offset() == 0.0F);
        instant.scrollTo(120.0F);
        check("target reported", instant.target() == 120.0F);
        instant.setSmoothing(true);
        check("re-enabling smoothing keeps the target", instant.target() == 120.0F);
    }

    /** Advances a scroller over real time until it stops moving. */
    static void settle(SmoothScroll scroll) throws Exception {
        for (int i = 0; i < 60; i++) {
            scroll.offset();
            Thread.sleep(8L);
        }
    }

    static void testNumberSetting() {
        section("NumberSetting");
        NumberSetting setting = new NumberSetting("t.n", "N", "", 5.0D, 0.0D, 10.0D, 0.5D);
        check("default applied", setting.get() == 5.0D);
        setting.set(20.0D);
        check("clamped to max", setting.get() == 10.0D);
        setting.set(-4.0D);
        check("clamped to min", setting.get() == 0.0D);
        setting.set(2.3D);
        check("snapped to step", setting.get() == 2.5D);
        setting.set(Double.NaN);
        check("NaN rejected", setting.get() == 5.0D);
        setting.set(null);
        check("null rejected", setting.get() == 5.0D);
        setting.setFraction(0.5D);
        check("fraction to value", setting.get() == 5.0D);
        check("value to fraction", Math.abs(setting.fraction() - 0.5F) < 0.001F);
        check("getInt rounds", setting.getInt() == 5);
        check("decimals from step", setting.decimals() == 1);
        setting.unit("%");
        check("display with unit", setting.displayValue().equals("5.0%"));
        check("isDefault", setting.isDefault());
        setting.set(1.0D);
        check("not default after change", !setting.isDefault());
        setting.reset();
        check("reset restores default", setting.get() == 5.0D);

        // Change listeners must fire once per real change only.
        int[] fired = {0};
        NumberSetting listened = new NumberSetting("t.n2", "N2", "", 1.0D, 0.0D, 10.0D, 1.0D);
        listened.onChange(value -> fired[0]++);
        listened.set(2.0D);
        listened.set(2.0D);
        check("listener fires once", fired[0] == 1);

        // Integer-stepped settings should report no decimals.
        NumberSetting whole = new NumberSetting("t.n3", "N3", "", 3.0D, 0.0D, 10.0D, 1.0D);
        check("integer display", whole.displayValue().equals("3"));
    }

    static void testColorSetting() {
        section("ColorSetting");
        ColorSetting alphaCapable = new ColorSetting("t.c", "C", "", 0x80FF0000, true);
        check("alpha preserved", alphaCapable.get() == 0x80FF0000);
        ColorSetting opaqueOnly = new ColorSetting("t.c2", "C2", "", 0x00FF0000, false);
        check("alpha forced opaque", opaqueOnly.get() == 0xFFFF0000);
        opaqueOnly.set(0x10203040);
        check("alpha forced on set", ColorUtil.alpha(opaqueOnly.get()) == 255);

        alphaCapable.setAlpha(0x40);
        check("setAlpha keeps rgb", alphaCapable.get() == 0x40FF0000);
        alphaCapable.setRgb(0xFF00FF00);
        check("setRgb keeps alpha", alphaCapable.get() == 0x4000FF00);

        check("no rainbow returns value", alphaCapable.effective() == alphaCapable.get());
        alphaCapable.setRainbow(true);
        check("rainbow keeps alpha", ColorUtil.alpha(alphaCapable.effective()) == 0x40);
        alphaCapable.setRainbow(false);
        check("rainbow off restores exact colour", alphaCapable.effective() == 0x4000FF00);

        check("serialises as hex", alphaCapable.write().getAsString().equals("#4000FF00"));
    }

    static void testEnumAndMultiSetting() {
        section("EnumSetting / MultiSelectSetting");
        EnumSetting<Style> style = new EnumSetting<>("t.e", "E", "", Style.SIMPLE);
        check("options complete", style.options().size() == 3);
        style.cycle();
        check("cycle advances", style.get() == Style.CROSS);
        style.cycle();
        style.cycle();
        check("cycle wraps", style.get() == Style.SIMPLE);
        style.select(2);
        check("select by index", style.get() == Style.DOT_ONLY);
        style.select(99);
        check("out of range index ignored", style.get() == Style.DOT_ONLY);
        check("label humanised", EnumSetting.label(Style.DOT_ONLY).equals("Dot Only"));
        check("is() works", style.is(Style.DOT_ONLY));

        MultiSelectSetting<Target> targets =
                new MultiSelectSetting<>("t.m", "M", "", Target.class, Target.PLAYERS);
        check("initial membership", targets.has(Target.PLAYERS) && !targets.has(Target.MOBS));
        targets.toggle(Target.MOBS);
        check("toggle adds", targets.has(Target.MOBS));
        targets.toggle(Target.PLAYERS);
        check("toggle removes", !targets.has(Target.PLAYERS));
        check("display counts", targets.displayValue().equals("Mobs"));
        targets.selectAll();
        check("select all", targets.displayValue().equals("All"));
        targets.clear();
        check("clear", targets.displayValue().equals("None"));
        targets.toggle(Target.MOBS);
        targets.toggle(Target.ANIMALS);
        check("two selected label", targets.displayValue().equals("2 selected"));
        check("labels ordered by declaration",
                targets.selectedLabels().equals(List.of("Mobs", "Animals")));
    }

    static void testRangeSetting() {
        section("RangeSetting");
        RangeSetting range = new RangeSetting("t.r", "R", "", 4.0D, 32.0D, 0.0D, 64.0D, 1.0D);
        check("low", range.low() == 4.0D);
        check("high", range.high() == 32.0D);
        check("contains inside", range.contains(10.0D));
        check("excludes outside", !range.contains(40.0D));
        range.setHigh(100.0D);
        check("high clamped", range.high() == 64.0D);
        // A crossed drag must reorder, not corrupt the pair.
        range.setLow(80.0D);
        check("crossed pair reordered", range.low() <= range.high());
        range.set(new double[]{50.0D, 10.0D});
        check("explicit crossed set reordered", range.low() == 10.0D && range.high() == 50.0D);
        check("display", range.displayValue().equals("10 - 50"));
        range.reset();
        check("reset", range.low() == 4.0D && range.high() == 32.0D);
        check("isDefault after reset", range.isDefault());
    }

    static void testStringAndKeybind() {
        section("StringSetting / KeybindSetting");
        StringSetting text = new StringSetting("t.s", "S", "", "hello", 8);
        text.set("this is far too long");
        check("truncated to max length", text.get().length() == 8);
        text.set(null);
        check("null falls back to default", text.get().equals("hello"));
        text.set("");
        check("blank detected", text.isBlank());

        KeybindSetting bind = new KeybindSetting("t.k", "K", "", KeybindSetting.UNBOUND);
        check("unbound by default", !bind.bound());
        check("unbound label", bind.displayValue().equals("None"));
        bind.set(344);
        check("bound", bind.bound());
        check("right shift name", bind.displayValue().equals("Right Shift"));
        check("matches", bind.matches(344) && !bind.matches(345));
        bind.clear();
        check("cleared", !bind.bound());
        check("letter key name", KeybindSetting.keyName(67).equals("C"));
        check("f key name", KeybindSetting.keyName(290).equals("F1"));
        check("digit key name", KeybindSetting.keyName(53).equals("5"));
        check("keybind lands in keybind section",
                bind.section().equals(de.nebrel.client.setting.SettingSection.KEYBIND));
    }

    static void testConditionalVisibility() {
        section("conditional visibility");
        SampleModule module = new SampleModule();
        check("dependent hidden when off", !module.rainbowSpeed.visible());
        module.rainbow.set(true);
        check("dependent shown when on", module.rainbowSpeed.visible());
        check("visible list excludes hidden",
                module.visibleSettings().contains(module.rainbowSpeed));
        module.rainbow.set(false);
        List<Setting<?>> visible = module.visibleSettings();
        check("hidden setting dropped from list", !visible.contains(module.rainbowSpeed));
        check("other settings still visible", visible.contains(module.gap));
        // A hidden setting must keep its value.
        module.rainbowSpeed.set(2.0D);
        check("hidden setting keeps value", module.rainbowSpeed.get() == 2.0D);
    }

    static void testModuleLifecycle() {
        section("module lifecycle");
        int[] enables = {0};
        int[] disables = {0};
        Module module = new Module("lifecycle", "Lifecycle", "", ModuleCategory.MISC, "L") {
            @Override
            protected void onEnable() {
                enables[0]++;
            }

            @Override
            protected void onDisable() {
                disables[0]++;
            }
        };
        check("starts disabled", !module.enabled());
        module.setEnabled(true);
        check("onEnable ran", enables[0] == 1);
        module.setEnabled(true);
        check("redundant enable is a no-op", enables[0] == 1);
        module.toggle();
        check("onDisable ran", disables[0] == 1);
        check("keybind auto registered", module.keybind() != null);
        check("no configurable settings", !module.hasConfigurableSettings());

        SampleModule sample = new SampleModule();
        check("sample has settings", sample.hasConfigurableSettings());
        sample.gap.set(7.0D);
        sample.resetSettings();
        check("resetSettings restores defaults", sample.gap.get() == 2.0D);

        // A module must not be able to claim a navigation-only category.
        boolean rejected = false;
        try {
            new Module("bad", "Bad", "", ModuleCategory.ALL, "X") {
            };
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check("virtual category rejected", rejected);
    }

    static void testModuleManager() {
        section("ModuleManager");
        ModuleManager manager = new ModuleManager();
        SampleModule sample = manager.register(new SampleModule());
        OtherModule other = manager.register(new OtherModule());

        check("size", manager.size() == 2);
        check("getById", manager.getById("sample").orElse(null) == sample);
        check("getById missing", manager.getById("nope").isEmpty());
        check("getByClass", manager.getByClass(OtherModule.class) == other);
        check("category listing", manager.getByCategory(ModuleCategory.VISUAL).equals(List.of(sample)));
        check("ALL returns everything", manager.getByCategory(ModuleCategory.ALL).size() == 2);
        check("empty category", manager.getByCategory(ModuleCategory.PLAYER).isEmpty());

        check("no modules enabled yet", manager.getEnabledModules().isEmpty());
        manager.enable("sample");
        check("enabled view updated", manager.getEnabledModules().equals(List.of(sample)));
        manager.toggle("sample");
        check("toggle disables", manager.getEnabledModules().isEmpty());

        check("no favourites yet", manager.getFavorites().isEmpty());
        manager.setFavorite(other, true);
        check("favourites view", manager.getFavorites().equals(List.of(other)));
        check("FAVORITES category maps to favourites",
                manager.getByCategory(ModuleCategory.FAVORITES).equals(List.of(other)));

        int[] changes = {0};
        manager.setChangeListener(() -> changes[0]++);
        manager.enable("sample");
        manager.setFavorite(sample, true);
        check("change listener fired twice", changes[0] == 2);
        manager.enable("sample");
        check("redundant enable does not notify", changes[0] == 2);

        // Duplicate ids must be rejected loudly rather than silently shadowing.
        boolean duplicateRejected = false;
        try {
            manager.register(new SampleModule());
        } catch (IllegalStateException expected) {
            duplicateRejected = true;
        }
        check("duplicate id rejected", duplicateRejected);

        manager.unregister("nofog");
        check("unregister removes", manager.size() == 1);
    }

    static void testSearch() {
        section("search");
        ModuleManager manager = new ModuleManager();
        manager.register(new SampleModule());
        manager.register(new OtherModule());

        check("blank query returns all", manager.search("").size() == 2);
        check("null query returns all", manager.search(null).size() == 2);
        check("prefix matches name", manager.search("cross").size() == 1);
        check("matches description", manager.search("terrain fog").size() == 1);
        check("matches setting name", manager.search("rainbow").size() == 1);
        check("case insensitive", manager.search("CROSS").size() == 1);
        check("trims whitespace", manager.search("  fog  ").size() == 1);
        check("no match returns empty", manager.search("zzzzz").isEmpty());
        check("scoped to category",
                manager.search(ModuleCategory.RENDER, "fog").size() == 1);
        check("scoped category excludes others",
                manager.search(ModuleCategory.VISUAL, "fog").isEmpty());
    }

    static void testAnchors() {
        section("HudAnchor");
        float screenW = 1920.0F;
        float screenH = 1080.0F;
        float w = 100.0F;
        float h = 20.0F;

        // Round trip: pixels to offset and back must be lossless.
        for (HudAnchor anchor : HudAnchor.values()) {
            float ox = anchor.toOffsetX(300.0F, screenW, w);
            float oy = anchor.toOffsetY(200.0F, screenH, h);
            float x = anchor.resolveX(ox, screenW, w);
            float y = anchor.resolveY(oy, screenH, h);
            check(anchor.name() + " x round trip", Math.abs(x - 300.0F) < 0.01F);
            check(anchor.name() + " y round trip", Math.abs(y - 200.0F) < 0.01F);
        }

        // The point of anchoring: a widget 10 px from the bottom-right corner
        // stays exactly 10 px from that corner on a different resolution.
        HudAnchor br = HudAnchor.BOTTOM_RIGHT;
        float ox = br.toOffsetX(screenW - w - 10.0F, screenW, w);
        float oy = br.toOffsetY(screenH - h - 10.0F, screenH, h);
        float smallW = 1280.0F;
        float smallH = 720.0F;
        float newLeft = br.resolveX(ox, smallW, w);
        float newTop = br.resolveY(oy, smallH, h);
        check("bottom right keeps its x margin",
                Math.abs((smallW - newLeft - w) - 10.0F) < 0.01F);
        check("bottom right keeps its y margin",
                Math.abs((smallH - newTop - h) - 10.0F) < 0.01F);

        // Top-left keeps its margin trivially; centre keeps its relative offset.
        float tlOffset = HudAnchor.TOP_LEFT.toOffsetX(12.0F, screenW, w);
        check("top left margin preserved",
                Math.abs(HudAnchor.TOP_LEFT.resolveX(tlOffset, smallW, w) - 12.0F) < 0.01F);
        float centreOffset = HudAnchor.CENTER.toOffsetX(screenW / 2.0F - w / 2.0F, screenW, w);
        check("centre anchor has zero offset when centred", Math.abs(centreOffset) < 0.01F);
        check("centre stays centred after resize",
                Math.abs(HudAnchor.CENTER.resolveX(centreOffset, smallW, w)
                        - (smallW / 2.0F - w / 2.0F)) < 0.01F);

        // Re-anchoring on drop must not move the widget.
        float[] moved = HudAnchor.reanchor(HudAnchor.TOP_LEFT, HudAnchor.BOTTOM_RIGHT,
                1500.0F, 900.0F, screenW, screenH, w, h);
        check("reanchor keeps x in place",
                Math.abs(HudAnchor.BOTTOM_RIGHT.resolveX(moved[0], screenW, w) - 1500.0F) < 0.01F);
        check("reanchor keeps y in place",
                Math.abs(HudAnchor.BOTTOM_RIGHT.resolveY(moved[1], screenH, h) - 900.0F) < 0.01F);

        check("nearest top left", HudAnchor.nearest(5.0F, 5.0F, screenW, screenH) == HudAnchor.TOP_LEFT);
        check("nearest centre",
                HudAnchor.nearest(screenW / 2.0F, screenH / 2.0F, screenW, screenH) == HudAnchor.CENTER);
        check("nearest bottom right",
                HudAnchor.nearest(screenW - 5.0F, screenH - 5.0F, screenW, screenH) == HudAnchor.BOTTOM_RIGHT);
    }

    static void testThemes() {
        section("themes");
        ThemeManager themes = new ThemeManager();
        check("starts dark", themes.baseTheme().dark());
        check("default accent", themes.accent() == Themes.DEFAULT_ACCENT);
        check("accent applied to active", themes.current().accent == Themes.DEFAULT_ACCENT);

        themes.setAccent(0xFF3B82F6);
        check("accent changed", themes.current().accent == 0xFF3B82F6);
        check("hover derived and lighter",
                ColorUtil.luminance(themes.current().accentHover)
                        > ColorUtil.luminance(themes.current().accent));
        check("pressed derived and darker",
                ColorUtil.luminance(themes.current().accentPressed)
                        < ColorUtil.luminance(themes.current().accent));
        check("soft accent is translucent", ColorUtil.alpha(themes.current().accentSoft) < 64);
        check("surfaces untouched by accent", themes.current().surface == Themes.DARK.surface);

        themes.toggleDarkLight();
        check("toggled to light", !themes.baseTheme().dark());
        check("accent survives theme switch", themes.current().accent == 0xFF3B82F6);
        check("light surface is bright", ColorUtil.luminance(themes.current().surface) > 0.8F);
        check("cross fade running", themes.transitioning());
        check("blend mixes towards target",
                themes.blend(0xFF000000, 0xFFFFFFFF) != 0xFFFFFFFF);

        themes.restore("nebrel_dark", Themes.DEFAULT_ACCENT);
        check("restore applies theme", themes.baseTheme().dark());
        check("restore does not animate", !themes.transitioning());
        check("unknown theme id falls back", Themes.byId("does_not_exist") == Themes.DARK);

        // Dark theme must not be pure black, and must have distinct layers.
        check("dark background is not pure black", Themes.DARK.background != 0xFF000000);
        check("surface layers are distinct",
                Themes.DARK.background != Themes.DARK.surface
                        && Themes.DARK.surface != Themes.DARK.surfaceElevated
                        && Themes.DARK.surfaceElevated != Themes.DARK.surfaceHover);
        check("dark layers get progressively lighter",
                ColorUtil.luminance(Themes.DARK.background) < ColorUtil.luminance(Themes.DARK.surface)
                        && ColorUtil.luminance(Themes.DARK.surface)
                        < ColorUtil.luminance(Themes.DARK.surfaceElevated));
        check("dark text contrasts with surface",
                ColorUtil.luminance(Themes.DARK.textPrimary) - ColorUtil.luminance(Themes.DARK.surface) > 0.5F);
        check("light text contrasts with surface",
                ColorUtil.luminance(Themes.LIGHT.surface) - ColorUtil.luminance(Themes.LIGHT.textPrimary) > 0.5F);
    }

    static void testConfigRoundTrip(Path root) {
        section("config round trip");
        ModuleManager manager = new ModuleManager();
        SampleModule sample = manager.register(new SampleModule());
        manager.register(new OtherModule());
        ThemeManager themes = new ThemeManager();

        ConfigManager config = new ConfigManager(root);
        config.register(new ModuleConfigSection(manager))
                .register(new KeybindConfigSection(manager))
                .register(new ThemeConfigSection(themes));
        config.load();

        // Change everything we can.
        manager.enable("sample");
        manager.setFavorite(sample, true);
        sample.gap.set(4.5D);
        sample.style.set(Style.DOT_ONLY);
        sample.color.set(0x80123456);
        sample.rainbow.set(true);
        sample.label.set("test label");
        sample.targets.selectAll();
        sample.distance.set(new double[]{8.0D, 40.0D});
        sample.keybind().set(67);
        themes.setAccent(0xFF3B82F6);
        themes.setTheme(Themes.LIGHT);

        config.markDirty();
        config.saveNow();
        check("modules.json written", Files.isRegularFile(root.resolve("nebrelclient/modules.json")));
        check("keybinds.json written", Files.isRegularFile(root.resolve("nebrelclient/keybinds.json")));
        check("theme.json written", Files.isRegularFile(root.resolve("nebrelclient/theme.json")));
        check("saved flag cleared", !config.dirty());

        // Reload into fresh objects and confirm every value survived.
        ModuleManager manager2 = new ModuleManager();
        SampleModule sample2 = manager2.register(new SampleModule());
        manager2.register(new OtherModule());
        ThemeManager themes2 = new ThemeManager();
        ConfigManager config2 = new ConfigManager(root);
        config2.register(new ModuleConfigSection(manager2))
                .register(new KeybindConfigSection(manager2))
                .register(new ThemeConfigSection(themes2));
        config2.load();

        check("enabled restored without callback", sample2.enabled());
        check("enabled view still empty before activation", manager2.getEnabledModules().isEmpty());
        manager2.activateLoadedModules();
        check("enabled view populated after activation",
                manager2.getEnabledModules().size() == 1);
        check("favourite restored", sample2.favorite());
        check("number restored", sample2.gap.get() == 4.5D);
        check("enum restored", sample2.style.get() == Style.DOT_ONLY);
        check("color restored", sample2.color.get() == 0x80123456);
        check("boolean restored", sample2.rainbow.get());
        check("string restored", sample2.label.get().equals("test label"));
        check("multi select restored", sample2.targets.has(Target.ANIMALS));
        check("range restored", sample2.distance.low() == 8.0D && sample2.distance.high() == 40.0D);
        check("keybind restored", sample2.keybind().get() == 67);
        check("theme restored", !themes2.baseTheme().dark());
        check("accent restored", themes2.accent() == 0xFF3B82F6);

        // Keybinds must live in keybinds.json, not modules.json.
        String modulesJson = read(root.resolve("nebrelclient/modules.json"));
        check("keybind not in modules.json", !modulesJson.contains("sample.keybind"));
        String keybindsJson = read(root.resolve("nebrelclient/keybinds.json"));
        check("keybind is in keybinds.json", keybindsJson.contains("sample.keybind"));
        check("version stamped", modulesJson.contains("\"configVersion\""));
    }

    static void testConfigResilience(Path root) {
        section("config resilience");
        Path dir = root.resolve("nebrelclient");

        // 1. Corrupt file must not throw, and must be quarantined.
        write(dir.resolve("modules.json"), "{ this is not valid json");
        ModuleManager manager = new ModuleManager();
        SampleModule sample = manager.register(new SampleModule());
        ConfigManager config = new ConfigManager(root);
        config.register(new ModuleConfigSection(manager));
        config.load();
        check("corrupt file falls back to defaults", sample.gap.get() == 2.0D);
        check("corrupt file quarantined", Files.isRegularFile(dir.resolve("modules.json.broken")));

        // 2. Missing keys keep defaults; unknown keys are ignored.
        write(dir.resolve("modules.json"),
                "{\"configVersion\":1,\"modules\":{\"sample\":{\"enabled\":true,"
                        + "\"settings\":{\"sample.gap\":9.0,\"sample.doesNotExist\":42}}},"
                        + "\"unknownTopLevel\":true}");
        ModuleManager m2 = new ModuleManager();
        SampleModule s2 = m2.register(new SampleModule());
        ConfigManager c2 = new ConfigManager(root);
        c2.register(new ModuleConfigSection(m2));
        c2.load();
        check("known key applied", s2.gap.get() == 9.0D);
        check("missing key keeps default", s2.style.get() == Style.SIMPLE);
        check("unknown keys ignored without throwing", s2.enabled());

        // 3. Wrong types must not crash, and must not corrupt the value.
        write(dir.resolve("modules.json"),
                "{\"configVersion\":1,\"modules\":{\"sample\":{\"enabled\":\"maybe\","
                        + "\"settings\":{\"sample.gap\":\"not a number\","
                        + "\"sample.style\":123,\"sample.targets\":\"not an array\"}}}}");
        ModuleManager m3 = new ModuleManager();
        SampleModule s3 = m3.register(new SampleModule());
        ConfigManager c3 = new ConfigManager(root);
        c3.register(new ModuleConfigSection(m3));
        c3.load();
        check("bad number keeps default", s3.gap.get() == 2.0D);
        check("bad enum keeps default", s3.style.get() == Style.SIMPLE);
        check("bad array keeps default", s3.targets.has(Target.PLAYERS));

        // 4. A config from a module that no longer exists must not break the load.
        write(dir.resolve("modules.json"),
                "{\"configVersion\":1,\"modules\":{\"removedModule\":{\"enabled\":true},"
                        + "\"sample\":{\"enabled\":false,\"settings\":{\"sample.gap\":3.0}}}}");
        ModuleManager m4 = new ModuleManager();
        SampleModule s4 = m4.register(new SampleModule());
        ConfigManager c4 = new ConfigManager(root);
        c4.register(new ModuleConfigSection(m4));
        c4.load();
        check("stale module entry ignored", s4.gap.get() == 3.0D);

        // 5. Version 0 (pre-versioning) must migrate rather than be discarded.
        write(dir.resolve("modules.json"),
                "{\"modules\":{\"sample\":{\"enabled\":true,\"settings\":{\"sample.gap\":6.0}}}}");
        ModuleManager m5 = new ModuleManager();
        SampleModule s5 = m5.register(new SampleModule());
        ConfigManager c5 = new ConfigManager(root);
        c5.register(new ModuleConfigSection(m5));
        c5.load();
        check("unversioned config migrated", s5.gap.get() == 6.0D);

        // 6. A config from a *newer* client must degrade, not wipe.
        write(dir.resolve("modules.json"),
                "{\"configVersion\":999,\"modules\":{\"sample\":"
                        + "{\"enabled\":true,\"settings\":{\"sample.gap\":7.0}}}}");
        ModuleManager m6 = new ModuleManager();
        SampleModule s6 = m6.register(new SampleModule());
        ConfigManager c6 = new ConfigManager(root);
        c6.register(new ModuleConfigSection(m6));
        c6.load();
        check("future config still read where possible", s6.gap.get() == 7.0D);

        // 7. Save debounce: tick must not write immediately after a load.
        ConfigManager c7 = new ConfigManager(root);
        c7.register(new ModuleConfigSection(m6));
        c7.load();
        c7.markDirty();
        c7.tick();
        check("tick respects the debounce window", c7.dirty());

        // 8. An atomic save must leave no temp file behind.
        c7.saveNow();
        check("no temp file left", !Files.exists(dir.resolve("modules.json.tmp")));

        // 9. Loading a directory that does not exist yet must be silent.
        ConfigManager fresh = new ConfigManager(root.resolve("brand-new"));
        fresh.register(new ModuleConfigSection(new ModuleManager()));
        fresh.load();
        check("fresh install loads cleanly", fresh.loaded());
    }

    static void testClientSettings() {
        section("ClientSettings");
        ClientSettings settings = new ClientSettings();
        check("default menu key is right shift",
                settings.menuKey.get() == ClientSettings.DEFAULT_MENU_KEY);
        check("menu key label", settings.menuKey.displayValue().equals("Right Shift"));
        check("background strength visible by default", settings.backgroundStrength.visible());
        settings.backgroundStyle.set(ClientSettings.BackgroundStyle.NONE);
        check("background strength hidden when style is none",
                !settings.backgroundStrength.visible());
        check("animation speed visible while animations on", settings.animationSpeed.visible());
        settings.animations.set(false);
        check("animation speed hidden when animations off", !settings.animationSpeed.visible());

        check("animations off collapses durations", settings.animationDuration(200L) == 1L);
        settings.animations.set(true);
        check("normal speed keeps duration", settings.animationDuration(200L) == 200L);
        settings.animationSpeed.set(2.0D);
        check("double speed halves duration", settings.animationDuration(200L) == 100L);
        settings.animationSpeed.set(0.5D);
        check("half speed doubles duration", settings.animationDuration(200L) == 400L);

        JsonObject root = new JsonObject();
        settings.write(root);
        ClientSettings restored = new ClientSettings();
        restored.read(root);
        check("client settings round trip", restored.animationSpeed.get() == 0.5D);
        check("client settings enum round trip",
                restored.backgroundStyle.get() == ClientSettings.BackgroundStyle.NONE);
    }

    // -- harness -------------------------------------------------------------


    // -- Nebrel+ --------------------------------------------------------------

    /**
     * A provider that grants whatever it is told, so the merge and the cache can
     * be driven without any of the real sources.
     */
    static final class StubProvider implements EntitlementProvider {
        final String id;
        final boolean authoritative;
        Set<NebrelEntitlement> grant = Set.of();
        UUID target;
        int calls;
        boolean throwOnCall;

        StubProvider(String id, boolean authoritative) {
            this.id = id;
            this.authoritative = authoritative;
        }

        @Override
        public String name() {
            return this.id;
        }

        @Override
        public boolean authoritative() {
            return this.authoritative;
        }

        @Override
        public Set<NebrelEntitlement> entitlementsOf(UUID playerId) {
            this.calls++;
            if (this.throwOnCall) {
                throw new IllegalStateException("provider failure");
            }
            return this.target == null || this.target.equals(playerId) ? this.grant : Set.of();
        }
    }

    /** Records every draw so a test can assert what was actually painted. */
    static final class RecordingSink implements GlyphSink {
        final List<String> glyphs = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        final List<Float> positions = new ArrayList<>();
        final List<Float> scales = new ArrayList<>();
        int plates;
        int outlines;
        boolean supportsPlates = true;

        @Override
        public void glyph(String text, float x, float y, float scale, float skew, int color,
                          int background) {
            this.glyphs.add(text);
            this.colors.add(color);
            this.positions.add(x);
            this.scales.add(scale);
            if (background != 0) {
                this.plates++;
            }
        }

        @Override
        public void plate(float x, float y, float width, float height, float radius, int color) {
            this.plates++;
        }

        @Override
        public void plateOutline(float x, float y, float width, float height, float radius,
                                 int color) {
            this.outlines++;
        }

        @Override
        public boolean supportsPlates() {
            return this.supportsPlates;
        }

        @Override
        public float width(String text) {
            // A fixed six pixels per character: the real widths come from the
            // game's font, and the geometry under test does not depend on them.
            return text == null ? 0.0F : text.length() * 6.0F;
        }

        @Override
        public float lineHeight() {
            return 9.0F;
        }

        String drawn() {
            return String.join("", this.glyphs);
        }
    }

    static void testEntitlements() {
        section("entitlements");

        PlusSettings settings = new PlusSettings();
        UUID local = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        LocalEntitlementProvider provider = new LocalEntitlementProvider(settings);
        provider.setLocalPlayer(local);

        check("local provider is not authoritative", !provider.authoritative());
        check("development mode defaults on", settings.developmentMode.get());
        check("local player is granted the development set",
                provider.entitlementsOf(local).contains(NebrelEntitlement.NEBREL_PLUS));
        check("another player is granted nothing",
                provider.entitlementsOf(other).isEmpty());
        check("a null player is granted nothing",
                provider.entitlementsOf(null).isEmpty());

        // The security boundary this class deliberately does not pretend to be:
        // with development mode off it grants nothing at all, to anyone.
        settings.developmentMode.set(false);
        check("development mode off grants nothing", provider.entitlementsOf(local).isEmpty());
        settings.developmentMode.set(true);

        check("the grant only covers implemented entitlements",
                LocalEntitlementProvider.developmentGrant().stream()
                        .allMatch(NebrelEntitlement::implemented));
        check("the grant is unmodifiable", unmodifiable(
                () -> LocalEntitlementProvider.developmentGrant()
                        .add(NebrelEntitlement.STAFF_BADGE)));

        // -- merging across providers ---------------------------------------
        EntitlementService service = new EntitlementService();
        StubProvider first = new StubProvider("first", false);
        StubProvider second = new StubProvider("second", false);
        service.addProvider(first);
        service.addProvider(second);

        check("no authoritative source yet", !service.hasAuthoritativeSource());
        check("nobody holds anything", !service.isPlus(local));

        first.grant = Set.of(NebrelEntitlement.NEBREL_PLUS);
        second.grant = Set.of(NebrelEntitlement.NAMETAG_DESIGNER);
        service.invalidate();
        Set<NebrelEntitlement> merged = service.entitlementsOf(local);
        check("providers are merged", merged.size() == 2
                && merged.contains(NebrelEntitlement.NEBREL_PLUS)
                && merged.contains(NebrelEntitlement.NAMETAG_DESIGNER));
        check("isPlus follows the merged set", service.isPlus(local));
        check("has() answers a specific capability",
                service.has(local, NebrelEntitlement.NAMETAG_DESIGNER));
        check("has() is false for one nobody granted",
                !service.has(local, NebrelEntitlement.STAFF_BADGE));

        // The answer is cached, so a second ask must not reach the providers.
        int before = first.calls;
        service.entitlementsOf(local);
        check("repeated lookups are cached", first.calls == before);

        service.invalidate();
        service.entitlementsOf(local);
        check("invalidate forces a refetch", first.calls > before);

        // A provider that throws must not take the others down with it.
        first.throwOnCall = true;
        service.invalidate();
        Set<NebrelEntitlement> survived = service.entitlementsOf(local);
        check("a throwing provider is skipped, not fatal",
                survived.contains(NebrelEntitlement.NAMETAG_DESIGNER));
        first.throwOnCall = false;

        StubProvider remote = new StubProvider("remote", true);
        service.addProvider(remote);
        check("an authoritative provider is reported",
                service.hasAuthoritativeSource());
    }

    static void testBadgeService() {
        section("badge service");

        PlusSettings settings = new PlusSettings();
        EntitlementService service = new EntitlementService();
        StubProvider provider = new StubProvider("stub", false);
        service.addProvider(provider);
        BadgeService badges = new BadgeService(service, settings);

        UUID player = UUID.randomUUID();
        check("no entitlement means no badge", !badges.hasBadge(player));
        check("getBadge is empty rather than null", badges.getBadge(player).isEmpty());

        provider.grant = Set.of(NebrelEntitlement.NEBREL_PLUS_BADGE);
        service.invalidate();
        check("the badge appears with the entitlement", badges.hasBadge(player));
        check("the badge is the Nebrel+ one",
                badges.getBadge(player).orElseThrow() == NebrelBadge.NEBREL_PLUS);
        check("the badge glyph is N", NebrelBadge.NEBREL_PLUS.glyph().equals("N"));

        // Switching the badge off hides it without touching the entitlement.
        settings.badgeEnabled.set(false);
        check("disabling the badge hides it", !badges.hasBadge(player));
        check("the entitlement is untouched",
                service.has(player, NebrelEntitlement.NEBREL_PLUS_BADGE));
        settings.badgeEnabled.set(true);

        // -- priority ordering ----------------------------------------------
        NebrelBadge staff = new NebrelBadge("staff", "Staff", "S", 0xFFFF0000, 10,
                NebrelEntitlement.STAFF_BADGE);
        badges.register(staff);
        provider.grant = Set.of(NebrelEntitlement.NEBREL_PLUS_BADGE,
                NebrelEntitlement.STAFF_BADGE);
        service.invalidate();
        check("both badges are held", badges.getBadges(player).size() == 2);
        check("the lower priority number wins the front",
                badges.getBadge(player).orElseThrow() == staff);

        // -- colour modes ----------------------------------------------------
        int accent = 0xFF8B5CF6;
        settings.badgeColorMode.set(PlusSettings.BadgeColorMode.NEBREL_ACCENT);
        check("accent mode follows the accent",
                badges.resolveColor(NebrelBadge.NEBREL_PLUS, accent, 0xFF00FF00) == accent);
        settings.badgeColorMode.set(PlusSettings.BadgeColorMode.CUSTOM);
        settings.badgeCustomColor.set(0xFF123456);
        check("custom mode uses the custom colour",
                badges.resolveColor(NebrelBadge.NEBREL_PLUS, accent, 0xFF00FF00) == 0xFF123456);
        settings.badgeColorMode.set(PlusSettings.BadgeColorMode.MATCH_NAMETAG);
        check("match mode follows the name",
                badges.resolveColor(NebrelBadge.NEBREL_PLUS, accent, 0xFF00FF00) == 0xFF00FF00);
        check("a badge with its own colour ignores the accent",
                staff.resolveColor(accent) == 0xFFFF0000);
        settings.badgeColorMode.set(PlusSettings.BadgeColorMode.NEBREL_ACCENT);

        // -- the profile cache ------------------------------------------------
        // From here the grant is aimed at one player, so a stranger genuinely
        // holds nothing rather than inheriting the stub's blanket grant.
        provider.target = player;
        service.invalidate();

        PlayerProfileCache cache = new PlayerProfileCache(service, badges, settings);
        cache.setLocalPlayerSupplier(() -> player);
        NebrelPlayerProfile profile = cache.get(player, "Tester");
        check("the profile carries the badges", profile.badges().size() == 2);
        check("the profile is not plain", !profile.isPlain());
        check("the cache retains the entry", cache.size() == 1);
        check("a second get returns the same instance", cache.get(player, "Tester") == profile);

        // The nametag style rides on the designer entitlement, not on merely
        // being the local player.
        check("no designer entitlement means no attached style",
                !profile.hasNametagStyle());
        provider.grant = Set.of(NebrelEntitlement.NEBREL_PLUS_BADGE,
                NebrelEntitlement.STAFF_BADGE, NebrelEntitlement.NAMETAG_DESIGNER);
        service.invalidate();
        cache.invalidate();
        check("the local player's nametag style is attached",
                cache.get(player, "Tester").hasNametagStyle());

        UUID stranger = UUID.randomUUID();
        NebrelPlayerProfile strangerProfile = cache.get(stranger, "Stranger");
        check("a stranger holds nothing", strangerProfile.isPlain());
        check("a remote player has no known nametag style",
                !strangerProfile.hasNametagStyle());

        cache.invalidate();
        check("invalidate empties the cache", cache.size() == 0);
        check("a null id yields an empty profile",
                cache.get(null, "Nobody").isPlain());
    }

    static void testAdditionalNametagSanitising() {
        section("additional nametag");

        check("plain text survives",
                AdditionalNametag.sanitise("Nebrel Player").equals("Nebrel Player"));
        check("null becomes empty", AdditionalNametag.sanitise(null).isEmpty());
        check("the section sign is stripped",
                !AdditionalNametag.sanitise("§cRed").contains("§"));
        check("control characters are stripped",
                AdditionalNametag.sanitise("ab" + (char) 7 + "c").equals("abc"));
        check("newlines cannot split the line",
                !AdditionalNametag.sanitise("one\ntwo").contains("\n"));
        check("whitespace is collapsed",
                AdditionalNametag.sanitise("a     b").equals("a b"));
        check("surrounding whitespace is trimmed",
                AdditionalNametag.sanitise("   padded   ").equals("padded"));

        String tooLong = "x".repeat(AdditionalNametag.MAX_LENGTH + 40);
        check("length is capped",
                AdditionalNametag.sanitise(tooLong).length() == AdditionalNametag.MAX_LENGTH);

        AdditionalNametag additional = new AdditionalNametag();
        check("inactive by default", !additional.active());
        additional.enabled.set(true);
        check("enabled but empty is still inactive", !additional.active());
        additional.text.set("  Hello  ");
        check("enabled with text is active", additional.active());
        check("resolvedText is sanitised", additional.resolvedText().equals("Hello"));
        check("the stored text respects the cap",
                additional.text.get().length() <= AdditionalNametag.MAX_LENGTH);
    }

    static void testEffectPipeline() {
        section("effect pipeline");

        NametagEffectPipeline pipeline = new NametagEffectPipeline();
        check("seven effects ship", pipeline.effects().size() == 7);
        check("all effects start off", pipeline.enabledCount() == 0);
        check("nothing is enabled", !pipeline.anyEnabled());
        check("no colour effect is enabled", !pipeline.anyColorEnabled());
        check("no geometric effect is enabled", !pipeline.anyGeometricEnabled());

        // Stage order is the pipeline's central invariant: a position effect
        // that ran before a colour effect would read a colour that is about to
        // change. The constructor asserts it; this proves the list still holds.
        int previous = -1;
        boolean ordered = true;
        for (NametagEffect effect : pipeline.effects()) {
            int stage = effect.stage().ordinal();
            if (stage < previous) {
                ordered = false;
            }
            previous = stage;
        }
        check("effects are in stage order", ordered);
        check("rainbow is a colour stage",
                pipeline.rainbow.stage() == EffectStage.COLOR);
        check("blinking is an alpha stage",
                pipeline.blinking.stage() == EffectStage.ALPHA);
        check("shaking is a position stage",
                pipeline.shaking.stage() == EffectStage.POSITION);
        check("growing is a transform stage",
                pipeline.growing.stage() == EffectStage.TRANSFORM);
        check("colour stages are not geometric", !pipeline.rainbow.geometric());
        check("position stages are geometric", pipeline.shaking.geometric());

        int white = 0xFFFFFFFF;

        // An off pipeline must leave the glyph exactly as it arrived.
        NametagRenderContext idle = pipeline.evaluate(1.0F, 0, 5, 'N', white);
        check("an idle pipeline keeps the colour", idle.color() == white);
        check("an idle pipeline keeps full alpha", Math.abs(idle.alpha() - 1.0F) < 1.0E-6F);
        check("an idle pipeline adds no offset",
                idle.offsetX() == 0.0F && idle.offsetY() == 0.0F);
        check("an idle pipeline keeps scale 1", Math.abs(idle.scale() - 1.0F) < 1.0E-6F);
        check("an idle pipeline adds no skew", idle.skew() == 0.0F);

        // -- each effect actually changes its own output ----------------------
        pipeline.rainbow.setEnabled(true);
        check("rainbow reports enabled", pipeline.anyEnabled() && pipeline.anyColorEnabled());
        check("rainbow is not geometric", !pipeline.anyGeometricEnabled());
        int rainbowColor = pipeline.evaluate(1.0F, 0, 5, 'N', white).color();
        check("rainbow changes the colour", rainbowColor != white);
        int laterChar = pipeline.evaluate(1.0F, 3, 5, 'N', white).color();
        check("rainbow spreads across characters", laterChar != rainbowColor);
        pipeline.rainbow.setEnabled(false);

        pipeline.blinking.setEnabled(true);
        boolean alphaMoved = false;
        for (int i = 0; i < 40 && !alphaMoved; i++) {
            if (pipeline.evaluate(i * 0.1F, 0, 5, 'N', white).alpha() < 0.999F) {
                alphaMoved = true;
            }
        }
        check("blinking moves the alpha", alphaMoved);
        pipeline.blinking.setEnabled(false);

        pipeline.shaking.setEnabled(true);
        check("shaking counts as geometric", pipeline.anyGeometricEnabled());
        boolean shifted = false;
        for (int i = 0; i < 40 && !shifted; i++) {
            NametagRenderContext ctx = pipeline.evaluate(i * 0.1F, 0, 5, 'N', white);
            if (Math.abs(ctx.offsetX()) > 1.0E-4F || Math.abs(ctx.offsetY()) > 1.0E-4F) {
                shifted = true;
            }
        }
        check("shaking moves the glyph", shifted);
        pipeline.shaking.setEnabled(false);

        pipeline.waving.setEnabled(true);
        boolean waved = false;
        for (int i = 0; i < 40 && !waved; i++) {
            if (Math.abs(pipeline.evaluate(i * 0.1F, 0, 5, 'N', white).offsetY()) > 1.0E-4F) {
                waved = true;
            }
        }
        check("waving moves the glyph vertically", waved);
        pipeline.waving.setEnabled(false);

        pipeline.growing.setEnabled(true);
        boolean grew = false;
        for (int i = 0; i < 40 && !grew; i++) {
            if (Math.abs(pipeline.evaluate(i * 0.1F, 0, 5, 'N', white).scale() - 1.0F) > 1.0E-4F) {
                grew = true;
            }
        }
        check("growing changes the scale", grew);
        pipeline.growing.setEnabled(false);

        pipeline.skewing.setEnabled(true);
        boolean leaned = false;
        for (int i = 0; i < 40 && !leaned; i++) {
            if (Math.abs(pipeline.evaluate(i * 0.1F, 0, 5, 'N', white).skew()) > 1.0E-4F) {
                leaned = true;
            }
        }
        check("skewing leans the glyph", leaned);
        pipeline.skewing.setEnabled(false);

        pipeline.chromatic.setEnabled(true);
        check("chromatic changes the colour",
                pipeline.evaluate(1.0F, 0, 5, 'N', white).color() != white);
        pipeline.chromatic.setEnabled(false);

        // -- determinism ------------------------------------------------------
        // Every effect must be a pure function of its inputs. Anything reaching
        // for Math.random() would render differently in the preview than above
        // the player's head, and differently on every frame.
        pipeline.shaking.setEnabled(true);
        pipeline.rainbow.setEnabled(true);
        pipeline.growing.setEnabled(true);
        NametagRenderContext once = pipeline.evaluate(4.25F, 2, 7, 'e', white);
        float x1 = once.offsetX();
        float y1 = once.offsetY();
        int c1 = once.color();
        float s1 = once.scale();
        NametagRenderContext twice = pipeline.evaluate(4.25F, 2, 7, 'e', white);
        check("evaluation is deterministic in x", twice.offsetX() == x1);
        check("evaluation is deterministic in y", twice.offsetY() == y1);
        check("evaluation is deterministic in colour", twice.color() == c1);
        check("evaluation is deterministic in scale", twice.scale() == s1);

        NametagRenderContext elsewhere = pipeline.evaluate(9.75F, 2, 7, 'e', white);
        check("a different time gives a different result",
                elsewhere.offsetX() != x1 || elsewhere.color() != c1
                        || elsewhere.scale() != s1);

        check("enabledCount tracks the switches", pipeline.enabledCount() == 3);
        pipeline.disableAll();
        check("disableAll switches everything off", pipeline.enabledCount() == 0);

        // Tuning settings only show while their effect is on, so the designer
        // does not list sliders for an effect nobody enabled.
        check("tuning is hidden while the effect is off",
                pipeline.rainbow.tuningSettings().stream().noneMatch(Setting::visible));
        pipeline.rainbow.setEnabled(true);
        check("tuning appears with the effect",
                pipeline.rainbow.tuningSettings().stream().allMatch(Setting::visible));

        pipeline.reset();
        check("reset switches everything off", pipeline.enabledCount() == 0);
    }

    static void testIdentityRendering() {
        section("identity rendering");

        PlusSettings settings = new PlusSettings();
        EntitlementService service = new EntitlementService();
        StubProvider provider = new StubProvider("stub", false);
        provider.grant = Set.of(NebrelEntitlement.NEBREL_PLUS_BADGE);
        service.addProvider(provider);
        BadgeService badges = new BadgeService(service, settings);
        IdentityRenderer renderer = new IdentityRenderer(settings, badges);

        NametagProfile profile = new NametagProfile();
        int accent = 0xFF8B5CF6;
        int white = 0xFFFFFFFF;

        // The point of this test: a badge is only implemented if something is
        // actually drawn. A flag saying "has badge" would pass every check
        // above this line and fail every check below it.
        settings.badgeStyle.set(PlusSettings.BadgeStyle.PLATE);
        RecordingSink sink = new RecordingSink();
        float consumed = renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F,
                accent, white, null, 0.0F);
        check("the badge draws a glyph", sink.glyphs.contains("N"));
        check("the plate style draws a plate", sink.plates == 1);
        check("the badge consumes width", consumed > 0.0F);
        check("the badge is drawn in the accent", sink.colors.contains(accent));

        settings.badgeStyle.set(PlusSettings.BadgeStyle.OUTLINE);
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white, null, 0.0F);
        check("the outline style draws an outline", sink.outlines == 1);
        check("the outline style still draws the glyph", sink.glyphs.contains("N"));

        // World space has no geometry, so the outline style has to fall back
        // rather than silently drawing nothing.
        sink = new RecordingSink();
        sink.supportsPlates = false;
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white, null, 0.0F);
        check("a plateless sink still shows the badge", sink.glyphs.contains("N"));
        check("a plateless sink draws no outline", sink.outlines == 0);

        settings.badgeStyle.set(PlusSettings.BadgeStyle.BRACKET);
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white, null, 0.0F);
        check("the bracket style brackets the glyph", sink.drawn().equals("[N]"));

        settings.badgeStyle.set(PlusSettings.BadgeStyle.PLAIN);
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white, null, 0.0F);
        check("the plain style draws the glyph alone", sink.drawn().equals("N"));
        check("the plain style draws no plate", sink.plates == 0);

        // Switched off, nothing is drawn and nothing is reserved.
        settings.badgeEnabled.set(false);
        sink = new RecordingSink();
        float none = renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F,
                accent, white, null, 0.0F);
        check("a disabled badge draws nothing", sink.glyphs.isEmpty());
        check("a disabled badge consumes no width", none == 0.0F);
        check("a disabled badge measures zero",
                renderer.badgeWidth(sink, NebrelBadge.NEBREL_PLUS) == 0.0F);
        settings.badgeEnabled.set(true);
        settings.badgeStyle.set(PlusSettings.BadgeStyle.PLATE);

        check("a null badge draws nothing",
                renderer.drawBadge(new RecordingSink(), null, 0.0F, 0.0F, accent, white,
                        null, 0.0F) == 0.0F);

        // -- the name --------------------------------------------------------
        sink = new RecordingSink();
        float width = renderer.drawStyledText(sink, "Nebrel", 0.0F, 0.0F, white, null, 0.0F);
        check("an unstyled name is one draw", sink.glyphs.size() == 1);
        check("an unstyled name is drawn whole", sink.drawn().equals("Nebrel"));
        check("an unstyled name reports its width", width == 36.0F);

        sink = new RecordingSink();
        renderer.drawStyledText(sink, "Nebrel", 0.0F, 0.0F, white, profile.effects, 0.0F);
        check("an idle pipeline is still one draw", sink.glyphs.size() == 1);

        profile.effects.rainbow.setEnabled(true);
        sink = new RecordingSink();
        width = renderer.drawStyledText(sink, "Nebrel", 0.0F, 0.0F, white, profile.effects, 1.0F);
        check("an animated name is drawn per glyph", sink.glyphs.size() == 6);
        check("the glyphs still spell the name", sink.drawn().equals("Nebrel"));
        check("an animated name keeps its width", width == 36.0F);
        check("the glyphs advance evenly",
                sink.positions.get(0) == 0.0F && sink.positions.get(1) == 6.0F
                        && sink.positions.get(5) == 30.0F);
        check("the glyphs are not all one colour",
                sink.colors.stream().distinct().count() > 1);

        // Scaling must not push neighbours around: the pulse happens in place.
        profile.effects.rainbow.setEnabled(false);
        profile.effects.growing.setEnabled(true);
        sink = new RecordingSink();
        renderer.drawStyledText(sink, "Nebrel", 0.0F, 0.0F, white, profile.effects, 1.0F);
        check("scaling does not change the advance",
                sink.positions.get(1) == 6.0F && sink.positions.get(5) == 30.0F);
        profile.effects.growing.setEnabled(false);

        // -- the badge follows the name's colour when asked ------------------
        settings.badgeColorMode.set(PlusSettings.BadgeColorMode.MATCH_NAMETAG);
        profile.effects.rainbow.setEnabled(true);
        int first = renderer.resolveFirstNameColor("Nebrel", white, profile.effects, 1.0F);
        check("the first name colour is resolved", first != white);
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, first, null, 0.0F);
        check("match mode paints the badge the name's colour",
                sink.colors.contains(first));
        settings.badgeColorMode.set(PlusSettings.BadgeColorMode.NEBREL_ACCENT);

        // The badge holds still by default: an animated identity marker is
        // harder to recognise, so colour effects only reach it on request.
        check("animate badge is off by default", !settings.animateBadge.get());
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white,
                profile.effects, 1.0F);
        check("a still badge keeps the accent", sink.colors.contains(accent));

        settings.animateBadge.set(true);
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white,
                profile.effects, 1.0F);
        check("an animated badge leaves the accent", !sink.colors.contains(accent));

        // Position effects must never reach the badge, animated or not.
        profile.effects.rainbow.setEnabled(false);
        profile.effects.shaking.setEnabled(true);
        sink = new RecordingSink();
        renderer.drawBadge(sink, NebrelBadge.NEBREL_PLUS, 0.0F, 0.0F, accent, white,
                profile.effects, 1.0F);
        check("the badge never moves", sink.positions.stream().allMatch(x -> x >= 0.0F));
        check("the badge never scales",
                sink.scales.stream().allMatch(s -> Math.abs(s - settings.badgeScale.getFloat())
                        < 1.0E-6F));

        // -- badge plus name, measured together ------------------------------
        settings.animateBadge.set(false);
        profile.effects.reset();
        sink = new RecordingSink();
        float total = renderer.totalWidth(sink, NebrelBadge.NEBREL_PLUS, "Nebrel");
        float badgeOnly = renderer.badgeWidth(sink, NebrelBadge.NEBREL_PLUS);
        check("the total is badge plus name", Math.abs(total - (badgeOnly + 36.0F)) < 1.0E-4F);
        check("the badge width includes its gap",
                badgeOnly > 6.0F * settings.badgeScale.getFloat());
    }

    static void testPlusConfigRoundTrip(Path root) throws Exception {
        section("Nebrel+ config");

        PlusSettings settings = new PlusSettings();
        check("Nebrel+ has its own file", settings.fileName().equals("plus.json"));

        settings.badgeStyle.set(PlusSettings.BadgeStyle.BRACKET);
        settings.badgeGap.set(5.0D);
        settings.showInChat.set(false);
        settings.nametag().useNameColor.set(false);
        settings.nametag().baseColor.set(0xFF00FF88);
        settings.nametag().effects.waving.setEnabled(true);
        settings.nametag().additional.enabled.set(true);
        settings.nametag().additional.text.set("Nebrel Player");

        JsonObject written = new JsonObject();
        settings.write(written);

        PlusSettings restored = new PlusSettings();
        restored.read(written);

        check("the badge style survives",
                restored.badgeStyle.get() == PlusSettings.BadgeStyle.BRACKET);
        check("a number survives", Math.abs(restored.badgeGap.getFloat() - 5.0F) < 1.0E-4F);
        check("a switch survives", !restored.showInChat.get());
        check("the nametag colour survives", restored.nametag().baseColor.get() == 0xFF00FF88);
        check("an enabled effect survives", restored.nametag().effects.waving.enabled());
        check("the extra line survives",
                restored.nametag().additional.resolvedText().equals("Nebrel Player"));

        // Nothing that does not belong in a plain file on disk may appear in it.
        String json = written.toString().toLowerCase(java.util.Locale.ROOT);
        check("no token is written", !json.contains("token"));
        check("no password is written", !json.contains("password"));
        check("no payment data is written",
                !json.contains("payment") && !json.contains("card"));

        // Written through the real config path, so the file itself is checked.
        Path directory = root.resolve("plus");
        ConfigFile file = new ConfigFile(directory, settings.fileName());
        JsonObject onDisk = new JsonObject();
        settings.write(onDisk);
        file.save(onDisk);
        String contents = read(directory.resolve("plus.json"));
        check("the file lands on disk", !contents.isEmpty());
        check("the file names the badge style", contents.contains("BRACKET"));

        PlusSettings reloaded = new PlusSettings();
        reloaded.read(file.load());
        check("a real round trip through the file works",
                reloaded.badgeStyle.get() == PlusSettings.BadgeStyle.BRACKET);

        settings.resetAll();
        check("reset clears the badge style",
                settings.badgeStyle.get() == PlusSettings.BadgeStyle.PLAIN);
        check("reset clears the effects", settings.nametag().effects.enabledCount() == 0);
        check("reset clears the extra line",
                settings.nametag().additional.resolvedText().isEmpty());
    }

    static void testRemoteEntitlementProvider() throws Exception {
        section("remote entitlement provider");

        // -- parsing, offline and deterministic -------------------------------
        UUID alice = UUID.randomUUID();
        String good = "{\"entitlements\":{\"" + alice
                + "\":[\"NEBREL_PLUS\",\"NEBREL_PLUS_BADGE\"]}}";
        Map<UUID, Set<NebrelEntitlement>> parsed = RemoteEntitlementProvider.parse(good);
        check("a well-formed document parses", parsed != null);
        check("the named player's entitlements are read",
                parsed.get(alice).contains(NebrelEntitlement.NEBREL_PLUS)
                        && parsed.get(alice).contains(NebrelEntitlement.NEBREL_PLUS_BADGE));

        check("a non-object body fails to parse", RemoteEntitlementProvider.parse("[1,2,3]") == null);
        check("a missing entitlements key fails to parse",
                RemoteEntitlementProvider.parse("{\"other\":1}") == null);
        check("garbage text fails to parse", RemoteEntitlementProvider.parse("not json") == null);
        Map<UUID, Set<NebrelEntitlement>> empty =
                RemoteEntitlementProvider.parse("{\"entitlements\":{}}");
        check("an empty but well-formed document parses to empty",
                empty != null && empty.isEmpty());

        String unknownName = "{\"entitlements\":{\"" + alice
                + "\":[\"NEBREL_PLUS\",\"MADE_UP_THING\"]}}";
        Map<UUID, Set<NebrelEntitlement>> withUnknown = RemoteEntitlementProvider.parse(unknownName);
        check("an unknown entitlement name is skipped, not fatal",
                withUnknown != null && withUnknown.get(alice).size() == 1
                        && withUnknown.get(alice).contains(NebrelEntitlement.NEBREL_PLUS));

        String badUuid = "{\"entitlements\":{\"not-a-uuid\":[\"NEBREL_PLUS\"],\"" + alice
                + "\":[\"NEBREL_PLUS\"]}}";
        Map<UUID, Set<NebrelEntitlement>> withBadUuid = RemoteEntitlementProvider.parse(badUuid);
        check("a malformed UUID entry is skipped, not fatal",
                withBadUuid != null && withBadUuid.size() == 1 && withBadUuid.containsKey(alice));

        String allUnknown = "{\"entitlements\":{\"" + alice + "\":[\"NOT_REAL\"]}}";
        Map<UUID, Set<NebrelEntitlement>> allUnknownParsed =
                RemoteEntitlementProvider.parse(allUnknown);
        check("a player left with no real entitlements is dropped entirely",
                allUnknownParsed != null && allUnknownParsed.isEmpty());

        // -- the provider unconfigured -----------------------------------------
        PlusSettings settings = new PlusSettings();
        RemoteEntitlementProvider provider = new RemoteEntitlementProvider(settings);
        check("not authoritative - a hosted list is not a verified account",
                !provider.authoritative());
        check("unconfigured by default", !provider.configured());
        check("an unconfigured provider answers nothing", provider.entitlementsOf(alice).isEmpty());
        check("a null player answers nothing", provider.entitlementsOf(null).isEmpty());
        provider.refresh();
        check("refresh with no URL does nothing", provider.knownPlayerCount() == 0);

        // -- a real fetch over real HTTP, on loopback ---------------------------
        // This is the property that actually matters: not that the parser is
        // correct in isolation, but that a genuine network round trip lands in
        // entitlementsOf(). A provider that merely claimed to fetch would pass
        // every assertion above and fail every one of these.
        UUID bob = UUID.randomUUID();
        String body = "{\"entitlements\":{\"" + bob + "\":[\"NEBREL_PLUS_BADGE\"]}}";
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        AtomicInteger hits = new AtomicInteger();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/entitlements.json", exchange -> {
            hits.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (var out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            settings.remoteEntitlementsUrl.set("http://127.0.0.1:" + port + "/entitlements.json");
            RemoteEntitlementProvider live = new RemoteEntitlementProvider(settings);
            check("configured once a URL is set", live.configured());

            live.refresh();
            boolean sawIt = false;
            for (int i = 0; i < 60 && !sawIt; i++) {
                Thread.sleep(50L);
                sawIt = live.entitlementsOf(bob).contains(NebrelEntitlement.NEBREL_PLUS_BADGE);
            }
            check("a real HTTP fetch populates the cache", sawIt);
            check("knownPlayerCount reflects the fetch", live.knownPlayerCount() == 1);
            check("a player the document never named stays empty",
                    live.entitlementsOf(UUID.randomUUID()).isEmpty());
            check("exactly one request was made for one refresh", hits.get() == 1);

            // The floor between real fetches must hold: asking again right away
            // must not reach the server a second time.
            live.refresh();
            check("a second immediate refresh is throttled, not fetched", hits.get() == 1);
            check("the cache from the first fetch is unaffected",
                    live.entitlementsOf(bob).contains(NebrelEntitlement.NEBREL_PLUS_BADGE));

            // -- failure is quiet, not fatal -------------------------------------
            settings.remoteEntitlementsUrl.set("http://127.0.0.1:1/definitely-nothing-here");
            RemoteEntitlementProvider unreachable = new RemoteEntitlementProvider(settings);
            unreachable.refresh();
            Thread.sleep(250L);
            check("a connection failure leaves the provider answering nothing, not throwing",
                    unreachable.entitlementsOf(bob).isEmpty());
        } finally {
            server.stop(0);
        }
    }

    static void testPixelIcons() {
        section("pixel icons");

        check("every shipped icon is square and the same size", Icons.ALL_ICONS.values().stream()
                .allMatch(icon -> icon.size() == Icons.ALL.size()));
        check("every shipped icon actually draws something",
                Icons.ALL_ICONS.values().stream().allMatch(PixelIcon::hasContent));
        check("fifteen icons ship", Icons.ALL_ICONS.size() == 15);

        // -- the constructor's own validation ---------------------------------
        check("a non-square grid is rejected",
                throwsIllegalArgument(() -> new PixelIcon("bad", "###", "###")));
        check("rows of differing length are rejected",
                throwsIllegalArgument(() -> new PixelIcon("bad", "###", "##", "###")));
        check("a stray character is rejected",
                throwsIllegalArgument(() -> new PixelIcon("bad", "#X#", "###", "###")));
        check("id cannot be blank",
                throwsIllegalArgument(() -> new PixelIcon("  ", "#")));

        // -- runsInRow: the piece the draw call actually leans on -------------
        PixelIcon single = new PixelIcon("single", "#.#", "...", ".#.");
        check("an isolated filled cell is its own run",
                java.util.Arrays.equals(single.runsInRow(0), new int[]{0, 1, 2, 3}));
        check("an empty row has no runs", single.runsInRow(1).length == 0);
        check("a middle cell is a run of one",
                java.util.Arrays.equals(single.runsInRow(2), new int[]{1, 2}));

        PixelIcon fullRow = new PixelIcon("full", "###", "###", "###");
        check("a fully filled row is a single run",
                java.util.Arrays.equals(fullRow.runsInRow(0), new int[]{0, 3}));

        check("filled() reads the same grid runsInRow walks",
                single.filled(0, 0) && !single.filled(0, 1) && single.filled(2, 1));

        // -- the module fallback: bespoke icon or the module's own glyph ------
        check("Nebrel HUD gets its bespoke icon", Icons.forModuleId("nebrel_hud") == Icons.HUD);
        check("Custom Nametags gets its bespoke icon",
                Icons.forModuleId("custom_nametags") == Icons.TAG);
        check("Full Bright gets its bespoke icon", Icons.forModuleId("fullbright") == Icons.SUN);
        check("Free Look gets its bespoke icon", Icons.forModuleId("freelook") == Icons.VISUAL);
        check("Auto Text gets its bespoke icon", Icons.forModuleId("auto_text") == Icons.CHAT);
        check("Tiers gets its bespoke icon", Icons.forModuleId("tiers") == Icons.BARS);
        check("Hitbox gets its bespoke icon", Icons.forModuleId("hitbox") == Icons.BOUNDS);
        check("Keystrokes gets its bespoke icon", Icons.forModuleId("keystrokes") == Icons.WASD);
        check("a module with no bespoke icon falls back to null, not a guess",
                Icons.forModuleId("no_fog") == null);
        check("an unknown id is also null, not an exception",
                Icons.forModuleId("not_a_real_module") == null);
    }

    /** True when constructing {@code action} throws {@link IllegalArgumentException}. */
    static boolean throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    /** True when {@code action} throws, used for immutability checks. */
    static boolean unmodifiable(Runnable action) {
        try {
            action.run();
            return false;
        } catch (RuntimeException expected) {
            return true;
        }
    }

    static void section(String name) {
        System.out.println("-- " + name);
    }

    static void check(String label, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("   FAIL  " + label);
        }
    }

    static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            return "";
        }
    }

    static void write(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static void deleteTree(Path root) {
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // best effort
                        }
                    });
        } catch (Exception ignored) {
            // best effort
        }
    }
}
