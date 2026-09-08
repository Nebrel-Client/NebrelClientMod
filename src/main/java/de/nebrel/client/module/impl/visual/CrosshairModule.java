package de.nebrel.client.module.impl.visual;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
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
import net.minecraft.util.hit.HitResult;

/**
 * A configurable crosshair drawn in place of the vanilla one.
 *
 * <p>Built from plain rectangles so it stays crisp at any GUI scale, with the
 * outline drawn as a slightly larger pass underneath rather than as four
 * separate strokes, which keeps the corners clean.</p>
 *
 * <p>The dynamic gap reacts to movement and to the attack cooldown, so the
 * crosshair communicates the same information vanilla's attack indicator does
 * without needing a second element on screen.</p>
 */
public final class CrosshairModule extends Module implements HudRenderHook, ClientTickHook {

    /** Crosshair shape. */
    public enum Style {
        CROSS("Cross"),
        DOT("Dot"),
        CIRCLE("Circle"),
        T_SHAPE("T Shape"),
        BRACKETS("Brackets");

        private final String display;

        Style(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final EnumSetting<Style> style;
    public final NumberSetting length;
    public final NumberSetting thickness;
    public final NumberSetting gap;
    public final BooleanSetting dot;
    public final NumberSetting dotSize;
    public final BooleanSetting outline;
    public final NumberSetting outlineThickness;
    public final ColorSetting mainColor;
    public final ColorSetting outlineColor;
    public final NumberSetting opacity;
    public final BooleanSetting rainbow;
    public final NumberSetting rainbowSpeed;
    public final BooleanSetting dynamic;
    public final NumberSetting movementExpansion;
    public final BooleanSetting attackIndicator;
    public final BooleanSetting hideVanilla;
    public final BooleanSetting hideOnThirdPerson;
    public final ColorSetting entityColor;
    public final BooleanSetting highlightEntity;

    /** Eased expansion, so the crosshair does not jitter with every step. */
    private float expansion;

    public CrosshairModule() {
        super("crosshair", "Crosshair", "Replace the vanilla crosshair with your own",
                ModuleCategory.VISUAL, "✛");

        this.style = choice("style", "Style", "Crosshair shape", Style.CROSS);
        this.hideVanilla = bool("hideVanilla", "Hide Vanilla Crosshair",
                "Stop the game drawing its own crosshair underneath", true);
        this.hideOnThirdPerson = bool("hideThirdPerson", "Hide in Third Person",
                "Only draw the crosshair in first person", true, SettingSection.BEHAVIOR);

        this.length = number("length", "Length", "Arm length in pixels",
                5.0D, 1.0D, 20.0D, 0.5D, SettingSection.APPEARANCE);
        this.length.visibleWhen(() -> !this.style.is(Style.DOT));

        this.thickness = number("thickness", "Thickness", "Arm thickness in pixels",
                1.0D, 1.0D, 6.0D, 1.0D, SettingSection.APPEARANCE);
        this.thickness.visibleWhen(() -> !this.style.is(Style.DOT));

        this.gap = number("gap", "Gap", "Space between the centre and each arm",
                2.0D, 0.0D, 12.0D, 0.5D, SettingSection.APPEARANCE);
        this.gap.visibleWhen(() -> !this.style.is(Style.DOT));

        this.dot = bool("dot", "Center Dot", "Draw a dot in the middle", false,
                SettingSection.APPEARANCE);
        this.dotSize = number("dotSize", "Dot Size", "Dot width in pixels",
                1.0D, 1.0D, 6.0D, 1.0D, SettingSection.APPEARANCE);
        this.dotSize.visibleWhen(() -> this.dot.get() || this.style.is(Style.DOT));

        this.outline = bool("outline", "Outline", "Draw a contrasting outline", true,
                SettingSection.APPEARANCE);
        this.outlineThickness = number("outlineThickness", "Outline Thickness",
                "Outline width in pixels", 1.0D, 1.0D, 3.0D, 1.0D, SettingSection.APPEARANCE);
        this.outlineThickness.visibleWhen(this.outline);

        this.mainColor = color("color", "Main Color", "Crosshair colour", 0xFFFFFFFF, true,
                SettingSection.APPEARANCE);
        this.outlineColor = color("outlineColor", "Outline Color", "Outline colour",
                0xC0000000, true, SettingSection.APPEARANCE);
        this.outlineColor.visibleWhen(this.outline);

        this.opacity = number("opacity", "Opacity", "Overall transparency",
                1.0D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.rainbow = bool("rainbow", "Rainbow", "Cycle the crosshair colour", false,
                SettingSection.APPEARANCE);
        this.rainbowSpeed = number("rainbowSpeed", "Rainbow Speed", "Colour cycles per second",
                0.3D, 0.05D, 2.0D, 0.05D, SettingSection.APPEARANCE);
        this.rainbowSpeed.visibleWhen(this.rainbow);

        this.highlightEntity = bool("highlightEntity", "Highlight on Entity",
                "Tint the crosshair when it is over an entity", false, SettingSection.BEHAVIOR);
        this.entityColor = color("entityColor", "Entity Color",
                "Colour used when aiming at an entity", 0xFFF2555A, true, SettingSection.APPEARANCE);
        this.entityColor.visibleWhen(this.highlightEntity);

        this.dynamic = bool("dynamic", "Dynamic", "Expand the gap while moving or attacking",
                false, SettingSection.BEHAVIOR);
        this.movementExpansion = number("expansion", "Movement Expansion",
                "How far the crosshair opens at full speed",
                3.0D, 0.0D, 10.0D, 0.5D, SettingSection.BEHAVIOR);
        this.movementExpansion.visibleWhen(this.dynamic);
        this.attackIndicator = bool("attackIndicator", "Attack Indicator",
                "Close the crosshair as the attack cooldown recovers", true,
                SettingSection.BEHAVIOR);
        this.attackIndicator.visibleWhen(this.dynamic);
    }

    @Override
    protected void onEnable() {
        NebrelState.hideCrosshair = this.hideVanilla.get();
    }

    @Override
    protected void onDisable() {
        NebrelState.hideCrosshair = false;
        this.expansion = 0.0F;
    }

    @Override
    public void onTick(MinecraftClient client) {
        // Kept in step with the setting even while the module stays enabled.
        NebrelState.hideCrosshair = this.hideVanilla.get();

        float target = 0.0F;
        ClientPlayerEntity player = client.player;
        if (this.dynamic.get() && player != null) {
            double dx = player.getX() - player.prevX;
            double dz = player.getZ() - player.prevZ;
            float speed = (float) Math.sqrt(dx * dx + dz * dz);
            // Roughly normalised: sprinting is about 0.28 blocks per tick.
            target += this.movementExpansion.getFloat()
                    * NebrelMath.clamp(speed / 0.28F, 0.0F, 1.0F);

            if (this.attackIndicator.get()) {
                float cooldown = player.getAttackCooldownProgress(0.0F);
                target += this.movementExpansion.getFloat() * (1.0F - cooldown);
            }
        }
        this.expansion = NebrelMath.lerp(this.expansion, target, 0.35F);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        if (this.hideOnThirdPerson.get() && !client.options.getPerspective().isFirstPerson()) {
            return;
        }
        if (client.currentScreen != null) {
            return;
        }

        float centerX = client.getWindow().getScaledWidth() / 2.0F;
        float centerY = client.getWindow().getScaledHeight() / 2.0F;

        int base = this.rainbow.get()
                ? ColorUtil.rainbow(System.currentTimeMillis(), this.rainbowSpeed.getFloat(), 0.0F)
                : this.mainColor.get();
        if (this.highlightEntity.get() && aimingAtEntity(client)) {
            base = this.entityColor.get();
        }
        int color = ColorUtil.fadeAlpha(base, this.opacity.getFloat());
        int outlineTint = ColorUtil.fadeAlpha(this.outlineColor.get(), this.opacity.getFloat());

        float gapValue = this.gap.getFloat() + this.expansion;
        float armLength = this.length.getFloat();
        float armThickness = this.thickness.getFloat();
        float outlineWidth = this.outline.get() ? this.outlineThickness.getFloat() : 0.0F;

        // Outline first, as a larger copy of the same geometry underneath.
        if (outlineWidth > 0.0F) {
            drawShape(context, centerX, centerY, gapValue - outlineWidth,
                    armLength + outlineWidth * 2.0F, armThickness + outlineWidth * 2.0F,
                    this.dotSize.getFloat() + outlineWidth * 2.0F, outlineTint);
        }
        drawShape(context, centerX, centerY, gapValue, armLength, armThickness,
                this.dotSize.getFloat(), color);
    }

    private boolean aimingAtEntity(MinecraftClient client) {
        HitResult target = client.crosshairTarget;
        return target != null && target.getType() == HitResult.Type.ENTITY;
    }

    private void drawShape(DrawContext context, float cx, float cy,
                           float gapValue, float armLength, float armThickness,
                           float dotWidth, int color) {
        float half = armThickness / 2.0F;
        Style shape = this.style.get();

        switch (shape) {
            case DOT -> drawDot(context, cx, cy, dotWidth, color);
            case CROSS -> {
                horizontalArms(context, cx, cy, gapValue, armLength, armThickness, half, color);
                verticalArms(context, cx, cy, gapValue, armLength, armThickness, half, color, true);
            }
            case T_SHAPE -> {
                horizontalArms(context, cx, cy, gapValue, armLength, armThickness, half, color);
                // Lower arm only: the upper one is what obscures a target's head.
                verticalArms(context, cx, cy, gapValue, armLength, armThickness, half, color, false);
            }
            case CIRCLE -> drawCircle(context, cx, cy, gapValue + armLength, armThickness, color);
            case BRACKETS -> drawBrackets(context, cx, cy, gapValue, armLength, armThickness, color);
        }

        if (this.dot.get() && shape != Style.DOT) {
            drawDot(context, cx, cy, dotWidth, color);
        }
    }

    private static void horizontalArms(DrawContext context, float cx, float cy, float gap,
                                       float length, float thickness, float half, int color) {
        RenderUtil.rect(context, cx - gap - length, cy - half, length, thickness, color);
        RenderUtil.rect(context, cx + gap, cy - half, length, thickness, color);
    }

    private static void verticalArms(DrawContext context, float cx, float cy, float gap,
                                     float length, float thickness, float half, int color,
                                     boolean includeTop) {
        if (includeTop) {
            RenderUtil.rect(context, cx - half, cy - gap - length, thickness, length, color);
        }
        RenderUtil.rect(context, cx - half, cy + gap, thickness, length, color);
    }

    private static void drawDot(DrawContext context, float cx, float cy, float size, int color) {
        RenderUtil.rect(context, cx - size / 2.0F, cy - size / 2.0F, size, size, color);
    }

    /** A ring approximated by stepping around the circumference. */
    private static void drawCircle(DrawContext context, float cx, float cy,
                                   float radius, float thickness, int color) {
        int steps = Math.max(16, (int) (radius * 4.0F));
        for (int i = 0; i < steps; i++) {
            double angle = (i / (double) steps) * Math.PI * 2.0D;
            float px = cx + (float) Math.cos(angle) * radius;
            float py = cy + (float) Math.sin(angle) * radius;
            RenderUtil.rect(context, px - thickness / 2.0F, py - thickness / 2.0F,
                    thickness, thickness, color);
        }
    }

    /** Four corner brackets framing the centre. */
    private static void drawBrackets(DrawContext context, float cx, float cy,
                                     float gap, float length, float thickness, int color) {
        float outer = gap + length;
        float arm = Math.max(2.0F, length * 0.6F);
        // Top left.
        RenderUtil.rect(context, cx - outer, cy - outer, arm, thickness, color);
        RenderUtil.rect(context, cx - outer, cy - outer, thickness, arm, color);
        // Top right.
        RenderUtil.rect(context, cx + outer - arm, cy - outer, arm, thickness, color);
        RenderUtil.rect(context, cx + outer - thickness, cy - outer, thickness, arm, color);
        // Bottom left.
        RenderUtil.rect(context, cx - outer, cy + outer - thickness, arm, thickness, color);
        RenderUtil.rect(context, cx - outer, cy + outer - arm, thickness, arm, color);
        // Bottom right.
        RenderUtil.rect(context, cx + outer - arm, cy + outer - thickness, arm, thickness, color);
        RenderUtil.rect(context, cx + outer - thickness, cy + outer - arm, thickness, arm, color);
    }
}
