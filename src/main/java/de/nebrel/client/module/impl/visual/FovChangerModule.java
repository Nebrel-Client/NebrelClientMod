package de.nebrel.client.module.impl.visual;

import de.nebrel.client.core.NebrelState;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;

/**
 * Full control over the field of view, including the effects vanilla ties to it.
 *
 * <p>Vanilla folds sprinting, the speed effect and bow pull into one FOV value
 * that cannot be separated. This computes its own target from the individual
 * settings and hands the result to {@code GameRendererMixin}, so a player can
 * keep the sprint zoom but drop the bow zoom, or vice versa.</p>
 *
 * <p>The value is eased per tick rather than per frame so it behaves the same
 * at any frame rate.</p>
 */
public final class FovChangerModule extends Module implements ClientTickHook {

    public final NumberSetting baseFov;
    public final BooleanSetting overrideBase;
    public final NumberSetting sprintModifier;
    public final NumberSetting speedEffectModifier;
    public final NumberSetting bowModifier;
    public final BooleanSetting dynamic;
    public final BooleanSetting smooth;
    public final NumberSetting smoothSpeed;

    private double currentFov = Double.NaN;

    public FovChangerModule() {
        super("fov_changer", "FOV Changer", "Separate control over every field of view effect",
                ModuleCategory.VISUAL, "◎");

        this.overrideBase = bool("overrideBase", "Override Base FOV",
                "Use the value below instead of the video settings slider", false);

        this.baseFov = number("base", "Base FOV", "Field of view in degrees",
                70.0D, 30.0D, 130.0D, 1.0D);
        this.baseFov.visibleWhen(this.overrideBase);

        this.dynamic = bool("dynamic", "Dynamic FOV",
                "Let sprinting and effects change the field of view at all", true,
                SettingSection.BEHAVIOR);

        this.sprintModifier = number("sprint", "Sprint FOV",
                "Extra degrees while sprinting", 8.0D, 0.0D, 30.0D, 1.0D, SettingSection.BEHAVIOR);
        this.sprintModifier.visibleWhen(this.dynamic);

        this.speedEffectModifier = number("speedEffect", "Speed Effect FOV",
                "Extra degrees per level of the speed effect",
                4.0D, 0.0D, 20.0D, 1.0D, SettingSection.BEHAVIOR);
        this.speedEffectModifier.visibleWhen(this.dynamic);

        this.bowModifier = number("bow", "Bow FOV",
                "Degrees removed at full bow draw", 15.0D, 0.0D, 40.0D, 1.0D,
                SettingSection.BEHAVIOR);

        this.smooth = bool("smooth", "Smooth Transitions",
                "Ease between field of view changes", true, SettingSection.ANIMATION);

        this.smoothSpeed = number("smoothSpeed", "Transition Speed",
                "How quickly the field of view eases", 0.25D, 0.05D, 1.0D, 0.05D,
                SettingSection.ANIMATION);
        this.smoothSpeed.visibleWhen(this.smooth);
    }

    @Override
    protected void onDisable() {
        NebrelState.fovOverride = Double.NaN;
        this.currentFov = Double.NaN;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            NebrelState.fovOverride = Double.NaN;
            return;
        }

        double target = this.overrideBase.get()
                ? this.baseFov.get()
                : client.options.getFov().getValue();

        if (this.dynamic.get()) {
            if (player.isSprinting()) {
                target += this.sprintModifier.get();
            }
            var speed = player.getStatusEffect(StatusEffects.SPEED);
            if (speed != null) {
                target += this.speedEffectModifier.get() * (speed.getAmplifier() + 1);
            }
        }

        // Bow and crossbow pull narrows the view, proportional to the draw.
        if (player.isUsingItem() && this.bowModifier.get() > 0.0D) {
            var active = player.getActiveItem();
            if (active.getItem() instanceof BowItem || active.getItem() instanceof CrossbowItem) {
                float progress = (active.getMaxUseTime(player) - player.getItemUseTimeLeft()) / 20.0F;
                progress = progress > 1.0F ? 1.0F : progress * progress;
                target -= this.bowModifier.get() * progress;
            }
        }

        target = NebrelMath.clamp(target, 1.0D, 179.0D);

        if (this.smooth.get() && !Double.isNaN(this.currentFov)) {
            this.currentFov = NebrelMath.lerp(this.currentFov, target,
                    NebrelMath.clamp(this.smoothSpeed.get(), 0.0D, 1.0D));
        } else {
            this.currentFov = target;
        }
        NebrelState.fovOverride = this.currentFov;
    }
}
