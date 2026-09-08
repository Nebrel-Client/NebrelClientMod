package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * A colour swatch that opens a saturation/value field with hue and alpha bars.
 *
 * <p>Editing works in HSB and only writes RGB back on change, so dragging
 * through a fully desaturated or fully dark region does not lose the hue the
 * user had selected.</p>
 */
public final class ColorPickerComponent extends Component {

    private static final float PANEL_WIDTH = 148.0F;
    private static final float FIELD_HEIGHT = 84.0F;
    private static final float BAR_HEIGHT = 10.0F;
    private static final float PANEL_PADDING = 8.0F;
    private static final float GAP = 7.0F;

    private static final int PRESET_COUNT = 8;
    private static final int[] PRESETS = {
            0xFFFFFFFF, 0xFF8B5CF6, 0xFF3B82F6, 0xFF3DD68C,
            0xFFF5B547, 0xFFF2555A, 0xFFEC6BC8, 0xFF101014
    };

    private final ColorSetting setting;
    private final Animation openAmount;

    private boolean open;
    private float hue;
    private float saturation;
    private float brightness;

    private enum Drag { NONE, FIELD, HUE, ALPHA }

    private Drag drag = Drag.NONE;

    public ColorPickerComponent(UiContext ui, ColorSetting setting) {
        super(ui);
        this.setting = setting;
        this.openAmount = new Animation(0.0F, ui.duration(160L), Easing.EASE_OUT_CUBIC);
        this.height = 18.0F;
        syncFromSetting();
    }

    @Override
    public float preferredHeight() {
        return 18.0F;
    }

    private void syncFromSetting() {
        float[] hsb = ColorUtil.rgbToHsb(this.setting.get());
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    private void writeBack() {
        int rgb = ColorUtil.hsbToRgb(this.hue, this.saturation, this.brightness);
        this.setting.setRgb(rgb);
    }

    // -- closed state --------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean hovered = isHovered(mouseX, mouseY) && enabled();

        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 5.0F,
                hovered || this.open ? theme.surfaceHover : theme.surfaceElevated);
        RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 5.0F,
                this.open ? theme.accent : theme.border);

        // Swatch with a checkerboard behind it so alpha is visible.
        float swatch = this.height - 6.0F;
        float swatchX = this.x + 3.0F;
        float swatchY = this.y + 3.0F;
        checkerboard(context, swatchX, swatchY, swatch, swatch);
        RenderUtil.roundedRect(context, swatchX, swatchY, swatch, swatch, 3.0F, this.setting.get());
        RenderUtil.roundedOutline(context, swatchX, swatchY, swatch, swatch, 3.0F,
                ColorUtil.withAlpha(theme.border, 160));

        RenderUtil.textFlat(context, ColorUtil.toHex(this.setting.get()),
                swatchX + swatch + 6.0F, this.y + (this.height - RenderUtil.lineHeight()) / 2.0F + 1.0F,
                enabled() ? theme.textSecondary : theme.textDisabled);
    }

    /** Two-tone grid used behind translucent swatches. */
    private static void checkerboard(DrawContext context, float x, float y, float width, float height) {
        int cell = 4;
        int cols = (int) Math.ceil(width / cell);
        int rows = (int) Math.ceil(height / cell);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int shade = ((row + col) & 1) == 0 ? 0xFF9A9AAB : 0xFF6C6C7A;
                float cellX = x + col * cell;
                float cellY = y + row * cell;
                float w = Math.min(cell, x + width - cellX);
                float h = Math.min(cell, y + height - cellY);
                RenderUtil.rect(context, cellX, cellY, w, h, shade);
            }
        }
    }

    // -- open panel ----------------------------------------------------------

    private float panelHeight() {
        float bars = this.setting.supportsAlpha() ? BAR_HEIGHT * 2.0F + GAP : BAR_HEIGHT;
        return PANEL_PADDING * 2.0F + FIELD_HEIGHT + GAP + bars + GAP + 14.0F;
    }

    private float panelTop() {
        float height = panelHeight();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        boolean above = this.y + this.height + height + 6.0F > screenHeight && this.y - height - 2.0F > 0.0F;
        return above ? this.y - height - 2.0F : this.y + this.height + 2.0F;
    }

    private float panelLeft() {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        // Keep the panel on screen when the swatch sits near the right edge.
        return NebrelMath.clamp(this.x, 4.0F, screenWidth - PANEL_WIDTH - 4.0F);
    }

    @Override
    public void renderOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
        float amount = this.openAmount.value();
        if (amount <= 0.01F) {
            return;
        }
        Theme theme = theme();
        float left = panelLeft();
        float top = panelTop();
        float height = panelHeight() * amount;

        RenderUtil.shadow(context, left, top, PANEL_WIDTH, height, 8.0F,
                ColorUtil.fadeAlpha(theme.shadow, amount), 5);
        RenderUtil.roundedRect(context, left, top, PANEL_WIDTH, height, 8.0F,
                ColorUtil.fadeAlpha(theme.surfaceElevated, amount));
        RenderUtil.roundedOutline(context, left, top, PANEL_WIDTH, height, 8.0F,
                ColorUtil.fadeAlpha(theme.border, amount));

        if (amount < 0.98F) {
            // Skip the contents mid-animation: a squashed picker reads as noise.
            return;
        }

        float contentX = left + PANEL_PADDING;
        float contentW = PANEL_WIDTH - PANEL_PADDING * 2.0F;
        float cursorY = top + PANEL_PADDING;

        // Saturation/value field for the current hue.
        int pure = ColorUtil.hsbToRgb(this.hue, 1.0F, 1.0F);
        RenderUtil.gradientHorizontal(context, contentX, cursorY, contentW, FIELD_HEIGHT,
                0xFFFFFFFF, pure);
        RenderUtil.gradientVertical(context, contentX, cursorY, contentW, FIELD_HEIGHT,
                0x00000000, 0xFF000000);
        RenderUtil.roundedOutline(context, contentX, cursorY, contentW, FIELD_HEIGHT, 3.0F,
                ColorUtil.withAlpha(theme.border, 190));

        float markerX = contentX + contentW * this.saturation;
        float markerY = cursorY + FIELD_HEIGHT * (1.0F - this.brightness);
        ring(context, markerX, markerY, 3.5F);
        cursorY += FIELD_HEIGHT + GAP;

        // Hue bar.
        hueBar(context, contentX, cursorY, contentW, BAR_HEIGHT);
        RenderUtil.roundedOutline(context, contentX, cursorY, contentW, BAR_HEIGHT,
                BAR_HEIGHT / 2.0F, ColorUtil.withAlpha(theme.border, 190));
        ring(context, contentX + contentW * this.hue, cursorY + BAR_HEIGHT / 2.0F, 3.5F);
        cursorY += BAR_HEIGHT + GAP;

        // Alpha bar, when the setting allows transparency.
        if (this.setting.supportsAlpha()) {
            checkerboard(context, contentX, cursorY, contentW, BAR_HEIGHT);
            int opaque = ColorUtil.withAlpha(this.setting.get(), 255);
            RenderUtil.gradientHorizontal(context, contentX, cursorY, contentW, BAR_HEIGHT,
                    ColorUtil.withAlpha(opaque, 0), opaque);
            RenderUtil.roundedOutline(context, contentX, cursorY, contentW, BAR_HEIGHT,
                    BAR_HEIGHT / 2.0F, ColorUtil.withAlpha(theme.border, 190));
            ring(context, contentX + contentW * (ColorUtil.alpha(this.setting.get()) / 255.0F),
                    cursorY + BAR_HEIGHT / 2.0F, 3.5F);
            cursorY += BAR_HEIGHT + GAP;
        }

        // Preset swatches.
        float presetSize = (contentW - (PRESET_COUNT - 1) * 3.0F) / PRESET_COUNT;
        for (int i = 0; i < PRESET_COUNT; i++) {
            float presetX = contentX + i * (presetSize + 3.0F);
            RenderUtil.roundedRect(context, presetX, cursorY, presetSize, 12.0F, 3.0F, PRESETS[i]);
            RenderUtil.roundedOutline(context, presetX, cursorY, presetSize, 12.0F, 3.0F,
                    ColorUtil.withAlpha(theme.border, 160));
        }
    }

    private static void hueBar(DrawContext context, float x, float y, float width, float height) {
        int columns = Math.max(1, Math.round(width));
        for (int i = 0; i < columns; i++) {
            int color = ColorUtil.hsbToRgb(i / (float) columns, 1.0F, 1.0F);
            RenderUtil.rect(context, x + i, y, 1.0F, height, color);
        }
    }

    /** A small white ring with a dark core, readable on any background. */
    private static void ring(DrawContext context, float centerX, float centerY, float radius) {
        RenderUtil.roundedOutline(context, centerX - radius, centerY - radius,
                radius * 2.0F, radius * 2.0F, radius, 0xFF000000);
        RenderUtil.roundedOutline(context, centerX - radius + 1.0F, centerY - radius + 1.0F,
                (radius - 1.0F) * 2.0F, (radius - 1.0F) * 2.0F, radius - 1.0F, 0xFFFFFFFF);
    }

    // -- interaction ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) {
            return false;
        }
        if (this.open) {
            float left = panelLeft();
            float top = panelTop();
            if (!RenderUtil.hovered(mouseX, mouseY, left, top, PANEL_WIDTH, panelHeight())) {
                closeOverlay();
                return isHovered(mouseX, mouseY);
            }

            float contentX = left + PANEL_PADDING;
            float contentW = PANEL_WIDTH - PANEL_PADDING * 2.0F;
            float cursorY = top + PANEL_PADDING;

            if (RenderUtil.hovered(mouseX, mouseY, contentX, cursorY, contentW, FIELD_HEIGHT)) {
                this.drag = Drag.FIELD;
                applyField(mouseX, mouseY, contentX, cursorY, contentW);
                return true;
            }
            cursorY += FIELD_HEIGHT + GAP;

            if (RenderUtil.hovered(mouseX, mouseY, contentX, cursorY, contentW, BAR_HEIGHT)) {
                this.drag = Drag.HUE;
                applyHue(mouseX, contentX, contentW);
                return true;
            }
            cursorY += BAR_HEIGHT + GAP;

            if (this.setting.supportsAlpha()) {
                if (RenderUtil.hovered(mouseX, mouseY, contentX, cursorY, contentW, BAR_HEIGHT)) {
                    this.drag = Drag.ALPHA;
                    applyAlpha(mouseX, contentX, contentW);
                    return true;
                }
                cursorY += BAR_HEIGHT + GAP;
            }

            float presetSize = (contentW - (PRESET_COUNT - 1) * 3.0F) / PRESET_COUNT;
            for (int i = 0; i < PRESET_COUNT; i++) {
                float presetX = contentX + i * (presetSize + 3.0F);
                if (RenderUtil.hovered(mouseX, mouseY, presetX, cursorY, presetSize, 12.0F)) {
                    this.setting.setRgb(PRESETS[i]);
                    syncFromSetting();
                    return true;
                }
            }
            return true;
        }

        if (isHovered(mouseX, mouseY)) {
            this.open = true;
            syncFromSetting();
            this.openAmount.animateTo(1.0F);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.drag == Drag.NONE) {
            return false;
        }
        float left = panelLeft();
        float top = panelTop();
        float contentX = left + PANEL_PADDING;
        float contentW = PANEL_WIDTH - PANEL_PADDING * 2.0F;

        switch (this.drag) {
            case FIELD -> applyField(mouseX, mouseY, contentX, top + PANEL_PADDING, contentW);
            case HUE -> applyHue(mouseX, contentX, contentW);
            case ALPHA -> applyAlpha(mouseX, contentX, contentW);
            case NONE -> {
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.drag == Drag.NONE) {
            return false;
        }
        this.drag = Drag.NONE;
        return true;
    }

    private void applyField(double mouseX, double mouseY, float fieldX, float fieldY, float fieldW) {
        this.saturation = (float) NebrelMath.clamp((mouseX - fieldX) / fieldW, 0.0D, 1.0D);
        this.brightness = 1.0F - (float) NebrelMath.clamp((mouseY - fieldY) / FIELD_HEIGHT, 0.0D, 1.0D);
        writeBack();
    }

    private void applyHue(double mouseX, float barX, float barW) {
        this.hue = (float) NebrelMath.clamp((mouseX - barX) / barW, 0.0D, 1.0D);
        writeBack();
    }

    private void applyAlpha(double mouseX, float barX, float barW) {
        double fraction = NebrelMath.clamp((mouseX - barX) / barW, 0.0D, 1.0D);
        this.setting.setAlpha((int) Math.round(fraction * 255.0D));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.open && keyCode == 256) {
            closeOverlay();
            return true;
        }
        return false;
    }

    @Override
    public boolean hasOpenOverlay() {
        return this.open;
    }

    @Override
    public void closeOverlay() {
        if (this.open) {
            this.open = false;
            this.drag = Drag.NONE;
            this.openAmount.animateTo(0.0F);
        }
    }

    @Override
    public String tooltip() {
        return this.setting.description().isEmpty() ? null : this.setting.description();
    }
}
