package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.SmoothScroll;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.gui.DrawContext;

/**
 * A clipped, scrollable viewport.
 *
 * <p>Owns the scissor rectangle, the smooth-scroll state and the scrollbar. The
 * caller supplies the content height and draws inside
 * {@link #begin}/{@link #end}, translating by {@link #offset()}.</p>
 *
 * <p>The scrollbar thumb is draggable, and dragging keeps working when the
 * pointer leaves the track, which is what people expect from a scrollbar.</p>
 */
public final class ScrollContainer extends Component {

    private static final float BAR_WIDTH = 4.0F;
    private static final float BAR_INSET = 3.0F;
    private static final float STEP_PIXELS = 28.0F;

    private final SmoothScroll scroll = new SmoothScroll();
    private float contentHeight;
    private boolean draggingThumb;
    private float dragGrabOffset;

    public ScrollContainer(UiContext ui) {
        super(ui);
    }

    /** Tells the container how tall its content is; call before rendering. */
    public void setContentHeight(float value) {
        this.contentHeight = Math.max(0.0F, value);
        this.scroll.updateBounds(this.contentHeight, this.height);
    }

    public float contentHeight() {
        return this.contentHeight;
    }

    /** Current scroll offset in pixels from the top of the content. */
    public float offset() {
        this.scroll.setSmoothing(this.ui.settings().smoothScrolling.get());
        return this.scroll.offset();
    }

    public boolean scrollable() {
        return this.scroll.scrollable();
    }

    public void reset() {
        this.scroll.reset();
    }

    /** Starts clipping to the viewport. Always pair with {@link #end}. */
    public void begin(DrawContext context) {
        RenderUtil.pushClip(context, this.x, this.y, this.width, this.height);
    }

    public void end(DrawContext context) {
        RenderUtil.popClip(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The container itself only draws the scrollbar; content is the caller's.
        if (!this.scroll.scrollable()) {
            return;
        }
        Theme theme = theme();
        boolean nearBar = RenderUtil.hovered(mouseX, mouseY,
                barLeft() - 4.0F, this.y, BAR_WIDTH + 8.0F, this.height);
        float emphasis = this.draggingThumb || nearBar ? 1.0F : 0.55F;

        RenderUtil.scrollbar(context, barLeft(), this.y + 1.0F, BAR_WIDTH, this.height - 2.0F,
                this.scroll.progress(), visibleRatio(),
                ColorUtil.fadeAlpha(theme.divider, emphasis * 0.8F),
                ColorUtil.fadeAlpha(this.draggingThumb ? theme.accent : theme.border, emphasis));
    }

    private float barLeft() {
        return this.x + this.width - BAR_WIDTH - BAR_INSET;
    }

    private float visibleRatio() {
        return this.contentHeight <= 0.0F ? 1.0F : this.height / this.contentHeight;
    }

    private float thumbHeight() {
        return Math.max(BAR_WIDTH * 2.0F,
                (this.height - 2.0F) * NebrelMath.clamp(visibleRatio(), 0.05F, 1.0F));
    }

    private float thumbTop() {
        float travel = (this.height - 2.0F) - thumbHeight();
        return this.y + 1.0F + travel * this.scroll.progress();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isHovered(mouseX, mouseY) || !this.scroll.scrollable()) {
            return false;
        }
        this.scroll.scroll(amount, STEP_PIXELS);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.scroll.scrollable()) {
            return false;
        }
        if (!RenderUtil.hovered(mouseX, mouseY, barLeft() - 4.0F, this.y, BAR_WIDTH + 8.0F, this.height)) {
            return false;
        }
        float top = thumbTop();
        float height = thumbHeight();
        if (mouseY >= top && mouseY <= top + height) {
            this.draggingThumb = true;
            this.dragGrabOffset = (float) (mouseY - top);
        } else {
            // Click on the track jumps the thumb to the pointer.
            this.draggingThumb = true;
            this.dragGrabOffset = height / 2.0F;
            applyThumbDrag(mouseY);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.draggingThumb) {
            return false;
        }
        applyThumbDrag(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.draggingThumb) {
            return false;
        }
        this.draggingThumb = false;
        return true;
    }

    private void applyThumbDrag(double mouseY) {
        float travel = (this.height - 2.0F) - thumbHeight();
        if (travel <= 0.0F) {
            return;
        }
        float progress = (float) NebrelMath.clamp(
                (mouseY - this.dragGrabOffset - (this.y + 1.0F)) / travel, 0.0D, 1.0D);
        this.scroll.scrollTo(progress * this.scroll.maxOffset());
    }

    public boolean draggingThumb() {
        return this.draggingThumb;
    }
}
