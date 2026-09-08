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
 * Sets the time of day the client renders.
 *
 * <p>Purely local. {@code ClientWorld.setTimeOfDay} changes the client's own
 * copy of the world time, which is what the sky, sun, moon and stars are drawn
 * from. Nothing is sent to the server, so mob spawning, crop growth and
 * daylight sensors all keep following the server's real time; a player using
 * this sees a different sky, not a different world.</p>
 *
 * <p>Because the server keeps sending time updates, the override is reapplied
 * every tick rather than set once.</p>
 */
public final class TimeChangerModule extends Module implements ClientTickHook {

    /** Named points in the Minecraft day, in ticks. */
    public enum TimePreset {
        SERVER("Server", -1L),
        SUNRISE("Sunrise", 23_000L),
        DAY("Day", 1_000L),
        NOON("Noon", 6_000L),
        SUNSET("Sunset", 12_000L),
        NIGHT("Night", 14_000L),
        MIDNIGHT("Midnight", 18_000L),
        CUSTOM("Custom", -1L);

        private final String display;
        private final long ticks;

        TimePreset(String display, long ticks) {
            this.display = display;
            this.ticks = ticks;
        }

        public long ticks() {
            return this.ticks;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final EnumSetting<TimePreset> preset;
    public final NumberSetting customTime;
    public final BooleanSetting freeze;
    public final BooleanSetting smooth;
    public final NumberSetting transitionSpeed;

    /** The time actually applied, eased towards the target when smoothing. */
    private double displayedTime = -1.0D;

    public TimeChangerModule() {
        super("time_changer", "Time Changer",
                "Choose the time of day you see, without touching the server",
                ModuleCategory.WORLD, "☀");

        this.preset = choice("preset", "Time", "Which time of day to render", TimePreset.DAY);

        this.customTime = number("custom", "Custom Time", "Time of day in ticks, 0 to 23999",
                6000.0D, 0.0D, 23_999.0D, 100.0D);
        this.customTime.visibleWhen(() -> this.preset.is(TimePreset.CUSTOM));

        this.freeze = bool("freeze", "Freeze", "Hold the time still instead of letting it advance",
                true, SettingSection.BEHAVIOR);

        this.smooth = bool("smooth", "Smooth Transition",
                "Ease into the new time instead of snapping", true, SettingSection.ANIMATION);

        this.transitionSpeed = number("speed", "Transition Speed",
                "How quickly the sky eases to the chosen time",
                0.08D, 0.01D, 0.5D, 0.01D, SettingSection.ANIMATION);
        this.transitionSpeed.visibleWhen(this.smooth);
    }

    @Override
    protected void onEnable() {
        // Start the ease from wherever the sky currently is.
        MinecraftClient client = MinecraftClient.getInstance();
        this.displayedTime = client.world == null ? -1.0D : client.world.getTimeOfDay() % 24_000L;
    }

    @Override
    protected void onDisable() {
        // Hand the sky straight back to the server's time.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            client.world.setTimeOfDay(client.world.getTime());
        }
        this.displayedTime = -1.0D;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || this.preset.is(TimePreset.SERVER)) {
            return;
        }

        long target = this.preset.is(TimePreset.CUSTOM)
                ? (long) this.customTime.get()
                : this.preset.get().ticks();
        if (target < 0L) {
            return;
        }

        if (!this.freeze.get()) {
            // Let the day advance from the chosen point rather than pinning it.
            target = (target + world.getTime() % 24_000L) % 24_000L;
        }

        if (this.smooth.get() && this.displayedTime >= 0.0D) {
            this.displayedTime = easeTowards(this.displayedTime, target,
                    this.transitionSpeed.get());
        } else {
            this.displayedTime = target;
        }

        world.setTimeOfDay((long) this.displayedTime);
    }

    /**
     * Eases around the 24000-tick circle by the shorter direction.
     *
     * <p>Interpolating linearly would run the sun backwards through the whole
     * day whenever the target wrapped past midnight.</p>
     */
    private static double easeTowards(double current, double target, double speed) {
        double difference = ((target - current + 36_000.0D) % 24_000.0D) - 12_000.0D;
        double next = current + difference * NebrelMath.clamp(speed, 0.0D, 1.0D);
        return ((next % 24_000.0D) + 24_000.0D) % 24_000.0D;
    }
}
