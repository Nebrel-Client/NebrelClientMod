package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

/**
 * Base class for every control in the client menu.
 *
 * <p>Components are positioned by their parent each frame rather than owning a
 * layout, which keeps the panel logic in one place and lets a component be
 * reused inside a card, a settings row or a modal without change.</p>
 */
public abstract class Component {

    protected final UiContext ui;

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    private boolean enabled = true;
    private boolean visible = true;

    protected Component(UiContext ui) {
        this.ui = ui;
    }

    // -- layout --------------------------------------------------------------

    public Component setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float width() {
        return this.width;
    }

    public float height() {
        return this.height;
    }

    /** Natural height for this control; parents use it to lay out a column. */
    public float preferredHeight() {
        return 20.0F;
    }

    // -- state ---------------------------------------------------------------

    public boolean enabled() {
        return this.enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean value) {
        this.visible = value;
    }

    protected Theme theme() {
        return this.ui.theme();
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return this.visible && RenderUtil.hovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    // -- interaction ---------------------------------------------------------

    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    /** @return true when the click was consumed */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    /**
     * Drawn after every component, so popovers escape their row's clipping.
     *
     * <p>Dropdowns and colour pickers use this; everything else ignores it.</p>
     */
    public void renderOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    /** True while a popover is open and should swallow clicks elsewhere. */
    public boolean hasOpenOverlay() {
        return false;
    }

    /** Closes any popover; called when the parent view changes. */
    public void closeOverlay() {
    }

    /** Tooltip text for this component, or null. */
    public String tooltip() {
        return null;
    }
}
