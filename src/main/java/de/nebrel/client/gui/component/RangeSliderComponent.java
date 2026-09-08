package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.RangeSetting;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.gui.DrawContext;

/**
 * A two-handle slider bound to a {@link RangeSetting}.
 *
 * <p>The grabbed handle is decided once on press by proximity, so dragging one
 * handle past the other does not hand the drag over mid-gesture; the setting
 * reorders the pair instead.</p>
 */
public final class RangeSliderComponent extends Component {

    private static final float TRACK_HEIGHT = 4.0F;
    private static final float KNOB_RADIUS = 5.0F;

    private final RangeSetting setting;
    private int draggingHandle = -1;

    public RangeSliderComponent(UiContext ui, RangeSetting setting) {
        super(ui);
        this.setting = setting;
        this.height = 12.0F;
    }

    @Override
    public float preferredHeight() {
        return 12.0F;
    }

    private float trackLeft() {
        return this.x + KNOB_RADIUS;
    }

    private float trackWidth() {
        return Math.max(1.0F, this.width - KNOB_RADIUS * 2.0F);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        float trackY = this.y + (this.height - TRACK_HEIGHT) / 2.0F;
        float left = trackLeft();
        float trackW = trackWidth();

        RenderUtil.roundedRect(context, left, trackY, trackW, TRACK_HEIGHT, TRACK_HEIGHT / 2.0F,
                ColorUtil.lerp(theme.surfaceHover, theme.border, 0.4F));

        float lowFraction = this.setting.lowFraction();
        float highFraction = this.setting.highFraction();
        float bandX = left + trackW * lowFraction;
        float bandW = trackW * (highFraction - lowFraction);
        int fill = enabled() ? theme.accent : theme.textDisabled;
        if (bandW > 0.0F) {
            RenderUtil.roundedRect(context, bandX, trackY, bandW, TRACK_HEIGHT,
                    TRACK_HEIGHT / 2.0F, fill);
        }

        drawHandle(context, left + trackW * lowFraction, fill);
        drawHandle(context, left + trackW * highFraction, fill);
    }

    private void drawHandle(DrawContext context, float centerX, int fill) {
        float centerY = this.y + this.height / 2.0F;
        RenderUtil.roundedRect(context, centerX - KNOB_RADIUS, centerY - KNOB_RADIUS,
                KNOB_RADIUS * 2.0F, KNOB_RADIUS * 2.0F, KNOB_RADIUS, 0xFFFFFFFF);
        RenderUtil.roundedRect(context, centerX - KNOB_RADIUS + 1.5F, centerY - KNOB_RADIUS + 1.5F,
                (KNOB_RADIUS - 1.5F) * 2.0F, (KNOB_RADIUS - 1.5F) * 2.0F, KNOB_RADIUS - 1.5F, fill);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) {
            return false;
        }
        if (!RenderUtil.hovered(mouseX, mouseY, this.x, this.y - 3.0F, this.width, this.height + 6.0F)) {
            return false;
        }
        float left = trackLeft();
        float trackW = trackWidth();
        double lowX = left + trackW * this.setting.lowFraction();
        double highX = left + trackW * this.setting.highFraction();
        // Whichever handle is nearer the press owns the whole gesture.
        this.draggingHandle = Math.abs(mouseX - lowX) <= Math.abs(mouseX - highX) ? 0 : 1;
        applyFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingHandle < 0) {
            return false;
        }
        applyFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingHandle < 0) {
            return false;
        }
        this.draggingHandle = -1;
        return true;
    }

    private void applyFromMouse(double mouseX) {
        double fraction = NebrelMath.clamp((mouseX - trackLeft()) / trackWidth(), 0.0D, 1.0D);
        double value = this.setting.min() + fraction * (this.setting.max() - this.setting.min());
        if (this.draggingHandle == 0) {
            this.setting.setLow(value);
        } else {
            this.setting.setHigh(value);
        }
    }

    @Override
    public String tooltip() {
        return this.setting.description().isEmpty() ? null : this.setting.description();
    }
}
