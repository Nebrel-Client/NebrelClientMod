package de.nebrel.client.module.impl.utility;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.WorldRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.WorldRenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shows a skill tier next to player names.
 *
 * <p>Built around {@link TierProvider} rather than one hard-wired service. The
 * client ships only a local provider, which reads tiers the player has entered
 * themselves; anything that talks to a network service is a provider someone
 * adds deliberately, which keeps player names from being sent anywhere the
 * player did not ask for.</p>
 *
 * <p>With no provider registered the module simply shows nothing, and says so
 * in its description rather than pretending to have data.</p>
 */
public final class TiersModule extends Module implements ClientTickHook, WorldRenderHook {

    private final List<TierProvider> providers = new ArrayList<>();

    public final BooleanSetting showAboveHead;
    public final NumberSetting scale;
    public final NumberSetting maxDistance;
    public final BooleanSetting showOwnTier;
    public final StringSetting localTiers;

    private long lastRefresh;

    public TiersModule() {
        super("tiers", "Tiers", "Show player skill tiers, from a source you choose",
                ModuleCategory.UTILITY, "◈");

        this.showAboveHead = bool("aboveHead", "Show Above Head",
                "Draw the tier above the player in world", true);

        this.scale = number("scale", "Scale", "Tier label size", 0.9D, 0.4D, 2.0D, 0.1D,
                SettingSection.APPEARANCE);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop labelling players beyond this many blocks",
                32.0D, 8.0D, 96.0D, 4.0D, SettingSection.BEHAVIOR);

        this.showOwnTier = bool("own", "Show Your Own",
                "Include yourself when in third person", false, SettingSection.BEHAVIOR);

        this.localTiers = text("local", "Local Tiers",
                "Your own list, as name=tier pairs separated by commas", "", 512);
        this.localTiers.placeholder("Steve=HT1, Alex=LT2");
        this.localTiers.section(SettingSection.ADVANCED);

        // The only provider shipped with the client: whatever the user typed.
        this.providers.add(new LocalTierProvider(this.localTiers));
    }

    /** Registers an additional provider, highest priority first. */
    public void addProvider(TierProvider provider) {
        if (provider != null) {
            this.providers.add(0, provider);
        }
    }

    public List<TierProvider> providers() {
        return List.copyOf(this.providers);
    }

    @Override
    protected void onDisable() {
        for (TierProvider provider : this.providers) {
            provider.clear();
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - this.lastRefresh < 1000L) {
            return;
        }
        this.lastRefresh = now;
        for (TierProvider provider : this.providers) {
            provider.refresh();
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!this.showAboveHead.get()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);
        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;

        for (Entity entity : client.world.getEntities()) {
            // getEntities is the client world's own typed accessor;
            // getPlayers is declared on World with a wildcard element type.
            if (!(entity instanceof AbstractClientPlayerEntity player)) {
                continue;
            }
            if (player == client.player && !this.showOwnTier.get()) {
                continue;
            }
            Vec3d position = player.getLerpedPos(tickDelta);
            if (WorldRenderUtil.squaredDistanceToCamera(context, position) > limitSquared) {
                continue;
            }
            Optional<TierProvider.Tier> tier = lookup(player.getUuid(),
                    player.getGameProfile().getName());
            if (tier.isEmpty()) {
                continue;
            }
            TierProvider.Tier value = tier.get();
            // Sits above the nametag, which vanilla places at height + 0.5.
            WorldRenderUtil.drawWorldText(context, value.label(),
                    position.add(0.0D, player.getHeight() + 0.85D, 0.0D),
                    value.color(), this.scale.getFloat(), 0x60000000);
        }
    }

    /** First provider with an answer wins. */
    private Optional<TierProvider.Tier> lookup(UUID id, String name) {
        for (TierProvider provider : this.providers) {
            Optional<TierProvider.Tier> tier = provider.lookup(id, name);
            if (tier.isPresent()) {
                return tier;
            }
        }
        return Optional.empty();
    }
}
