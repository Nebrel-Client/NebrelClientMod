package de.nebrel.client.module.impl.render;

import de.nebrel.client.event.WorldRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.WorldRenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.MultiSelectSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Draws entity bounding boxes.
 *
 * <p>Rendering only: no hitbox is resized and no interaction range changes.
 * The outlines go through the depth-tested lines layer, so an entity behind a
 * wall stays hidden. This shows where the boxes already are, it does not
 * reveal anything the player could not already see.</p>
 */
public final class HitboxModule extends Module implements WorldRenderHook {

    /** Which entity kinds get a box. */
    public enum Target {
        PLAYERS,
        HOSTILE,
        PASSIVE,
        PROJECTILES,
        ITEMS,
        OTHER
    }

    public final MultiSelectSetting<Target> targets;
    public final ColorSetting boxColor;
    public final ColorSetting playerColor;
    public final NumberSetting opacity;
    public final NumberSetting expand;
    public final BooleanSetting eyeDirection;
    public final NumberSetting eyeLineLength;
    public final NumberSetting maxDistance;
    public final BooleanSetting hideSelf;

    public HitboxModule() {
        super("hitbox", "Hitbox", "Outline the collision boxes of nearby entities",
                ModuleCategory.RENDER, "⬚");

        this.targets = multi("targets", "Targets", "Which entities to outline",
                Target.class, Target.PLAYERS, Target.HOSTILE);

        this.boxColor = color("color", "Box Color", "Outline colour", 0xB3FFFFFF, true,
                SettingSection.APPEARANCE);
        this.playerColor = color("playerColor", "Player Color",
                "Outline colour used for players", 0xB33DD68C, true, SettingSection.APPEARANCE);
        this.opacity = number("opacity", "Opacity", "Outline transparency",
                0.7D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);
        this.expand = number("expand", "Expand", "Grow the box by this many blocks",
                0.0D, 0.0D, 0.5D, 0.01D, SettingSection.APPEARANCE);

        this.eyeDirection = bool("eyeDirection", "Eye Direction",
                "Draw a short line showing where the entity is looking", false,
                SettingSection.APPEARANCE);
        this.eyeLineLength = number("eyeLength", "Eye Line Length",
                "Length of the look line in blocks", 1.5D, 0.5D, 6.0D, 0.5D,
                SettingSection.APPEARANCE);
        this.eyeLineLength.visibleWhen(this.eyeDirection);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop outlining beyond this many blocks", 32.0D, 4.0D, 96.0D, 4.0D,
                SettingSection.BEHAVIOR);
        this.hideSelf = bool("hideSelf", "Hide Your Own",
                "Skip your own hitbox in third person", true, SettingSection.BEHAVIOR);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;
        float alpha = this.opacity.getFloat();
        float grow = this.expand.getFloat();
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);

        for (Entity entity : client.world.getEntities()) {
            if (!entity.isAlive()) {
                continue;
            }
            if (this.hideSelf.get() && entity == client.player) {
                continue;
            }
            if (!this.targets.has(classify(entity))) {
                continue;
            }

            Vec3d position = entity.getLerpedPos(tickDelta);
            if (WorldRenderUtil.squaredDistanceToCamera(context, position) > limitSquared) {
                continue;
            }

            // Rebuild the box at the interpolated position so it tracks the
            // rendered model rather than the last ticked one.
            Box box = entity.getBoundingBox().offset(position.subtract(entity.getPos()));
            if (grow > 0.0F) {
                box = box.expand(grow);
            }

            int color = entity instanceof PlayerEntity
                    ? this.playerColor.get()
                    : this.boxColor.get();
            WorldRenderUtil.drawBoxOutline(context, box,
                    ColorUtil.fadeAlpha(color, alpha), 1.0F);

            if (this.eyeDirection.get() && entity instanceof LivingEntity living) {
                // getEyePos is public; the standing eye height accessor is not.
                Vec3d eye = living.getEyePos();
                Vec3d look = living.getRotationVec(tickDelta)
                        .multiply(this.eyeLineLength.get());
                WorldRenderUtil.drawPolyline(context,
                        new Vec3d[]{eye, eye.add(look)}, 2,
                        ColorUtil.fadeAlpha(color, alpha),
                        ColorUtil.fadeAlpha(ColorUtil.withAlpha(color, 40), alpha));
            }
        }
    }

    private static Target classify(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return Target.PLAYERS;
        }
        if (entity instanceof HostileEntity) {
            return Target.HOSTILE;
        }
        if (entity instanceof PassiveEntity || entity instanceof ArmorStandEntity) {
            return Target.PASSIVE;
        }
        if (entity instanceof ProjectileEntity) {
            return Target.PROJECTILES;
        }
        if (entity instanceof net.minecraft.entity.ItemEntity) {
            return Target.ITEMS;
        }
        return Target.OTHER;
    }
}
