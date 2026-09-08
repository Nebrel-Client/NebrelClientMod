package de.nebrel.client.module.impl.utility;

import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists what is inside a shulker box without opening it.
 *
 * <p>Reads the container data component the stack already carries; the server
 * sends it precisely so the client can show it. Nothing is requested and no
 * hidden inventory is exposed.</p>
 *
 * <p>Hooked through Fabric's tooltip callback rather than a mixin, and the
 * listener checks the module's enabled state on every call, so switching the
 * module off takes effect immediately without unregistering anything.</p>
 */
public final class ShulkerTooltipModule extends Module {

    /** Vanilla shows this many lines before folding the rest into a count. */
    private static final int HARD_LINE_LIMIT = 27;

    public final BooleanSetting compact;
    public final NumberSetting maxLines;
    public final BooleanSetting showCounts;
    public final BooleanSetting showTotals;
    public final BooleanSetting bundlesToo;

    public ShulkerTooltipModule() {
        super("shulker_tooltip", "Shulker Tooltip", "See what is in a shulker box before opening it",
                ModuleCategory.UTILITY, "▥");

        this.compact = bool("compact", "Compact", "Merge identical stacks onto one line", true);
        this.showCounts = bool("counts", "Show Counts", "Print how many of each item", true,
                SettingSection.APPEARANCE);
        this.showTotals = bool("totals", "Show Totals",
                "Add a summary line with the slot and item totals", true, SettingSection.APPEARANCE);

        this.maxLines = number("maxLines", "Max Lines",
                "How many item lines to show before folding the rest into a count",
                9.0D, 1.0D, HARD_LINE_LIMIT, 1.0D, SettingSection.APPEARANCE);

        this.bundlesToo = bool("bundles", "Bundles Too",
                "Apply the same listing to bundles", true, SettingSection.BEHAVIOR);
    }

    /**
     * Registers the tooltip listener.
     *
     * <p>Called once during client initialisation. The callback stays
     * registered for the session; it returns immediately while the module is
     * off.</p>
     */
    public void registerTooltipListener() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!enabled()) {
                return;
            }
            appendContents(stack, lines);
        });
    }

    private void appendContents(ItemStack stack, List<Text> lines) {
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            if (!this.bundlesToo.get()) {
                return;
            }
            var bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundle == null) {
                return;
            }
            appendStacks(bundle.iterate(), lines);
            return;
        }
        appendStacks(container.iterateNonEmpty(), lines);
    }

    private void appendStacks(Iterable<ItemStack> contents, List<Text> lines) {
        List<ItemStack> collected = new ArrayList<>();
        for (ItemStack entry : contents) {
            if (!entry.isEmpty()) {
                collected.add(entry);
            }
        }
        if (collected.isEmpty()) {
            lines.add(Text.literal("Empty").formatted(Formatting.DARK_GRAY));
            return;
        }

        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (ItemStack entry : collected) {
            String name = entry.getName().getString();
            int index = this.compact.get() ? names.indexOf(name) : -1;
            if (index >= 0) {
                counts.set(index, counts.get(index) + entry.getCount());
            } else {
                names.add(name);
                counts.add(entry.getCount());
            }
        }

        int limit = Math.min((int) this.maxLines.get(), names.size());
        for (int i = 0; i < limit; i++) {
            MutableText line = Text.literal("  ").formatted(Formatting.DARK_GRAY);
            if (this.showCounts.get()) {
                line.append(Text.literal(counts.get(i) + " x ").formatted(Formatting.GRAY));
            }
            line.append(Text.literal(names.get(i)).formatted(Formatting.WHITE));
            lines.add(line);
        }

        int hidden = names.size() - limit;
        if (hidden > 0) {
            lines.add(Text.literal("  and " + hidden + " more")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }

        if (this.showTotals.get()) {
            int totalItems = 0;
            for (int count : counts) {
                totalItems += count;
            }
            lines.add(Text.literal("  " + collected.size() + " slots, " + totalItems + " items")
                    .formatted(Formatting.DARK_GRAY));
        }
    }
}
