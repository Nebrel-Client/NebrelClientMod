package de.nebrel.client.plus;

import java.util.Set;
import java.util.UUID;

/**
 * A source of entitlement data.
 *
 * <p>The client ships one implementation, {@link LocalEntitlementProvider},
 * which reads a local development file. A remote provider backed by the Nebrel
 * backend slots in beside it later without any feature changing.</p>
 *
 * <p><b>Lookups must not block.</b> {@link #entitlementsOf} is called while
 * rendering nametags and the tablist. A provider that needs the network is
 * expected to fetch in the background and answer from its own cache, returning
 * an empty set until the answer arrives.</p>
 */
public interface EntitlementProvider {

    /** Short name, shown in the Nebrel+ page so the active source is visible. */
    String name();

    /**
     * Whether this provider's answers can be trusted for anything that matters.
     *
     * <p>A local file the user can edit is not authoritative. Once a backend
     * exists it will be, and anything that needs a real answer should consult
     * this rather than assume.</p>
     */
    boolean authoritative();

    /**
     * The entitlements this provider knows for a player.
     *
     * <p>Must return immediately and must never return null; an unknown player
     * yields an empty set.</p>
     */
    Set<NebrelEntitlement> entitlementsOf(UUID playerId);

    /** Called periodically so a provider can refresh its own cache. */
    default void refresh() {
    }

    /** Drops any cached state. */
    default void invalidate() {
    }
}
