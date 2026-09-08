package de.nebrel.client.core;

import de.nebrel.client.util.ClickTracker;
import de.nebrel.client.util.FpsCounter;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

/**
 * Derived numbers the HUD reads: frame rate, click rate, movement speed, ping.
 *
 * <p>Collected once per tick or frame in one place so several widgets showing
 * the same value do not each recompute it, and so nothing here runs at all when
 * no widget is displayed.</p>
 */
public final class ClientStats {

    private final FpsCounter fps = new FpsCounter();
    private final ClickTracker leftClicks = new ClickTracker();
    private final ClickTracker rightClicks = new ClickTracker();

    private double lastX;
    private double lastZ;
    private boolean hasLastPosition;
    private float horizontalSpeed;
    private float verticalSpeed;
    private double lastY;

    // -- collection ----------------------------------------------------------

    /** Called once per rendered frame. */
    public void onFrame() {
        this.fps.frame();
    }

    /** Called once per client tick. */
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            this.hasLastPosition = false;
            this.horizontalSpeed = 0.0F;
            this.verticalSpeed = 0.0F;
            return;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (this.hasLastPosition) {
            double dx = x - this.lastX;
            double dz = z - this.lastZ;
            double dy = y - this.lastY;
            // Twenty ticks per second, so a per-tick delta scales straight up.
            float instantaneous = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0D);
            // Light smoothing: a raw per-tick value jitters too much to read.
            this.horizontalSpeed = NebrelMath.lerp(this.horizontalSpeed, instantaneous, 0.35F);
            this.verticalSpeed = NebrelMath.lerp(this.verticalSpeed, (float) (dy * 20.0D), 0.35F);
        }
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.hasLastPosition = true;
    }

    public void recordLeftClick() {
        this.leftClicks.click();
    }

    public void recordRightClick() {
        this.rightClicks.click();
    }

    // -- queries -------------------------------------------------------------

    public int fps() {
        return this.fps.smoothedFps();
    }

    public int leftCps() {
        return this.leftClicks.cps();
    }

    public int rightCps() {
        return this.rightClicks.cps();
    }

    /** Horizontal speed in blocks per second. */
    public float speed() {
        return this.horizontalSpeed;
    }

    /** Vertical speed in blocks per second; negative while falling. */
    public float verticalSpeed() {
        return this.verticalSpeed;
    }

    /**
     * Round-trip time to the current server in milliseconds.
     *
     * @return -1 in single player or before the player list has arrived
     */
    public int ping(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return -1;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    /** Heap in use, in megabytes. */
    public static long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L;
    }

    /** Heap ceiling, in megabytes. */
    public static long maxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / 1_048_576L;
    }

    /** Fraction of the heap in use, 0.0-1.0. */
    public static float memoryFraction() {
        long max = maxMemoryMb();
        return max <= 0L ? 0.0F : NebrelMath.clamp(usedMemoryMb() / (float) max, 0.0F, 1.0F);
    }
}
