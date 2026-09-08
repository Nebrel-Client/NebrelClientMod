package de.nebrel.client.module.impl.visual;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * A screen tint when the player takes damage.
 *
 * <p>Watches the player's own health each tick rather than hooking the damage
 * event, so it reacts to every source including drowning and poison, and never
 * needs to interpret a damage type.</p>
 *
 * <p>Drawn as a vignette by default: four edge gradients rather than a flat
 * fill, so the middle of the screen stays readable during a fight.</p>
 */
public final class DamageTintModule extends Module implements ClientTickHook, HudRenderHook {

    /** How the tint covers the screen. */
    public enum Shape {
        VIGNETTE("Vignette"),
        FULL_SCREEN("Full Screen"),
        EDGES("Thin Edges");

        private final String display;

        Shape(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final ColorSetting tintColor;
    public final EnumSetting<Shape> shape;
    public final NumberSetting intensity;
    public final NumberSetting duration;
    public final NumberSetting size;
    public final BooleanSetting fade;
    public final BooleanSetting scaleWithDamage;
    public final BooleanSetting lowHealthPulse;
    public final NumberSetting lowHealthThreshold;

    private float lastHealth = -1.0F;
    private long tintStartedAt;
    private float tintStrength;

    public DamageTintModule() {
        super("damage_tint", "Damage Tint", "Flash the screen when you take damage",
                ModuleCategory.VISUAL, "◍");

        this.tintColor = color("color", "Color", "Tint colour", 0xFFD62828, false);
        this.shape = choice("shape", "Shape", "How the tint covers the screen", Shape.VIGNETTE,
                SettingSection.APPEARANCE);

        this.intensity = number("intensity", "Intensity", "Peak opacity of the tint",
                0.5D, 0.05D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.size = number("size", "Size", "How far the tint reaches in from the edges",
                0.25D, 0.05D, 0.6D, 0.01D, SettingSection.APPEARANCE);
        this.size.visibleWhen(() -> !this.shape.is(Shape.FULL_SCREEN));

        this.duration = number("duration", "Duration", "How long the tint lasts, in seconds",
                0.6D, 0.1D, 3.0D, 0.1D, SettingSection.ANIMATION);

        this.fade = bool("fade", "Fade Out", "Ease the tint away instead of cutting it", true,
                SettingSection.ANIMATION);

        this.scaleWithDamage = bool("scaleWithDamage", "Scale With Damage",
                "Make bigger hits produce a stronger tint", true, SettingSection.BEHAVIOR);

        this.lowHealthPulse = bool("lowHealthPulse", "Low Health Pulse",
                "Pulse continuously while health is low", false, SettingSection.BEHAVIOR);

        this.lowHealthThreshold = number("lowHealthThreshold", "Low Health Threshold",
                "Health points below which the pulse starts",
                6.0D, 1.0D, 19.0D, 1.0D, SettingSection.BEHAVIOR);
        this.lowHealthThreshold.visibleWhen(this.lowHealthPulse);
    }

    @Override
    protected void onEnable() {
        this.lastHealth = -1.0F;
        this.tintStrength = 0.0F;
    }

    @Override
    protected void onDisable() {
        this.tintStrength = 0.0F;
        this.lastHealth = -1.0F;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            this.lastHealth = -1.0F;
            return;
        }
        float health = player.getHealth();
        if (this.lastHealth < 0.0F) {
            this.lastHealth = health;
            return;
        }
        if (health < this.lastHealth) {
            float taken = this.lastHealth - health;
            // Two hearts of damage is treated as a full-strength hit.
            this.tintStrength = this.scaleWithDamage.get()
                    ? NebrelMath.clamp(taken / 4.0F, 0.25F, 1.0F)
                    : 1.0F;
            this.tintStartedAt = System.currentTimeMillis();
        }
        this.lastHealth = health;
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        float alpha = currentAlpha(client.player);
        if (alpha <= 0.004F) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int color = ColorUtil.withAlpha(this.tintColor.get(), Math.round(alpha * 255.0F));

        switch (this.shape.get()) {
            case FULL_SCREEN -> RenderUtil.rect(context, 0.0F, 0.0F, width, height, color);
            case VIGNETTE -> drawVignette(context, width, height, color,
                    this.size.getFloat());
            case EDGES -> drawVignette(context, width, height, color,
                    this.size.getFloat() * 0.35F);
        }
    }

    /** Current tint opacity from the hit timer and the low-health pulse. */
    private float currentAlpha(ClientPlayerEntity player) {
        float peak = this.intensity.getFloat();
        float fromHit = 0.0F;

        long elapsed = System.currentTimeMillis() - this.tintStartedAt;
        long lifetime = Math.round(this.duration.get() * 1000.0D);
        if (this.tintStrength > 0.0F && elapsed < lifetime) {
            float progress = elapsed / (float) lifetime;
            float envelope = this.fade.get() ? 1.0F - Easing.EASE_OUT_CUBIC.apply(progress) : 1.0F;
            fromHit = peak * this.tintStrength * envelope;
        }

        float fromPulse = 0.0F;
        if (this.lowHealthPulse.get() && player.getHealth() <= this.lowHealthThreshold.get()) {
            // A slow sine, stronger the closer to death.
            float severity = 1.0F - NebrelMath.clamp(
                    player.getHealth() / this.lowHealthThreshold.getFloat(), 0.0F, 1.0F);
            float wave = (float) (0.5D + 0.5D * Math.sin(System.currentTimeMillis() / 320.0D));
            fromPulse = peak * 0.55F * severity * wave;
        }

        return NebrelMath.clamp(Math.max(fromHit, fromPulse), 0.0F, 1.0F);
    }

    /** Four inward gradients, leaving the centre of the screen clear. */
    private static void drawVignette(DrawContext context, int width, int height,
                                     int color, float extent) {
        int transparent = ColorUtil.withAlpha(color, 0);
        float bandX = width * extent;
        float bandY = height * extent;

        // Top and bottom use the vertical gradient directly.
        RenderUtil.gradientVertical(context, 0.0F, 0.0F, width, bandY, color, transparent);
        RenderUtil.gradientVertical(context, 0.0F, height - bandY, width, bandY, transparent, color);
        // Left and right are stepped, since fillGradient only runs vertically.
        RenderUtil.gradientHorizontal(context, 0.0F, 0.0F, bandX, height, color, transparent);
        RenderUtil.gradientHorizontal(context, width - bandX, 0.0F, bandX, height, transparent, color);
    }
}
