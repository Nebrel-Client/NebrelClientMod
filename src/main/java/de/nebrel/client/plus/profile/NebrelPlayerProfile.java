package de.nebrel.client.plus.profile;

import de.nebrel.client.plus.NebrelEntitlement;
import de.nebrel.client.plus.badge.NebrelBadge;
import de.nebrel.client.plus.nametag.NametagProfile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What the client knows about one player's Nebrel identity.
 *
 * <p>The shape a backend will eventually return: who they are, what they hold,
 * which badges that grants and how they have styled their name. Building the
 * renderers against this now means the remote path can be added later without
 * any of them changing.</p>
 *
 * <p>{@code nametag} is null for remote players today. Only the local player's
 * styling exists on this machine; another member's is something only a backend
 * could supply, and inventing one would be showing the viewer something that is
 * not true.</p>
 */
public final class NebrelPlayerProfile {

    private final UUID uuid;
    private final String username;
    private final Set<NebrelEntitlement> entitlements;
    private final List<NebrelBadge> badges;
    private final NametagProfile nametag;
    private final long fetchedAt;

    public NebrelPlayerProfile(UUID uuid, String username,
                               Set<NebrelEntitlement> entitlements,
                               List<NebrelBadge> badges,
                               NametagProfile nametag) {
        this.uuid = uuid;
        this.username = username == null ? "" : username;
        this.entitlements = Set.copyOf(entitlements);
        this.badges = List.copyOf(badges);
        this.nametag = nametag;
        this.fetchedAt = System.currentTimeMillis();
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String username() {
        return this.username;
    }

    public Set<NebrelEntitlement> entitlements() {
        return this.entitlements;
    }

    public boolean has(NebrelEntitlement entitlement) {
        return this.entitlements.contains(entitlement);
    }

    public boolean isPlus() {
        return has(NebrelEntitlement.NEBREL_PLUS);
    }

    /** Badges in priority order; empty when none apply. */
    public List<NebrelBadge> badges() {
        return this.badges;
    }

    /** The badge to draw, or null. */
    public NebrelBadge primaryBadge() {
        return this.badges.isEmpty() ? null : this.badges.get(0);
    }

    /**
     * This player's nametag styling, or null when it is not known.
     *
     * <p>Null for every player but the local one until a backend exists.</p>
     */
    public NametagProfile nametag() {
        return this.nametag;
    }

    public boolean hasNametagStyle() {
        return this.nametag != null;
    }

    public long ageMillis() {
        return System.currentTimeMillis() - this.fetchedAt;
    }

    /** True when nothing about this player needs custom rendering. */
    public boolean isPlain() {
        return this.badges.isEmpty()
                && (this.nametag == null
                || (!this.nametag.modifiesName() && !this.nametag.additional.active()));
    }

    /** An empty profile, for a player Nebrel knows nothing about. */
    public static NebrelPlayerProfile empty(UUID uuid, String username) {
        return new NebrelPlayerProfile(uuid, username, Set.of(), List.of(), null);
    }
}
