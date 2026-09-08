package de.nebrel.client.hud;

import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A HUD widget made of label/value rows.
 *
 * <p>Covers most of the Nebrel HUD: a name and a number, sometimes several
 * lines of them. The row list is a reusable field refilled in {@link #update()}
 * once per frame, so drawing the HUD allocates nothing.</p>
 */
public abstract class LabeledHudWidget extends HudWidget {

    /** How a row's label is presented. */
    public enum LabelMode {
        LABEL_AND_VALUE("Label and Value"),
        VALUE_ONLY("Value Only"),
        LABEL_ONLY("Label Only");

        private final String display;

        LabelMode(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    /** One line of the widget. */
    protected static final class Row {
        String label = "";
        String value = "";

        void set(String label, String value) {
            this.label = label == null ? "" : label;
            this.value = value == null ? "" : value;
        }
    }

    private static final float ROW_GAP = 1.0F;
    private static final int LABEL_VALUE_GAP = 4;

    /** Reused across frames; grown but never shrunk. */
    private final List<Row> pool = new ArrayList<>(4);
    private int rowCount;

    public final EnumSetting<LabelMode> labelMode;
    public final BooleanSetting alignRight;

    protected LabeledHudWidget(String id, String name, HudAnchor defaultAnchor,
                               float defaultOffsetX, float defaultOffsetY, boolean enabledByDefault) {
        super(id, name, defaultAnchor, defaultOffsetX, defaultOffsetY, enabledByDefault);

        this.labelMode = add(new EnumSetting<>("hud." + id + ".labelMode", "Label",
                "How the row label is shown", LabelMode.LABEL_AND_VALUE));
        this.labelMode.section(SettingSection.APPEARANCE);

        this.alignRight = add(new BooleanSetting("hud." + id + ".alignRight", "Right Align",
                "Align rows to the right edge", false));
        this.alignRight.section(SettingSection.APPEARANCE);
    }

    // -- row building --------------------------------------------------------

    /** Clears the row list; call at the top of {@link #buildRows()}. */
    private void resetRows() {
        this.rowCount = 0;
    }

    /** Appends a row, reusing a pooled instance. */
    protected final void row(String label, String value) {
        Row target;
        if (this.rowCount < this.pool.size()) {
            target = this.pool.get(this.rowCount);
        } else {
            target = new Row();
            this.pool.add(target);
        }
        target.set(label, value);
        this.rowCount++;
    }

    /** Fills the row list for this frame using {@link #row(String, String)}. */
    protected abstract void buildRows();

    @Override
    public final void update() {
        resetRows();
        buildRows();
    }

    @Override
    public boolean hasContent() {
        return this.rowCount > 0;
    }

    protected final int rowCount() {
        return this.rowCount;
    }

    // -- measurement and drawing ---------------------------------------------

    private String labelTextOf(Row row) {
        return switch (this.labelMode.get()) {
            case LABEL_AND_VALUE, LABEL_ONLY -> row.label;
            case VALUE_ONLY -> "";
        };
    }

    private String valueTextOf(Row row) {
        return this.labelMode.get() == LabelMode.LABEL_ONLY ? "" : row.value;
    }

    private float rowWidth(Row row) {
        String label = labelTextOf(row);
        String value = valueTextOf(row);
        float width = 0.0F;
        if (!label.isEmpty()) {
            width += RenderUtil.textWidth(label);
        }
        if (!value.isEmpty()) {
            if (width > 0.0F) {
                width += LABEL_VALUE_GAP;
            }
            width += RenderUtil.textWidth(value);
        }
        return width;
    }

    @Override
    public float contentWidth() {
        float widest = 0.0F;
        for (int i = 0; i < this.rowCount; i++) {
            widest = Math.max(widest, rowWidth(this.pool.get(i)));
        }
        // Never collapse to nothing: a zero-width widget cannot be grabbed.
        return Math.max(widest, 8.0F);
    }

    @Override
    public float contentHeight() {
        if (this.rowCount == 0) {
            return RenderUtil.lineHeight();
        }
        return this.rowCount * RenderUtil.lineHeight() + (this.rowCount - 1) * ROW_GAP;
    }

    @Override
    protected void renderContent(DrawContext context, float x, float y,
                                 int valueColor, int labelColor, boolean shadow) {
        float lineHeight = RenderUtil.lineHeight();
        float total = contentWidth();

        for (int i = 0; i < this.rowCount; i++) {
            Row row = this.pool.get(i);
            String label = labelTextOf(row);
            String value = valueTextOf(row);
            float rowY = y + i * (lineHeight + ROW_GAP);

            // Right alignment shifts the whole row, keeping label and value
            // adjacent rather than pushing them to opposite edges.
            float cursor = x;
            if (this.alignRight.get()) {
                cursor = x + (total - rowWidth(row));
            }

            if (!label.isEmpty()) {
                if (shadow) {
                    RenderUtil.text(context, label, cursor, rowY, labelColor);
                } else {
                    RenderUtil.textFlat(context, label, cursor, rowY, labelColor);
                }
                cursor += RenderUtil.textWidth(label);
                if (!value.isEmpty()) {
                    cursor += LABEL_VALUE_GAP;
                }
            }
            if (!value.isEmpty()) {
                if (shadow) {
                    RenderUtil.text(context, value, cursor, rowY, valueColor);
                } else {
                    RenderUtil.textFlat(context, value, cursor, rowY, valueColor);
                }
            }
        }
    }
}
