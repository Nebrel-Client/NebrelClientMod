package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.RangeSetting;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.setting.StringSetting;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * One labelled setting: a name, an optional value readout, and a control.
 *
 * <p>Two layouts, chosen by the control's shape. Compact controls (a toggle, a
 * dropdown, a swatch) sit on the right of the label. Wide controls (sliders,
 * chip groups) get their own line underneath, because squeezing a slider into
 * the right third makes it fiddly to drag accurately.</p>
 */
public final class SettingRow extends Component {

    private static final float INLINE_CONTROL_WIDTH = 92.0F;
    private static final float LABEL_GAP = 4.0F;
    private static final float ROW_PADDING_Y = 5.0F;

    private final Setting<?> setting;
    private final Component control;
    private final boolean stacked;

    public SettingRow(UiContext ui, Setting<?> setting, Consumer<Boolean> keybindCapture) {
        super(ui);
        this.setting = setting;
        this.control = build(ui, setting, keybindCapture);
        this.stacked = this.control instanceof SliderComponent
                || this.control instanceof RangeSliderComponent
                || this.control instanceof MultiSelectComponent<?>;
    }

    /** Maps a setting to the control that edits it. */
    private static Component build(UiContext ui, Setting<?> setting, Consumer<Boolean> keybindCapture) {
        if (setting instanceof KeybindSetting keybind) {
            return new KeybindComponent(ui, keybind, keybindCapture);
        }
        if (setting instanceof BooleanSetting bool) {
            return new ToggleComponent(ui, bool::get, bool::set);
        }
        if (setting instanceof NumberSetting number) {
            return new SliderComponent(ui, number);
        }
        if (setting instanceof ColorSetting color) {
            return new ColorPickerComponent(ui, color);
        }
        if (setting instanceof StringSetting string) {
            TextFieldComponent field = new TextFieldComponent(ui, string::get, string::set,
                    string.maxLength());
            field.placeholder(string.placeholder());
            return field;
        }
        if (setting instanceof RangeSetting range) {
            return new RangeSliderComponent(ui, range);
        }
        if (setting instanceof MultiSelectSetting<?> multi) {
            return buildMulti(ui, multi);
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            return buildDropdown(ui, enumSetting);
        }
        throw new IllegalArgumentException("No control for setting type " + setting.getClass().getName());
    }

    // The two helpers below exist purely to capture the enum type variable,
    // which cannot be done inline in an instanceof pattern.
    private static <E extends Enum<E>> Component buildDropdown(UiContext ui, EnumSetting<E> setting) {
        return new DropdownComponent<>(ui, setting);
    }

    private static <E extends Enum<E>> Component buildMulti(UiContext ui, MultiSelectSetting<E> setting) {
        return new MultiSelectComponent<>(ui, setting);
    }

    public Setting<?> setting() {
        return this.setting;
    }

    public Component control() {
        return this.control;
    }

    @Override
    public boolean visible() {
        return super.visible() && this.setting.visible();
    }

    @Override
    public float preferredHeight() {
        if (!visible()) {
            return 0.0F;
        }
        float labelHeight = RenderUtil.lineHeight();
        if (!this.stacked) {
            return Math.max(labelHeight, this.control.preferredHeight()) + ROW_PADDING_Y * 2.0F;
        }
        if (this.control instanceof MultiSelectComponent<?> multi) {
            // Chips wrap, so the height depends on the available width.
            multi.measure(Math.max(40.0F, this.width));
        }
        return labelHeight + LABEL_GAP + this.control.preferredHeight() + ROW_PADDING_Y * 2.0F;
    }

    /** Positions the control for the current bounds. Call before rendering. */
    public void layout() {
        float labelHeight = RenderUtil.lineHeight();
        if (this.stacked) {
            this.control.setBounds(this.x, this.y + ROW_PADDING_Y + labelHeight + LABEL_GAP,
                    this.width, this.control.preferredHeight());
        } else {
            float controlWidth = this.control instanceof ToggleComponent
                    ? ToggleComponent.TRACK_WIDTH
                    : INLINE_CONTROL_WIDTH;
            float controlHeight = this.control.preferredHeight();
            this.control.setBounds(this.x + this.width - controlWidth,
                    this.y + (height() - controlHeight) / 2.0F,
                    controlWidth, controlHeight);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        layout();

        boolean rowHovered = isHovered(mouseX, mouseY);
        if (rowHovered) {
            RenderUtil.roundedRect(context, this.x - 6.0F, this.y, this.width + 12.0F, this.height, 5.0F,
                    ColorUtil.withAlpha(theme.surfaceHover, 90));
        }

        float labelY = this.y + ROW_PADDING_Y;
        int labelColor = this.setting.isDefault() ? theme.textSecondary : theme.textPrimary;

        // The value readout doubles as the control's label for wide controls.
        String value = this.setting.displayValue();
        float valueWidth = this.stacked ? RenderUtil.textWidth(value) + 6.0F : 0.0F;
        float labelBudget = this.width - (this.stacked ? valueWidth : INLINE_CONTROL_WIDTH + 8.0F);

        RenderUtil.textFlat(context,
                RenderUtil.truncate(this.setting.name(), (int) Math.max(20.0F, labelBudget)),
                this.x, labelY, labelColor);

        if (this.stacked) {
            RenderUtil.textRight(context, value, this.x + this.width, labelY, theme.accent);
        }

        this.control.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
        if (visible()) {
            this.control.renderOverlay(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible()) {
            return false;
        }
        // Right-clicking a row's label resets that setting to its default.
        if (button == 1 && isHovered(mouseX, mouseY) && !this.control.isHovered(mouseX, mouseY)) {
            this.setting.reset();
            return true;
        }
        return this.control.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return visible() && this.control.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return visible() && this.control.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return visible() && this.control.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return visible() && this.control.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return visible() && this.control.charTyped(chr, modifiers);
    }

    @Override
    public boolean hasOpenOverlay() {
        return visible() && this.control.hasOpenOverlay();
    }

    @Override
    public void closeOverlay() {
        this.control.closeOverlay();
    }

    @Override
    public String tooltip() {
        String description = this.setting.description();
        return description == null || description.isBlank() ? null : description;
    }
}
