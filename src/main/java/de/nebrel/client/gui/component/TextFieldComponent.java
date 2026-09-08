package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A single-line text input.
 *
 * <p>Written from scratch rather than wrapping the vanilla widget so it follows
 * the theme, scrolls horizontally inside the client's own clip rectangle, and
 * can live in a scrolling settings list without fighting vanilla's focus
 * handling.</p>
 */
public class TextFieldComponent extends Component {

    private static final long BLINK_MILLIS = 530L;
    private static final float PADDING = 6.0F;

    private final Supplier<String> reader;
    private final Consumer<String> writer;
    private final int maxLength;

    private String placeholder = "";
    private boolean chrome = true;
    private boolean focused;
    private int cursor;
    private int selectionAnchor;
    private float scrollOffset;
    private long focusedAt;

    public TextFieldComponent(UiContext ui, Supplier<String> reader, Consumer<String> writer,
                              int maxLength) {
        super(ui);
        this.reader = reader;
        this.writer = writer;
        this.maxLength = Math.max(1, maxLength);
        this.height = 18.0F;
        this.cursor = text().length();
        this.selectionAnchor = this.cursor;
    }

    @Override
    public float preferredHeight() {
        return 18.0F;
    }

    public TextFieldComponent placeholder(String text) {
        this.placeholder = text == null ? "" : text;
        return this;
    }

    /**
     * Turns the field's own background and outline off.
     *
     * <p>Used when the field is embedded in a container that already draws the
     * surface, such as the search box, so the two borders do not stack.</p>
     */
    public TextFieldComponent chrome(boolean draw) {
        this.chrome = draw;
        return this;
    }

    protected String text() {
        String value = this.reader.get();
        return value == null ? "" : value;
    }

    protected void setText(String value) {
        String clipped = value.length() > this.maxLength ? value.substring(0, this.maxLength) : value;
        this.writer.accept(clipped);
        this.cursor = NebrelMath.clamp(this.cursor, 0, clipped.length());
        this.selectionAnchor = NebrelMath.clamp(this.selectionAnchor, 0, clipped.length());
    }

    public boolean focused() {
        return this.focused;
    }

    public void setFocused(boolean value) {
        if (this.focused == value) {
            return;
        }
        this.focused = value;
        this.focusedAt = System.currentTimeMillis();
        if (value) {
            this.cursor = text().length();
            this.selectionAnchor = this.cursor;
        }
    }

    // -- rendering -----------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean hovered = isHovered(mouseX, mouseY) && enabled();

        if (this.chrome) {
            RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 5.0F,
                    this.focused || hovered ? theme.surfaceHover : theme.surfaceElevated);
            RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 5.0F,
                    this.focused ? theme.accent : theme.border);
        }

        String value = text();
        float textY = this.y + (this.height - RenderUtil.lineHeight()) / 2.0F + 1.0F;
        float innerX = this.x + PADDING;
        float innerWidth = this.width - PADDING * 2.0F;

        updateScroll(value, innerWidth);

        RenderUtil.pushClip(context, innerX, this.y + 1.0F, innerWidth, this.height - 2.0F);

        if (value.isEmpty() && !this.focused) {
            RenderUtil.textFlat(context, RenderUtil.truncate(this.placeholder, (int) innerWidth),
                    innerX, textY, theme.textDisabled);
        } else {
            if (hasSelection()) {
                int start = Math.min(this.cursor, this.selectionAnchor);
                int end = Math.max(this.cursor, this.selectionAnchor);
                float selectionX = innerX - this.scrollOffset + RenderUtil.textWidth(value.substring(0, start));
                float selectionWidth = RenderUtil.textWidth(value.substring(start, end));
                RenderUtil.rect(context, selectionX, this.y + 3.0F, selectionWidth, this.height - 6.0F,
                        ColorUtil.withAlpha(theme.accent, 70));
            }
            RenderUtil.textFlat(context, value, innerX - this.scrollOffset, textY,
                    enabled() ? theme.textPrimary : theme.textDisabled);
        }

        if (this.focused && caretVisible()) {
            float caretX = innerX - this.scrollOffset
                    + RenderUtil.textWidth(value.substring(0, NebrelMath.clamp(this.cursor, 0, value.length())));
            RenderUtil.rect(context, caretX, this.y + 3.5F, 1.0F, this.height - 7.0F, theme.accent);
        }

        RenderUtil.popClip(context);
    }

    private boolean caretVisible() {
        return ((System.currentTimeMillis() - this.focusedAt) / BLINK_MILLIS) % 2 == 0;
    }

    private boolean hasSelection() {
        return this.cursor != this.selectionAnchor;
    }

    /** Keeps the caret inside the visible area as the text grows. */
    private void updateScroll(String value, float innerWidth) {
        float caretX = RenderUtil.textWidth(value.substring(0, NebrelMath.clamp(this.cursor, 0, value.length())));
        if (caretX - this.scrollOffset > innerWidth - 2.0F) {
            this.scrollOffset = caretX - innerWidth + 2.0F;
        }
        if (caretX - this.scrollOffset < 0.0F) {
            this.scrollOffset = caretX;
        }
        float total = RenderUtil.textWidth(value);
        this.scrollOffset = NebrelMath.clamp(this.scrollOffset, 0.0F, Math.max(0.0F, total - innerWidth + 2.0F));
    }

    // -- interaction ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled()) {
            return false;
        }
        boolean inside = isHovered(mouseX, mouseY);
        if (!inside) {
            setFocused(false);
            return false;
        }
        setFocused(true);
        this.cursor = indexAt(mouseX);
        this.selectionAnchor = this.cursor;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.focused) {
            return false;
        }
        this.cursor = indexAt(mouseX);
        return true;
    }

    /** Nearest character boundary to a screen x coordinate. */
    private int indexAt(double mouseX) {
        String value = text();
        float local = (float) (mouseX - (this.x + PADDING) + this.scrollOffset);
        float accumulated = 0.0F;
        for (int i = 0; i < value.length(); i++) {
            float charWidth = RenderUtil.textWidth(String.valueOf(value.charAt(i)));
            if (local < accumulated + charWidth / 2.0F) {
                return i;
            }
            accumulated += charWidth;
        }
        return value.length();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused || !enabled()) {
            return false;
        }
        String value = text();
        boolean control = (modifiers & 0x2) != 0;
        boolean shift = (modifiers & 0x1) != 0;

        switch (keyCode) {
            case 259 -> { // Backspace
                if (hasSelection()) {
                    deleteSelection(value);
                } else if (this.cursor > 0) {
                    setText(value.substring(0, this.cursor - 1) + value.substring(this.cursor));
                    this.cursor--;
                    this.selectionAnchor = this.cursor;
                }
                return true;
            }
            case 261 -> { // Delete
                if (hasSelection()) {
                    deleteSelection(value);
                } else if (this.cursor < value.length()) {
                    setText(value.substring(0, this.cursor) + value.substring(this.cursor + 1));
                }
                return true;
            }
            case 263 -> { // Left
                this.cursor = Math.max(0, this.cursor - 1);
                if (!shift) {
                    this.selectionAnchor = this.cursor;
                }
                return true;
            }
            case 262 -> { // Right
                this.cursor = Math.min(value.length(), this.cursor + 1);
                if (!shift) {
                    this.selectionAnchor = this.cursor;
                }
                return true;
            }
            case 268 -> { // Home
                this.cursor = 0;
                if (!shift) {
                    this.selectionAnchor = 0;
                }
                return true;
            }
            case 269 -> { // End
                this.cursor = value.length();
                if (!shift) {
                    this.selectionAnchor = this.cursor;
                }
                return true;
            }
            case 257, 335 -> { // Enter, numpad Enter
                onSubmit();
                return true;
            }
            case 256 -> { // Escape
                setFocused(false);
                return true;
            }
            default -> {
                if (control) {
                    return handleShortcut(keyCode, value);
                }
                return false;
            }
        }
    }

    private boolean handleShortcut(int keyCode, String value) {
        MinecraftClient client = MinecraftClient.getInstance();
        switch (keyCode) {
            case 65 -> { // A
                this.selectionAnchor = 0;
                this.cursor = value.length();
                return true;
            }
            case 67 -> { // C
                if (hasSelection()) {
                    client.keyboard.setClipboard(selectedText(value));
                }
                return true;
            }
            case 88 -> { // X
                if (hasSelection()) {
                    client.keyboard.setClipboard(selectedText(value));
                    deleteSelection(value);
                }
                return true;
            }
            case 86 -> { // V
                insert(client.keyboard.getClipboard());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private String selectedText(String value) {
        int start = Math.min(this.cursor, this.selectionAnchor);
        int end = Math.max(this.cursor, this.selectionAnchor);
        return value.substring(start, end);
    }

    private void deleteSelection(String value) {
        int start = Math.min(this.cursor, this.selectionAnchor);
        int end = Math.max(this.cursor, this.selectionAnchor);
        setText(value.substring(0, start) + value.substring(end));
        this.cursor = start;
        this.selectionAnchor = start;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.focused || !enabled() || chr == '\n' || chr == '\r') {
            return false;
        }
        insert(String.valueOf(chr));
        return true;
    }

    private void insert(String addition) {
        if (addition == null || addition.isEmpty()) {
            return;
        }
        String cleaned = addition.replaceAll("[\\n\\r\\t]", " ");
        String value = text();
        if (hasSelection()) {
            deleteSelection(value);
            value = text();
        }
        int room = this.maxLength - value.length();
        if (room <= 0) {
            return;
        }
        String toInsert = cleaned.length() > room ? cleaned.substring(0, room) : cleaned;
        setText(value.substring(0, this.cursor) + toInsert + value.substring(this.cursor));
        this.cursor += toInsert.length();
        this.selectionAnchor = this.cursor;
    }

    /** Hook for subclasses; the base field does nothing on Enter. */
    protected void onSubmit() {
        setFocused(false);
    }

    @Override
    public boolean hasOpenOverlay() {
        return this.focused;
    }

    @Override
    public void closeOverlay() {
        setFocused(false);
    }
}
