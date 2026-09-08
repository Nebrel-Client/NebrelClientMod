package de.nebrel.client.hud;

import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One movable element of the Nebrel HUD.
 *
 * <p>A widget owns its placement (anchor plus a pixel offset from that anchor),
 * its scale and a small style block. Subclasses only have to describe their
 * content: {@link #contentWidth()}, {@link #contentHeight()} and
 * {@link #renderContent}. The frame, background, padding, scaling and the
 * editor's hit testing are handled here so every widget behaves identically.</p>
 */
public abstract class HudWidget {

    private final String id;
    private final String name;

    private final List<Setting<?>> settings = new ArrayList<>();

    public final BooleanSetting enabled;
    public final EnumSetting<HudAnchor> anchor;
    public final NumberSetting scale;
    public final NumberSetting opacity;
    public final BooleanSetting background;
    public final ColorSetting backgroundColor;
    public final NumberSetting cornerRadius;
    public final NumberSetting padding;
    public final BooleanSetting border;
    public final ColorSetting borderColor;
    public final ColorSetting textColor;
    public final BooleanSetting useAccent;
    public final BooleanSetting textShadow;

    /** Offset in pixels from the anchor's reference point. */
    private float offsetX;
    private float offsetY;

    /** Last resolved bounds, kept so the editor can hit test without recomputing. */
    private float lastX;
    private float lastY;
    private float lastWidth;
    private float lastHeight;

    protected HudWidget(String id, String name, HudAnchor defaultAnchor,
                        float defaultOffsetX, float defaultOffsetY, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;

        this.enabled = add(new BooleanSetting(key("enabled"), name, "Show the " + name + " widget",
                enabledByDefault));
        this.anchor = add(new EnumSetting<>(key("anchor"), "Anchor",
                "Screen edge this widget is measured from", defaultAnchor));
        this.anchor.section(SettingSection.GENERAL);

        this.scale = add(new NumberSetting(key("scale"), "Scale", "Widget size", 1.0D, 0.5D, 2.5D, 0.05D));
        this.scale.section(SettingSection.APPEARANCE);

        this.opacity = add(new NumberSetting(key("opacity"), "Opacity", "Overall transparency",
                1.0D, 0.1D, 1.0D, 0.05D));
        this.opacity.section(SettingSection.APPEARANCE);

        this.background = add(new BooleanSetting(key("background"), "Background",
                "Draw a panel behind the widget", true));
        this.background.section(SettingSection.APPEARANCE);

        this.backgroundColor = add(new ColorSetting(key("backgroundColor"), "Background Color",
                "Panel colour", 0x9E101016, true));
        this.backgroundColor.section(SettingSection.APPEARANCE);
        this.backgroundColor.visibleWhen(this.background);

        this.cornerRadius = add(new NumberSetting(key("radius"), "Corner Radius",
                "Panel corner rounding", 4.0D, 0.0D, 12.0D, 1.0D));
        this.cornerRadius.section(SettingSection.APPEARANCE);
        this.cornerRadius.visibleWhen(this.background);

        this.padding = add(new NumberSetting(key("padding"), "Padding",
                "Space between the panel edge and the content", 4.0D, 0.0D, 12.0D, 1.0D));
        this.padding.section(SettingSection.APPEARANCE);
        this.padding.visibleWhen(this.background);

        this.border = add(new BooleanSetting(key("border"), "Border", "Outline the panel", false));
        this.border.section(SettingSection.APPEARANCE);
        this.border.visibleWhen(this.background);

        this.borderColor = add(new ColorSetting(key("borderColor"), "Border Color",
                "Outline colour", 0x40FFFFFF, true));
        this.borderColor.section(SettingSection.APPEARANCE);
        this.borderColor.visibleWhen(() -> this.background.get() && this.border.get());

        this.textColor = add(new ColorSetting(key("textColor"), "Text Color",
                "Value colour", 0xFFFFFFFF, true));
        this.textColor.section(SettingSection.APPEARANCE);

        this.useAccent = add(new BooleanSetting(key("useAccent"), "Use Accent",
                "Tint the label with the client accent colour", true));
        this.useAccent.section(SettingSection.APPEARANCE);

        this.textShadow = add(new BooleanSetting(key("shadow"), "Text Shadow",
                "Draw a shadow behind the text", true));
        this.textShadow.section(SettingSection.APPEARANCE);
    }

    private String key(String suffix) {
        return "hud." + this.id + "." + suffix;
    }

    protected final <S extends Setting<?>> S add(S setting) {
        this.settings.add(setting);
        return setting;
    }

    public final String id() {
        return this.id;
    }

    public final String name() {
        return this.name;
    }

    public final List<Setting<?>> settings() {
        return List.copyOf(this.settings);
    }

    public final List<Setting<?>> visibleSettings() {
        List<Setting<?>> result = new ArrayList<>(this.settings.size());
        for (Setting<?> setting : this.settings) {
            if (setting.visible()) {
                result.add(setting);
            }
        }
        return result;
    }

    // -- placement -----------------------------------------------------------

    public final float offsetX() {
        return this.offsetX;
    }

    public final float offsetY() {
        return this.offsetY;
    }

    public final void setOffset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    /**
     * Moves the widget to an absolute screen position, keeping it on screen.
     *
     * <p>The clamp uses the widget's own size, so a wide widget cannot be
     * dragged so far right that only its left edge remains visible.</p>
     */
    public final void moveTo(float left, float top, float screenWidth, float screenHeight) {
        float width = totalWidth();
        float height = totalHeight();
        float clampedLeft = NebrelMath.clamp(left, 0.0F, Math.max(0.0F, screenWidth - width));
        float clampedTop = NebrelMath.clamp(top, 0.0F, Math.max(0.0F, screenHeight - height));
        HudAnchor current = this.anchor.get();
        this.offsetX = current.toOffsetX(clampedLeft, screenWidth, width);
        this.offsetY = current.toOffsetY(clampedTop, screenHeight, height);
    }

    /**
     * Switches anchor without moving the widget on screen.
     *
     * <p>Called when a drag ends, so a widget dropped near a corner starts
     * tracking that corner from then on.</p>
     */
    public final void rebindAnchor(HudAnchor newAnchor, float screenWidth, float screenHeight) {
        if (newAnchor == this.anchor.get()) {
            return;
        }
        float[] moved = HudAnchor.reanchor(this.anchor.get(), newAnchor,
                this.offsetX, this.offsetY, screenWidth, screenHeight, totalWidth(), totalHeight());
        this.anchor.set(newAnchor);
        this.offsetX = moved[0];
        this.offsetY = moved[1];
    }

    /** Widget width including padding and scale. */
    public final float totalWidth() {
        float pad = this.background.get() ? this.padding.getFloat() * 2.0F : 0.0F;
        return (contentWidth() + pad) * this.scale.getFloat();
    }

    /** Widget height including padding and scale. */
    public final float totalHeight() {
        float pad = this.background.get() ? this.padding.getFloat() * 2.0F : 0.0F;
        return (contentHeight() + pad) * this.scale.getFloat();
    }

    public final float lastX() {
        return this.lastX;
    }

    public final float lastY() {
        return this.lastY;
    }

    public final float lastWidth() {
        return this.lastWidth;
    }

    public final float lastHeight() {
        return this.lastHeight;
    }

    public final boolean containsPoint(double x, double y) {
        return NebrelMath.inside(x, y, this.lastX, this.lastY, this.lastWidth, this.lastHeight);
    }

    // -- rendering -----------------------------------------------------------

    /**
     * Resolves the widget's position and draws it.
     *
     * @param accent the client accent, for widgets that opt into it
     * @param force  draw even when the widget is switched off (editor preview)
     */
    public final void render(DrawContext context, float screenWidth, float screenHeight,
                             int accent, boolean force) {
        if (!this.enabled.get() && !force) {
            // Keep the stored bounds empty so the editor cannot select it.
            this.lastWidth = 0.0F;
            this.lastHeight = 0.0F;
            return;
        }

        float width = totalWidth();
        float height = totalHeight();
        HudAnchor current = this.anchor.get();
        float x = current.resolveX(this.offsetX, screenWidth, width);
        float y = current.resolveY(this.offsetY, screenHeight, height);

        this.lastX = x;
        this.lastY = y;
        this.lastWidth = width;
        this.lastHeight = height;

        float alpha = this.opacity.getFloat();
        float widgetScale = this.scale.getFloat();

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(widgetScale, widgetScale, 1.0F);

        float pad = this.background.get() ? this.padding.getFloat() : 0.0F;
        float innerWidth = contentWidth();
        float innerHeight = contentHeight();

        if (this.background.get()) {
            RenderUtil.roundedRect(context, 0.0F, 0.0F,
                    innerWidth + pad * 2.0F, innerHeight + pad * 2.0F,
                    this.cornerRadius.getFloat(),
                    ColorUtil.fadeAlpha(this.backgroundColor.get(), alpha));
            if (this.border.get()) {
                RenderUtil.roundedOutline(context, 0.0F, 0.0F,
                        innerWidth + pad * 2.0F, innerHeight + pad * 2.0F,
                        this.cornerRadius.getFloat(),
                        ColorUtil.fadeAlpha(this.borderColor.get(), alpha));
            }
        }

        int value = ColorUtil.fadeAlpha(this.textColor.get(), alpha);
        int label = ColorUtil.fadeAlpha(this.useAccent.get() ? accent : this.textColor.get(), alpha);
        renderContent(context, pad, pad, value, label, this.textShadow.get());

        matrices.pop();
    }

    /**
     * Refreshes any derived state once per frame.
     *
     * <p>{@link #contentWidth()}, {@link #contentHeight()} and
     * {@link #renderContent} are each called during a single frame, so
     * recomputing inside them would do the same work three times. The manager
     * calls this once beforehand instead.</p>
     */
    public void update() {
    }

    /** Content width in unscaled pixels, excluding padding. */
    public abstract float contentWidth();

    /** Content height in unscaled pixels, excluding padding. */
    public abstract float contentHeight();

    /**
     * Draws the widget's content.
     *
     * <p>The matrix stack is already translated and scaled, so implementations
     * draw at {@code (x, y)} in their own local space.</p>
     *
     * @param valueColor the resolved text colour, alpha already applied
     * @param labelColor the accent or text colour for labels
     */
    protected abstract void renderContent(DrawContext context, float x, float y,
                                          int valueColor, int labelColor, boolean shadow);

    /**
     * Whether the widget has anything to show right now.
     *
     * <p>A widget with no data (no server to ping, no effects active) is
     * skipped in game but still drawn in the editor so it can be positioned.</p>
     */
    public boolean hasContent() {
        return true;
    }

    /** Editor preview text used when the real value is unavailable. */
    protected static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    /** Convenience for subclasses drawing themed placeholder chrome. */
    protected static int themedMuted(Theme theme) {
        return theme.textSecondary;
    }
}
