package de.nebrel.client.module.impl.player;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;

/**
 * Look around without turning the player.
 *
 * <p>Strictly a camera feature. The player's yaw and pitch are saved when free
 * look starts and restored when it ends, and nothing in between changes what
 * the player is facing, what they would hit, or what the server is told. It
 * cannot be used to see round a corner and then act on it, because the moment
 * the key is released the view snaps back to where the player is actually
 * facing.</p>
 */
public final class FreeLookModule extends Module implements ClientTickHook {

    /** How the key behaves. */
    public enum Mode {
        HOLD("Hold"),
        TOGGLE("Toggle");

        private final String display;

        Mode(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final KeybindSetting activationKey;
    public final EnumSetting<Mode> mode;
    public final NumberSetting sensitivity;
    public final BooleanSetting forceThirdPerson;
    public final BooleanSetting smooth;
    public final NumberSetting smoothness;
    public final NumberSetting pitchLimit;

    private boolean active;
    private boolean keyWasDown;
    private Perspective savedPerspective;
    private float cameraYaw;
    private float cameraPitch;
    private float targetYaw;
    private float targetPitch;
    private double lastCursorX;
    private double lastCursorY;

    public FreeLookModule() {
        super("freelook", "Free Look", "Look around while your player keeps facing forward",
                ModuleCategory.PLAYER, "◉");

        this.activationKey = register(new KeybindSetting("freelook.activationKey", "Free Look Key",
                "Key that engages free look", KeybindSetting.UNBOUND));

        this.mode = choice("mode", "Key Mode", "Hold the key, or toggle it", Mode.HOLD);

        this.sensitivity = number("sensitivity", "Sensitivity",
                "How far the camera turns per unit of mouse movement",
                1.0D, 0.2D, 3.0D, 0.05D, SettingSection.BEHAVIOR);

        this.pitchLimit = number("pitchLimit", "Pitch Limit",
                "How far up and down the camera may look, in degrees",
                90.0D, 30.0D, 90.0D, 5.0D, SettingSection.BEHAVIOR);

        this.forceThirdPerson = bool("thirdPerson", "Force Third Person",
                "Switch to third person while free look is active", true, SettingSection.BEHAVIOR);

        this.smooth = bool("smooth", "Smooth Camera", "Ease the camera instead of snapping",
                true, SettingSection.ANIMATION);
        this.smoothness = number("smoothness", "Smoothness",
                "How much the camera lags behind the mouse", 0.35D, 0.05D, 1.0D, 0.05D,
                SettingSection.ANIMATION);
        this.smoothness.visibleWhen(this.smooth);
    }

    @Override
    protected void onDisable() {
        stop(MinecraftClient.getInstance());
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.currentScreen != null) {
            if (this.active) {
                stop(client);
            }
            return;
        }

        int key = this.activationKey.get();
        if (key == KeybindSetting.UNBOUND) {
            if (this.active) {
                stop(client);
            }
            return;
        }

        boolean down = NebrelClient.get().keybinds().isDown(key);
        boolean tapped = down && !this.keyWasDown;
        this.keyWasDown = down;

        boolean shouldBeActive = this.mode.get() == Mode.HOLD ? down : this.active ^ tapped;
        if (shouldBeActive && !this.active) {
            start(client, player);
        } else if (!shouldBeActive && this.active) {
            stop(client);
        }

        if (this.active) {
            update(client);
        }
    }

    private void start(MinecraftClient client, ClientPlayerEntity player) {
        this.active = true;
        this.cameraYaw = player.getYaw();
        this.cameraPitch = player.getPitch();
        this.targetYaw = this.cameraYaw;
        this.targetPitch = this.cameraPitch;
        this.lastCursorX = client.mouse.getX();
        this.lastCursorY = client.mouse.getY();

        if (this.forceThirdPerson.get()) {
            this.savedPerspective = client.options.getPerspective();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        NebrelState.freeLookActive = true;
        NebrelState.freeLookYaw = this.cameraYaw;
        NebrelState.freeLookPitch = this.cameraPitch;
    }

    private void stop(MinecraftClient client) {
        if (!this.active) {
            NebrelState.freeLookActive = false;
            return;
        }
        this.active = false;
        NebrelState.freeLookActive = false;

        if (this.savedPerspective != null && client.options != null) {
            client.options.setPerspective(this.savedPerspective);
            this.savedPerspective = null;
        }
    }

    /**
     * Turns raw cursor movement into camera rotation.
     *
     * <p>Read from the cursor rather than from the player's rotation delta,
     * because the player is deliberately not rotating while this is active.</p>
     */
    private void update(MinecraftClient client) {
        double cursorX = client.mouse.getX();
        double cursorY = client.mouse.getY();
        double deltaX = cursorX - this.lastCursorX;
        double deltaY = cursorY - this.lastCursorY;
        this.lastCursorX = cursorX;
        this.lastCursorY = cursorY;

        float factor = this.sensitivity.getFloat() * 0.15F;
        this.targetYaw += (float) (deltaX * factor);
        float limit = this.pitchLimit.getFloat();
        this.targetPitch = NebrelMath.clamp(
                this.targetPitch + (float) (deltaY * factor), -limit, limit);

        if (this.smooth.get()) {
            float ease = (float) NebrelMath.clamp(this.smoothness.get(), 0.05D, 1.0D);
            this.cameraYaw = NebrelMath.lerp(this.cameraYaw, this.targetYaw, ease);
            this.cameraPitch = NebrelMath.lerp(this.cameraPitch, this.targetPitch, ease);
        } else {
            this.cameraYaw = this.targetYaw;
            this.cameraPitch = this.targetPitch;
        }

        NebrelState.freeLookYaw = this.cameraYaw;
        NebrelState.freeLookPitch = this.cameraPitch;
    }

    public boolean active() {
        return this.active;
    }
}
