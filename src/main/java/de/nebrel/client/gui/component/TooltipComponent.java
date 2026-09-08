package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A single tooltip, shared by the whole screen.
 *
 * <p>One instance rather than one per component: only one tooltip can be
 * visible, and centralising it means the delay, fade and edge clamping are
 * written once. A screen calls {@link #request} while collecting hover state
 * and {@link #render} last, so the tooltip always sits on top.</p>
 */
public final class TooltipComponent {

    private static final long DELAY_MILLIS = 350L;
    private static final float MAX_WIDTH = 190.0F;
    private static final float PADDING = 6.0F;

    private final UiContext ui;
    private final Animation fade;
    private final List<String> lines = new ArrayList<>(4);

    private String pending;
    private String shown;
    private long hoverStartedAt;
    private float anchorX;
    private float anchorY;

    public TooltipComponent(UiContext ui) {
        this.ui = ui;
        this.fade = new Animation(0.0F, ui.duration(140L), Easing.EASE_OUT);
    }

    /** Called each frame with the text under the pointer, or null for none. */
    public void request(String text, float mouseX, float mouseY) {
        this.anchorX = mouseX;
        this.anchorY = mouseY;

        if (text == null || text.isBlank()) {
            this.pending = null;
            this.fade.animateTo(0.0F);
            return;
        }
        if (!text.equals(this.pending)) {
            this.pending = text;
            this.hoverStartedAt = System.currentTimeMillis();
            this.fade.animateTo(0.0F);
            return;
        }
        if (System.currentTimeMillis() - this.hoverStartedAt >= DELAY_MILLIS) {
            if (!text.equals(this.shown)) {
                this.shown = text;
                wrap(text);
            }
            this.fade.animateTo(1.0F);
        }
    }

    /** Splits the text into lines that fit {@link #MAX_WIDTH}. */
    private void wrap(String text) {
        this.lines.clear();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (RenderUtil.textWidth(candidate) > MAX_WIDTH - PADDING * 2.0F && !line.isEmpty()) {
                this.lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            this.lines.add(line.toString());
        }
    }

    public void render(DrawContext context) {
        float amount = this.fade.value();
        if (amount <= 0.01F || this.lines.isEmpty()) {
            return;
        }
        Theme theme = this.ui.theme();
        int lineHeight = RenderUtil.lineHeight();

        float width = 0.0F;
        for (String line : this.lines) {
            width = Math.max(width, RenderUtil.textWidth(line));
        }
        width += PADDING * 2.0F;
        float height = this.lines.size() * (lineHeight + 1.0F) - 1.0F + PADDING * 2.0F;

        // Offset from the cursor, then clamped so it never leaves the window.
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        float left = NebrelMath.clamp(this.anchorX + 10.0F, 4.0F, screenWidth - width - 4.0F);
        float top = this.anchorY + 14.0F;
        if (top + height > screenHeight - 4.0F) {
            // Flip above the cursor when there is no room below.
            top = this.anchorY - height - 6.0F;
        }
        top = NebrelMath.clamp(top, 4.0F, Math.max(4.0F, screenHeight - height - 4.0F));

        RenderUtil.shadow(context, left, top, width, height, 6.0F,
                ColorUtil.fadeAlpha(theme.shadow, amount), 3);
        RenderUtil.roundedRect(context, left, top, width, height, 6.0F,
                ColorUtil.fadeAlpha(theme.surfaceElevated, amount));
        RenderUtil.roundedOutline(context, left, top, width, height, 6.0F,
                ColorUtil.fadeAlpha(theme.border, amount));

        for (int i = 0; i < this.lines.size(); i++) {
            RenderUtil.textFlat(context, this.lines.get(i),
                    left + PADDING, top + PADDING + i * (lineHeight + 1.0F),
                    ColorUtil.fadeAlpha(theme.textSecondary, amount));
        }
    }

    /** Hides immediately, e.g. when the view changes under the cursor. */
    public void dismiss() {
        this.pending = null;
        this.shown = null;
        this.fade.set(0.0F);
        this.lines.clear();
    }
}
