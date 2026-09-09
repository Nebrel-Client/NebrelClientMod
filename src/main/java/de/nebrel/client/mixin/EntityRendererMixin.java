package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands the label above an entity to Nebrel's nametag coordinator.
 *
 * <p>Cancelled only when the coordinator actually drew something, so a label
 * nothing wants to change is left entirely to vanilla. That keeps the common
 * case — most entities, most of the time — on the original code path.</p>
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void nebrel$customNametag(Entity entity, Text text, MatrixStack matrices,
                                      VertexConsumerProvider consumers, int light, float tickDelta,
                                      CallbackInfo info) {
        if (!NebrelClient.ready()) {
            return;
        }
        NebrelClient client = NebrelClient.get();
        if (client.plus().nametagCoordinator()
                .render(entity, text, matrices, consumers, light, client.accent())) {
            info.cancel();
        }
    }
}
