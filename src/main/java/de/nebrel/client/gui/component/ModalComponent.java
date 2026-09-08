package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A centred confirmation dialog with a scrim.
 *
 * <p>Used for destructive actions such as resetting a module's settings or the
 * whole HUD layout. While open it swallows every click and key, so the screen
 * behind it cannot be interacted with by accident.</p>
 */
public final class ModalComponent {

    private static final float WIDTH = 230.0F;
    private static final float PADDING = 14.0F;
    private static final float BUTTON_HEIGHT = 20.0F;
    private static final float BUTTON_GAP = 8.0F;

    private final UiContext ui;
    private final Animation fade;
    private final List<String> bodyLines = new ArrayList<>(3);

    private boolean open;
    private String title = "";
    private String confirmLabel = "Confirm";
    private Runnable onConfirm = () -> {
    };

    public ModalComponent(UiContext ui) {
        this.ui = ui;
        this.fade = new Animation(0.0F, ui.duration(170L), Easing.EASE_OUT_CUBIC);
    }

    public boolean open() {
        return this.open;
    }

    /** Opens the dialog. {@code onConfirm} runs only if the user confirms. */
    public void show(String title, String body, String confirmLabel, Runnable onConfirm) {
        this.title = title == null ? "" : title;
        this.confirmLabel = confirmLabel == null ? "Confirm" : confirmLabel;
        this.onConfirm = onConfirm == null ? () -> {
        } : onConfirm;
        wrap(body == null ? "" : body);
        this.open = true;
        this.fade.set(0.0F);
        this.fade.animateTo(1.0F);
    }

    public void close() {
        if (this.open) {
            this.open = false;
            this.fade.animateTo(0.0F);
        }
    }

    private void wrap(String text) {
        this.bodyLines.clear();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (RenderUtil.textWidth(candidate) > WIDTH - PADDING * 2.0F && !line.isEmpty()) {
                this.bodyLines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            this.bodyLines.add(line.toString());
        }
    }

    private float height() {
        int lineHeight = RenderUtil.lineHeight();
        return PADDING * 2.0F + lineHeight + 6.0F
                + this.bodyLines.size() * (lineHeight + 2.0F)
                + 10.0F + BUTTON_HEIGHT;
    }

    private float left() {
        return (MinecraftClient.getInstance().getWindow().getScaledWidth() - WIDTH) / 2.0F;
    }

    private float top() {
        return (MinecraftClient.getInstance().getWindow().getScaledHeight() - height()) / 2.0F;
    }

    private float buttonWidth() {
        return (WIDTH - PADDING * 2.0F - BUTTON_GAP) / 2.0F;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float amount = this.fade.value();
        if (amount <= 0.01F) {
            return;
        }
        Theme theme = this.ui.theme();
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        RenderUtil.rect(context, 0.0F, 0.0F, screenWidth, screenHeight,
                ColorUtil.fadeAlpha(0xB0000000, amount));

        float boxLeft = left();
        float boxTop = top();
        float boxHeight = height();
        // Rise slightly as it fades in.
        boxTop += (1.0F - amount) * 8.0F;

        RenderUtil.shadow(context, boxLeft, boxTop, WIDTH, boxHeight, 10.0F,
                ColorUtil.fadeAlpha(theme.shadow, amount), 6);
        RenderUtil.roundedRect(context, boxLeft, boxTop, WIDTH, boxHeight, 10.0F,
                ColorUtil.fadeAlpha(theme.surfaceElevated, amount));
        RenderUtil.roundedOutline(context, boxLeft, boxTop, WIDTH, boxHeight, 10.0F,
                ColorUtil.fadeAlpha(theme.border, amount));

        int lineHeight = RenderUtil.lineHeight();
        float cursorY = boxTop + PADDING;
        RenderUtil.textFlat(context, this.title, boxLeft + PADDING, cursorY,
                ColorUtil.fadeAlpha(theme.textPrimary, amount));
        cursorY += lineHeight + 6.0F;

        for (String line : this.bodyLines) {
            RenderUtil.textFlat(context, line, boxLeft + PADDING, cursorY,
                    ColorUtil.fadeAlpha(theme.textSecondary, amount));
            cursorY += lineHeight + 2.0F;
        }

        cursorY += 10.0F;
        float buttonW = buttonWidth();

        boolean cancelHovered = RenderUtil.hovered(mouseX, mouseY,
                boxLeft + PADDING, cursorY, buttonW, BUTTON_HEIGHT);
        RenderUtil.roundedRect(context, boxLeft + PADDING, cursorY, buttonW, BUTTON_HEIGHT, 5.0F,
                ColorUtil.fadeAlpha(cancelHovered ? theme.surfaceHover : theme.surface, amount));
        RenderUtil.roundedOutline(context, boxLeft + PADDING, cursorY, buttonW, BUTTON_HEIGHT, 5.0F,
                ColorUtil.fadeAlpha(theme.border, amount));
        RenderUtil.textCentered(context, "Cancel", boxLeft + PADDING + buttonW / 2.0F,
                cursorY + (BUTTON_HEIGHT - lineHeight) / 2.0F + 1.0F,
                ColorUtil.fadeAlpha(theme.textSecondary, amount));

        float confirmX = boxLeft + PADDING + buttonW + BUTTON_GAP;
        boolean confirmHovered = RenderUtil.hovered(mouseX, mouseY,
                confirmX, cursorY, buttonW, BUTTON_HEIGHT);
        RenderUtil.roundedRect(context, confirmX, cursorY, buttonW, BUTTON_HEIGHT, 5.0F,
                ColorUtil.fadeAlpha(confirmHovered ? theme.error : ColorUtil.shade(theme.error, -0.12F),
                        amount));
        RenderUtil.textCentered(context, this.confirmLabel, confirmX + buttonW / 2.0F,
                cursorY + (BUTTON_HEIGHT - lineHeight) / 2.0F + 1.0F,
                ColorUtil.fadeAlpha(0xFFFFFFFF, amount));
    }

    /** @return true when the dialog consumed the click */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.open) {
            return false;
        }
        if (button == 0) {
            float boxLeft = left();
            float boxTop = top();
            float cursorY = boxTop + PADDING + RenderUtil.lineHeight() + 6.0F
                    + this.bodyLines.size() * (RenderUtil.lineHeight() + 2.0F) + 10.0F;
            float buttonW = buttonWidth();

            if (RenderUtil.hovered(mouseX, mouseY, boxLeft + PADDING, cursorY, buttonW, BUTTON_HEIGHT)) {
                close();
                return true;
            }
            float confirmX = boxLeft + PADDING + buttonW + BUTTON_GAP;
            if (RenderUtil.hovered(mouseX, mouseY, confirmX, cursorY, buttonW, BUTTON_HEIGHT)) {
                Runnable action = this.onConfirm;
                close();
                action.run();
                return true;
            }
        }
        // Everything else is swallowed: the modal is exclusive while open.
        return true;
    }

    /** @return true when the dialog consumed the key */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.open) {
            return false;
        }
        if (keyCode == 256) { // Escape
            close();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter
            Runnable action = this.onConfirm;
            close();
            action.run();
            return true;
        }
        return true;
    }
}
