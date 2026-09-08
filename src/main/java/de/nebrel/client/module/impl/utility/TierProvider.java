package de.nebrel.client.module.impl.utility;

import java.util.Optional;
import java.util.UUID;

/**
 * A source of player skill tiers.
 *
 * <p>Kept as an interface with no network code behind it on purpose. Tier data
 * belongs to whichever service a server or community runs, and wiring one
 * provider's API directly into the client would make the feature useless
 * everywhere else and would send player names to a third party without the
 * player choosing to. A provider is registered by whoever wants one.</p>
 *
 * <p>Lookups must never block: the module calls {@link #lookup} from the client
 * thread and expects an answer that is already cached, or nothing.</p>
 */
public interface TierProvider {

    /** One player's tier in one game mode. */
    record Tier(String gamemode, String label, int rank, int color) {
    }

    /** A short name shown in the module's settings. */
    String name();

    /**
     * Returns the cached tier for a player, if this provider has one.
     *
     * <p>Must return immediately. A provider that fetches over the network is
     * expected to do so in the background and answer from its own cache.</p>
     */
    Optional<Tier> lookup(UUID playerId, String playerName);

    /** Called once per second, so a provider can refresh its cache. */
    default void refresh() {
    }

    /** Called when the module is switched off, so caches can be dropped. */
    default void clear() {
    }
}
