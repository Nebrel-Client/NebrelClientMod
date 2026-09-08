package de.nebrel.client.module.impl.render;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Gives the outer skin layer visible thickness.
 *
 * <p>Vanilla draws the hat, jacket, sleeve and trouser layers as flat shells a
 * hair's breadth outside the body, which is why a hat or a jacket reads as
 * painted on. Scaling those parts outward makes them stand off the model, so
 * hair, hoods and jackets gain real depth.</p>
 *
 * <p>Implemented without a mixin: the model instance belongs to the player
 * renderer and lives for the whole session, and vanilla resets each part's
 * angles every frame but not its scale. Setting the scale once per tick is
 * therefore both sufficient and cheap.</p>
 *
 * <p>The effect is a scaled shell rather than fully rebuilt geometry, so the
 * layer thickens outward without becoming a separate solid box. Turning the
 * module off restores every part to a scale of exactly one.</p>
 */
public final class SkinLayers3dModule extends Module implements ClientTickHook {

    public final NumberSetting thickness;
    public final BooleanSetting head;
    public final BooleanSetting body;
    public final BooleanSetting arms;
    public final BooleanSetting legs;
    public final NumberSetting maxDistance;

    /** Every model this module has touched, so all of them can be restored. */
    private final java.util.Set<PlayerEntityModel<?>> touched =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public SkinLayers3dModule() {
        super("skin_layers_3d", "3D Skin Layers",
                "Give hats, jackets and sleeves real depth",
                ModuleCategory.RENDER, "◳");

        this.thickness = number("thickness", "Thickness",
                "How far the outer layer stands off the body, as a percentage",
                2.5D, 0.5D, 8.0D, 0.1D);
        this.thickness.unit("%");

        this.head = bool("head", "Head", "Apply to the hat layer", true,
                SettingSection.APPEARANCE);
        this.body = bool("body", "Body", "Apply to the jacket layer", true,
                SettingSection.APPEARANCE);
        this.arms = bool("arms", "Arms", "Apply to the sleeve layers", true,
                SettingSection.APPEARANCE);
        this.legs = bool("legs", "Legs", "Apply to the trouser layers", true,
                SettingSection.APPEARANCE);

        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop thickening layers on players beyond this many blocks",
                24.0D, 4.0D, 64.0D, 4.0D, SettingSection.ADVANCED);
    }

    @Override
    protected void onDisable() {
        for (PlayerEntityModel<?> model : this.touched) {
            restore(model);
        }
        this.touched.clear();
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;

        for (Entity entity : client.world.getEntities()) {
            // getEntities is the client world's own typed accessor;
            // getPlayers is declared on World with a wildcard element type.
            if (!(entity instanceof AbstractClientPlayerEntity player)) {
                continue;
            }
            if (player.squaredDistanceTo(client.player) > limitSquared) {
                // Out of range: put this player's model back to normal.
                PlayerEntityModel<?> model = modelOf(client, player);
                if (model != null && this.touched.contains(model)) {
                    restore(model);
                }
                continue;
            }
            PlayerEntityModel<?> model = modelOf(client, player);
            if (model == null) {
                continue;
            }
            this.touched.add(model);
            apply(model);
        }
    }

    /**
     * Resolves the model behind a player's renderer.
     *
     * <p>There are two player models in play, one for the classic arm shape and
     * one for the slim one, so this must be asked per player rather than
     * cached.</p>
     */
    private static PlayerEntityModel<?> modelOf(MinecraftClient client,
                                                AbstractClientPlayerEntity player) {
        EntityRenderer<?> renderer = client.getEntityRenderDispatcher().getRenderer(player);
        if (renderer instanceof PlayerEntityRenderer playerRenderer) {
            return playerRenderer.getModel();
        }
        return null;
    }

    private void apply(PlayerEntityModel<?> model) {
        float scale = 1.0F + this.thickness.getFloat() / 100.0F;

        setScale(model.hat, this.head.get() ? scale : 1.0F);
        setScale(model.jacket, this.body.get() ? scale : 1.0F);
        setScale(model.leftSleeve, this.arms.get() ? scale : 1.0F);
        setScale(model.rightSleeve, this.arms.get() ? scale : 1.0F);
        setScale(model.leftPants, this.legs.get() ? scale : 1.0F);
        setScale(model.rightPants, this.legs.get() ? scale : 1.0F);
    }

    private static void restore(PlayerEntityModel<?> model) {
        setScale(model.hat, 1.0F);
        setScale(model.jacket, 1.0F);
        setScale(model.leftSleeve, 1.0F);
        setScale(model.rightSleeve, 1.0F);
        setScale(model.leftPants, 1.0F);
        setScale(model.rightPants, 1.0F);
    }

    private static void setScale(ModelPart part, float scale) {
        if (part == null) {
            return;
        }
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }
}
