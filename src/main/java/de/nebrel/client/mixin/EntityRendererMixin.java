package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.module.impl.render.CustomNametagsModule;
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
 * Hands nametag drawing to the Custom Nametags module.
 *
 * <p>Injected at the head of the vanilla label render and cancelled when the
 * module wants to draw it instead. That keeps the two implementations mutually
 * exclusive rather than stacked, so a tag is never drawn twice.</p>
 *
 * <p>The module is looked up through the registry rather than held statically,
 * because a mixin class is loaded long before the client is constructed.</p>
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
        CustomNametagsModule module = NebrelClient.get().modules()
                .getById("custom_nametags")
                .filter(candidate -> candidate instanceof CustomNametagsModule)
                .map(candidate -> (CustomNametagsModule) candidate)
                .orElse(null);
        if (module == null || !module.enabled()) {
            return;
        }
        if (module.renderNametag(entity, text, matrices, consumers, light)) {
            info.cancel();
        }
    }
}
