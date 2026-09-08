package de.nebrel.client.module.impl.render;

import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

/**
 * Replaces the vanilla nametag with a styled one.
 *
 * <p>Draws the same text the game was about to draw, with a themed plate,
 * configurable scale and optional health and distance readouts appended. All of
 * it comes from data the client already has for a visible entity, so nothing is
 * shown that the player could not read off the screen already.</p>
 *
 * <p>The drawing itself lives here rather than in the mixin, so the mixin stays
 * a two-line handoff and this class stays testable in isolation.</p>
 */
public final class CustomNametagsModule extends Module {

    public final BooleanSetting background;
    public final ColorSetting backgroundColor;
    public final NumberSetting scale;
    public final NumberSetting opacity;
    public final BooleanSetting textShadow;
    public final BooleanSetting showHealth;
    public final BooleanSetting showDistance;
    public final BooleanSetting playersOnly;
    public final ColorSetting textColor;
    public final BooleanSetting useNameColor;
    public final NumberSetting maxDistance;

    public CustomNametagsModule() {
        super("custom_nametags", "Custom Nametags", "Restyle the name plates above entities",
                ModuleCategory.RENDER, "🏷");

        this.background = bool("background", "Background", "Draw a plate behind the name", true);
        this.backgroundColor = color("backgroundColor", "Background Color", "Plate colour",
                0x99101016, true, SettingSection.APPEARANCE);
        this.backgroundColor.visibleWhen(this.background);

        this.scale = number("scale", "Scale", "Nametag size", 1.0D, 0.4D, 2.5D, 0.05D,
                SettingSection.APPEARANCE);
        this.opacity = number("opacity", "Opacity", "Overall transparency",
                1.0D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.textColor = color("textColor", "Text Color", "Name colour", 0xFFFFFFFF, true,
                SettingSection.APPEARANCE);
        this.useNameColor = bool("useNameColor", "Keep Original Colors",
                "Use the colours the name already has instead of the one above", true,
                SettingSection.APPEARANCE);
        this.textColor.visibleWhen(() -> !this.useNameColor.get());

        this.textShadow = bool("shadow", "Text Shadow", "Draw a shadow behind the text", true,
                SettingSection.APPEARANCE);

        this.showHealth = bool("health", "Show Health",
                "Append the entity's remaining health", false, SettingSection.BEHAVIOR);
        this.showDistance = bool("distance", "Show Distance",
                "Append how far away the entity is", false, SettingSection.BEHAVIOR);
        this.playersOnly = bool("playersOnly", "Players Only",
                "Leave mob and item-frame labels alone", true, SettingSection.BEHAVIOR);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop restyling labels beyond this many blocks",
                48.0D, 8.0D, 128.0D, 4.0D, SettingSection.BEHAVIOR);
    }

    /**
     * Draws the styled nametag.
     *
     * @return true when this module drew the label and vanilla should not
     */
    public boolean renderNametag(Entity entity, Text text, MatrixStack matrices,
                                 VertexConsumerProvider consumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null || client.textRenderer == null) {
            return false;
        }
        if (this.playersOnly.get() && !(entity instanceof PlayerEntity)) {
            return false;
        }

        double distanceSquared = entity.squaredDistanceTo(camera.getPos());
        double limit = this.maxDistance.get();
        if (distanceSquared > limit * limit) {
            // Beyond the range this module covers: let vanilla draw it.
            return false;
        }

        String label = buildLabel(entity, text, Math.sqrt(distanceSquared));
        TextRenderer font = client.textRenderer;
        float alpha = this.opacity.getFloat();
        float sizeScale = this.scale.getFloat();

        matrices.push();
        // Same placement vanilla uses: just above the entity, facing the camera.
        matrices.translate(0.0F, entity.getHeight() + 0.5F, 0.0F);
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.025F * sizeScale, -0.025F * sizeScale, 0.025F * sizeScale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float width = font.getWidth(label);
        float left = -width / 2.0F;

        int plate = this.background.get()
                ? ColorUtil.fadeAlpha(this.backgroundColor.get(), alpha)
                : 0;
        int color = this.useNameColor.get()
                ? ColorUtil.fadeAlpha(0xFFFFFFFF, alpha)
                : ColorUtil.fadeAlpha(this.textColor.get(), alpha);

        // The text renderer draws its own background plate when given one, and
        // it is already batched into the same layer, so it costs nothing extra.
        font.draw(label, left, 0.0F, color, this.textShadow.get(), matrix, consumers,
                TextRenderer.TextLayerType.NORMAL, plate, light);

        matrices.pop();
        return true;
    }

    private String buildLabel(Entity entity, Text text, double distance) {
        StringBuilder builder = new StringBuilder(text.getString());
        if (this.showHealth.get() && entity instanceof LivingEntity living) {
            builder.append("  ").append(trim(living.getHealth())).append('/')
                    .append(trim(living.getMaxHealth()));
        }
        if (this.showDistance.get()) {
            builder.append("  ").append(Math.round(distance)).append('m');
        }
        return builder.toString();
    }

    private static String trim(float value) {
        return value == Math.floor(value)
                ? String.valueOf((int) value)
                : String.format("%.1f", value);
    }
}
