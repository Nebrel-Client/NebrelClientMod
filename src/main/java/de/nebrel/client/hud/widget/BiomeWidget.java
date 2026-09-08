package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.LabeledHudWidget;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.Locale;
import java.util.Optional;

/**
 * The biome the player is standing in.
 *
 * <p>Resolved from the registry key rather than a translation lookup, so a
 * modded biome shows its own id instead of a missing translation string.</p>
 */
public final class BiomeWidget extends LabeledHudWidget {

    public final BooleanSetting showNamespace;

    /** Cached so the registry is not queried for every frame in the same chunk. */
    private String cached = "";
    private long cachedChunkKey = Long.MIN_VALUE;

    public BiomeWidget() {
        super("biome", "Biome", HudAnchor.TOP_RIGHT, -4.0F, 46.0F, false);

        this.showNamespace = add(new BooleanSetting("hud.biome.namespace", "Show Namespace",
                "Prefix modded biomes with their mod id", false));
        this.showNamespace.section(SettingSection.APPEARANCE);
    }

    @Override
    protected void buildRows() {
        MinecraftClient client = client();
        if (client.player == null || client.world == null) {
            row("Biome", "-");
            return;
        }

        net.minecraft.util.math.BlockPos pos = client.player.getBlockPos();
        // Biomes are stored per 4x4x4 cell; re-resolving inside one is wasted work.
        long cellKey = (((long) (pos.getX() >> 2)) << 42)
                ^ (((long) (pos.getY() >> 2)) << 21)
                ^ (pos.getZ() >> 2);
        if (cellKey != this.cachedChunkKey) {
            this.cachedChunkKey = cellKey;
            this.cached = resolve(client.world.getBiome(pos));
        }
        row("Biome", this.cached);
    }

    private String resolve(RegistryEntry<Biome> entry) {
        Optional<RegistryKey<Biome>> key = entry.getKey();
        if (key.isEmpty()) {
            return "Unknown";
        }
        Identifier id = key.get().getValue();
        String path = id.getPath().replace('_', ' ');
        String pretty = capitalise(path);
        return this.showNamespace.get() && !"minecraft".equals(id.getNamespace())
                ? id.getNamespace() + ": " + pretty
                : pretty;
    }

    private static String capitalise(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean startOfWord = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            builder.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = c == ' ';
        }
        return builder.toString();
    }

    /** Clears the cache; called when the player changes world. */
    public void invalidate() {
        this.cachedChunkKey = Long.MIN_VALUE;
        this.cached = "";
    }

    @Override
    public String toString() {
        return "BiomeWidget[" + this.cached.toLowerCase(Locale.ROOT) + "]";
    }
}
