package de.nebrel.client.plus;

import de.nebrel.client.plus.badge.BadgeService;
import de.nebrel.client.plus.nametag.NametagProfile;
import de.nebrel.client.plus.profile.NebrelPlayerProfile;
import de.nebrel.client.plus.profile.PlayerProfileCache;
import de.nebrel.client.plus.render.HudGlyphSink;
import de.nebrel.client.plus.render.IdentityRenderer;
import de.nebrel.client.plus.render.IdentityText;
import de.nebrel.client.plus.render.NametagClock;
import de.nebrel.client.plus.render.NametagCoordinator;
import de.nebrel.client.plus.reward.MonthlyRewardService;
import de.nebrel.client.plus.server.ServerIdentityBridge;
import de.nebrel.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.UUID;

/**
 * Nebrel+ as a whole: entitlements, badges, profiles and identity rendering.
 *
 * <p>One facade so the rest of the client has a single thing to talk to, and so
 * the wiring between the services is visible in one file rather than spread
 * across the features that use them.</p>
 */
public final class NebrelPlus {

    private final PlusSettings settings;
    private final EntitlementService entitlements = new EntitlementService();
    private final LocalEntitlementProvider localProvider;
    private final BadgeService badges;
    private final PlayerProfileCache profiles;
    private final IdentityRenderer identityRenderer;
    private final IdentityText identityText;
    private final NametagCoordinator nametagCoordinator;
    private final MonthlyRewardService rewards = new MonthlyRewardService();
    private final HudGlyphSink hudSink = new HudGlyphSink();

    private ServerIdentityBridge serverBridge = ServerIdentityBridge.NONE;
    private Runnable changeListener = () -> {
    };
    private UUID localPlayerId;
    private String localPlayerName = "";

    public NebrelPlus(PlusSettings settings) {
        this.settings = settings;

        this.localProvider = new LocalEntitlementProvider(settings);
        this.entitlements.addProvider(this.localProvider);

        this.badges = new BadgeService(this.entitlements, settings);
        this.profiles = new PlayerProfileCache(this.entitlements, this.badges, settings);
        this.profiles.setLocalPlayerSupplier(() -> this.localPlayerId);

        this.identityRenderer = new IdentityRenderer(settings, this.badges);
        this.identityText = new IdentityText(settings, this.badges);
        this.nametagCoordinator =
                new NametagCoordinator(settings, this.profiles, this.identityRenderer);

        // Every preference marks the config stale. Only two of them change an
        // answer a cache is holding: whether membership is granted at all, and
        // whether the badge exists. The rest — style, colours, gap, scale, the
        // effects — are read live while drawing, so there is nothing to drop.
        for (Setting<?> setting : settings.allSettings()) {
            setting.onChange(value -> this.changeListener.run());
        }
        settings.developmentMode.onChange(value -> invalidateCaches());
        settings.badgeEnabled.onChange(value -> invalidateCaches());
    }

    /**
     * Called whenever a Nebrel+ preference changes.
     *
     * <p>Installed by the client once the config manager exists; Nebrel+ is
     * constructed before it, so it cannot reach for it itself.</p>
     */
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener == null ? () -> {
        } : listener;
    }

    // -- accessors -----------------------------------------------------------

    public PlusSettings settings() {
        return this.settings;
    }

    public EntitlementService entitlements() {
        return this.entitlements;
    }

    public BadgeService badges() {
        return this.badges;
    }

    public PlayerProfileCache profiles() {
        return this.profiles;
    }

    public IdentityRenderer identityRenderer() {
        return this.identityRenderer;
    }

    public IdentityText identityText() {
        return this.identityText;
    }

    public NametagCoordinator nametagCoordinator() {
        return this.nametagCoordinator;
    }

    public MonthlyRewardService rewards() {
        return this.rewards;
    }

    public NametagProfile nametag() {
        return this.settings.nametag();
    }

    /** A sink for drawing previews into the client menu. */
    public HudGlyphSink hudSink() {
        return this.hudSink;
    }

    public ServerIdentityBridge serverBridge() {
        return this.serverBridge;
    }

    /** Installs a server integration. Nothing does this yet; see the interface. */
    public void setServerBridge(ServerIdentityBridge bridge) {
        this.serverBridge = bridge == null ? ServerIdentityBridge.NONE : bridge;
    }

    // -- local player --------------------------------------------------------

    public UUID localPlayerId() {
        return this.localPlayerId;
    }

    public String localPlayerName() {
        return this.localPlayerName.isEmpty() ? "Player" : this.localPlayerName;
    }

    /** True when the local player currently holds membership. */
    public boolean localIsPlus() {
        return this.localPlayerId != null && this.entitlements.isPlus(this.localPlayerId);
    }

    /** True when the local player holds a specific capability. */
    public boolean localHas(NebrelEntitlement entitlement) {
        return this.localPlayerId != null
                && this.entitlements.has(this.localPlayerId, entitlement);
    }

    /** The local player's profile, resolved through the cache. */
    public NebrelPlayerProfile localProfile() {
        return this.profiles.get(this.localPlayerId, this.localPlayerName);
    }

    // -- lifecycle -----------------------------------------------------------

    /** Called once per client tick. */
    public void tick(MinecraftClient client) {
        UUID currentId = client.player == null ? null : client.player.getUuid();
        if (currentId != null && !currentId.equals(this.localPlayerId)) {
            // A new world or a reconnect: re-anchor and drop anything cached
            // against the previous session.
            this.localPlayerId = currentId;
            this.localPlayerName = client.player.getGameProfile().getName();
            this.localProvider.setLocalPlayer(currentId);
            this.entitlements.invalidate();
            this.profiles.invalidate();
        } else if (currentId == null && this.localPlayerId != null) {
            this.localPlayerId = null;
            this.localPlayerName = "";
            this.localProvider.setLocalPlayer(null);
            this.profiles.invalidate();
        }

        this.entitlements.tick();
        this.profiles.tick();
    }

    /**
     * Drops every cached answer.
     *
     * <p>Called when a Nebrel+ setting changes so the next frame reflects it
     * rather than waiting out the cache.</p>
     */
    public void invalidateCaches() {
        this.entitlements.invalidate();
        this.profiles.invalidate();
    }

    // -- surface integration -------------------------------------------------

    /**
     * Decorates a tab list entry's name.
     *
     * @return the original component when nothing applies
     */
    public Text decorateTablistName(PlayerListEntry entry, Text original) {
        if (!this.settings.showInTablist.get() || original == null) {
            return original;
        }
        UUID id = entry.getProfile() == null ? null : entry.getProfile().getId();
        if (id == null) {
            return original;
        }
        String name = entry.getProfile().getName();
        NebrelPlayerProfile profile = this.profiles.get(id, name);
        return this.identityText.decorate(profile, original, accent(), NametagClock.seconds());
    }

    /**
     * Decorates a chat line belonging to the local member.
     *
     * <p>See {@code ChatHudMixin} for why sender detection is a heuristic. This
     * errs towards leaving the message alone.</p>
     *
     * @return the original component when nothing applies
     */
    public Text decorateChatMessage(Text message) {
        if (!this.settings.showInChat.get() || message == null || this.localPlayerId == null) {
            return message;
        }
        if (!localHas(NebrelEntitlement.NEBREL_PLUS_BADGE)) {
            return message;
        }
        if (this.localPlayerName.isEmpty()) {
            return message;
        }

        String plain = message.getString();
        if (!plain.toLowerCase(Locale.ROOT).contains(this.localPlayerName.toLowerCase(Locale.ROOT))) {
            return message;
        }

        NebrelPlayerProfile profile = localProfile();
        if (profile.primaryBadge() == null) {
            return message;
        }
        Text badge = this.identityText.badgeComponent(profile.primaryBadge(), accent(), 0xFFFFFFFF);
        if (badge == null) {
            return message;
        }
        // The original is appended whole, never rebuilt, so its click and hover
        // events and its signature status are untouched.
        return Text.empty().append(badge).append(message);
    }

    private int accent() {
        return de.nebrel.client.core.NebrelClient.get().accent();
    }
}
