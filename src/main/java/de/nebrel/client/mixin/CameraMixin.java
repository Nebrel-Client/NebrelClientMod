package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the Free Look module point the camera independently of the player.
 *
 * <p>Only the camera is moved. The player's own yaw and pitch are untouched, so
 * nothing different is sent to the server and the player still faces, aims and
 * attacks exactly where they were facing before free look was held. This is a
 * viewing aid, not a movement or aiming change.</p>
 *
 * <p>Applied after vanilla's own update so the position is already correct and
 * only the rotation is replaced.</p>
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Invoker("setRotation")
    abstract void nebrel$setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void nebrel$applyFreeLook(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                      boolean inverseView, float tickDelta, CallbackInfo info) {
        if (!NebrelState.freeLookActive) {
            return;
        }
        float yaw = NebrelState.freeLookYaw;
        float pitch = NebrelState.freeLookPitch;
        // Vanilla flips the camera when looking backwards in third person.
        nebrel$setRotation(inverseView ? yaw + 180.0F : yaw, inverseView ? -pitch : pitch);
    }
}
