package de.nebrel.client.module.impl.utility;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * Borderless windowed mode.
 *
 * <p>Removes the window decorations and sizes the window to fill the monitor it
 * is on. Unlike exclusive fullscreen this keeps alt-tabbing instant and lets
 * overlays draw on top, which is the reason most people want it.</p>
 *
 * <p>The original position, size and decoration state are captured before the
 * first change and restored when the module is switched off or the game closes,
 * so this never leaves a window the user cannot get back.</p>
 */
public final class BorderlessWindowModule extends Module implements ClientTickHook {

    public final BooleanSetting fillMonitor;
    public final BooleanSetting rememberMonitor;
    public final BooleanSetting restoreOnDisable;
    public final KeybindSetting toggleKey;

    private boolean applied;
    private boolean savedState;
    private int savedX;
    private int savedY;
    private int savedWidth;
    private int savedHeight;
    private boolean savedDecorated;
    private int monitorIndex = -1;

    public BorderlessWindowModule() {
        super("borderless_window", "Borderless Window",
                "Fill the screen without exclusive fullscreen",
                ModuleCategory.UTILITY, "▢");

        this.fillMonitor = bool("fillMonitor", "Fill Monitor",
                "Resize the window to cover the whole monitor", true);

        this.rememberMonitor = bool("rememberMonitor", "Remember Monitor",
                "Return to the same monitor next time", true, SettingSection.BEHAVIOR);

        this.restoreOnDisable = bool("restore", "Restore Window State",
                "Put the window back where it was when switching this off", true,
                SettingSection.BEHAVIOR);

        this.toggleKey = register(new KeybindSetting("borderless_window.toggleKey", "Toggle Key",
                "Switches borderless mode on and off", KeybindSetting.UNBOUND));
    }

    @Override
    protected void onEnable() {
        apply(MinecraftClient.getInstance());
    }

    @Override
    protected void onDisable() {
        restore(MinecraftClient.getInstance());
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (this.toggleKey.bound() && client.currentScreen == null
                && NebrelClient.get().keybinds().justPressed(this.toggleKey.get())) {
            if (this.applied) {
                restore(client);
            } else {
                apply(client);
            }
            return;
        }
        if (!this.applied && this.fillMonitor.get()) {
            apply(client);
        }
    }

    private void apply(MinecraftClient client) {
        if (this.applied || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        if (handle == 0L) {
            return;
        }
        // Exclusive fullscreen owns the window; borderless would fight it.
        if (client.getWindow().isFullscreen()) {
            NebrelClient.get().notifications().warning("Borderless Window",
                    "Leave fullscreen first, then enable this.");
            return;
        }

        saveState(handle);

        long monitor = resolveMonitor(handle);
        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        if (mode == null) {
            return;
        }
        int[] monitorX = new int[1];
        int[] monitorY = new int[1];
        GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        if (this.fillMonitor.get()) {
            GLFW.glfwSetWindowPos(handle, monitorX[0], monitorY[0]);
            GLFW.glfwSetWindowSize(handle, mode.width(), mode.height());
        }
        this.applied = true;
    }

    private void restore(MinecraftClient client) {
        if (!this.applied || client.getWindow() == null) {
            this.applied = false;
            return;
        }
        long handle = client.getWindow().getHandle();
        if (handle == 0L) {
            this.applied = false;
            return;
        }

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED,
                this.savedDecorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        if (this.restoreOnDisable.get() && this.savedState) {
            GLFW.glfwSetWindowPos(handle, this.savedX, this.savedY);
            GLFW.glfwSetWindowSize(handle, this.savedWidth, this.savedHeight);
        }
        this.applied = false;
    }

    private void saveState(long handle) {
        if (this.savedState) {
            return;
        }
        int[] x = new int[1];
        int[] y = new int[1];
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetWindowPos(handle, x, y);
        GLFW.glfwGetWindowSize(handle, width, height);
        this.savedX = x[0];
        this.savedY = y[0];
        this.savedWidth = width[0];
        this.savedHeight = height[0];
        this.savedDecorated = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE;
        this.savedState = true;
    }

    /**
     * Picks the monitor to fill.
     *
     * <p>Chooses whichever monitor the window's centre currently sits on, so a
     * multi-monitor setup fills the screen the game is actually on. The choice
     * is remembered when the setting asks for it.</p>
     */
    private long resolveMonitor(long handle) {
        long[] monitors = monitorList();
        if (monitors.length == 0) {
            return GLFW.glfwGetPrimaryMonitor();
        }
        if (this.rememberMonitor.get() && this.monitorIndex >= 0
                && this.monitorIndex < monitors.length) {
            return monitors[this.monitorIndex];
        }

        int[] windowX = new int[1];
        int[] windowY = new int[1];
        int[] windowWidth = new int[1];
        int[] windowHeight = new int[1];
        GLFW.glfwGetWindowPos(handle, windowX, windowY);
        GLFW.glfwGetWindowSize(handle, windowWidth, windowHeight);
        int centerX = windowX[0] + windowWidth[0] / 2;
        int centerY = windowY[0] + windowHeight[0] / 2;

        for (int i = 0; i < monitors.length; i++) {
            GLFWVidMode mode = GLFW.glfwGetVideoMode(monitors[i]);
            if (mode == null) {
                continue;
            }
            int[] monitorX = new int[1];
            int[] monitorY = new int[1];
            GLFW.glfwGetMonitorPos(monitors[i], monitorX, monitorY);
            if (centerX >= monitorX[0] && centerX < monitorX[0] + mode.width()
                    && centerY >= monitorY[0] && centerY < monitorY[0] + mode.height()) {
                this.monitorIndex = i;
                return monitors[i];
            }
        }
        this.monitorIndex = 0;
        return monitors[0];
    }

    private static long[] monitorList() {
        org.lwjgl.PointerBuffer buffer = GLFW.glfwGetMonitors();
        if (buffer == null) {
            return new long[0];
        }
        long[] monitors = new long[buffer.limit()];
        for (int i = 0; i < monitors.length; i++) {
            monitors[i] = buffer.get(i);
        }
        return monitors;
    }
}
