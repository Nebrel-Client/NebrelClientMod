package de.nebrel.client.hud.widget;

import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.hud.HudWidget;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The equipped armour pieces and, optionally, the held items.
 *
 * <p>Draws the real item models with their stack overlay, so enchantment glint
 * and the vanilla durability bar come along for free, plus an optional
 * durability percentage that colours as the piece wears out.</p>
 */
public final class ArmorWidget extends HudWidget {

    private static final int SLOT_SIZE = 16;
    private static final int SLOT_GAP = 2;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Layout direction. */
    public enum Direction {
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical");

        private final String display;

        Direction(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public final EnumSetting<Direction> direction;
    public final BooleanSetting showHeldItems;
    public final BooleanSetting showDurability;
    public final BooleanSetting hideEmptySlots;

    /** Refilled each frame in {@link #update()}; never reallocated. */
    private final List<ItemStack> visible = new ArrayList<>(6);

    public ArmorWidget() {
        super("armor", "Armor", HudAnchor.BOTTOM_CENTER, 0.0F, -60.0F, false);

        this.direction = add(new EnumSetting<>("hud.armor.direction", "Direction",
                "Lay the slots out across or down", Direction.HORIZONTAL));
        this.direction.section(SettingSection.APPEARANCE);

        this.showHeldItems = add(new BooleanSetting("hud.armor.held", "Held Items",
                "Also show the main hand and off hand", true));
        this.showHeldItems.section(SettingSection.APPEARANCE);

        this.showDurability = add(new BooleanSetting("hud.armor.durability", "Durability",
                "Show remaining durability as a percentage", true));
        this.showDurability.section(SettingSection.APPEARANCE);

        this.hideEmptySlots = add(new BooleanSetting("hud.armor.hideEmpty", "Hide Empty Slots",
                "Skip slots with nothing equipped", true));
        this.hideEmptySlots.section(SettingSection.BEHAVIOR);
    }

    @Override
    public void update() {
        this.visible.clear();
        ClientPlayerEntity player = client().player;
        if (player == null) {
            return;
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            collect(player.getEquippedStack(slot));
        }
        if (this.showHeldItems.get()) {
            collect(player.getEquippedStack(EquipmentSlot.MAINHAND));
            collect(player.getEquippedStack(EquipmentSlot.OFFHAND));
        }
    }

    private void collect(ItemStack stack) {
        if (stack.isEmpty() && this.hideEmptySlots.get()) {
            return;
        }
        this.visible.add(stack);
    }

    @Override
    public boolean hasContent() {
        return !this.visible.isEmpty();
    }

    private int slotStride() {
        // A durability label sits under each icon in horizontal layout.
        return SLOT_SIZE + SLOT_GAP;
    }

    private boolean durabilityShown() {
        return this.showDurability.get();
    }

    @Override
    public float contentWidth() {
        int count = Math.max(1, this.visible.size());
        if (this.direction.get() == Direction.HORIZONTAL) {
            return count * slotStride() - SLOT_GAP;
        }
        return SLOT_SIZE + (durabilityShown() ? 4.0F + RenderUtil.textWidth("100%") : 0.0F);
    }

    @Override
    public float contentHeight() {
        int count = Math.max(1, this.visible.size());
        if (this.direction.get() == Direction.HORIZONTAL) {
            return SLOT_SIZE + (durabilityShown() ? RenderUtil.lineHeight() + 1.0F : 0.0F);
        }
        return count * slotStride() - SLOT_GAP;
    }

    @Override
    protected void renderContent(DrawContext context, float x, float y,
                                 int valueColor, int labelColor, boolean shadow) {
        boolean horizontal = this.direction.get() == Direction.HORIZONTAL;

        for (int i = 0; i < this.visible.size(); i++) {
            ItemStack stack = this.visible.get(i);
            int slotX = Math.round(horizontal ? x + i * slotStride() : x);
            int slotY = Math.round(horizontal ? y : y + i * slotStride());

            if (!stack.isEmpty()) {
                context.drawItem(stack, slotX, slotY);
                // The vanilla overlay supplies the stack count and damage bar.
                context.drawStackOverlay(RenderUtil.font(), stack, slotX, slotY);
            } else {
                // An empty slot still needs a footprint so the layout is stable.
                RenderUtil.roundedRect(context, slotX, slotY, SLOT_SIZE, SLOT_SIZE, 3.0F,
                        ColorUtil.withAlpha(valueColor, 26));
            }

            if (durabilityShown() && !stack.isEmpty() && stack.isDamageable()) {
                int max = stack.getMaxDamage();
                int remaining = max - stack.getDamage();
                int percent = Math.max(0, Math.round(remaining * 100.0F / max));
                int color = durabilityColor(percent, valueColor);
                String label = percent + "%";
                if (horizontal) {
                    RenderUtil.textScaledCentered(context, label,
                            slotX + SLOT_SIZE / 2.0F, slotY + SLOT_SIZE + 1.0F, 0.75F, color);
                } else {
                    RenderUtil.textScaled(context, label,
                            slotX + SLOT_SIZE + 4.0F, slotY + (SLOT_SIZE - RenderUtil.lineHeight()) / 2.0F,
                            0.85F, color, shadow);
                }
            }
        }
    }

    /** Green through amber to red as the piece nears breaking. */
    private static int durabilityColor(int percent, int fallback) {
        if (percent > 50) {
            return ColorUtil.withAlpha(0xFF3DD68C, ColorUtil.alpha(fallback));
        }
        if (percent > 20) {
            return ColorUtil.withAlpha(0xFFF5B547, ColorUtil.alpha(fallback));
        }
        return ColorUtil.withAlpha(0xFFF2555A, ColorUtil.alpha(fallback));
    }
}
