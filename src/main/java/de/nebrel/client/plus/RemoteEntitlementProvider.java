package de.nebrel.client.plus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entitlements read from a JSON list at a URL, so a badge can be seen by
 * players who are not this machine.
 *
 * <p>This exists because of a limit worth being exact about: {@link
 * LocalEntitlementProvider} only ever answers for the local player, by design
 * — one machine has no way to know what a <em>different</em> machine's player
 * has locally granted itself, and a client mod cannot ask another player's
 * client anything directly (packets go client&nbsp;↔ server&nbsp;↔ client, and
 * an unmodified server does not relay a custom payload between two clients on
 * its own). Without something in between, "everyone running Nebrel Client
 * sees each other's badge" does not actually happen; each player only ever
 * sees their own.</p>
 *
 * <p>A shared list closes that gap without touching any Minecraft server at
 * all. Whoever hosts the URL — a community, a server owner, eventually Nebrel
 * itself — maintains a small JSON document naming which players hold which
 * entitlements; every Nebrel Client fetches it independently and merges it in
 * through {@link EntitlementService} exactly like any other provider. No
 * plugin, no mod, no cooperation from the server software is required.</p>
 *
 * <p>The shape expected:</p>
 * <pre>{@code
 * {
 *   "entitlements": {
 *     "<player-uuid>": ["NEBREL_PLUS", "NEBREL_PLUS_BADGE"]
 *   }
 * }
 * }</pre>
 *
 * <p>Unknown entitlement names and malformed UUIDs are skipped rather than
 * failing the whole document, since a list edited by hand will have typos.
 * A response that cannot be parsed at all leaves the previous good answer in
 * place rather than clearing it — a bad deploy on the host's end should not
 * make every badge vanish for one bad minute.</p>
 *
 * <p><b>This is deliberately not {@link #authoritative()}.</b> Whoever
 * controls the URL can put any UUID in it, unverified — that is a real step up
 * from a player editing their own {@code plus.json} (the subject of an entry
 * cannot self-grant here), but it is not the verified Nebrel account the
 * architecture is built toward. Nothing in this client claims otherwise; the
 * Nebrel+ page states plainly what granted what.</p>
 */
public final class RemoteEntitlementProvider implements EntitlementProvider {

    /** Floor between real network fetches, so a typo'd URL cannot be hammered. */
    private static final long MIN_REFRESH_INTERVAL_MILLIS = 5 * 60_000L;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final PlusSettings settings;
    private final HttpClient client;

    private volatile Map<UUID, Set<NebrelEntitlement>> cached = Map.of();
    private volatile long lastFetchStarted;
    private final AtomicBoolean fetching = new AtomicBoolean(false);

    public RemoteEntitlementProvider(PlusSettings settings) {
        this(settings, HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    /** Package-visible: lets a test point this at a client with no real network. */
    RemoteEntitlementProvider(PlusSettings settings, HttpClient client) {
        this.settings = settings;
        this.client = client;
    }

    @Override
    public String name() {
        return "Shared list";
    }

    @Override
    public boolean authoritative() {
        return false;
    }

    @Override
    public Set<NebrelEntitlement> entitlementsOf(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        return this.cached.getOrDefault(playerId, Set.of());
    }

    /** Whether a URL has been configured at all. */
    public boolean configured() {
        return !this.settings.remoteEntitlementsUrl.isBlank();
    }

    /** How many players the last successful fetch named. */
    public int knownPlayerCount() {
        return this.cached.size();
    }

    @Override
    public void refresh() {
        String url = this.settings.remoteEntitlementsUrl.get();
        if (url == null || url.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastFetchStarted < MIN_REFRESH_INTERVAL_MILLIS) {
            return;
        }
        if (!this.fetching.compareAndSet(false, true)) {
            return;
        }
        this.lastFetchStarted = now;
        fetchAsync(url);
    }

    private void fetchAsync(String url) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
        } catch (RuntimeException malformedUrl) {
            this.fetching.set(false);
            return;
        }

        this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        return;
                    }
                    Map<UUID, Set<NebrelEntitlement>> parsed = parse(response.body());
                    // null means the body could not be understood; keep the
                    // previous cache rather than blank every badge on a bad
                    // deploy. An empty-but-valid document is a real answer.
                    if (parsed != null) {
                        this.cached = parsed;
                    }
                })
                .exceptionally(networkFailure -> null)
                .whenComplete((ignored, error) -> this.fetching.set(false));
    }

    /**
     * Parses the document, or returns {@code null} when it cannot be
     * understood at all. Never throws.
     *
     * <p>Public and side-effect-free so the parsing rules — an unknown
     * entitlement name or a malformed UUID is skipped rather than failing the
     * whole document — can be verified directly, with no network involved.</p>
     */
    public static Map<UUID, Set<NebrelEntitlement>> parse(String body) {
        JsonElement root;
        try {
            root = JsonParser.parseString(body);
        } catch (RuntimeException malformed) {
            return null;
        }
        if (root == null || !root.isJsonObject()) {
            return null;
        }
        JsonElement listElement = root.getAsJsonObject().get("entitlements");
        if (listElement == null || !listElement.isJsonObject()) {
            return null;
        }

        JsonObject list = listElement.getAsJsonObject();
        Map<UUID, Set<NebrelEntitlement>> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : list.entrySet()) {
            UUID playerId = parseUuid(entry.getKey());
            if (playerId == null || !entry.getValue().isJsonArray()) {
                continue;
            }
            EnumSet<NebrelEntitlement> granted = EnumSet.noneOf(NebrelEntitlement.class);
            for (JsonElement item : entry.getValue().getAsJsonArray()) {
                NebrelEntitlement entitlement = parseEntitlement(item);
                if (entitlement != null) {
                    granted.add(entitlement);
                }
            }
            if (!granted.isEmpty()) {
                result.put(playerId, Collections.unmodifiableSet(granted));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static UUID parseUuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (RuntimeException notAUuid) {
            return null;
        }
    }

    private static NebrelEntitlement parseEntitlement(JsonElement item) {
        try {
            return NebrelEntitlement.valueOf(item.getAsString().toUpperCase(Locale.ROOT));
        } catch (RuntimeException unknownName) {
            return null;
        }
    }

    @Override
    public void invalidate() {
        this.cached = Map.of();
        this.lastFetchStarted = 0L;
    }
}
