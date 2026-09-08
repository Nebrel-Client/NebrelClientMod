package de.nebrel.client.module.impl.render;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Adds extra particles to combat feedback, under a hard ceiling.
 *
 * <p>Two halves. The ceiling is enforced in the particle manager and refuses
 * new particles once the live count reaches the limit, so this module can only
 * ever make the game cheaper to render than its own setting allows. The extra
 * particles are spawned from here, on the client tick, at positions the client
 * already knows about.</p>
 *
 * <p>Everything spawned is decorative and client-side. No packet is sent, and
 * the particles carry no gameplay effect; a player using this sees more sparks,
 * and the server sees nothing at all.</p>
 */
public final class OverflowParticlesModule extends Module implements ClientTickHook {

    /** Which events get extra particles. */
    public enum Source {
        CRITICAL_HITS,
        SPRINTING,
        LANDING,
        SPLASHES
    }

    /** Never spawn more than this in one tick, whatever the multiplier says. */
    private static final int PER_TICK_LIMIT = 48;

    public final NumberSetting multiplier;
    public final NumberSetting maximumParticles;
    public final MultiSelectSetting<Source> sources;
    public final NumberSetting scale;
    public final BooleanSetting reduceWhenLagging;

    private final Random random = new Random();
    private boolean wasOnGround = true;
    private double lastFallDistance;

    public OverflowParticlesModule() {
        super("overflow_particles", "Overflow Particles",
                "More particle feedback, with a ceiling that protects frame rate",
                ModuleCategory.RENDER, "✺");

        this.multiplier = number("multiplier", "Particle Multiplier",
                "How many extra particles each event produces", 2.0D, 1.0D, 8.0D, 1.0D);

        this.maximumParticles = number("maximum", "Maximum Particles",
                "Hard ceiling on live particles; new ones are refused above it",
                8000.0D, 500.0D, 32_000.0D, 500.0D);

        this.sources = multi("sources", "Sources", "Which events produce extra particles",
                Source.class, Source.CRITICAL_HITS, Source.LANDING);

        this.scale = number("scale", "Spread", "How far the extra particles scatter",
                0.4D, 0.1D, 1.5D, 0.05D, SettingSection.APPEARANCE);

        this.reduceWhenLagging = bool("reduceWhenLagging", "Back Off When Slow",
                "Spawn fewer particles while the frame rate is low", true,
                SettingSection.ADVANCED);
    }

    @Override
    protected void onEnable() {
        NebrelState.particleBoostActive = true;
        NebrelState.particleCap = (int) this.maximumParticles.get();
    }

    @Override
    protected void onDisable() {
        NebrelState.particleBoostActive = false;
        NebrelState.particleMultiplier = 1;
        NebrelState.particleCap = 16_384;
        NebrelState.particleLiveEstimate = 0;
    }

    @Override
    public void onTick(MinecraftClient client) {
        NebrelState.particleCap = (int) this.maximumParticles.get();
        NebrelState.particleMultiplier = (int) this.multiplier.get();

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            NebrelState.particleLiveEstimate = 0;
            return;
        }

        // Decay the estimate so it tracks particles expiring as well as spawning.
        NebrelState.particleLiveEstimate = Math.max(0,
                (int) (NebrelState.particleLiveEstimate * 0.92F));

        int budget = perTickBudget(client);
        if (budget <= 0) {
            return;
        }

        boolean onGround = player.isOnGround();
        if (this.sources.has(Source.LANDING) && onGround && !this.wasOnGround
                && this.lastFallDistance > 1.0D) {
            spawnBurst(client, player.getPos(), ParticleTypes.CLOUD,
                    Math.min(budget, (int) (this.multiplier.get() * 3)));
        }
        this.lastFallDistance = onGround ? 0.0D : player.fallDistance;
        this.wasOnGround = onGround;

        if (this.sources.has(Source.SPRINTING) && player.isSprinting() && onGround) {
            spawnBurst(client, player.getPos(), ParticleTypes.POOF,
                    Math.min(budget, (int) this.multiplier.get()));
        }

        if (this.sources.has(Source.SPLASHES) && player.isTouchingWater() && player.isSprinting()) {
            spawnBurst(client, player.getPos().add(0.0D, 0.2D, 0.0D), ParticleTypes.SPLASH,
                    Math.min(budget, (int) this.multiplier.get() * 2));
        }

        if (this.sources.has(Source.CRITICAL_HITS) && player.getAttackCooldownProgress(0.0F) >= 1.0F
                && client.options.attackKey.isPressed() && client.crosshairTarget != null
                && client.crosshairTarget.getType()
                == net.minecraft.util.hit.HitResult.Type.ENTITY) {
            spawnBurst(client, client.crosshairTarget.getPos(), ParticleTypes.CRIT,
                    Math.min(budget, (int) this.multiplier.get() * 2));
        }
    }

    /** How many particles may be spawned this tick. */
    private int perTickBudget(MinecraftClient client) {
        int budget = PER_TICK_LIMIT;
        if (this.reduceWhenLagging.get()) {
            int fps = de.nebrel.client.core.NebrelClient.get().stats().fps();
            if (fps > 0 && fps < 45) {
                // Scale the budget down with the frame rate rather than cutting
                // out entirely, so the effect degrades instead of flickering.
                budget = Math.max(0, budget * fps / 45);
            }
        }
        int headroom = NebrelState.particleCap - NebrelState.particleLiveEstimate;
        return Math.max(0, Math.min(budget, headroom));
    }

    private void spawnBurst(MinecraftClient client, Vec3d center, ParticleEffect effect, int count) {
        if (count <= 0 || client.world == null) {
            return;
        }
        double spread = this.scale.get();
        for (int i = 0; i < count; i++) {
            double offsetX = (this.random.nextDouble() - 0.5D) * spread;
            double offsetY = this.random.nextDouble() * spread * 0.5D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * spread;
            client.world.addParticle(effect,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    offsetX * 0.1D, offsetY * 0.1D, offsetZ * 0.1D);
        }
        NebrelState.particleLiveEstimate += count;
    }
}
