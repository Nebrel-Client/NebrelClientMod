package de.nebrel.client.keybind;

import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.setting.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Polls physical key and mouse state once per client tick.
 *
 * <p>Polling rather than hooking the key callback keeps this mixin-free and
 * keeps every consumer reading the same snapshot: a keybind toggle, a
 * hold-to-activate module and the keystroke display all agree on what was down
 * this tick.</p>
 *
 * <p>Bindings only fire on a rising edge, and only while no screen has focus,
 * so typing in chat or in the client's own search box never toggles a
 * module.</p>
 */
public final class KeybindManager {

    /** Highest GLFW key code worth tracking (GLFW_KEY_LAST is 348). */
    private static final int KEY_COUNT = 350;
    private static final int MOUSE_BUTTON_COUNT = 8;

    private final boolean[] keyDown = new boolean[KEY_COUNT];
    private final boolean[] keyDownPrevious = new boolean[KEY_COUNT];
    private final boolean[] mouseDown = new boolean[MOUSE_BUTTON_COUNT];
    private final boolean[] mouseDownPrevious = new boolean[MOUSE_BUTTON_COUNT];

    private final ModuleManager modules;
    private final ClientSettings settings;

    private Consumer<Module> moduleToggleListener = module -> {
    };
    private Runnable menuKeyListener = () -> {
    };
    private final List<Runnable> tickListeners = new ArrayList<>();

    /** Set while the user is rebinding a key, to suppress all bindings. */
    private boolean capturing;

    public KeybindManager(ModuleManager modules, ClientSettings settings) {
        this.modules = modules;
        this.settings = settings;
    }

    public void onModuleToggled(Consumer<Module> listener) {
        if (listener != null) {
            this.moduleToggleListener = listener;
        }
    }

    public void onMenuKey(Runnable listener) {
        if (listener != null) {
            this.menuKeyListener = listener;
        }
    }

    /** Registers a callback that runs after the input snapshot is refreshed. */
    public void addTickListener(Runnable listener) {
        if (listener != null) {
            this.tickListeners.add(listener);
        }
    }

    public void setCapturing(boolean value) {
        this.capturing = value;
    }

    // -- polling -------------------------------------------------------------

    public void tick(MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();

        System.arraycopy(this.keyDown, 0, this.keyDownPrevious, 0, KEY_COUNT);
        System.arraycopy(this.mouseDown, 0, this.mouseDownPrevious, 0, MOUSE_BUTTON_COUNT);

        for (int key = 0; key < KEY_COUNT; key++) {
            this.keyDown[key] = InputUtil.isKeyPressed(handle, key);
        }
        for (int button = 0; button < MOUSE_BUTTON_COUNT; button++) {
            this.mouseDown[button] = GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
        }

        for (int i = 0; i < this.tickListeners.size(); i++) {
            this.tickListeners.get(i).run();
        }

        // A focused screen owns the keyboard: never toggle a module underneath it.
        if (this.capturing || client.currentScreen != null) {
            return;
        }

        int menuKey = this.settings.menuKey.get();
        if (menuKey != KeybindSetting.UNBOUND && justPressed(menuKey)) {
            this.menuKeyListener.run();
            return;
        }

        for (Module module : this.modules.getModules()) {
            int bound = module.keybind().get();
            if (bound != KeybindSetting.UNBOUND && justPressed(bound)) {
                this.modules.setEnabled(module, !module.enabled());
                this.moduleToggleListener.accept(module);
            }
        }
    }

    // -- queries -------------------------------------------------------------

    /** True while the key is held. */
    public boolean isDown(int key) {
        return key >= 0 && key < KEY_COUNT && this.keyDown[key];
    }

    /** True only on the tick the key went down. */
    public boolean justPressed(int key) {
        return key >= 0 && key < KEY_COUNT && this.keyDown[key] && !this.keyDownPrevious[key];
    }

    /** True only on the tick the key came up. */
    public boolean justReleased(int key) {
        return key >= 0 && key < KEY_COUNT && !this.keyDown[key] && this.keyDownPrevious[key];
    }

    public boolean isMouseDown(int button) {
        return button >= 0 && button < MOUSE_BUTTON_COUNT && this.mouseDown[button];
    }

    public boolean mouseJustPressed(int button) {
        return button >= 0 && button < MOUSE_BUTTON_COUNT
                && this.mouseDown[button] && !this.mouseDownPrevious[button];
    }

    /**
     * Resolves the key a vanilla binding is currently mapped to.
     *
     * <p>Used by the keystroke display so it follows the user's real movement
     * keys rather than assuming WASD.</p>
     */
    public static int vanillaKeyCode(net.minecraft.client.option.KeyBinding binding) {
        if (binding == null) {
            return KeybindSetting.UNBOUND;
        }
        InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        return key.getCategory() == InputUtil.Type.KEYSYM ? key.getCode() : KeybindSetting.UNBOUND;
    }

    /** True when the vanilla binding's mouse button is held. */
    public boolean isVanillaMouseDown(net.minecraft.client.option.KeyBinding binding) {
        if (binding == null) {
            return false;
        }
        InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        return key.getCategory() == InputUtil.Type.MOUSE && isMouseDown(key.getCode());
    }
}
