package de.nebrel.client.module;

import de.nebrel.client.core.ClientStats;
import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.hud.HudManager;
import de.nebrel.client.hud.widget.ArmorWidget;
import de.nebrel.client.hud.widget.BiomeWidget;
import de.nebrel.client.hud.widget.ClockWidget;
import de.nebrel.client.hud.widget.CoordinatesWidget;
import de.nebrel.client.hud.widget.CpsWidget;
import de.nebrel.client.hud.widget.DirectionWidget;
import de.nebrel.client.hud.widget.FpsWidget;
import de.nebrel.client.hud.widget.MemoryWidget;
import de.nebrel.client.hud.widget.PingWidget;
import de.nebrel.client.hud.widget.PlayerInfoWidget;
import de.nebrel.client.hud.widget.PotionEffectsWidget;
import de.nebrel.client.hud.widget.ServerWidget;
import de.nebrel.client.hud.widget.SpeedWidget;
import de.nebrel.client.module.impl.hud.AppleSkinModule;
import de.nebrel.client.module.impl.hud.KeystrokesModule;
import de.nebrel.client.module.impl.hud.NebrelHudModule;
import de.nebrel.client.module.impl.hud.TntTimerModule;
import de.nebrel.client.module.impl.hud.VanillaHudModule;
import de.nebrel.client.module.impl.player.FreeLookModule;
import de.nebrel.client.module.impl.player.ToggleSprintModule;
import de.nebrel.client.module.impl.render.BlurModule;
import de.nebrel.client.module.impl.render.CustomNametagsModule;
import de.nebrel.client.module.impl.render.GlintColorizerModule;
import de.nebrel.client.module.impl.render.HealthIndicatorsModule;
import de.nebrel.client.module.impl.render.HitboxModule;
import de.nebrel.client.module.impl.render.ItemHighlighterModule;
import de.nebrel.client.module.impl.render.ItemModelModule;
import de.nebrel.client.module.impl.render.NoFogModule;
import de.nebrel.client.module.impl.render.OverflowParticlesModule;
import de.nebrel.client.module.impl.render.SkinLayers3dModule;
import de.nebrel.client.module.impl.render.WaveyCapesModule;
import de.nebrel.client.module.impl.utility.AutoTextModule;
import de.nebrel.client.module.impl.utility.BorderlessWindowModule;
import de.nebrel.client.module.impl.utility.PackTweaksModule;
import de.nebrel.client.module.impl.utility.QuestsModule;
import de.nebrel.client.module.impl.utility.ShulkerTooltipModule;
import de.nebrel.client.module.impl.utility.TiersModule;
import de.nebrel.client.module.impl.visual.AnimationsModule;
import de.nebrel.client.module.impl.visual.ArrowTrailModule;
import de.nebrel.client.module.impl.visual.CrosshairModule;
import de.nebrel.client.module.impl.visual.DamageTintModule;
import de.nebrel.client.module.impl.visual.FovChangerModule;
import de.nebrel.client.module.impl.visual.FullBrightModule;
import de.nebrel.client.module.impl.world.TimeChangerModule;
import de.nebrel.client.module.impl.world.WeatherChangerModule;

/**
 * Builds the module and widget registries.
 *
 * <p>One place that lists everything the client ships, so adding a feature is a
 * single line here plus its class. Registration order is the order the menu
 * shows modules in within a category, so related features sit together.</p>
 *
 * <p>HUD widgets are registered before the modules, because the Nebrel HUD
 * module reads the widget list in its constructor to surface each widget's
 * toggle.</p>
 */
public final class Modules {

    private Modules() {
    }

    public static void registerAll(NebrelClient client) {
        registerWidgets(client.hud(), client.stats());
        registerModules(client);
    }

    private static void registerWidgets(HudManager hud, ClientStats stats) {
        hud.register(new FpsWidget(stats));
        hud.register(new PingWidget(stats));
        hud.register(new CoordinatesWidget());
        hud.register(new DirectionWidget());
        hud.register(new SpeedWidget(stats));
        hud.register(new CpsWidget(stats));
        hud.register(new ServerWidget());
        hud.register(new MemoryWidget());
        hud.register(new ClockWidget());
        hud.register(new BiomeWidget());
        hud.register(new ArmorWidget());
        hud.register(new PotionEffectsWidget());
        hud.register(new PlayerInfoWidget());
    }

    private static void registerModules(NebrelClient client) {
        ModuleManager modules = client.modules();

        // HUD
        modules.register(new NebrelHudModule(client.hud()));
        modules.register(new KeystrokesModule());
        modules.register(new AppleSkinModule());
        modules.register(new TntTimerModule());
        modules.register(new VanillaHudModule());

        // Visual
        modules.register(new CrosshairModule());
        modules.register(new FullBrightModule());
        modules.register(new FovChangerModule());
        modules.register(new DamageTintModule());
        modules.register(new AnimationsModule());
        modules.register(new ArrowTrailModule());

        // Player
        modules.register(new ToggleSprintModule());
        modules.register(new FreeLookModule());

        // World
        modules.register(new TimeChangerModule());
        modules.register(new WeatherChangerModule());

        // Render
        modules.register(new NoFogModule());
        modules.register(new BlurModule());
        modules.register(new GlintColorizerModule());
        modules.register(new ItemModelModule());
        modules.register(new CustomNametagsModule());
        modules.register(new HealthIndicatorsModule());
        modules.register(new HitboxModule());
        modules.register(new ItemHighlighterModule());
        modules.register(new OverflowParticlesModule());
        modules.register(new SkinLayers3dModule());
        modules.register(new WaveyCapesModule());

        // Utility
        modules.register(new AutoTextModule());
        ShulkerTooltipModule shulker = modules.register(new ShulkerTooltipModule());
        // The tooltip callback is registered once and checks the module's own
        // enabled flag, so there is nothing to unregister when it is switched off.
        shulker.registerTooltipListener();
        modules.register(new PackTweaksModule());
        modules.register(new BorderlessWindowModule());
        modules.register(new TiersModule());
        modules.register(new QuestsModule());
    }
}
