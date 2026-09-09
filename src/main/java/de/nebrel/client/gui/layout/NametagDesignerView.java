package de.nebrel.client.gui.layout;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.component.NametagPreviewComponent;
import de.nebrel.client.plus.NebrelPlus;
import de.nebrel.client.plus.PlusSections;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * The nametag designer: a live preview pinned above the settings that drive it.
 *
 * <p>The preview stays fixed while the settings scroll under it, because the
 * point of the designer is watching a slider change the thing it controls — a
 * preview that scrolls out of view while you drag a slider is no preview at
 * all.</p>
 *
 * <p>The settings themselves are the ordinary {@link SettingsListView}. Nebrel+
 * gets no bespoke controls: a slider here behaves exactly like a slider in a
 * module's settings, and every setting shown is a real, persisted setting rather
 * than a designer-only knob.</p>
 */
public final class NametagDesignerView {

    private static final float PREVIEW_GAP = 10.0F;

    private final NebrelPlus plus;
    private final NametagPreviewComponent preview;
    private final SettingsListView settings;

    public NametagDesignerView(UiContext ui, NebrelPlus plus, Consumer<Boolean> keybindCapture) {
        this.plus = plus;
        this.preview = new NametagPreviewComponent(ui, plus);
        this.settings = new SettingsListView(ui);
        this.settings.setSectionOrder(PlusSections.DESIGNER_ORDER);
        this.settings.setSettings(plus.settings().allSettings(), keybindCapture);
    }

    public void setBounds(float x, float y, float width, float height) {
        float previewHeight = this.preview.preferredHeight();
        this.preview.setBounds(x, y, width, previewHeight);
        this.settings.setBounds(x, y + previewHeight + PREVIEW_GAP, width,
                Math.max(0.0F, height - previewHeight - PREVIEW_GAP));
    }

    public void resetScroll() {
        this.settings.resetScroll();
    }

    public void closeOverlays() {
        this.settings.closeOverlays();
    }

    /** Puts every Nebrel+ preference, styling included, back to its default. */
    public void resetAll() {
        this.plus.settings().resetAll();
    }

    // -- rendering -----------------------------------------------------------

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.preview.render(context, mouseX, mouseY, delta);
        this.settings.render(context, mouseX, mouseY, delta);
    }

    // -- input ---------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The settings list goes first: an open dropdown or colour picker may be
        // drawn over the preview and has to get the click.
        if (this.settings.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return this.preview.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.settings.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        return this.settings.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.settings.mouseScrolled(mouseX, mouseY, amount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.settings.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return this.settings.charTyped(chr, modifiers);
    }

    public boolean hasOpenOverlay() {
        return this.settings.hasOpenOverlay();
    }

    public String tooltipAt(double mouseX, double mouseY) {
        return this.settings.tooltipAt(mouseX, mouseY);
    }
}
