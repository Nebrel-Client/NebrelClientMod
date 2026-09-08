package de.nebrel.client.module.impl.utility;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.KeybindSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Bind a chat message or command to a key.
 *
 * <p>Strictly one message per deliberate key press: the send is edge triggered,
 * a cooldown blocks a held key from repeating, and nothing here runs on a timer.
 * There is no scheduled or repeating send, so this cannot be used to spam.</p>
 */
public final class AutoTextModule extends Module implements ClientTickHook {

    /** How many bindable slots the module offers. */
    private static final int SLOT_COUNT = 6;

    /** One bindable message. */
    public static final class Slot {
        public final StringSetting label;
        public final StringSetting message;
        public final KeybindSetting key;
        public final BooleanSetting enabled;

        private long lastSentAt;

        Slot(StringSetting label, StringSetting message, KeybindSetting key, BooleanSetting enabled) {
            this.label = label;
            this.message = message;
            this.key = key;
            this.enabled = enabled;
        }
    }

    private final List<Slot> slots = new ArrayList<>(SLOT_COUNT);

    public final NumberSetting cooldown;
    public final BooleanSetting requireWorld;

    public AutoTextModule() {
        super("auto_text", "Auto Text", "Send a saved message or command with a key",
                ModuleCategory.UTILITY, "✎");

        this.cooldown = number("cooldown", "Cooldown",
                "Minimum seconds between two sends of the same entry",
                1.0D, 0.5D, 10.0D, 0.5D, SettingSection.BEHAVIOR);

        this.requireWorld = bool("requireWorld", "In Game Only",
                "Ignore the keys unless you are in a world", true, SettingSection.BEHAVIOR);

        for (int i = 1; i <= SLOT_COUNT; i++) {
            String section = "Entry " + i;

            StringSetting label = text("slot" + i + ".label", "Name",
                    "A name for this entry, shown in the client only", "Entry " + i, 32);
            label.section(section);

            StringSetting message = text("slot" + i + ".message", "Message",
                    "Chat message, or a command starting with a slash", "", 256);
            message.placeholder("/help");
            message.section(section);

            KeybindSetting key = register(new KeybindSetting("auto_text.slot" + i + ".key",
                    "Key", "Key that sends this entry", KeybindSetting.UNBOUND));
            key.section(section);

            BooleanSetting enabled = bool("slot" + i + ".enabled", "Enabled",
                    "Whether this entry responds to its key", false);
            enabled.section(section);

            this.slots.add(new Slot(label, message, key, enabled));
        }
    }

    public List<Slot> slots() {
        return List.copyOf(this.slots);
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (this.requireWorld.get() && (player == null || client.world == null)) {
            return;
        }
        // A screen owns the keyboard: never fire while chat or a menu is open.
        if (client.currentScreen != null || player == null) {
            return;
        }

        var keybinds = NebrelClient.get().keybinds();
        long now = System.currentTimeMillis();
        long cooldownMillis = Math.round(this.cooldown.get() * 1000.0D);

        for (Slot slot : this.slots) {
            if (!slot.enabled.get() || !slot.key.bound() || slot.message.isBlank()) {
                continue;
            }
            // Edge triggered: one send per press, never per tick held.
            if (!keybinds.justPressed(slot.key.get())) {
                continue;
            }
            if (now - slot.lastSentAt < cooldownMillis) {
                continue;
            }
            slot.lastSentAt = now;
            send(player, slot.message.get().trim());
        }
    }

    private void send(ClientPlayerEntity player, String content) {
        if (content.isEmpty() || player.networkHandler == null) {
            return;
        }
        if (content.startsWith("/")) {
            // sendChatCommand expects the command without its leading slash.
            player.networkHandler.sendChatCommand(content.substring(1));
        } else {
            player.networkHandler.sendChatMessage(content);
        }
    }
}
