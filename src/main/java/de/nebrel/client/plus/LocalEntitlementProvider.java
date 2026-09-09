package de.nebrel.client.plus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Development-only entitlements, granted locally.
 *
 * <p>There is no backend yet, so Nebrel+ would be impossible to build or look
 * at without a way to turn it on. This provider does that and nothing else.</p>
 *
 * <p><b>This is not a security boundary and is not pretending to be one.</b>
 * It grants entitlements to the local player on request, and a user can of
 * course switch it on. That is fine, because it is
 * {@link #authoritative() not authoritative}: when a backend exists it becomes
 * the source of truth, and anything that must not be self-granted (a paid
 * cosmetic, a server-visible prefix, a raised limit) has to check with the
 * server rather than trust this. What this provider gates is purely local
 * rendering on the user's own machine, which they could change by editing the
 * jar anyway.</p>
 *
 * <p>Entitlements are only ever granted to the local player. A remote player's
 * status is not something this machine gets to decide.</p>
 */
public final class LocalEntitlementProvider implements EntitlementProvider {

    /** What development mode grants: the features that actually exist today. */
    private static final Set<NebrelEntitlement> DEVELOPMENT_GRANT = Collections.unmodifiableSet(
            EnumSet.of(
                    NebrelEntitlement.NEBREL_PLUS,
                    NebrelEntitlement.NEBREL_PLUS_BADGE,
                    NebrelEntitlement.NAMETAG_DESIGNER,
                    NebrelEntitlement.ADDITIONAL_NAMETAG));

    private final PlusSettings settings;
    private UUID localPlayerId;

    public LocalEntitlementProvider(PlusSettings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "Local (development)";
    }

    @Override
    public boolean authoritative() {
        // Never. A local file cannot vouch for a membership.
        return false;
    }

    /** Told who the local player is when a world is entered. */
    public void setLocalPlayer(UUID playerId) {
        this.localPlayerId = playerId;
    }

    @Override
    public Set<NebrelEntitlement> entitlementsOf(UUID playerId) {
        if (!this.settings.developmentMode.get()) {
            return Set.of();
        }
        if (playerId == null || !playerId.equals(this.localPlayerId)) {
            // Only ever the local player. Whether someone else has Nebrel+ is
            // not a question this machine can answer.
            return Set.of();
        }
        return DEVELOPMENT_GRANT;
    }

    @Override
    public void invalidate() {
        this.localPlayerId = null;
    }

    /** The grant set, for the UI to describe what development mode does. */
    public static Set<NebrelEntitlement> developmentGrant() {
        return DEVELOPMENT_GRANT;
    }

    /** Mutable copy, for a caller that wants to narrow the grant. */
    public static EnumSet<NebrelEntitlement> developmentGrantCopy() {
        return EnumSet.copyOf(DEVELOPMENT_GRANT);
    }
}
