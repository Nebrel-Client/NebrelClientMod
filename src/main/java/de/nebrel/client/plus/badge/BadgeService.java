package de.nebrel.client.plus.badge;

import de.nebrel.client.plus.EntitlementService;
import de.nebrel.client.plus.PlusSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Decides which badges a player shows.
 *
 * <p>Every surface that draws a name asks this rather than testing membership
 * itself, so the world nametag, the tablist, the chat and the designer preview
 * cannot drift apart.</p>
 *
 * <p>Registration is open so a later badge needs a line here and nothing else.
 * The list is small and fixed after startup, so lookups walk it directly rather
 * than paying for a map.</p>
 */
public final class BadgeService {

    private final List<NebrelBadge> registry = new ArrayList<>();
    private final EntitlementService entitlements;
    private final PlusSettings settings;

    public BadgeService(EntitlementService entitlements, PlusSettings settings) {
        this.entitlements = entitlements;
        this.settings = settings;
        register(NebrelBadge.NEBREL_PLUS);
    }

    public void register(NebrelBadge badge) {
        if (badge == null) {
            return;
        }
        this.registry.add(badge);
        this.registry.sort(Comparator.comparingInt(NebrelBadge::priority));
    }

    public List<NebrelBadge> registered() {
        return List.copyOf(this.registry);
    }

    /**
     * Every badge the player is entitled to and has not switched off.
     *
     * <p>Ordered by priority, so a future staff badge would precede Nebrel+.</p>
     */
    public List<NebrelBadge> getBadges(UUID playerId) {
        if (playerId == null || !this.settings.badgeEnabled.get()) {
            return List.of();
        }
        List<NebrelBadge> held = new ArrayList<>(1);
        for (NebrelBadge badge : this.registry) {
            if (this.entitlements.has(playerId, badge.entitlement())) {
                held.add(badge);
            }
        }
        return held;
    }

    /** The badge to draw in front of the name: the highest priority one held. */
    public Optional<NebrelBadge> getBadge(UUID playerId) {
        List<NebrelBadge> held = getBadges(playerId);
        return held.isEmpty() ? Optional.empty() : Optional.of(held.get(0));
    }

    public boolean hasBadge(UUID playerId) {
        return !getBadges(playerId).isEmpty();
    }

    /**
     * The colour a badge should be drawn in, honouring the user's preference.
     *
     * @param accent      the current client accent
     * @param nametagColor the resolved first-character colour of the name, used
     *                     by the "match nametag" preference
     */
    public int resolveColor(NebrelBadge badge, int accent, int nametagColor) {
        return switch (this.settings.badgeColorMode.get()) {
            case NEBREL_ACCENT -> badge.resolveColor(accent);
            case CUSTOM -> this.settings.badgeCustomColor.get();
            case MATCH_NAMETAG -> nametagColor;
        };
    }
}
