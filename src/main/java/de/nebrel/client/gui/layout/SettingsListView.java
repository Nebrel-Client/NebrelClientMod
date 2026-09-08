package de.nebrel.client.gui.layout;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.component.Component;
import de.nebrel.client.gui.component.ScrollContainer;
import de.nebrel.client.gui.component.SectionComponent;
import de.nebrel.client.gui.component.SettingRow;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A scrollable, sectioned list of settings.
 *
 * <p>Sections come from each setting's declared group, ordered by
 * {@link SettingSection#rank}. A section whose settings are all currently
 * hidden by a visibility condition is dropped entirely, heading included, so
 * turning an option off does not leave an empty "Animation" header behind.</p>
 *
 * <p>Rows are built once when the list is set, not per frame; only layout and
 * drawing happen each frame.</p>
 */
public final class SettingsListView {

    private static final float ROW_SPACING = 2.0F;
    private static final float SECTION_SPACING = 6.0F;

    private final UiContext ui;
    private final ScrollContainer scroll;

    private final List<Component> entries = new ArrayList<>();
    private final List<SettingRow> rows = new ArrayList<>();

    private float x;
    private float y;
    private float width;
    private float height;

    public SettingsListView(UiContext ui) {
        this.ui = ui;
        this.scroll = new ScrollContainer(ui);
    }

    /**
     * Rebuilds the list.
     *
     * @param settings        the settings to show, in declaration order
     * @param keybindCapture  notified while a keybind row is capturing a key
     */
    public void setSettings(List<Setting<?>> settings, Consumer<Boolean> keybindCapture) {
        this.entries.clear();
        this.rows.clear();

        Map<String, List<Setting<?>>> grouped = new LinkedHashMap<>();
        for (Setting<?> setting : settings) {
            grouped.computeIfAbsent(setting.section(), key -> new ArrayList<>()).add(setting);
        }

        List<String> sections = new ArrayList<>(grouped.keySet());
        sections.sort(Comparator.comparingInt(SettingSection::rank).thenComparing(name -> name));

        for (String section : sections) {
            List<Setting<?>> members = grouped.get(section);
            SectionComponent header = new SectionComponent(this.ui, section);
            this.entries.add(header);
            for (Setting<?> setting : members) {
                SettingRow row = new SettingRow(this.ui, setting, keybindCapture);
                this.entries.add(row);
                this.rows.add(row);
            }
        }
        this.scroll.reset();
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scroll.setBounds(x, y, width, height);
    }

    public boolean isEmpty() {
        return this.rows.isEmpty();
    }

    /** Width available to a row, leaving room for the scrollbar. */
    private float rowWidth() {
        return this.width - (this.scroll.scrollable() ? 12.0F : 2.0F);
    }

    /**
     * Whether a section header should be drawn.
     *
     * <p>A header is only useful if at least one setting under it is visible
     * right now.</p>
     */
    private boolean headerHasVisibleRows(int headerIndex) {
        for (int i = headerIndex + 1; i < this.entries.size(); i++) {
            Component entry = this.entries.get(i);
            if (entry instanceof SectionComponent) {
                return false;
            }
            if (entry.visible()) {
                return true;
            }
        }
        return false;
    }

    /** Measures the list and returns its total content height. */
    private float layout() {
        float cursor = 0.0F;
        float available = rowWidth();

        for (int i = 0; i < this.entries.size(); i++) {
            Component entry = this.entries.get(i);

            if (entry instanceof SectionComponent) {
                if (!headerHasVisibleRows(i)) {
                    entry.setVisible(false);
                    continue;
                }
                entry.setVisible(true);
                if (cursor > 0.0F) {
                    cursor += SECTION_SPACING;
                }
                entry.setBounds(this.x, this.y + cursor, available, entry.preferredHeight());
                cursor += entry.preferredHeight();
                continue;
            }

            if (!entry.visible()) {
                continue;
            }
            // Width must be set before the height is asked for: wrapping
            // controls decide their row count from the space they are given.
            entry.setBounds(this.x, this.y + cursor, available, 0.0F);
            float rowHeight = entry.preferredHeight();
            entry.setBounds(this.x, this.y + cursor, available, rowHeight);
            cursor += rowHeight + ROW_SPACING;
        }
        return cursor;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float contentHeight = layout();
        this.scroll.setContentHeight(contentHeight);
        float offset = this.scroll.offset();

        // Shift everything up by the scroll offset before drawing.
        for (Component entry : this.entries) {
            if (entry.visible()) {
                entry.setBounds(entry.x(), entry.y() - offset, entry.width(), entry.height());
            }
        }

        this.scroll.begin(context);
        for (Component entry : this.entries) {
            if (!entry.visible()) {
                continue;
            }
            // Skip anything fully outside the viewport.
            if (entry.y() + entry.height() < this.y || entry.y() > this.y + this.height) {
                continue;
            }
            entry.render(context, mouseX, mouseY, delta);
        }
        this.scroll.end(context);

        // Popovers are drawn unclipped so they can extend past the viewport.
        for (Component entry : this.entries) {
            if (entry.visible() && entry.hasOpenOverlay()) {
                entry.renderOverlay(context, mouseX, mouseY, delta);
            }
        }

        this.scroll.render(context, mouseX, mouseY, delta);

        if (this.rows.isEmpty()) {
            RenderUtil.textCentered(context, "No settings for this module",
                    this.x + this.width / 2.0F,
                    this.y + this.height / 2.0F - RenderUtil.lineHeight() / 2.0F,
                    this.ui.theme().textDisabled);
        }
    }

    /** Text of whatever the pointer is over, for the shared tooltip. */
    public String tooltipAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY)) {
            return null;
        }
        for (Component entry : this.entries) {
            if (entry.visible() && entry.isHovered(mouseX, mouseY)) {
                return entry.tooltip();
            }
        }
        return null;
    }

    private boolean inside(double mouseX, double mouseY) {
        return RenderUtil.hovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    // -- events --------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // An open popover gets first refusal, wherever the click landed: it may
        // need to close itself or select an option outside the viewport.
        for (Component entry : this.entries) {
            if (entry.visible() && entry.hasOpenOverlay()
                    && entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (this.scroll.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        for (Component entry : this.entries) {
            if (entry.visible() && entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = this.scroll.mouseReleased(mouseX, mouseY, button);
        for (Component entry : this.entries) {
            if (entry.visible()) {
                handled |= entry.mouseReleased(mouseX, mouseY, button);
            }
        }
        return handled;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.scroll.mouseDragged(mouseX, mouseY, button, dx, dy)) {
            return true;
        }
        for (Component entry : this.entries) {
            if (entry.visible() && entry.mouseDragged(mouseX, mouseY, button, dx, dy)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (Component entry : this.entries) {
            if (entry.visible() && entry.hasOpenOverlay()
                    && entry.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        for (Component entry : this.entries) {
            if (entry.visible() && entry.isHovered(mouseX, mouseY)
                    && entry.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return this.scroll.mouseScrolled(mouseX, mouseY, amount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Component entry : this.entries) {
            if (entry.visible() && entry.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        for (Component entry : this.entries) {
            if (entry.visible() && entry.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }

    /** True while any row has a popover or capture active. */
    public boolean hasOpenOverlay() {
        for (Component entry : this.entries) {
            if (entry.visible() && entry.hasOpenOverlay()) {
                return true;
            }
        }
        return false;
    }

    public void closeOverlays() {
        for (Component entry : this.entries) {
            entry.closeOverlay();
        }
    }

    public void resetScroll() {
        this.scroll.reset();
    }
}
