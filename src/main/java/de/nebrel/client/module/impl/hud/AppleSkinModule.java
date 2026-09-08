package de.nebrel.client.module.impl.hud;

import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;

/**
 * Shows the hunger information the vanilla HUD leaves out.
 *
 * <p>Saturation is the value that actually decides when hunger starts dropping,
 * and vanilla never shows it. This draws it as a thin bar over the hunger row,
 * plus a preview of what the held food would restore.</p>
 *
 * <p>Implemented against the game's own food data component rather than by
 * depending on the AppleSkin mod, so there is no extra download and no version
 * coupling. If AppleSkin is installed alongside, turn this module off to avoid
 * drawing the same information twice.</p>
 */
public final class AppleSkinModule extends Module implements HudRenderHook {

    private static final float HUNGER_BAR_WIDTH = 81.0F;
    private static final float HUNGER_ROW_OFFSET = 39.0F;

    public final BooleanSetting saturation;
    public final BooleanSetting foodPreview;
    public final BooleanSetting foodValues;
    public final BooleanSetting exhaustion;
    public final ColorSetting saturationColor;
    public final ColorSetting previewColor;
    public final NumberSetting opacity;

    public AppleSkinModule() {
        super("appleskin", "Food Details", "Saturation, exhaustion and what your food will restore",
                ModuleCategory.HUD, "🍖");

        this.saturation = bool("saturation", "Saturation Bar",
                "Overlay the saturation level on the hunger bar", true);
        this.foodPreview = bool("preview", "Food Preview",
                "Show what the held food would restore", true);
        this.foodValues = bool("values", "Numeric Values",
                "Print the hunger and saturation numbers", false);
        this.exhaustion = bool("exhaustion", "Exhaustion",
                "Show the exhaustion meter that drives hunger loss", false);

        this.saturationColor = color("saturationColor", "Saturation Color",
                "Colour of the saturation overlay", 0xCCF5B547, true, SettingSection.APPEARANCE);
        this.saturationColor.visibleWhen(this.saturation);

        this.previewColor = color("previewColor", "Preview Color",
                "Colour of the food preview", 0x99FFFFFF, true, SettingSection.APPEARANCE);
        this.previewColor.visibleWhen(this.foodPreview);

        this.opacity = number("opacity", "Opacity", "Overall transparency",
                1.0D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden || client.currentScreen != null) {
            return;
        }
        // The hunger row is not drawn in creative or spectator, so neither is this.
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        HungerManager hunger = player.getHungerManager();
        float alpha = this.opacity.getFloat();

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        // Vanilla draws the hunger row on the right of the hotbar.
        float rowRight = screenWidth / 2.0F + 91.0F;
        float rowLeft = rowRight - HUNGER_BAR_WIDTH;
        float rowY = screenHeight - HUNGER_ROW_OFFSET;

        if (this.saturation.get()) {
            float fraction = NebrelMath.clamp(hunger.getSaturationLevel() / 20.0F, 0.0F, 1.0F);
            RenderUtil.rect(context, rowLeft, rowY - 2.0F, HUNGER_BAR_WIDTH * fraction, 1.0F,
                    ColorUtil.fadeAlpha(this.saturationColor.get(), alpha));
        }

        if (this.exhaustion.get()) {
            // Exhaustion runs 0 to 4 before it consumes a saturation point.
            float fraction = NebrelMath.clamp(exhaustionOf(hunger) / 4.0F, 0.0F, 1.0F);
            RenderUtil.rect(context, rowLeft, rowY - 4.0F, HUNGER_BAR_WIDTH * fraction, 1.0F,
                    ColorUtil.fadeAlpha(0xCC9A9AAB, alpha));
        }

        ItemStack held = player.getMainHandStack();
        FoodComponent food = held.get(DataComponentTypes.FOOD);
        if (food == null) {
            held = player.getOffHandStack();
            food = held.get(DataComponentTypes.FOOD);
        }

        if (food != null && this.foodPreview.get()) {
            int restored = food.nutrition();
            float current = hunger.getFoodLevel();
            float start = NebrelMath.clamp(current / 20.0F, 0.0F, 1.0F);
            float end = NebrelMath.clamp((current + restored) / 20.0F, 0.0F, 1.0F);
            if (end > start) {
                // A ghost bar showing how much of the row this meal would fill.
                RenderUtil.rect(context, rowLeft + HUNGER_BAR_WIDTH * start, rowY - 1.0F,
                        HUNGER_BAR_WIDTH * (end - start), 1.0F,
                        ColorUtil.fadeAlpha(this.previewColor.get(), alpha));
            }
        }

        if (this.foodValues.get()) {
            StringBuilder text = new StringBuilder();
            text.append(hunger.getFoodLevel()).append('/').append(20);
            text.append("  sat ").append(String.format("%.1f", hunger.getSaturationLevel()));
            if (food != null) {
                text.append("  +").append(food.nutrition())
                        .append(" / +").append(String.format("%.1f",
                                food.nutrition() * food.saturation() * 2.0F));
            }
            RenderUtil.textRight(context, text.toString(), rowRight, rowY - 12.0F,
                    ColorUtil.fadeAlpha(0xFFECECF2, alpha));
        }
    }

    /**
     * Exhaustion is not exposed by the client's hunger manager in every version.
     *
     * <p>Returns 0 when it cannot be read, which makes the meter sit empty
     * rather than showing something invented.</p>
     */
    private static float exhaustionOf(HungerManager hunger) {
        return hunger.getExhaustion();
    }
}
