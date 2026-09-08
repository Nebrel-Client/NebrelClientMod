package de.nebrel.client.module.impl.render;

import de.nebrel.client.event.WorldRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.WorldRenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Outlines and labels dropped items.
 *
 * <p>Deliberately not an item finder. The outline uses the depth-tested lines
 * layer, so an item behind terrain is occluded exactly as the item model itself
 * is; this makes items that are already on screen easier to pick out of grass
 * and rubble, and nothing more.</p>
 *
 * <p>The filter is a comma-separated list matched against the item's display
 * name and its registry path, so both "diamond" and "netherite_ingot" work.</p>
 */
public final class ItemHighlighterModule extends Module implements WorldRenderHook {

    public final StringSetting filter;
    public final BooleanSetting outline;
    public final BooleanSetting label;
    public final BooleanSetting showCount;
    public final BooleanSetting onlyFiltered;
    public final ColorSetting highlightColor;
    public final ColorSetting matchColor;
    public final NumberSetting maxDistance;
    public final NumberSetting scale;
    public final NumberSetting boxPadding;

    /** Split filter terms, recomputed only when the filter text changes. */
    private String[] terms = new String[0];
    private String cachedFilter = "";

    public ItemHighlighterModule() {
        super("item_highlighter", "Item Highlighter",
                "Make dropped items easier to spot on the ground",
                ModuleCategory.RENDER, "◆");

        this.filter = text("filter", "Item Filter",
                "Comma-separated names to highlight; empty matches everything", "", 200);
        this.filter.placeholder("diamond, netherite, totem");

        this.onlyFiltered = bool("onlyFiltered", "Only Show Matches",
                "Skip items that do not match the filter entirely", false,
                SettingSection.BEHAVIOR);

        this.outline = bool("outline", "Outline", "Draw a box around each item", true,
                SettingSection.APPEARANCE);
        this.label = bool("label", "Label", "Print the item name above it", true,
                SettingSection.APPEARANCE);
        this.showCount = bool("count", "Show Count", "Append the stack size", true,
                SettingSection.APPEARANCE);
        this.showCount.visibleWhen(this.label);

        this.highlightColor = color("color", "Color", "Colour for ordinary items",
                0x99FFFFFF, true, SettingSection.APPEARANCE);
        this.matchColor = color("matchColor", "Match Color",
                "Colour for items matching the filter", 0xCC8B5CF6, true, SettingSection.APPEARANCE);

        this.scale = number("scale", "Label Scale", "Label size", 0.8D, 0.3D, 2.5D, 0.1D,
                SettingSection.APPEARANCE);
        this.boxPadding = number("padding", "Box Padding",
                "Grow the outline by this many blocks", 0.05D, 0.0D, 0.4D, 0.01D,
                SettingSection.APPEARANCE);
        this.boxPadding.visibleWhen(this.outline);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop highlighting beyond this many blocks", 24.0D, 4.0D, 64.0D, 2.0D,
                SettingSection.BEHAVIOR);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        refreshTerms();

        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
                continue;
            }
            Vec3d position = item.getLerpedPos(tickDelta);
            if (WorldRenderUtil.squaredDistanceToCamera(context, position) > limitSquared) {
                continue;
            }

            ItemStack stack = item.getStack();
            boolean matches = matches(stack);
            if (this.onlyFiltered.get() && this.terms.length > 0 && !matches) {
                continue;
            }

            int color = matches && this.terms.length > 0
                    ? this.matchColor.get()
                    : this.highlightColor.get();

            if (this.outline.get()) {
                Box box = item.getBoundingBox()
                        .offset(position.subtract(item.getPos()))
                        .expand(this.boxPadding.get());
                WorldRenderUtil.drawBoxOutline(context, box, color, 1.0F);
            }

            if (this.label.get()) {
                String name = stack.getName().getString();
                if (this.showCount.get() && stack.getCount() > 1) {
                    name = name + " x" + stack.getCount();
                }
                WorldRenderUtil.drawWorldText(context, name,
                        position.add(0.0D, 0.6D, 0.0D), color, this.scale.getFloat(), 0x50000000);
            }
        }
    }

    /** Re-splits the filter only when the text actually changed. */
    private void refreshTerms() {
        String current = this.filter.get();
        if (current.equals(this.cachedFilter)) {
            return;
        }
        this.cachedFilter = current;
        if (current.isBlank()) {
            this.terms = new String[0];
            return;
        }
        String[] parts = current.toLowerCase(java.util.Locale.ROOT).split(",");
        java.util.List<String> cleaned = new java.util.ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        this.terms = cleaned.toArray(new String[0]);
    }

    private boolean matches(ItemStack stack) {
        if (this.terms.length == 0) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase(java.util.Locale.ROOT);
        String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem())
                .getPath().toLowerCase(java.util.Locale.ROOT);
        for (String term : this.terms) {
            if (name.contains(term) || id.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
