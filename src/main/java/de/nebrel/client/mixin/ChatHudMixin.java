package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Adds the Nebrel+ badge to the member's own chat lines.
 *
 * <p>The incoming component is never taken apart. The badge is prepended as a
 * sibling and the original is appended whole, so click and hover events,
 * message signatures, translation arguments and the server's own formatting all
 * survive exactly as they arrived.</p>
 *
 * <p>Which lines get a badge is a judgement call, and worth stating plainly: a
 * chat line is an arbitrary server-formatted component with no marked "sender"
 * field, so there is no reliable way to know who sent it. Nebrel matches the
 * local player's name against the line's text — which is a heuristic, and will
 * miss a server that renders names unusually, or match a line that merely
 * mentions the player. It errs towards doing nothing, and the whole behaviour
 * can be switched off in the Nebrel+ settings.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"), argsOnly = true)
    private Text nebrel$decorateMessage(Text message) {
        if (!NebrelClient.ready() || message == null) {
            return message;
        }
        return NebrelClient.get().plus().decorateChatMessage(message);
    }
}
