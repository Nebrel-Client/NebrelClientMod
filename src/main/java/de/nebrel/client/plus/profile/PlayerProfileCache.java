package de.nebrel.client.plus.profile;

import de.nebrel.client.plus.EntitlementService;
import de.nebrel.client.plus.PlusSettings;
import de.nebrel.client.plus.badge.BadgeService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Resolves and caches player profiles.
 *
 * <p>The cache is the point of this class. Profiles are asked for while drawing
 * the world nametag of every visible player, every entry of the tab list and
 * every chat line — easily hundreds of lookups per second. Without a cache that
 * would mean rebuilding badge lists constantly today, and firing HTTP requests
 * per frame once a backend exists, which is the failure mode this design exists
 * to prevent.</p>
 *
 * <p>Entries expire after {@link #TTL_MILLIS} so a membership that changes
 * mid-session is picked up without a restart, and the map is swept on a timer
 * rather than on read so a player who logs out stops costing memory.</p>
 */
public final class PlayerProfileCache {

    /** How long a resolved profile is reused. */
    private static final long TTL_MILLIS = 30_000L;

    /** How often expired entries are swept out. */
    private static final long SWEEP_INTERVAL_MILLIS = 10_000L;

    private record Entry(NebrelPlayerProfile profile, long expiresAt) {
    }

    private final Map<UUID, Entry> cache = new HashMap<>();
    private final EntitlementService entitlements;
    private final BadgeService badges;
    private final PlusSettings settings;

    /** Supplies the local player's id, so their own styling can be attached. */
    private Supplier<UUID> localPlayerId = () -> null;

    private long lastSweep;

    public PlayerProfileCache(EntitlementService entitlements, BadgeService badges,
                              PlusSettings settings) {
        this.entitlements = entitlements;
        this.badges = badges;
        this.settings = settings;
    }

    public void setLocalPlayerSupplier(Supplier<UUID> supplier) {
        if (supplier != null) {
            this.localPlayerId = supplier;
        }
    }

    /**
     * The profile for a player, from cache when possible.
     *
     * <p>Never null and never blocks.</p>
     */
    public NebrelPlayerProfile get(UUID playerId, String username) {
        if (playerId == null) {
            return NebrelPlayerProfile.empty(null, username);
        }
        long now = System.currentTimeMillis();
        Entry cached = this.cache.get(playerId);
        if (cached != null && cached.expiresAt() > now) {
            return cached.profile();
        }

        NebrelPlayerProfile resolved = resolve(playerId, username);
        this.cache.put(playerId, new Entry(resolved, now + TTL_MILLIS));
        return resolved;
    }

    /**
     * Builds a profile from the services currently available.
     *
     * <p>The nametag style is only attached for the local player: it is the only
     * one this machine actually knows. When a backend supplies other members'
     * styles, this is the single place that changes.</p>
     */
    private NebrelPlayerProfile resolve(UUID playerId, String username) {
        var held = this.entitlements.entitlementsOf(playerId);
        var playerBadges = this.badges.getBadges(playerId);

        boolean isLocal = playerId.equals(this.localPlayerId.get());
        var nametag = isLocal && held.contains(
                de.nebrel.client.plus.NebrelEntitlement.NAMETAG_DESIGNER)
                ? this.settings.nametag()
                : null;

        return new NebrelPlayerProfile(playerId, username, held, playerBadges, nametag);
    }

    /** Called once per client tick. Sweeps expired entries on a timer. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - this.lastSweep < SWEEP_INTERVAL_MILLIS) {
            return;
        }
        this.lastSweep = now;
        this.cache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    /**
     * Drops everything.
     *
     * <p>Called when a setting changes, so the next frame reflects it rather
     * than waiting out the TTL, and on disconnect so nothing carries into the
     * next server.</p>
     */
    public void invalidate() {
        this.cache.clear();
    }

    public void invalidate(UUID playerId) {
        this.cache.remove(playerId);
    }

    public int size() {
        return this.cache.size();
    }
}
