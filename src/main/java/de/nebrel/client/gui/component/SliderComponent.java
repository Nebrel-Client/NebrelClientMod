package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.gui.DrawContext;

/**
 * A single-handle slider bound to a {@link NumberSetting}.
 *
 * <p>The setting itself does the clamping and step quantisation, so dragging
 * can work in raw fractions and still never produce an out-of-range value.</p>
 */
public final class SliderComponent extends Component {

    private static final float TRACK_HEIGHT = 4.0F;
    private static final float KNOB_RADIUS = 5.0F;

    private final NumberSetting setting;
    private final Animation hover;
    private boolean dragging;

    public SliderComponent(UiContext ui, NumberSetting setting) {
        super(ui);
        this.setting = setting;
        this.hover = new Animation(0.0F, ui.duration(140L), Easing.EASE_OUT);
        this.height = 12.0F;
    }

    @Override
    public float preferredHeight() {
        return 12.0F;
    }

    /** Track bounds, inset so the knob never overhangs the row. */
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
        boolean active = this.dragging || (isHovered(mouseX, mouseY) && enabled());
        this.hover.animateTo(active);
        float hoverAmount = this.hover.value();

        float fraction = this.setting.fraction();
        float trackY = this.y + (this.height - TRACK_HEIGHT) / 2.0F;
        float left = trackLeft();
        float trackW = trackWidth();

        int trackColor = ColorUtil.lerp(theme.surfaceHover, theme.border, 0.4F);
        RenderUtil.roundedRect(context, left, trackY, trackW, TRACK_HEIGHT,
                TRACK_HEIGHT / 2.0F, enabled() ? trackColor : ColorUtil.fadeAlpha(trackColor, 0.5F));

        int fill = enabled() ? theme.accent : theme.textDisabled;
        if (hoverAmount > 0.0F) {
            fill = ColorUtil.lerp(fill, theme.accentHover, hoverAmount);
        }
        if (fraction > 0.0F) {
            RenderUtil.roundedRect(context, left, trackY, trackW * fraction, TRACK_HEIGHT,
                    TRACK_HEIGHT / 2.0F, fill);
        }

        float knobX = left + trackW * fraction;
        float knobY = this.y + this.height / 2.0F;
        float radius = KNOB_RADIUS + hoverAmount * 1.0F;

        // A ring in the surface colour separates the knob from the filled track.
        RenderUtil.roundedRect(context, knobX - radius, knobY - radius, radius * 2.0F, radius * 2.0F,
                radius, enabled() ? 0xFFFFFFFF : ColorUtil.fadeAlpha(0xFFFFFFFF, 0.5F));
        RenderUtil.roundedRect(context, knobX - radius + 1.5F, knobY - radius + 1.5F,
                (radius - 1.5F) * 2.0F, (radius - 1.5F) * 2.0F, radius - 1.5F, fill);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) {
            return false;
        }
        // Generous vertical grab area: the visible track is only 4 px tall.
        if (!RenderUtil.hovered(mouseX, mouseY, this.x, this.y - 3.0F, this.width, this.height + 6.0F)) {
            return false;
        }
        this.dragging = true;
        applyFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.dragging) {
            return false;
        }
        applyFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.dragging) {
            return false;
        }
        this.dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!enabled() || !isHovered(mouseX, mouseY)) {
            return false;
        }
        // One notch moves exactly one step, which is what the step is for.
        this.setting.set(this.setting.get() + Math.signum(amount) * this.setting.step());
        return true;
    }

    private void applyFromMouse(double mouseX) {
        double fraction = (mouseX - trackLeft()) / trackWidth();
        this.setting.setFraction(NebrelMath.clamp(fraction, 0.0D, 1.0D));
    }

    public boolean dragging() {
        return this.dragging;
    }

    @Override
    public String tooltip() {
        return this.setting.description().isEmpty() ? null : this.setting.description();
    }
}
