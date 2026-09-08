package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * A horizontal tab strip with a sliding underline.
 *
 * <p>The underline animates between tabs rather than snapping, which makes the
 * relationship between the old and new selection obvious.</p>
 */
public final class TabComponent extends Component {

    private static final float UNDERLINE_HEIGHT = 2.0F;
    private static final float TAB_PADDING = 10.0F;

    private final List<String> labels;
    private final IntSupplier selected;
    private final Consumer<Integer> onSelect;

    private final Animation underlineX;
    private final Animation underlineWidth;
    private int lastSelected = -1;

    public TabComponent(UiContext ui, List<String> labels, IntSupplier selected, Consumer<Integer> onSelect) {
        super(ui);
        this.labels = List.copyOf(labels);
        this.selected = selected;
        this.onSelect = onSelect;
        this.underlineX = new Animation(0.0F, ui.duration(200L), Easing.EASE_OUT_CUBIC);
        this.underlineWidth = new Animation(0.0F, ui.duration(200L), Easing.EASE_OUT_CUBIC);
        this.height = 24.0F;
    }

    @Override
    public float preferredHeight() {
        return 24.0F;
    }

    private float tabWidth(String label) {
        return RenderUtil.textWidth(label) + TAB_PADDING * 2.0F;
    }

    private float tabLeft(int index) {
        float cursor = this.x;
        for (int i = 0; i < index; i++) {
            cursor += tabWidth(this.labels.get(i));
        }
        return cursor;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible() || this.labels.isEmpty()) {
            return;
        }
        Theme theme = theme();
        int current = clampIndex(this.selected.getAsInt());

        if (current != this.lastSelected) {
            this.lastSelected = current;
            float target = tabLeft(current);
            float targetWidth = tabWidth(this.labels.get(current));
            if (this.underlineWidth.target() == 0.0F) {
                // First frame: place the underline rather than sliding it in.
                this.underlineX.set(target);
                this.underlineWidth.set(targetWidth);
            } else {
                this.underlineX.animateTo(target);
                this.underlineWidth.animateTo(targetWidth);
            }
        }

        for (int i = 0; i < this.labels.size(); i++) {
            String label = this.labels.get(i);
            float left = tabLeft(i);
            float tabW = tabWidth(label);
            boolean hovered = RenderUtil.hovered(mouseX, mouseY, left, this.y, tabW,
                    this.height - UNDERLINE_HEIGHT);
            boolean isSelected = i == current;

            int color = isSelected ? theme.textPrimary
                    : (hovered ? theme.textPrimary : theme.textSecondary);
            RenderUtil.textCentered(context, label, left + tabW / 2.0F,
                    this.y + (this.height - UNDERLINE_HEIGHT - RenderUtil.lineHeight()) / 2.0F + 1.0F,
                    color);
        }

        // Track line under the whole strip, then the accent segment on top.
        RenderUtil.rect(context, this.x, this.y + this.height - UNDERLINE_HEIGHT,
                this.width, 1.0F, ColorUtil.withAlpha(theme.divider, 200));
        RenderUtil.roundedRect(context, this.underlineX.value(),
                this.y + this.height - UNDERLINE_HEIGHT,
                this.underlineWidth.value(), UNDERLINE_HEIGHT,
                UNDERLINE_HEIGHT / 2.0F, theme.accent);
    }

    private int clampIndex(int index) {
        if (this.labels.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(this.labels.size() - 1, index));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) {
            return false;
        }
        for (int i = 0; i < this.labels.size(); i++) {
            float left = tabLeft(i);
            float tabW = tabWidth(this.labels.get(i));
            if (RenderUtil.hovered(mouseX, mouseY, left, this.y, tabW, this.height)) {
                this.onSelect.accept(i);
                return true;
            }
        }
        return false;
    }
}
