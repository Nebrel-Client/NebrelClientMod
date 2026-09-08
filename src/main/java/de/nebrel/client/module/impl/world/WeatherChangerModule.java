package de.nebrel.client.module.impl.world;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

/**
 * Sets the weather the client renders.
 *
 * <p>Local only, in the same sense as the time changer: the rain and thunder
 * gradients are the client's own interpolation values for drawing rain, fog
 * density and sky darkening. Setting them changes what the player sees.
 * Whether it is actually raining on the server, and therefore whether crops
 * grow or a boat fills, is untouched.</p>
 */
public final class WeatherChangerModule extends Module implements ClientTickHook {

    /** The weather to render. */
    public enum WeatherPreset {
        SERVER("Server"),
        CLEAR("Clear"),
        RAIN("Rain"),
        THUNDER("Thunder");

        private final String display;

        WeatherPreset(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final EnumSetting<WeatherPreset> preset;
    public final NumberSetting rainStrength;
    public final NumberSetting thunderStrength;
    public final BooleanSetting particles;
    public final BooleanSetting smooth;
    public final NumberSetting transitionSpeed;

    private float currentRain;
    private float currentThunder;
    private boolean initialised;

    public WeatherChangerModule() {
        super("weather_changer", "Weather Changer",
                "Choose the weather you see, without touching the server",
                ModuleCategory.WORLD, "☁");

        this.preset = choice("preset", "Weather", "Which weather to render", WeatherPreset.CLEAR);

        this.rainStrength = number("rain", "Rain Strength", "How heavy the rain looks",
                1.0D, 0.0D, 1.0D, 0.05D, SettingSection.APPEARANCE);
        this.rainStrength.visibleWhen(() -> this.preset.is(WeatherPreset.RAIN)
                || this.preset.is(WeatherPreset.THUNDER));

        this.thunderStrength = number("thunder", "Thunder Strength",
                "How dark the sky gets during a storm",
                1.0D, 0.0D, 1.0D, 0.05D, SettingSection.APPEARANCE);
        this.thunderStrength.visibleWhen(() -> this.preset.is(WeatherPreset.THUNDER));

        this.particles = bool("particles", "Rain Particles",
                "Draw falling rain and its splashes", true, SettingSection.APPEARANCE);
        this.particles.visibleWhen(() -> this.preset.is(WeatherPreset.RAIN)
                || this.preset.is(WeatherPreset.THUNDER));

        this.smooth = bool("smooth", "Smooth Transition",
                "Ease between weather states instead of snapping", true, SettingSection.ANIMATION);

        this.transitionSpeed = number("speed", "Transition Speed",
                "How quickly the weather eases", 0.05D, 0.01D, 0.5D, 0.01D,
                SettingSection.ANIMATION);
        this.transitionSpeed.visibleWhen(this.smooth);
    }

    @Override
    protected void onEnable() {
        this.initialised = false;
    }

    @Override
    protected void onDisable() {
        // The server sends weather updates continuously, so the next packet
        // restores the real values on its own; nudge them now so the change is
        // immediate rather than waiting for one.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            client.world.setRainGradient(client.world.isRaining() ? 1.0F : 0.0F);
            client.world.setThunderGradient(client.world.isThundering() ? 1.0F : 0.0F);
        }
        this.initialised = false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || this.preset.is(WeatherPreset.SERVER)) {
            return;
        }

        float targetRain;
        float targetThunder;
        switch (this.preset.get()) {
            case CLEAR -> {
                targetRain = 0.0F;
                targetThunder = 0.0F;
            }
            case RAIN -> {
                targetRain = this.rainStrength.getFloat();
                targetThunder = 0.0F;
            }
            case THUNDER -> {
                targetRain = this.rainStrength.getFloat();
                targetThunder = this.thunderStrength.getFloat();
            }
            default -> {
                return;
            }
        }

        // Rain particles are driven by the rain gradient, so zeroing it is how
        // "no particles" is expressed while the sky stays overcast.
        if (!this.particles.get()) {
            targetRain = 0.0F;
        }

        if (!this.initialised) {
            this.currentRain = world.getRainGradient(1.0F);
            this.currentThunder = world.getThunderGradient(1.0F);
            this.initialised = true;
        }

        if (this.smooth.get()) {
            float speed = (float) NebrelMath.clamp(this.transitionSpeed.get(), 0.0D, 1.0D);
            this.currentRain = NebrelMath.lerp(this.currentRain, targetRain, speed);
            this.currentThunder = NebrelMath.lerp(this.currentThunder, targetThunder, speed);
        } else {
            this.currentRain = targetRain;
            this.currentThunder = targetThunder;
        }

        world.setRainGradient(this.currentRain);
        world.setThunderGradient(this.currentThunder);
    }
}
