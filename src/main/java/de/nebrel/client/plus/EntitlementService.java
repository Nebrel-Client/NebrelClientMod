package de.nebrel.client.plus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The single place anything asks what a player is entitled to.
 *
 * <p>Providers are consulted in order and their answers are merged, so a future
 * backend provider can sit alongside the local development one without either
 * knowing about the other.</p>
 *
 * <p>Answers are cached per player for {@link #CACHE_MILLIS} because this is
 * called while drawing nametags and the tablist — potentially once per player
 * per frame. Without the cache a remote provider would be asked far more often
 * than it could possibly answer.</p>
 */
public final class EntitlementService {

    /** How long a merged answer is reused before providers are asked again. */
    private static final long CACHE_MILLIS = 5_000L;

    private record CacheEntry(Set<NebrelEntitlement> entitlements, long expiresAt) {
    }

    private final List<EntitlementProvider> providers = new ArrayList<>();
    private final java.util.Map<UUID, CacheEntry> cache = new java.util.HashMap<>();

    private long lastRefresh;

    public void addProvider(EntitlementProvider provider) {
        if (provider != null) {
            this.providers.add(provider);
            this.cache.clear();
        }
    }

    public List<EntitlementProvider> providers() {
        return List.copyOf(this.providers);
    }

    /** True when at least one provider can actually vouch for a membership. */
    public boolean hasAuthoritativeSource() {
        for (EntitlementProvider provider : this.providers) {
            if (provider.authoritative()) {
                return true;
            }
        }
        return false;
    }

    // -- queries -------------------------------------------------------------

    /** The merged entitlements for a player. Never null. */
    public Set<NebrelEntitlement> entitlementsOf(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        long now = System.currentTimeMillis();
        CacheEntry cached = this.cache.get(playerId);
        if (cached != null && cached.expiresAt() > now) {
            return cached.entitlements();
        }

        EnumSet<NebrelEntitlement> merged = EnumSet.noneOf(NebrelEntitlement.class);
        for (EntitlementProvider provider : this.providers) {
            try {
                merged.addAll(provider.entitlementsOf(playerId));
            } catch (RuntimeException ignored) {
                // A misbehaving provider must not stop the others answering.
            }
        }
        Set<NebrelEntitlement> result = java.util.Collections.unmodifiableSet(merged);
        this.cache.put(playerId, new CacheEntry(result, now + CACHE_MILLIS));
        return result;
    }

    /** The question every feature should ask, instead of "is this player premium". */
    public boolean has(UUID playerId, NebrelEntitlement entitlement) {
        return entitlementsOf(playerId).contains(entitlement);
    }

    /** True when the player holds membership itself. */
    public boolean isPlus(UUID playerId) {
        return has(playerId, NebrelEntitlement.NEBREL_PLUS);
    }

    // -- maintenance ---------------------------------------------------------

    /** Called once per second. Lets providers refresh and expires stale rows. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefresh < 1000L) {
            return;
        }
        this.lastRefresh = now;

        for (EntitlementProvider provider : this.providers) {
            try {
                provider.refresh();
            } catch (RuntimeException ignored) {
                // As above: one bad provider must not break the loop.
            }
        }
        this.cache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    /** Drops every cached answer, e.g. after a settings change or a reconnect. */
    public void invalidate() {
        this.cache.clear();
        for (EntitlementProvider provider : this.providers) {
            provider.invalidate();
        }
    }

    /** Drops one player's cached answer. */
    public void invalidate(UUID playerId) {
        this.cache.remove(playerId);
    }

    public int cachedPlayerCount() {
        return this.cache.size();
    }
}
