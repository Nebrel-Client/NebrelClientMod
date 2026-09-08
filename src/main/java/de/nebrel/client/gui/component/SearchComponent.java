package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * The module search box: a text field with a glyph and a clear button.
 *
 * <p>Reports every keystroke to {@code onQueryChanged} so filtering is live;
 * the module list is small enough that filtering per keystroke is cheaper than
 * any debounce would be.</p>
 */
public final class SearchComponent extends Component {

    private static final float ICON_WIDTH = 16.0F;
    private static final float CLEAR_WIDTH = 16.0F;

    private final TextFieldComponent field;
    private final StringBuilder buffer = new StringBuilder();
    private final Consumer<String> onQueryChanged;

    public SearchComponent(UiContext ui, Consumer<String> onQueryChanged) {
        super(ui);
        this.onQueryChanged = onQueryChanged == null ? query -> {
        } : onQueryChanged;
        this.field = new TextFieldComponent(ui,
                this.buffer::toString,
                value -> {
                    this.buffer.setLength(0);
                    this.buffer.append(value);
                    this.onQueryChanged.accept(value);
                },
                48);
        this.field.placeholder("Search modules");
        // The search box paints the surface; the field only draws text.
        this.field.chrome(false);
        this.height = 20.0F;
    }

    @Override
    public float preferredHeight() {
        return 20.0F;
    }

    public String query() {
        return this.buffer.toString();
    }

    public boolean hasQuery() {
        return !this.buffer.isEmpty();
    }

    public void clear() {
        if (this.buffer.isEmpty()) {
            return;
        }
        this.buffer.setLength(0);
        this.onQueryChanged.accept("");
    }

    public void focus() {
        this.field.setFocused(true);
    }

    public boolean focused() {
        return this.field.focused();
    }

    @Override
    public Component setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        // The field owns the middle; the glyph and clear button flank it.
        this.field.setBounds(x + ICON_WIDTH, y, width - ICON_WIDTH - CLEAR_WIDTH, height);
        return this;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean focused = this.field.focused();

        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 6.0F,
                focused ? theme.surfaceHover : theme.surface);
        RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 6.0F,
                focused ? theme.accent : theme.border);

        // Magnifier: a small ring with a diagonal tail.
        float glyphX = this.x + 8.0F;
        float glyphY = this.y + this.height / 2.0F - 1.0F;
        int glyphColor = focused ? theme.accent : theme.textSecondary;
        RenderUtil.roundedOutline(context, glyphX - 3.0F, glyphY - 3.5F, 6.0F, 6.0F, 3.0F, glyphColor);
        RenderUtil.rect(context, glyphX + 2.0F, glyphY + 2.5F, 1.0F, 1.0F, glyphColor);
        RenderUtil.rect(context, glyphX + 3.0F, glyphY + 3.5F, 1.0F, 1.0F, glyphColor);

        this.field.render(context, mouseX, mouseY, delta);

        if (hasQuery()) {
            float clearX = this.x + this.width - CLEAR_WIDTH + 3.0F;
            float clearY = this.y + this.height / 2.0F;
            boolean hovered = RenderUtil.hovered(mouseX, mouseY,
                    clearX - 5.0F, clearY - 6.0F, 12.0F, 12.0F);
            int color = hovered ? theme.textPrimary : theme.textSecondary;
            // A small cross drawn from two diagonals.
            for (int i = -3; i <= 3; i++) {
                RenderUtil.rect(context, clearX + i, clearY + i, 1.0F, 1.0F, color);
                RenderUtil.rect(context, clearX + i, clearY - i, 1.0F, 1.0F, color);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled()) {
            return false;
        }
        if (hasQuery()) {
            float clearX = this.x + this.width - CLEAR_WIDTH + 3.0F;
            float clearY = this.y + this.height / 2.0F;
            if (RenderUtil.hovered(mouseX, mouseY, clearX - 6.0F, clearY - 7.0F, 14.0F, 14.0F)) {
                clear();
                return true;
            }
        }
        if (isHovered(mouseX, mouseY)) {
            // Clicking anywhere in the box focuses the field, not just the text.
            this.field.setFocused(true);
            this.field.mouseClicked(
                    Math.max(mouseX, this.field.x() + 1.0F), this.field.y() + this.field.height() / 2.0F,
                    button);
            return true;
        }
        this.field.setFocused(false);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.field.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.field.focused()) {
            return false;
        }
        // Escape clears first, and only unfocuses once the box is already empty.
        if (keyCode == 256 && hasQuery()) {
            clear();
            return true;
        }
        return this.field.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.field.charTyped(chr, modifiers);
    }

    @Override
    public boolean hasOpenOverlay() {
        return this.field.focused();
    }

    @Override
    public void closeOverlay() {
        this.field.setFocused(false);
    }
}
