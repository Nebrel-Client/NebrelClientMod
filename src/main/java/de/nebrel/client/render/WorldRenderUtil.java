package de.nebrel.client.render;

import de.nebrel.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * World-space drawing shared by the in-world modules.
 *
 * <p>Everything here works in camera-relative coordinates: the caller passes
 * absolute world positions and this subtracts the camera before touching the
 * matrix stack, which is the part that is easy to get subtly wrong and is
 * worth having in exactly one place.</p>
 *
 * <p>Line work goes through {@link RenderLayer#getLines()}, which is depth
 * tested. That is deliberate: outlines are occluded by terrain, so no module
 * built on this can show anything through a wall.</p>
 */
public final class WorldRenderUtil {

    private WorldRenderUtil() {
    }

    public static Vec3d cameraPos(WorldRenderContext context) {
        Camera camera = context.camera();
        return camera == null ? Vec3d.ZERO : camera.getPos();
    }

    /**
     * Outlines a box in world space.
     *
     * @param box   absolute world coordinates
     * @param color packed ARGB
     */
    public static void drawBoxOutline(WorldRenderContext context, Box box, int color, float lineWidth) {
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) {
            return;
        }
        Vec3d camera = cameraPos(context);

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        WorldRenderer.drawBox(matrices, lines, box,
                ColorUtil.red(color) / 255.0F,
                ColorUtil.green(color) / 255.0F,
                ColorUtil.blue(color) / 255.0F,
                ColorUtil.alpha(color) / 255.0F);

        matrices.pop();
    }

    /**
     * Draws a polyline through a run of world positions.
     *
     * <p>Colours are interpolated between {@code startColor} and
     * {@code endColor} along the run, which is what gives a trail its fade.</p>
     */
    public static void drawPolyline(WorldRenderContext context, Vec3d[] points, int count,
                                    int startColor, int endColor) {
        if (count < 2) {
            return;
        }
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) {
            return;
        }
        Vec3d camera = cameraPos(context);

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();

        for (int i = 0; i < count - 1; i++) {
            Vec3d from = points[i];
            Vec3d to = points[i + 1];
            if (from == null || to == null) {
                continue;
            }
            int colorFrom = ColorUtil.lerp(startColor, endColor, i / (float) (count - 1));
            int colorTo = ColorUtil.lerp(startColor, endColor, (i + 1) / (float) (count - 1));

            // The lines layer needs a normal per segment; use its direction.
            float dx = (float) (to.x - from.x);
            float dy = (float) (to.y - from.y);
            float dz = (float) (to.z - from.z);
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-5F) {
                continue;
            }
            dx /= length;
            dy /= length;
            dz /= length;

            lines.vertex(entry, (float) from.x, (float) from.y, (float) from.z)
                    .color(colorFrom)
                    .normal(entry, dx, dy, dz);
            lines.vertex(entry, (float) to.x, (float) to.y, (float) to.z)
                    .color(colorTo)
                    .normal(entry, dx, dy, dz);
        }

        matrices.pop();
    }

    /**
     * Draws text at a world position, always facing the camera.
     *
     * @param position  absolute world coordinates of the text's centre
     * @param scale     size relative to the default nametag scale
     * @param background packed ARGB for the plate behind the text, 0 for none
     */
    public static void drawWorldText(WorldRenderContext context, String text, Vec3d position,
                                     int color, float scale, int background) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        if (consumers == null || matrices == null || camera == null || text == null || text.isEmpty()) {
            return;
        }
        TextRenderer font = client.textRenderer;
        Vec3d cameraPos = camera.getPos();

        matrices.push();
        matrices.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
        // Face the camera, then flip: text is authored with y growing downward.
        matrices.multiply(camera.getRotation());
        float finalScale = 0.025F * scale;
        matrices.scale(-finalScale, -finalScale, finalScale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float halfWidth = -font.getWidth(text) / 2.0F;

        font.draw(text, halfWidth, 0.0F, color, false, matrix, consumers,
                TextRenderer.TextLayerType.NORMAL, background, 0xF000F0);

        matrices.pop();
    }

    /** Squared distance from the camera, for range checks without a sqrt. */
    public static double squaredDistanceToCamera(WorldRenderContext context, Vec3d position) {
        Vec3d camera = cameraPos(context);
        double dx = position.x - camera.x;
        double dy = position.y - camera.y;
        double dz = position.z - camera.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
