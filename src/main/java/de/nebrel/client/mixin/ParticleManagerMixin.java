package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enforces the Overflow Particles module's ceiling.
 *
 * <p>Only the cap is implemented here, and only as a rejection: when the live
 * particle count is already at the limit, the new particle is dropped. Vanilla
 * has its own ceiling; this one can be set lower to protect frame rate on a
 * busy server, or higher when the machine can take it.</p>
 *
 * <p>Deliberately no multiplication happens in this injection. Spawning extra
 * particles from inside the manager's own add path risks re-entering it, so the
 * module does that from its tick hook where the state is stable.</p>
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("HEAD"), cancellable = true)
    private void nebrel$enforceCap(Particle particle, CallbackInfo info) {
        if (!NebrelState.particleBoostActive) {
            return;
        }
        if (nebrel$liveCount() >= NebrelState.particleCap) {
            info.cancel();
        }
    }

    /**
     * Approximate count of live particles.
     *
     * <p>The manager keeps particles bucketed per texture sheet; summing the
     * buckets each time a particle spawns would be wasteful, so this tracks a
     * running estimate maintained by the module instead.</p>
     */
    private static int nebrel$liveCount() {
        return NebrelState.particleLiveEstimate;
    }
}
