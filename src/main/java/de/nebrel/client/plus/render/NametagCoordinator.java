package de.nebrel.client.plus.render;

import de.nebrel.client.plus.PlusSettings;
import de.nebrel.client.plus.badge.NebrelBadge;
import de.nebrel.client.plus.nametag.AdditionalNametag;
import de.nebrel.client.plus.nametag.NametagProfile;
import de.nebrel.client.plus.profile.NebrelPlayerProfile;
import de.nebrel.client.plus.profile.PlayerProfileCache;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/**
 * The single owner of the label above an entity.
 *
 * <p>Two features want that label: the Custom Nametags module, which restyles
 * the plate, and Nebrel+, which adds a badge, animates the name and can add a
 * second line. Letting both hook the render would mean drawing the label twice
 * or having one silently win, so both come through here: the module supplies
 * the frame as a {@link WorldNametagStyle} and Nebrel+ supplies the content.</p>
 *
 * <p>When neither has anything to contribute this declines and vanilla draws
 * the label exactly as it always did.</p>
 */
public final class NametagCoordinator {

    private final PlusSettings settings;
    private final PlayerProfileCache profiles;
    private final IdentityRenderer identity;
    private final WorldGlyphSink sink = new WorldGlyphSink();

    /** Supplies the current frame style; the module installs itself here. */
    private Supplier<WorldNametagStyle> styleSupplier = () -> WorldNametagStyle.VANILLA_LIKE;

    /** Appends per-entity extras such as health, supplied by the module. */
    private java.util.function.BiFunction<Entity, Double, String> suffixSupplier =
            (entity, distance) -> "";

    public NametagCoordinator(PlusSettings settings, PlayerProfileCache profiles,
                              IdentityRenderer identity) {
        this.settings = settings;
        this.profiles = profiles;
        this.identity = identity;
    }

    public void setStyleSupplier(Supplier<WorldNametagStyle> supplier) {
        if (supplier != null) {
            this.styleSupplier = supplier;
        }
    }

    public void setSuffixSupplier(java.util.function.BiFunction<Entity, Double, String> supplier) {
        if (supplier != null) {
            this.suffixSupplier = supplier;
        }
    }

    /**
     * Draws the label, if anything wants to change it.
     *
     * @return true when this drew the label and vanilla should not
     */
    public boolean render(Entity entity, Text text, MatrixStack matrices,
                          VertexConsumerProvider consumers, int light, int accent) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer == null ? null : client.gameRenderer.getCamera();
        if (camera == null || client.textRenderer == null || text == null) {
            return false;
        }

        WorldNametagStyle style = this.styleSupplier.get();

        NebrelPlayerProfile profile = null;
        boolean plusApplies = this.settings.showInWorld.get() && entity instanceof PlayerEntity;
        if (plusApplies) {
            profile = this.profiles.get(entity.getUuid(), entity.getName().getString());
            if (profile.isPlain()) {
                profile = null;
            }
        }

        // Nothing to add: let vanilla do what it always did.
        if (!style.active() && profile == null) {
            return false;
        }
        if (style.active() && style.playersOnly() && !(entity instanceof PlayerEntity)) {
            if (profile == null) {
                return false;
            }
        }

        double distance = Math.sqrt(entity.squaredDistanceTo(camera.getPos()));
        if (style.active() && distance > style.maxDistance() && profile == null) {
            return false;
        }

        String name = text.getString();
        String suffix = this.suffixSupplier.apply(entity, distance);
        if (suffix != null && !suffix.isEmpty()) {
            name = name + suffix;
        }

        NametagProfile nametag = profile != null && profile.hasNametagStyle()
                ? profile.nametag()
                : null;
        NebrelBadge badge = profile == null ? null : profile.primaryBadge();

        float time = NametagClock.seconds();
        float alpha = style.opacity();
        int baseColor = ColorUtil.fadeAlpha(
                nametag == null ? 0xFFFFFFFF : nametag.resolveBaseColor(0xFFFFFFFF), alpha);

        this.sink.bind(matrices, consumers, light, style.textShadow());

        // Vanilla's own placement: just above the entity, facing the camera,
        // with y growing downward in text space.
        matrices.push();
        matrices.translate(0.0F, entity.getHeight() + 0.5F, 0.0F);
        matrices.multiply(camera.getRotation());
        float finalScale = 0.025F * style.scale();
        matrices.scale(-finalScale, -finalScale, finalScale);

        float lineHeight = this.sink.lineHeight();
        float badgeWidth = this.identity.badgeWidth(this.sink, badge);
        float nameWidth = this.sink.width(name);
        float totalWidth = badgeWidth + nameWidth;
        float left = -totalWidth / 2.0F;

        int plate = style.background()
                ? ColorUtil.fadeAlpha(style.backgroundColor(), alpha)
                : 0;

        // One plate behind the whole line rather than one per glyph. The text
        // renderer draws its background per call, so an effect that splits the
        // name into characters would otherwise produce a plate per letter.
        // Drawing the composed line once with a fully transparent text colour
        // lays down just the background quad.
        if (plate != 0) {
            this.sink.glyph(plateSpacer(badge, name), left, 0.0F, 1.0F, 0.0F, 0x00000000, plate);
        }

        int firstNameColor = this.identity.resolveFirstNameColor(name, baseColor,
                nametag == null ? null : nametag.effects, time);

        float cursor = left;
        cursor += this.identity.drawBadge(this.sink, badge, cursor, 0.0F, accent, firstNameColor,
                nametag == null ? null : nametag.effects, time);
        this.identity.drawStyledText(this.sink, name, cursor, 0.0F, baseColor,
                nametag == null ? null : nametag.effects, time);

        renderAdditionalLine(matrices, nametag, left, totalWidth, lineHeight, alpha, time);

        matrices.pop();
        return true;
    }

    /**
     * The string whose width the plate should match.
     *
     * <p>Drawn with a transparent text colour purely to size the background, so
     * the plate spans badge and name even when the name is split per glyph.</p>
     */
    private String plateSpacer(NebrelBadge badge, String name) {
        if (badge == null || !this.settings.badgeEnabled.get()) {
            return name;
        }
        // The badge's glyph plus its gap, approximated in spaces, then the name.
        float gapWidth = this.settings.badgeGap.getFloat();
        float spaceWidth = Math.max(1.0F, this.sink.width(" "));
        int spaces = Math.max(1, Math.round(gapWidth / spaceWidth));
        return badge.glyph() + " ".repeat(spaces) + name;
    }

    /** Draws the optional second line, above or below the name. */
    private void renderAdditionalLine(MatrixStack matrices, NametagProfile nametag, float left,
                                      float totalWidth, float lineHeight, float alpha, float time) {
        if (nametag == null || !nametag.additional.active()) {
            return;
        }
        AdditionalNametag additional = nametag.additional;
        String line = additional.resolvedText();
        float scale = additional.scale.getFloat();
        float unscaledWidth = this.sink.width(line);

        float y = additional.position.get() == AdditionalNametag.Position.BELOW
                ? lineHeight + 1.0F
                : -(lineHeight * scale) - 1.0F;
        float centerX = left + totalWidth / 2.0F;
        int color = ColorUtil.fadeAlpha(additional.color.get(), alpha);

        // The line is scaled as a whole about (centerX, y) rather than per
        // glyph. That keeps its letter spacing, keeps it centred under the name
        // — the sink scales a glyph about its own untransformed centre, so
        // centring on the scaled width would push the line sideways — and it is
        // what lets the effects run over a scaled line at all, since
        // drawStyledText has no scale of its own.
        matrices.push();
        matrices.translate(centerX, y, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        matrices.translate(-centerX, -y, 0.0F);
        this.identity.drawStyledText(this.sink, line, centerX - unscaledWidth / 2.0F, y, color,
                additional.applyEffects.get() ? nametag.effects : null, time);
        matrices.pop();
    }
}
