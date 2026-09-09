package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.plus.NebrelPlus;
import de.nebrel.client.plus.PlusSettings;
import de.nebrel.client.plus.badge.NebrelBadge;
import de.nebrel.client.plus.nametag.AdditionalNametag;
import de.nebrel.client.plus.nametag.NametagEffect;
import de.nebrel.client.plus.nametag.NametagEffectPipeline;
import de.nebrel.client.plus.nametag.NametagProfile;
import de.nebrel.client.plus.render.HudGlyphSink;
import de.nebrel.client.plus.render.IdentityRenderer;
import de.nebrel.client.plus.render.NametagClock;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Live preview of the player's nametag, on each surface it can appear.
 *
 * <p>The important property here is that this is not a mock-up. It draws through
 * the same {@link IdentityRenderer} and the same effect pipeline the real
 * nametag uses, onto a {@link HudGlyphSink} pointed at the menu instead of the
 * world. A change to the renderer changes the preview in the same frame, and the
 * preview cannot drift out of agreement with the thing it is previewing.</p>
 *
 * <p>The three tabs are honest about their differences rather than showing the
 * same drawing three times. The world tab is the full render. The tab-list and
 * chat tabs go through text components in reality, which carry colour but have
 * no position, so those tabs draw with the geometric effects suppressed and say
 * so underneath. See {@code IdentityText} for why that limit is real.</p>
 */
public final class NametagPreviewComponent extends Component {

    /** Which surface the preview is imitating. */
    public enum Surface {
        WORLD("Above Player"),
        TABLIST("Tab List"),
        CHAT("Chat");

        private final String display;

        Surface(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    /** Height of the drawing area below the tab strip. */
    private static final float STAGE_HEIGHT = 74.0F;
    private static final float NOTE_HEIGHT = 12.0F;

    private static final Surface[] SURFACES = Surface.values();

    private final NebrelPlus plus;
    private final PlusSettings settings;
    private final HudGlyphSink sink = new HudGlyphSink();
    private final TabComponent tabs;

    private int surfaceIndex;

    public NametagPreviewComponent(UiContext ui, NebrelPlus plus) {
        super(ui);
        this.plus = plus;
        this.settings = plus.settings();

        List<String> labels = List.of(
                Surface.WORLD.toString(), Surface.TABLIST.toString(), Surface.CHAT.toString());
        this.tabs = new TabComponent(ui, labels, () -> this.surfaceIndex,
                index -> this.surfaceIndex = index);
    }

    public Surface surface() {
        return SURFACES[this.surfaceIndex];
    }

    @Override
    public float preferredHeight() {
        return this.tabs.preferredHeight() + 6.0F + STAGE_HEIGHT + NOTE_HEIGHT;
    }

    // -- rendering -----------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Theme theme = theme();

        this.tabs.setBounds(this.x, this.y, this.width, this.tabs.preferredHeight());
        this.tabs.render(context, mouseX, mouseY, delta);

        float stageY = this.y + this.tabs.preferredHeight() + 6.0F;
        Surface surface = surface();

        // The stage is deliberately not theme-coloured: a nametag is read
        // against the world, so previewing it on the menu's own background would
        // flatter colours that are unreadable in game.
        int stageTop = surface == Surface.WORLD ? 0xFF2A3348 : 0xFF15161C;
        int stageBottom = surface == Surface.WORLD ? 0xFF3E4A63 : 0xFF101116;
        RenderUtil.roundedRect(context, this.x, stageY, this.width, STAGE_HEIGHT, 8.0F, stageTop);
        RenderUtil.gradientVertical(context, this.x + 1.0F, stageY + STAGE_HEIGHT / 2.0F,
                this.width - 2.0F, STAGE_HEIGHT / 2.0F - 1.0F, stageTop, stageBottom);
        RenderUtil.roundedOutline(context, this.x, stageY, this.width, STAGE_HEIGHT, 8.0F,
                theme.border);

        RenderUtil.pushClip(context, this.x + 1.0F, stageY + 1.0F,
                this.width - 2.0F, STAGE_HEIGHT - 2.0F);
        switch (surface) {
            case WORLD -> renderWorldStage(context, stageY);
            case TABLIST -> renderTablistStage(context, stageY);
            case CHAT -> renderChatStage(context, stageY);
        }
        RenderUtil.popClip(context);

        String note = noteFor(surface);
        if (!note.isEmpty()) {
            RenderUtil.textScaled(context, note, this.x + 2.0F, stageY + STAGE_HEIGHT + 3.0F, 0.8F,
                    ColorUtil.withAlpha(theme.textDisabled, 230), false);
        }
    }

    /**
     * The full nametag, exactly as the world draws it.
     *
     * <p>Composed in the same order as {@code NametagCoordinator}: the extra line
     * above, then badge and name on the baseline, then the extra line below.</p>
     */
    private void renderWorldStage(DrawContext context, float stageY) {
        NametagProfile profile = this.plus.nametag();
        IdentityRenderer renderer = this.plus.identityRenderer();
        NebrelBadge badge = previewBadge();
        String name = this.plus.localPlayerName();
        float time = NametagClock.seconds();

        // No shadow: the world nametag draws onto a plate, not over terrain.
        this.sink.bind(context, false);

        NametagEffectPipeline pipeline = profile.effects;
        int baseColor = profile.resolveBaseColor(0xFFFFFFFF);
        float lineHeight = RenderUtil.lineHeight();

        float totalWidth = renderer.totalWidth(this.sink, badge, name);
        float centerX = this.x + this.width / 2.0F;
        float baseY = stageY + STAGE_HEIGHT / 2.0F - lineHeight / 2.0F;

        AdditionalNametag additional = profile.additional;
        boolean above = additional.active()
                && additional.position.get() == AdditionalNametag.Position.ABOVE;
        boolean below = additional.active()
                && additional.position.get() == AdditionalNametag.Position.BELOW;
        // Keep the whole label centred on the stage whichever side the extra
        // line is on, so switching Position does not make it jump.
        if (above) {
            baseY += lineHeight * 0.5F;
        } else if (below) {
            baseY -= lineHeight * 0.5F;
        }

        // Vanilla's translucent plate, so the colours are judged against what
        // they will actually sit on.
        float padding = 2.0F;
        RenderUtil.roundedRect(context, centerX - totalWidth / 2.0F - padding, baseY - 2.0F,
                totalWidth + padding * 2.0F, lineHeight + 2.0F, 2.0F, 0x40000000);

        if (above) {
            drawAdditional(context, additional, profile, centerX, baseY - lineHeight - 1.0F, time);
        }

        float cursor = centerX - totalWidth / 2.0F;
        int nameColor = renderer.resolveFirstNameColor(name, baseColor, pipeline, time);
        cursor += renderer.drawBadge(this.sink, badge, cursor, baseY,
                this.ui.themes().accent(), nameColor, pipeline, time);
        renderer.drawStyledText(this.sink, name, cursor, baseY, baseColor, pipeline, time);

        if (below) {
            drawAdditional(context, additional, profile, centerX, baseY + lineHeight + 1.0F, time);
        }
    }

    private void drawAdditional(DrawContext context, AdditionalNametag additional,
                                NametagProfile profile, float centerX, float y, float time) {
        String text = additional.resolvedText();
        if (text.isEmpty()) {
            return;
        }
        float scale = additional.scale.getFloat();
        NametagEffectPipeline pipeline = additional.applyEffects.get() ? profile.effects : null;
        float unscaledWidth = RenderUtil.textWidth(text);

        // The extra line is scaled as a whole rather than per glyph, so a small
        // line keeps its letter spacing instead of drifting apart. (centerX, y)
        // is the transform's fixed point, so the line stays centred and its top
        // stays put whatever the scale.
        context.getMatrices().push();
        context.getMatrices().translate(centerX, y, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.getMatrices().translate(-centerX, -y, 0.0F);
        this.plus.identityRenderer().drawStyledText(this.sink, text,
                centerX - unscaledWidth / 2.0F, y, additional.color.get(), pipeline, time);
        context.getMatrices().pop();
    }

    /** A few tab-list rows, with the member's row decorated. */
    private void renderTablistStage(DrawContext context, float stageY) {
        String[] others = {"Steve", "Alex"};
        float rowHeight = RenderUtil.lineHeight() + 4.0F;
        float top = stageY + (STAGE_HEIGHT - rowHeight * 3.0F) / 2.0F;
        float left = this.x + 12.0F;
        float rowWidth = this.width - 24.0F;

        drawListRow(context, left, top, rowWidth, rowHeight, others[0], false);
        drawListRow(context, left, top + rowHeight, rowWidth, rowHeight,
                this.plus.localPlayerName(), true);
        drawListRow(context, left, top + rowHeight * 2.0F, rowWidth, rowHeight, others[1], false);
    }

    private void drawListRow(DrawContext context, float x, float y, float width, float height,
                             String name, boolean self) {
        RenderUtil.rect(context, x, y, width, height - 1.0F, self ? 0x50FFFFFF : 0x30000000);
        // Stand-in for the player head the real tab list draws.
        float headSize = RenderUtil.lineHeight() - 1.0F;
        RenderUtil.roundedRect(context, x + 2.0F, y + 2.0F, headSize, headSize, 1.5F,
                self ? this.ui.themes().accent() : 0xFF6B7280);

        float textX = x + headSize + 6.0F;
        float textY = y + 2.5F;
        if (self) {
            drawFlatIdentity(context, name, textX, textY, true);
        } else {
            this.sink.bind(context, false);
            this.sink.glyph(name, textX, textY, 1.0F, 0.0F, 0xFFD8DAE3, 0);
        }
    }

    /** A short chat log, with the member's line badged. */
    private void renderChatStage(DrawContext context, float stageY) {
        float lineHeight = RenderUtil.lineHeight() + 3.0F;
        float top = stageY + (STAGE_HEIGHT - lineHeight * 3.0F) / 2.0F;
        float left = this.x + 10.0F;

        this.sink.bind(context, false);
        this.sink.glyph("<Steve> hey", left, top, 1.0F, 0.0F, 0xFFC7C9D2, 0);

        float cursor = left;
        this.sink.glyph("<", cursor, top + lineHeight, 1.0F, 0.0F, 0xFFC7C9D2, 0);
        cursor += RenderUtil.textWidth("<");
        cursor += drawFlatIdentity(context, this.plus.localPlayerName(),
                cursor, top + lineHeight, true);
        this.sink.bind(context, false);
        this.sink.glyph("> hey back", cursor, top + lineHeight, 1.0F, 0.0F, 0xFFC7C9D2, 0);

        this.sink.glyph("<Alex> nice tag", left, top + lineHeight * 2.0F, 1.0F, 0.0F,
                0xFFC7C9D2, 0);
    }

    /**
     * Badge and name with the geometric effects suppressed.
     *
     * <p>What the tab list and the chat can genuinely do: they hand the game a
     * text component, which carries colour per character but has nowhere to put
     * an offset, a scale or a lean. Drawing the moving effects here would
     * promise something those surfaces cannot deliver.</p>
     *
     * @return the width consumed
     */
    private float drawFlatIdentity(DrawContext context, String name, float x, float y,
                                   boolean colorEffects) {
        NametagProfile profile = this.plus.nametag();
        IdentityRenderer renderer = this.plus.identityRenderer();
        NebrelBadge badge = previewBadge();
        float time = NametagClock.seconds();
        int baseColor = profile.resolveBaseColor(0xFFFFFFFF);

        NametagEffectPipeline pipeline =
                colorEffects && profile.effects.anyColorEnabled() ? profile.effects : null;

        this.sink.bind(context, false);
        float consumed = 0.0F;
        int nameColor = renderer.resolveFirstNameColor(name, baseColor, pipeline, time);
        consumed += renderer.drawBadge(this.sink, badge, x, y,
                this.ui.themes().accent(), nameColor, pipeline, time);
        consumed += renderer.drawStyledText(this.sink, name, x + consumed, y,
                baseColor, pipeline, time);
        return consumed;
    }

    /**
     * The badge the preview shows.
     *
     * <p>The preview always shows the badge when it is switched on, even without
     * an entitlement, so the designer is usable while the styling is being set
     * up. That is a preview, not a grant: nothing else in the client reads this,
     * and the real surfaces all go through {@code EntitlementService}.</p>
     */
    private NebrelBadge previewBadge() {
        return this.settings.badgeEnabled.get() ? NebrelBadge.NEBREL_PLUS : null;
    }

    /** Says what the selected surface cannot show, when that is the case. */
    private String noteFor(Surface surface) {
        if (surface == Surface.WORLD) {
            return "";
        }
        List<String> hidden = new ArrayList<>();
        for (NametagEffect effect : this.plus.nametag().effects.effects()) {
            if (effect.enabled() && effect.geometric()) {
                hidden.add(effect.displayName());
            }
        }
        if (hidden.isEmpty()) {
            return "Colour effects only on this surface.";
        }
        return String.join(", ", hidden) + " cannot show here.";
    }

    // -- input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.tabs.mouseClicked(mouseX, mouseY, button);
    }
}
