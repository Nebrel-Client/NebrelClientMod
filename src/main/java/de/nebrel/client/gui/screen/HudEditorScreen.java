package de.nebrel.client.gui.screen;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.component.IconButtonComponent;
import de.nebrel.client.gui.component.ModalComponent;
import de.nebrel.client.gui.layout.SettingsListView;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.HudManager;
import de.nebrel.client.hud.HudWidget;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct-manipulation editor for the HUD.
 *
 * <p>Widgets are dragged where they belong and snap to the screen's centre
 * lines, its edges and to each other's edges, with the guides drawn only while
 * a snap is active. Releasing a widget re-anchors it to the nearest corner or
 * edge, so a HUD laid out here survives a resolution change.</p>
 */
public final class HudEditorScreen extends Screen {

    private static final float SNAP_DISTANCE = 5.0F;
    private static final float PANEL_WIDTH = 190.0F;
    private static final float PANEL_PADDING = 12.0F;
    private static final float TOOLBAR_HEIGHT = 30.0F;

    private final NebrelClient nebrel;
    private final UiContext ui;
    private final HudManager hud;
    private final Screen parent;

    private final SettingsListView settingsList;
    private final ModalComponent modal;
    private final IconButtonComponent doneButton;
    private final IconButtonComponent resetButton;
    private final Animation panelSlide;

    private HudWidget selected;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;

    /** Guide lines to draw this frame; rebuilt on each drag step. */
    private final List<float[]> activeGuides = new ArrayList<>(4);

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Nebrel HUD Editor"));
        this.parent = parent;
        this.nebrel = NebrelClient.get();
        this.ui = this.nebrel.ui();
        this.hud = this.nebrel.hud();

        this.settingsList = new SettingsListView(this.ui);
        this.modal = new ModalComponent(this.ui);
        this.panelSlide = new Animation(0.0F, this.ui.duration(200L), Easing.EASE_OUT_CUBIC);

        this.doneButton = new IconButtonComponent(this.ui, "✓", this::finish)
                .style(IconButtonComponent.Style.ACCENT)
                .label("Done");
        this.resetButton = new IconButtonComponent(this.ui, "↺", this::confirmReset)
                .style(IconButtonComponent.Style.SOFT)
                .label("Reset Layout");
    }

    @Override
    protected void init() {
        // Every widget is drawn while editing, including disabled ones, so the
        // user can position something before switching it on.
        this.hud.setEditorPreview(true);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        this.hud.setEditorPreview(false);
        this.nebrel.config().saveNow();
    }

    private void finish() {
        this.client.setScreen(this.parent);
    }

    private void confirmReset() {
        this.modal.show("Reset the HUD layout?",
                "Every widget goes back to its default position, size and style. "
                        + "This cannot be undone.",
                "Reset",
                () -> {
                    this.hud.resetAll();
                    this.selected = null;
                    this.nebrel.config().markDirty();
                    this.nebrel.notifications().info("HUD", "Layout reset to defaults.");
                });
    }

    private void select(HudWidget widget) {
        if (this.selected == widget) {
            return;
        }
        this.selected = widget;
        this.settingsList.closeOverlays();
        if (widget == null) {
            this.panelSlide.animateTo(0.0F);
        } else {
            this.settingsList.setSettings(widget.settings(), capturing -> {
            });
            this.settingsList.resetScroll();
            this.panelSlide.animateTo(1.0F);
        }
    }

    // -- rendering -----------------------------------------------------------

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // A light scrim only: the point of the editor is to see the real HUD
        // against the real world.
        RenderUtil.rect(context, 0.0F, 0.0F, this.width, this.height,
                ColorUtil.withAlpha(this.ui.theme().scrim, 90));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        Theme theme = this.ui.theme();

        if (this.ui.settings().hudEditorGrid.get()) {
            renderGrid(context, theme);
        }

        // The widgets themselves, drawn by the same code that draws them in game.
        this.hud.render(context, this.width, this.height, this.nebrel.accent());

        renderWidgetChrome(context, mouseX, mouseY, theme);
        renderGuides(context, theme);
        renderToolbar(context, mouseX, mouseY, delta, theme);
        renderSidePanel(context, mouseX, mouseY, delta, theme);

        this.modal.render(context, mouseX, mouseY, delta);
    }

    private void renderGrid(DrawContext context, Theme theme) {
        float spacing = this.ui.settings().hudEditorGridSize.getFloat();
        if (spacing < 2.0F) {
            return;
        }
        int color = ColorUtil.withAlpha(theme.border, 46);
        for (float x = 0.0F; x < this.width; x += spacing) {
            RenderUtil.rect(context, x, 0.0F, 1.0F, this.height, color);
        }
        for (float y = 0.0F; y < this.height; y += spacing) {
            RenderUtil.rect(context, 0.0F, y, this.width, 1.0F, color);
        }
    }

    /** Outlines every widget, and highlights the hovered and selected ones. */
    private void renderWidgetChrome(DrawContext context, int mouseX, int mouseY, Theme theme) {
        HudWidget hovered = this.dragging ? this.selected : this.hud.widgetAt(mouseX, mouseY);

        for (HudWidget widget : this.hud.widgets()) {
            if (widget.lastWidth() <= 0.0F) {
                continue;
            }
            boolean isSelected = widget == this.selected;
            boolean isHovered = widget == hovered;
            boolean off = !widget.enabled.get();

            int outline;
            if (isSelected) {
                outline = theme.accent;
            } else if (isHovered) {
                outline = ColorUtil.withAlpha(theme.textPrimary, 170);
            } else {
                outline = ColorUtil.withAlpha(off ? theme.textDisabled : theme.border, 120);
            }

            RenderUtil.roundedOutline(context, widget.lastX() - 1.0F, widget.lastY() - 1.0F,
                    widget.lastWidth() + 2.0F, widget.lastHeight() + 2.0F, 4.0F, outline);

            if (off) {
                // Disabled widgets are visible but clearly marked as not shown.
                RenderUtil.roundedRect(context, widget.lastX() - 1.0F, widget.lastY() - 1.0F,
                        widget.lastWidth() + 2.0F, widget.lastHeight() + 2.0F, 4.0F,
                        ColorUtil.withAlpha(theme.background, 130));
                RenderUtil.textScaledCentered(context, "off",
                        widget.lastX() + widget.lastWidth() / 2.0F,
                        widget.lastY() + widget.lastHeight() / 2.0F - 3.0F, 0.8F,
                        ColorUtil.withAlpha(theme.textDisabled, 230));
            }

            if (isSelected) {
                // Corner ticks, so the selection reads even over a busy scene.
                float size = 3.0F;
                float left = widget.lastX() - 1.0F;
                float top = widget.lastY() - 1.0F;
                float right = left + widget.lastWidth() + 2.0F;
                float bottom = top + widget.lastHeight() + 2.0F;
                RenderUtil.rect(context, left - 1.0F, top - 1.0F, size, size, theme.accent);
                RenderUtil.rect(context, right - size + 1.0F, top - 1.0F, size, size, theme.accent);
                RenderUtil.rect(context, left - 1.0F, bottom - size + 1.0F, size, size, theme.accent);
                RenderUtil.rect(context, right - size + 1.0F, bottom - size + 1.0F, size, size,
                        theme.accent);

                RenderUtil.textScaled(context, widget.name(), left, top - 10.0F, 0.85F,
                        theme.accent, true);
            }
        }
    }

    private void renderGuides(DrawContext context, Theme theme) {
        if (this.activeGuides.isEmpty()) {
            return;
        }
        int color = ColorUtil.withAlpha(theme.accent, 190);
        for (float[] guide : this.activeGuides) {
            // {isVertical, position}
            if (guide[0] > 0.5F) {
                RenderUtil.rect(context, guide[1], 0.0F, 1.0F, this.height, color);
            } else {
                RenderUtil.rect(context, 0.0F, guide[1], this.width, 1.0F, color);
            }
        }
    }

    private void renderToolbar(DrawContext context, int mouseX, int mouseY, float delta, Theme theme) {
        float toolbarWidth = this.doneButton.measuredWidth() + this.resetButton.measuredWidth() + 22.0F;
        float toolbarX = (this.width - toolbarWidth) / 2.0F;
        float toolbarY = this.height - TOOLBAR_HEIGHT - 10.0F;

        RenderUtil.shadow(context, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT, 9.0F,
                theme.shadow, 4);
        RenderUtil.roundedRect(context, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT, 9.0F,
                theme.surfaceElevated);
        RenderUtil.roundedOutline(context, toolbarX, toolbarY, toolbarWidth, TOOLBAR_HEIGHT, 9.0F,
                theme.border);

        float buttonY = toolbarY + (TOOLBAR_HEIGHT - 20.0F) / 2.0F;
        this.resetButton.setBounds(toolbarX + 8.0F, buttonY, this.resetButton.measuredWidth(), 20.0F);
        this.resetButton.render(context, mouseX, mouseY, delta);

        this.doneButton.setBounds(toolbarX + 14.0F + this.resetButton.measuredWidth(), buttonY,
                this.doneButton.measuredWidth(), 20.0F);
        this.doneButton.render(context, mouseX, mouseY, delta);

        if (this.selected == null) {
            RenderUtil.textCentered(context, "Click a widget to move and configure it",
                    this.width / 2.0F, toolbarY - 14.0F,
                    ColorUtil.withAlpha(theme.textSecondary, 220));
        }
    }

    private void renderSidePanel(DrawContext context, int mouseX, int mouseY, float delta, Theme theme) {
        float amount = this.panelSlide.value();
        if (amount <= 0.01F || this.selected == null) {
            return;
        }
        float panelX = this.width - PANEL_WIDTH * amount;
        float panelY = 0.0F;
        float panelHeight = this.height;

        RenderUtil.rect(context, panelX, panelY, PANEL_WIDTH, panelHeight,
                ColorUtil.fadeAlpha(theme.surface, 0.97F * amount));
        RenderUtil.rect(context, panelX, panelY, 1.0F, panelHeight,
                ColorUtil.fadeAlpha(theme.divider, amount));

        RenderUtil.textFlat(context, this.selected.name(), panelX + PANEL_PADDING, 14.0F,
                ColorUtil.fadeAlpha(theme.textPrimary, amount));
        RenderUtil.textScaled(context,
                this.selected.anchor.get().displayName() + "  ·  "
                        + Math.round(this.selected.offsetX()) + ", "
                        + Math.round(this.selected.offsetY()),
                panelX + PANEL_PADDING, 14.0F + RenderUtil.lineHeight() + 2.0F, 0.85F,
                ColorUtil.fadeAlpha(theme.textSecondary, amount), false);

        RenderUtil.rect(context, panelX + PANEL_PADDING, 42.0F,
                PANEL_WIDTH - PANEL_PADDING * 2.0F, 1.0F,
                ColorUtil.fadeAlpha(theme.divider, amount));

        this.settingsList.setBounds(panelX + PANEL_PADDING, 48.0F,
                PANEL_WIDTH - PANEL_PADDING * 2.0F, panelHeight - 48.0F - TOOLBAR_HEIGHT - 22.0F);
        this.settingsList.render(context, mouseX, mouseY, delta);
    }

    private boolean overSidePanel(double mouseX) {
        return this.selected != null && this.panelSlide.value() > 0.5F
                && mouseX >= this.width - PANEL_WIDTH;
    }

    // -- dragging and snapping -----------------------------------------------

    /**
     * Snaps a proposed position to the nearest guide and records the guides hit.
     *
     * @return the adjusted {left, top}
     */
    private float[] applySnapping(HudWidget widget, float left, float top) {
        this.activeGuides.clear();
        float widgetWidth = widget.totalWidth();
        float widgetHeight = widget.totalHeight();

        float bestDx = Float.MAX_VALUE;
        float snappedLeft = left;
        float bestDy = Float.MAX_VALUE;
        float snappedTop = top;
        float guideX = Float.NaN;
        float guideY = Float.NaN;

        // Candidate vertical lines: screen edges, screen centre, other widgets.
        List<Float> verticals = new ArrayList<>();
        verticals.add(0.0F);
        verticals.add(this.width / 2.0F);
        verticals.add((float) this.width);
        List<Float> horizontals = new ArrayList<>();
        horizontals.add(0.0F);
        horizontals.add(this.height / 2.0F);
        horizontals.add((float) this.height);

        for (HudWidget other : this.hud.widgets()) {
            if (other == widget || other.lastWidth() <= 0.0F) {
                continue;
            }
            verticals.add(other.lastX());
            verticals.add(other.lastX() + other.lastWidth());
            horizontals.add(other.lastY());
            horizontals.add(other.lastY() + other.lastHeight());
        }

        // Each of the widget's own three x reference points can snap to a line.
        float[] widgetXs = {left, left + widgetWidth / 2.0F, left + widgetWidth};
        float[] widgetXOffsets = {0.0F, -widgetWidth / 2.0F, -widgetWidth};
        for (float line : verticals) {
            for (int i = 0; i < widgetXs.length; i++) {
                float distance = Math.abs(widgetXs[i] - line);
                if (distance < SNAP_DISTANCE && distance < bestDx) {
                    bestDx = distance;
                    snappedLeft = line + widgetXOffsets[i];
                    guideX = line;
                }
            }
        }

        float[] widgetYs = {top, top + widgetHeight / 2.0F, top + widgetHeight};
        float[] widgetYOffsets = {0.0F, -widgetHeight / 2.0F, -widgetHeight};
        for (float line : horizontals) {
            for (int i = 0; i < widgetYs.length; i++) {
                float distance = Math.abs(widgetYs[i] - line);
                if (distance < SNAP_DISTANCE && distance < bestDy) {
                    bestDy = distance;
                    snappedTop = line + widgetYOffsets[i];
                    guideY = line;
                }
            }
        }

        if (!Float.isNaN(guideX)) {
            this.activeGuides.add(new float[]{1.0F, guideX});
        }
        if (!Float.isNaN(guideY)) {
            this.activeGuides.add(new float[]{0.0F, guideY});
        }
        return new float[]{snappedLeft, snappedTop};
    }

    // -- input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modal.open()) {
            return this.modal.mouseClicked(mouseX, mouseY, button);
        }
        if (this.doneButton.mouseClicked(mouseX, mouseY, button)
                || this.resetButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (overSidePanel(mouseX) && this.settingsList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (overSidePanel(mouseX)) {
            // Swallow clicks that land on the panel background.
            return true;
        }

        HudWidget hit = this.hud.widgetAt(mouseX, mouseY);
        if (button == 0) {
            select(hit);
            if (hit != null) {
                this.dragging = true;
                this.dragOffsetX = (float) (mouseX - hit.lastX());
                this.dragOffsetY = (float) (mouseY - hit.lastY());
            }
            return true;
        }
        if (button == 1 && hit != null) {
            // Right click toggles a widget on or off in place.
            hit.enabled.toggle();
            this.nebrel.config().markDirty();
            select(hit);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.modal.open()) {
            return true;
        }
        if (this.dragging && this.selected != null) {
            float left = (float) (mouseX - this.dragOffsetX);
            float top = (float) (mouseY - this.dragOffsetY);
            float[] snapped = applySnapping(this.selected, left, top);
            this.selected.moveTo(snapped[0], snapped[1], this.width, this.height);
            return true;
        }
        if (overSidePanel(mouseX)) {
            return this.settingsList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging && this.selected != null) {
            this.dragging = false;
            this.activeGuides.clear();
            // Re-anchor to whichever corner or edge the widget ended up nearest,
            // so it tracks that part of the screen from now on.
            float centerX = this.selected.lastX() + this.selected.lastWidth() / 2.0F;
            float centerY = this.selected.lastY() + this.selected.lastHeight() / 2.0F;
            HudAnchor nearest = HudAnchor.nearest(centerX, centerY, this.width, this.height);
            this.selected.rebindAnchor(nearest, this.width, this.height);
            this.nebrel.config().markDirty();
            return true;
        }
        this.settingsList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.modal.open()) {
            return true;
        }
        if (overSidePanel(mouseX)) {
            return this.settingsList.mouseScrolled(mouseX, mouseY, vertical);
        }
        // Scrolling over a widget scales it, which is the fastest way to size one.
        HudWidget hit = this.hud.widgetAt(mouseX, mouseY);
        if (hit != null) {
            hit.scale.set(hit.scale.get() + Math.signum(vertical) * hit.scale.step());
            this.nebrel.config().markDirty();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.modal.open()) {
            return this.modal.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.selected != null && this.settingsList.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) { // Escape
            if (this.selected != null) {
                select(null);
            } else {
                finish();
            }
            return true;
        }
        if (this.selected != null) {
            // Arrow keys nudge by one pixel, or ten with shift held.
            float step = (modifiers & 0x1) != 0 ? 10.0F : 1.0F;
            float dx = 0.0F;
            float dy = 0.0F;
            switch (keyCode) {
                case 263 -> dx = -step;
                case 262 -> dx = step;
                case 265 -> dy = -step;
                case 264 -> dy = step;
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            this.selected.moveTo(
                    NebrelMath.clamp(this.selected.lastX() + dx, 0.0F,
                            this.width - this.selected.totalWidth()),
                    NebrelMath.clamp(this.selected.lastY() + dy, 0.0F,
                            this.height - this.selected.totalHeight()),
                    this.width, this.height);
            this.nebrel.config().markDirty();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.selected != null && this.settingsList.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        finish();
    }
}
