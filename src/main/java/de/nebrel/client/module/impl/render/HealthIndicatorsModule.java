package de.nebrel.client.module.impl.render;

import de.nebrel.client.event.WorldRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.WorldRenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.RangeSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Health readouts above living entities.
 *
 * <p>Uses the health value the client already tracks for every visible entity,
 * which is the same number the vanilla health bar and the boss bar are drawn
 * from. The labels are depth tested, so nothing behind a wall is labelled.</p>
 */
public final class HealthIndicatorsModule extends Module implements WorldRenderHook {

    /** How the value is presented. */
    public enum Format {
        NUMBER("Number"),
        HEARTS("Hearts"),
        PERCENTAGE("Percentage"),
        BAR("Bar");

        private final String display;

        Format(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    /** Which entity kinds get a label. */
    public enum Target {
        PLAYERS,
        HOSTILE,
        PASSIVE
    }

    public final MultiSelectSetting<Target> targets;
    public final EnumSetting<Format> format;
    public final BooleanSetting colorByHealth;
    public final BooleanSetting showAbsorption;
    public final NumberSetting scale;
    public final NumberSetting heightOffset;
    public final RangeSetting distance;
    public final BooleanSetting background;
    public final BooleanSetting hideFullHealth;

    public HealthIndicatorsModule() {
        super("health_indicators", "Health Indicators",
                "Show how much health nearby entities have left",
                ModuleCategory.RENDER, "♥");

        this.targets = multi("targets", "Targets", "Which entities to label",
                Target.class, Target.PLAYERS, Target.HOSTILE);
        this.format = choice("format", "Format", "How the value is written", Format.NUMBER);

        this.colorByHealth = bool("colorByHealth", "Color By Health",
                "Shade the label from green to red as health drops", true,
                SettingSection.APPEARANCE);
        this.showAbsorption = bool("absorption", "Include Absorption",
                "Add absorption hearts to the total", true, SettingSection.APPEARANCE);
        this.scale = number("scale", "Scale", "Label size", 0.9D, 0.4D, 3.0D, 0.1D,
                SettingSection.APPEARANCE);
        this.heightOffset = number("height", "Height Offset",
                "Extra height above the entity in blocks", 0.6D, 0.0D, 2.0D, 0.1D,
                SettingSection.APPEARANCE);
        this.background = bool("background", "Background", "Draw a plate behind the label",
                true, SettingSection.APPEARANCE);

        this.distance = range("distance", "Distance", "Only label entities inside this band",
                0.0D, 32.0D, 0.0D, 96.0D, 1.0D);
        this.distance.unit("m");

        this.hideFullHealth = bool("hideFull", "Hide at Full Health",
                "Only label entities that have taken damage", false, SettingSection.BEHAVIOR);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);
        double near = this.distance.low();
        double far = this.distance.high();

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            if (living == client.player) {
                continue;
            }
            Target kind = classify(living);
            if (kind == null || !this.targets.has(kind)) {
                continue;
            }

            Vec3d base = living.getLerpedPos(tickDelta);
            double distanceSquared = WorldRenderUtil.squaredDistanceToCamera(context, base);
            if (distanceSquared < near * near || distanceSquared > far * far) {
                continue;
            }

            float health = living.getHealth();
            float max = Math.max(1.0F, living.getMaxHealth());
            float absorption = this.showAbsorption.get() ? living.getAbsorptionAmount() : 0.0F;

            if (this.hideFullHealth.get() && health >= max && absorption <= 0.0F) {
                continue;
            }

            float fraction = NebrelMath.clamp(health / max, 0.0F, 1.0F);
            int color = this.colorByHealth.get() ? healthColor(fraction) : 0xFFFFFFFF;

            Vec3d position = base.add(0.0D,
                    living.getHeight() + this.heightOffset.get(), 0.0D);
            WorldRenderUtil.drawWorldText(context, label(health, max, absorption, fraction),
                    position, color, this.scale.getFloat(),
                    this.background.get() ? 0x60000000 : 0);
        }
    }

    private String label(float health, float max, float absorption, float fraction) {
        String text = switch (this.format.get()) {
            case NUMBER -> trim(health) + (absorption > 0.0F ? " +" + trim(absorption) : "");
            case HEARTS -> trim((health + absorption) / 2.0F) + "❤";
            case PERCENTAGE -> Math.round(fraction * 100.0F) + "%";
            case BAR -> bar(fraction);
        };
        return text;
    }

    /** Green at full, amber in the middle, red when nearly dead. */
    private static int healthColor(float fraction) {
        if (fraction > 0.5F) {
            return ColorUtil.lerp(0xFFF5B547, 0xFF3DD68C, (fraction - 0.5F) * 2.0F);
        }
        return ColorUtil.lerp(0xFFF2555A, 0xFFF5B547, fraction * 2.0F);
    }

    private static String bar(float fraction) {
        int filled = Math.round(fraction * 10.0F);
        StringBuilder builder = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            builder.append(i < filled ? '█' : '░');
        }
        return builder.toString();
    }

    private static String trim(float value) {
        return value == Math.floor(value)
                ? String.valueOf((int) value)
                : String.format("%.1f", value);
    }

    private static Target classify(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            return Target.PLAYERS;
        }
        if (entity instanceof HostileEntity) {
            return Target.HOSTILE;
        }
        if (entity instanceof PassiveEntity) {
            return Target.PASSIVE;
        }
        return null;
    }
}
