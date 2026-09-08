package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the Animations module's swing speed.
 *
 * <p>{@code getHandSwingDuration} is how long, in ticks, the arm takes to
 * complete one swing. It is used for the visual animation only; attack timing,
 * cooldown and reach all come from elsewhere, so changing it alters how the
 * swing looks and nothing about what it does.</p>
 *
 * <p>Restricted to the client's own player, so other players on a server keep
 * their normal animation.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void nebrel$swingDuration(CallbackInfoReturnable<Integer> info) {
        int override = NebrelState.swingDurationTicks;
        if (override <= 0) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && self == client.player) {
            info.setReturnValue(override);
        }
    }
}
