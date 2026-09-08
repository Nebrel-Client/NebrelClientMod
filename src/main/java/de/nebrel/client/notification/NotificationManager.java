package de.nebrel.client.notification;

import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.gui.theme.ThemeManager;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Toast notifications in the top-right corner.
 *
 * <p>Capped at {@link #MAX_VISIBLE} so a misbehaving module cannot bury the
 * screen: extra toasts queue and appear as slots free up.</p>
 */
public final class NotificationManager {

    private static final int MAX_VISIBLE = 4;
    private static final int WIDTH = 172;
    private static final int PADDING = 8;
    private static final int GAP = 6;
    private static final long DEFAULT_LIFETIME = 3_600L;

    private final List<Notification> active = new ArrayList<>();
    private final Deque<Notification> queued = new ArrayDeque<>();
    private final ThemeManager themes;
    private final ClientSettings settings;

    public NotificationManager(ThemeManager themes, ClientSettings settings) {
        this.themes = themes;
        this.settings = settings;
    }

    public void push(String title, String message, Notification.Level level) {
        if (!this.settings.notifications.get()) {
            return;
        }
        Notification notification = new Notification(title, message, level, DEFAULT_LIFETIME);
        if (this.active.size() < MAX_VISIBLE) {
            this.active.add(notification);
        } else if (this.queued.size() < 16) {
            this.queued.addLast(notification);
        }
        // Beyond 16 queued, drop: nobody is going to read them anyway.
    }

    public void info(String title, String message) {
        push(title, message, Notification.Level.INFO);
    }

    public void success(String title, String message) {
        push(title, message, Notification.Level.SUCCESS);
    }

    public void warning(String title, String message) {
        push(title, message, Notification.Level.WARNING);
    }

    public void error(String title, String message) {
        push(title, message, Notification.Level.ERROR);
    }

    public void clear() {
        this.active.clear();
        this.queued.clear();
    }

    /** Drops expired toasts and promotes queued ones. Called once per tick. */
    public void tick() {
        this.active.removeIf(Notification::expired);
        while (this.active.size() < MAX_VISIBLE && !this.queued.isEmpty()) {
            this.active.add(this.queued.pollFirst());
        }
    }

    /** Draws the stack. {@code screenWidth} is in scaled GUI pixels. */
    public void render(DrawContext context, int screenWidth) {
        if (this.active.isEmpty()) {
            return;
        }
        Theme theme = this.themes.current();
        int lineHeight = RenderUtil.lineHeight();
        float y = 8.0F;

        for (int i = 0; i < this.active.size(); i++) {
            Notification notification = this.active.get(i);
            float visibility = Easing.EASE_OUT_CUBIC.apply(notification.visibility());
            if (visibility <= 0.01F) {
                continue;
            }

            float height = PADDING * 2.0F + lineHeight
                    + (notification.hasMessage() ? lineHeight + 2.0F : 0.0F);
            // Slide in from the right edge as it fades.
            float x = screenWidth - WIDTH - 8.0F + (1.0F - visibility) * (WIDTH + 12.0F);

            RenderUtil.shadow(context, x, y, WIDTH, height, 7.0F,
                    ColorUtil.fadeAlpha(theme.shadow, visibility), 3);
            RenderUtil.roundedRect(context, x, y, WIDTH, height, 7.0F,
                    ColorUtil.fadeAlpha(theme.surfaceElevated, visibility));
            RenderUtil.roundedOutline(context, x, y, WIDTH, height, 7.0F,
                    ColorUtil.fadeAlpha(theme.border, visibility));

            // A coloured rail on the left communicates severity without an icon.
            int rail = switch (notification.level()) {
                case SUCCESS -> theme.success;
                case WARNING -> theme.warning;
                case ERROR -> theme.error;
                case INFO -> theme.accent;
            };
            RenderUtil.roundedRect(context, x + 3.0F, y + 6.0F, 2.0F, height - 12.0F, 1.0F,
                    ColorUtil.fadeAlpha(rail, visibility));

            float textX = x + PADDING + 3.0F;
            int textWidth = WIDTH - PADDING * 2 - 6;
            RenderUtil.textFlat(context,
                    RenderUtil.truncate(notification.title(), textWidth),
                    textX, y + PADDING,
                    ColorUtil.fadeAlpha(theme.textPrimary, visibility));

            if (notification.hasMessage()) {
                RenderUtil.textScaled(context,
                        RenderUtil.truncate(notification.message(), (int) (textWidth / 0.85F)),
                        textX, y + PADDING + lineHeight + 2.0F, 0.85F,
                        ColorUtil.fadeAlpha(theme.textSecondary, visibility), false);
            }

            y += height + GAP;
        }
    }
}
