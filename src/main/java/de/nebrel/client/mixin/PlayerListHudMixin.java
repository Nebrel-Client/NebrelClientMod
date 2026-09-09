package de.nebrel.client.mixin;

import de.nebrel.client.core.NebrelClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the Nebrel+ badge to tab list entries.
 *
 * <p>{@code getPlayerName} is the one place the game decides what text an entry
 * shows, so decorating its return value is enough: the head, the ping bars, the
 * game-mode formatting and the column layout are all computed elsewhere and stay
 * exactly as they were.</p>
 *
 * <p>The decorated component keeps the original as an intact child unless a
 * colour effect is switched on, so team colours and spectator styling survive.
 * See {@code IdentityText} for why position effects cannot appear here.</p>
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void nebrel$decorateName(PlayerListEntry entry,
                                     CallbackInfoReturnable<Text> info) {
        if (!NebrelClient.ready() || entry == null) {
            return;
        }
        Text decorated = NebrelClient.get().plus().decorateTablistName(entry, info.getReturnValue());
        if (decorated != null && decorated != info.getReturnValue()) {
            info.setReturnValue(decorated);
        }
    }
}
