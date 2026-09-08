package de.nebrel.client.module.impl.visual;

import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.WorldRenderHook;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.WorldRenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.ColorSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Leaves a fading trail behind arrows and thrown projectiles.
 *
 * <p>Trails are stored per entity in a fixed-length ring buffer, so a long
 * flight costs the same memory as a short one and nothing is allocated while
 * the arrow is in the air. Entries for entities that have landed or despawned
 * are dropped on the tick after they disappear, so the map cannot grow without
 * bound on a busy server.</p>
 */
public final class ArrowTrailModule extends Module implements ClientTickHook, WorldRenderHook {

    /** Hard ceiling regardless of the length setting. */
    private static final int MAX_POINTS = 96;

    /** A fixed-capacity ring of recent positions for one projectile. */
    private static final class Trail {
        final Vec3d[] points = new Vec3d[MAX_POINTS];
        int head;
        int size;
        int lastSeenTick;

        void add(Vec3d point) {
            this.points[this.head] = point;
            this.head = (this.head + 1) % MAX_POINTS;
            if (this.size < MAX_POINTS) {
                this.size++;
            }
        }

        /** Copies the ring into {@code out} oldest first; returns how many. */
        int snapshot(Vec3d[] out, int limit) {
            int count = Math.min(Math.min(this.size, limit), out.length);
            for (int i = 0; i < count; i++) {
                int index = Math.floorMod(this.head - count + i, MAX_POINTS);
                out[i] = this.points[index];
            }
            return count;
        }
    }

    private final Map<Integer, Trail> trails = new HashMap<>();
    private final Vec3d[] scratch = new Vec3d[MAX_POINTS];
    private int tickCounter;

    public final ColorSetting trailColor;
    public final BooleanSetting rainbow;
    public final NumberSetting rainbowSpeed;
    public final NumberSetting length;
    public final NumberSetting opacity;
    public final BooleanSetting fade;
    public final BooleanSetting ownArrowsOnly;
    public final NumberSetting maxDistance;

    public ArrowTrailModule() {
        super("arrow_trail", "Arrow Trail", "Draw a fading trail behind projectiles in flight",
                ModuleCategory.VISUAL, "➶");

        this.trailColor = color("color", "Color", "Trail colour", 0xFF8B5CF6, true);

        this.rainbow = bool("rainbow", "Rainbow", "Cycle the trail colour", false,
                SettingSection.APPEARANCE);
        this.rainbowSpeed = number("rainbowSpeed", "Rainbow Speed", "Colour cycles per second",
                0.4D, 0.05D, 2.0D, 0.05D, SettingSection.APPEARANCE);
        this.rainbowSpeed.visibleWhen(this.rainbow);

        this.length = number("length", "Length", "How many recent positions to keep",
                40.0D, 4.0D, MAX_POINTS, 2.0D, SettingSection.APPEARANCE);
        this.opacity = number("opacity", "Opacity", "Trail transparency",
                0.8D, 0.1D, 1.0D, 0.05D, SettingSection.APPEARANCE);
        this.fade = bool("fade", "Fade Out", "Fade the tail of the trail to nothing", true,
                SettingSection.APPEARANCE);

        this.ownArrowsOnly = bool("ownOnly", "Your Arrows Only",
                "Only trail projectiles you fired", false, SettingSection.BEHAVIOR);
        this.maxDistance = number("maxDistance", "Max Distance",
                "Stop tracking projectiles beyond this many blocks",
                64.0D, 16.0D, 160.0D, 8.0D, SettingSection.BEHAVIOR);
    }

    @Override
    protected void onDisable() {
        this.trails.clear();
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            this.trails.clear();
            return;
        }
        this.tickCounter++;
        double limit = this.maxDistance.get();
        double limitSquared = limit * limit;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PersistentProjectileEntity projectile) || !projectile.isAlive()) {
                continue;
            }
            // A landed arrow keeps existing but stops moving; velocity is the
            // public signal for that, and `inGround` is not accessible here.
            if (projectile.isOnGround()
                    || projectile.getVelocity().lengthSquared() < 1.0E-4D) {
                continue;
            }
            if (this.ownArrowsOnly.get() && projectile.getOwner() != client.player) {
                continue;
            }
            if (projectile.squaredDistanceTo(client.player) > limitSquared) {
                continue;
            }

            Trail trail = this.trails.computeIfAbsent(projectile.getId(), key -> new Trail());
            trail.add(projectile.getPos());
            trail.lastSeenTick = this.tickCounter;
        }

        // Drop trails whose projectile stopped being tracked last tick.
        Iterator<Map.Entry<Integer, Trail>> iterator = this.trails.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Trail> entry = iterator.next();
            if (this.tickCounter - entry.getValue().lastSeenTick > 20) {
                iterator.remove();
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.trails.isEmpty()) {
            return;
        }
        int limit = (int) this.length.get();
        float alpha = this.opacity.getFloat();

        int head = this.rainbow.get()
                ? ColorUtil.rainbow(System.currentTimeMillis(), this.rainbowSpeed.getFloat(), 0.0F)
                : this.trailColor.get();
        int headColor = ColorUtil.fadeAlpha(head, alpha);
        int tailColor = this.fade.get()
                ? ColorUtil.withAlpha(head, 0)
                : headColor;

        for (Trail trail : this.trails.values()) {
            int count = trail.snapshot(this.scratch, limit);
            if (count < 2) {
                continue;
            }
            // Oldest first, so the tail colour belongs at the start.
            WorldRenderUtil.drawPolyline(context, this.scratch, count, tailColor, headColor);
        }
    }
}
