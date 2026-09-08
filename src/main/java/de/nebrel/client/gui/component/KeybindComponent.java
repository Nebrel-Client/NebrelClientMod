package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * A button that captures the next key press as a binding.
 *
 * <p>While listening it swallows every key, so binding Escape is possible;
 * right-clicking clears the binding instead. The parent is told when capture
 * starts and stops so global keybind handling can be suspended.</p>
 */
public final class KeybindComponent extends Component {

    private final KeybindSetting setting;
    private final Consumer<Boolean> captureListener;

    private boolean listening;

    public KeybindComponent(UiContext ui, KeybindSetting setting, Consumer<Boolean> captureListener) {
        super(ui);
        this.setting = setting;
        this.captureListener = captureListener == null ? value -> {
        } : captureListener;
        this.height = 18.0F;
    }

    @Override
    public float preferredHeight() {
        return 18.0F;
    }

    public boolean listening() {
        return this.listening;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean hovered = isHovered(mouseX, mouseY) && enabled();

        int background = this.listening ? theme.accentSoft
                : (hovered ? theme.surfaceHover : theme.surfaceElevated);
        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 5.0F, background);
        RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 5.0F,
                this.listening ? theme.accent : theme.border);

        String label = this.listening ? "Press a key..." : this.setting.displayValue();
        int color = this.listening ? theme.accent
                : (this.setting.bound() ? theme.textPrimary : theme.textDisabled);

        RenderUtil.textCentered(context, RenderUtil.truncate(label, (int) this.width - 8),
                this.x + this.width / 2.0F,
                this.y + (this.height - RenderUtil.lineHeight()) / 2.0F + 1.0F, color);

        if (this.listening) {
            RenderUtil.textScaledCentered(context, "right click to clear",
                    this.x + this.width / 2.0F, this.y + this.height + 2.0F, 0.7F,
                    ColorUtil.withAlpha(theme.textSecondary, 190));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled() || !isHovered(mouseX, mouseY)) {
            if (this.listening) {
                stopListening();
                return true;
            }
            return false;
        }
        if (button == 1) {
            this.setting.clear();
            stopListening();
            return true;
        }
        if (button == 0) {
            if (this.listening) {
                stopListening();
            } else {
                this.listening = true;
                this.captureListener.accept(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.listening) {
            return false;
        }
        // GLFW_KEY_ESCAPE cancels rather than binding; GLFW_KEY_DELETE clears.
        if (keyCode == 256) {
            stopListening();
            return true;
        }
        if (keyCode == 261) {
            this.setting.clear();
        } else {
            this.setting.set(keyCode);
        }
        stopListening();
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Swallow character input while capturing so it never reaches a text field.
        return this.listening;
    }

    private void stopListening() {
        if (this.listening) {
            this.listening = false;
            this.captureListener.accept(false);
        }
    }

    @Override
    public void closeOverlay() {
        stopListening();
    }

    @Override
    public boolean hasOpenOverlay() {
        return this.listening;
    }

    @Override
    public String tooltip() {
        return this.setting.description().isEmpty() ? null : this.setting.description();
    }
}
