package de.nebrel.client.module.impl.render;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

/**
 * Gives capes movement beyond vanilla's stiff swing.
 *
 * <p>Vanilla derives the cape angle from the gap between the player's position
 * and a trailing point it stores on the player as {@code capeX/Y/Z}, updated
 * once per tick. Nudging that trailing point is therefore the natural place to
 * add motion: wind, gravity and extra sway all become small offsets to a value
 * the game already integrates, and the cape keeps reacting to sprinting,
 * turning and falling exactly as it did.</p>
 *
 * <p>No mixin is involved, which also means this cannot conflict with another
 * mod that renders the cape differently. The trailing point is left alone the
 * moment the module is switched off, and vanilla's own update converges it back
 * within a tick or two.</p>
 */
public final class WaveyCapesModule extends Module implements ClientTickHook {

    public final NumberSetting strength;
    public final NumberSetting smoothness;
    public final NumberSetting gravity;
    public final NumberSetting wind;
    public final NumberSetting windSpeed;
    public final NumberSetting movementInfluence;
    public final BooleanSetting affectOtherPlayers;
    public final NumberSetting maxDistance;

    /** Phase of the wind oscillation, advanced per tick. */
    private double windPhase;

    public WaveyCapesModule() {
        super("wavey_capes", "Wavey Capes", "Let capes flow instead of hanging stiff",
                ModuleCategory.RENDER, "▽");

        this.strength = number("strength", "Strength",
                "Overall amount of extra cape movement", 1.0D, 0.0D, 3.0D, 0.05D);

        this.smoothness = number("smoothness", "Smoothness",
                "How gently the cape settles; higher is looser",
                0.5D, 0.05D, 1.0D, 0.05D, SettingSection.BEHAVIOR);

        this.gravity = number("gravity", "Gravity",
                "How strongly the cape is pulled downward",
                1.0D, 0.0D, 3.0D, 0.05D, SettingSection.BEHAVIOR);

        this.wind = number("wind", "Wind", "Strength of the idle sway",
                0.4D, 0.0D, 2.0D, 0.05D, SettingSection.BEHAVIOR);
        this.windSpeed = number("windSpeed", "Wind Speed", "How fast the sway oscillates",
                1.0D, 0.1D, 4.0D, 0.1D, SettingSection.BEHAVIOR);
        this.windSpeed.visibleWhen(() -> this.wind.get() > 0.0D);

        this.movementInfluence = number("movement", "Movement Influence",
                "How much running and turning throws the cape",
                1.0D, 0.0D, 3.0D, 0.05D, SettingSection.BEHAVIOR);

        this.affectOtherPlayers = bool("others", "Other Players",
                "Apply the same motion to everyone else's cape", true, SettingSection.BEHAVIOR);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop animating capes beyond this many blocks",
                32.0D, 8.0D, 96.0D, 4.0D, SettingSection.ADVANCED);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        this.windPhase += 0.05D * this.windSpeed.get();

        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;

        for (Entity entity : client.world.getEntities()) {
            // getEntities is the client world's own typed accessor;
            // getPlayers is declared on World with a wildcard element type.
            if (!(entity instanceof AbstractClientPlayerEntity player)) {
                continue;
            }
            if (player != client.player) {
                if (!this.affectOtherPlayers.get()
                        || player.squaredDistanceTo(client.player) > limitSquared) {
                    continue;
                }
            }
            animate(player);
        }
    }

    /**
     * Offsets the trailing point vanilla measures the cape against.
     *
     * <p>Runs after vanilla's own cape update for this tick, so the offsets add
     * to a value that is already converging rather than replacing it.</p>
     */
    private void animate(AbstractClientPlayerEntity player) {
        float amount = this.strength.getFloat();
        if (amount <= 0.0F) {
            return;
        }

        // How fast the player is actually travelling, in blocks per tick.
        double dx = player.getX() - player.prevX;
        double dz = player.getZ() - player.prevZ;
        double speed = Math.sqrt(dx * dx + dz * dz);

        // Wind: a slow sine, offset per player so a crowd does not sway in
        // lockstep. The entity id is stable and gives a good spread.
        double personalPhase = this.windPhase + (player.getId() % 32) * 0.4D;
        double sway = Math.sin(personalPhase) * this.wind.get() * 0.06D;
        double swayZ = Math.cos(personalPhase * 0.7D) * this.wind.get() * 0.04D;

        // Movement throws the cape backwards along the direction of travel.
        double throwFactor = this.movementInfluence.get()
                * NebrelMath.clamp(speed * 3.0D, 0.0D, 1.0D) * 0.08D;
        double throwX = speed > 1.0E-4D ? -(dx / speed) * throwFactor : 0.0D;
        double throwZ = speed > 1.0E-4D ? -(dz / speed) * throwFactor : 0.0D;

        // Gravity pulls the trailing point down, which lifts the cape's hem.
        double drop = this.gravity.get() * 0.02D;

        // Smoothness decides how much of the offset survives into this tick.
        double blend = NebrelMath.clamp(this.smoothness.get(), 0.05D, 1.0D);

        player.capeX += (sway + throwX) * amount * blend;
        player.capeZ += (swayZ + throwZ) * amount * blend;
        player.capeY -= drop * amount * blend;
    }
}
