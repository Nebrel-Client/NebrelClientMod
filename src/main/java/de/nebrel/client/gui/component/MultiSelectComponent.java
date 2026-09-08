package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * A row of togglable chips, one per enum constant.
 *
 * <p>Chips rather than a checklist popover: the option count here is small
 * (three or four entity kinds, a handful of particle types) and showing them
 * all inline means the current selection is readable at a glance.</p>
 *
 * @param <E> the enum being selected from
 */
public final class MultiSelectComponent<E extends Enum<E>> extends Component {

    private static final float CHIP_HEIGHT = 15.0F;
    private static final float CHIP_GAP = 4.0F;
    private static final float CHIP_PADDING = 7.0F;

    private final MultiSelectSetting<E> setting;
    private int rows = 1;

    public MultiSelectComponent(UiContext ui, MultiSelectSetting<E> setting) {
        super(ui);
        this.setting = setting;
        this.height = CHIP_HEIGHT;
    }

    @Override
    public float preferredHeight() {
        return this.rows * CHIP_HEIGHT + (this.rows - 1) * CHIP_GAP;
    }

    private float chipWidth(E option) {
        return RenderUtil.textWidth(EnumSetting.label(option)) + CHIP_PADDING * 2.0F;
    }

    /**
     * Lays the chips out, wrapping onto further rows when they do not fit.
     *
     * @param visitor receives each chip's bounds and option
     */
    private void layout(ChipVisitor<E> visitor) {
        List<E> options = this.setting.options();
        float cursorX = this.x;
        float cursorY = this.y;
        int row = 1;

        for (E option : options) {
            float chipW = chipWidth(option);
            if (cursorX > this.x && cursorX + chipW > this.x + this.width) {
                cursorX = this.x;
                cursorY += CHIP_HEIGHT + CHIP_GAP;
                row++;
            }
            visitor.visit(option, cursorX, cursorY, chipW);
            cursorX += chipW + CHIP_GAP;
        }
        this.rows = row;
    }

    private interface ChipVisitor<E> {
        void visit(E option, float chipX, float chipY, float chipWidth);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        layout((option, chipX, chipY, chipW) -> {
            boolean selected = this.setting.has(option);
            boolean hovered = RenderUtil.hovered(mouseX, mouseY, chipX, chipY, chipW, CHIP_HEIGHT)
                    && enabled();

            int background = selected
                    ? theme.accentSoft
                    : (hovered ? theme.surfaceHover : theme.surface);
            RenderUtil.roundedRect(context, chipX, chipY, chipW, CHIP_HEIGHT, CHIP_HEIGHT / 2.0F,
                    background);
            RenderUtil.roundedOutline(context, chipX, chipY, chipW, CHIP_HEIGHT, CHIP_HEIGHT / 2.0F,
                    selected ? theme.accent : theme.border);

            int textColor = selected ? theme.accent
                    : (enabled() ? theme.textSecondary : theme.textDisabled);
            RenderUtil.textFlat(context, EnumSetting.label(option), chipX + CHIP_PADDING,
                    chipY + (CHIP_HEIGHT - RenderUtil.lineHeight()) / 2.0F + 1.0F, textColor);
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) {
            return false;
        }
        boolean[] consumed = {false};
        layout((option, chipX, chipY, chipW) -> {
            if (!consumed[0] && RenderUtil.hovered(mouseX, mouseY, chipX, chipY, chipW, CHIP_HEIGHT)) {
                this.setting.toggle(option);
                consumed[0] = true;
            }
        });
        return consumed[0];
    }

    @Override
    public String tooltip() {
        return this.setting.description().isEmpty() ? null : this.setting.description();
    }

    /** Short summary of the selection, shown in the settings row header. */
    public String summary() {
        return this.setting.displayValue();
    }

    /**
     * Recomputes the wrapped row count for the given width.
     *
     * <p>The settings view calls this before asking for
     * {@link #preferredHeight()}, because a chip row that wraps needs two lines
     * of space rather than one.</p>
     */
    public void measure(float availableWidth) {
        this.width = availableWidth;
        layout((option, chipX, chipY, chipW) -> {
        });
    }
}
