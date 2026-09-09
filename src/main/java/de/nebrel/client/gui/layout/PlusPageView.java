package de.nebrel.client.gui.layout;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.component.IconButtonComponent;
import de.nebrel.client.gui.component.NametagPreviewComponent;
import de.nebrel.client.gui.component.ScrollContainer;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.plus.NebrelEntitlement;
import de.nebrel.client.plus.NebrelPlus;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The Nebrel+ landing page: membership status, a live badge preview and what
 * membership actually gets you.
 *
 * <p>The benefit list is generated from {@link NebrelEntitlement} rather than
 * written out by hand, and each entry is labelled from
 * {@link NebrelEntitlement#implemented()}. That is deliberate: a page listing
 * benefits is exactly where a client starts advertising features it does not
 * have, and generating it from the enum means an unimplemented benefit is
 * marked "Coming soon" automatically, with no way to forget.</p>
 */
public final class PlusPageView {

    private static final float CARD_HEIGHT = 46.0F;
    private static final float CARD_GAP = 6.0F;
    private static final float CARD_MIN_WIDTH = 168.0F;

    private final UiContext ui;
    private final NebrelPlus plus;
    private final ScrollContainer scroll;
    private final NametagPreviewComponent preview;
    private final IconButtonComponent designerButton;

    private float x;
    private float y;
    private float width;
    private float height;

    public PlusPageView(UiContext ui, NebrelPlus plus, Runnable openDesigner) {
        this.ui = ui;
        this.plus = plus;
        this.scroll = new ScrollContainer(ui);
        this.preview = new NametagPreviewComponent(ui, plus);
        this.designerButton = new IconButtonComponent(ui, "✎", openDesigner)
                .label("Open Nametag Designer")
                .style(IconButtonComponent.Style.ACCENT)
                .tooltip("Design your badge, nametag colours and effects");
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scroll.setBounds(x, y, width, height);
    }

    public void resetScroll() {
        this.scroll.reset();
    }

    // -- rendering -----------------------------------------------------------

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float contentWidth = this.width - (this.scroll.scrollable() ? 12.0F : 0.0F);
        this.scroll.setContentHeight(measure(contentWidth));
        float cursor = this.y - this.scroll.offset();

        this.scroll.begin(context);
        cursor = renderStatusCard(context, cursor, contentWidth);
        cursor += 10.0F;

        this.preview.setBounds(this.x, cursor, contentWidth, this.preview.preferredHeight());
        this.preview.render(context, mouseX, mouseY, delta);
        cursor += this.preview.preferredHeight() + 10.0F;

        this.designerButton.setBounds(this.x, cursor, Math.min(contentWidth, 190.0F), 24.0F);
        this.designerButton.render(context, mouseX, mouseY, delta);
        cursor += 24.0F + 14.0F;

        cursor = renderBenefits(context, cursor, contentWidth, mouseX, mouseY, delta);
        renderRewards(context, cursor, contentWidth);
        this.scroll.end(context);
        this.scroll.render(context, mouseX, mouseY, delta);
    }

    /** Total content height, so the scrollbar knows the extent. */
    private float measure(float contentWidth) {
        float total = statusCardHeight() + 10.0F;
        total += this.preview.preferredHeight() + 10.0F;
        total += 24.0F + 14.0F;
        int columns = columnCount(contentWidth);
        total += RenderUtil.lineHeight() + 6.0F;
        total += rows(activeBenefits().size(), columns) * (CARD_HEIGHT + CARD_GAP);
        total += 10.0F + RenderUtil.lineHeight() + 6.0F;
        total += rows(comingSoon().size(), columns) * (CARD_HEIGHT + CARD_GAP);
        total += 12.0F + rewardsHeight();
        return total;
    }

    private static int rows(int count, int columns) {
        return (int) Math.ceil(count / (float) columns);
    }

    private int columnCount(float contentWidth) {
        return Math.max(1, (int) ((contentWidth + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP)));
    }

    private float statusCardHeight() {
        return this.plus.remoteEntitlements().configured() ? 68.0F : 56.0F;
    }

    /** Membership state, said plainly including where the answer came from. */
    private float renderStatusCard(DrawContext context, float top, float contentWidth) {
        Theme theme = this.ui.theme();
        int accent = this.ui.themes().accent();
        float cardHeight = statusCardHeight();

        RenderUtil.roundedRect(context, this.x, top, contentWidth, cardHeight, 8.0F,
                theme.surfaceElevated);
        RenderUtil.gradientHorizontal(context, this.x + 1.0F, top + 1.0F,
                contentWidth - 2.0F, cardHeight - 2.0F,
                ColorUtil.withAlpha(accent, 34), ColorUtil.withAlpha(accent, 0));
        RenderUtil.roundedOutline(context, this.x, top, contentWidth, cardHeight, 8.0F,
                ColorUtil.withAlpha(accent, 90));

        float textX = this.x + 12.0F;
        RenderUtil.textFlat(context, "Nebrel", textX, top + 12.0F, theme.textPrimary);
        float plusX = textX + RenderUtil.textWidth("Nebrel");
        RenderUtil.textFlat(context, "+", plusX, top + 12.0F, accent);

        boolean active = this.plus.localIsPlus();
        String label = active ? "ACTIVE" : "INACTIVE";
        int pillColor = active ? theme.success : theme.textDisabled;
        float pillWidth = RenderUtil.textWidthScaled(label, 0.8F) + 12.0F;
        float pillX = this.x + contentWidth - pillWidth - 12.0F;
        RenderUtil.roundedRect(context, pillX, top + 10.0F, pillWidth, 14.0F, 7.0F,
                ColorUtil.withAlpha(pillColor, 45));
        RenderUtil.textScaledCentered(context, label, pillX + pillWidth / 2.0F, top + 13.5F, 0.8F,
                pillColor);

        // Where the entitlement came from matters more than that it exists: a
        // local grant is a development convenience, not a membership, and the
        // page should never let those look the same.
        String source = this.plus.entitlements().hasAuthoritativeSource()
                ? "Verified with your Nebrel account."
                : (active
                        ? "Granted locally by Development Mode. Not a real membership."
                        : "Turn on Development Mode in Advanced to try these features.");
        RenderUtil.textScaled(context, source, textX, top + 28.0F, 0.85F, theme.textSecondary,
                false);

        String player = this.plus.localPlayerId() == null
                ? "Not in a world"
                : this.plus.localPlayerName();
        RenderUtil.textScaled(context, player, textX, top + 40.0F, 0.8F, theme.textDisabled, false);

        // Separate from "source" above: this line is about whether OTHER
        // players can see this member's badge at all, which local
        // Development Mode never achieves by itself (LocalEntitlementProvider
        // only ever answers for this machine's own player).
        if (this.plus.remoteEntitlements().configured()) {
            int known = this.plus.remoteEntitlements().knownPlayerCount();
            String sharedLine = known > 0
                    ? "Shared list active — " + known + " player(s) known, unverified."
                    : "Shared list configured — fetching...";
            RenderUtil.textScaled(context, sharedLine, textX, top + 52.0F, 0.8F,
                    theme.textDisabled, false);
        }
        return top + cardHeight;
    }

    private float renderBenefits(DrawContext context, float top, float contentWidth,
                                 int mouseX, int mouseY, float delta) {
        Theme theme = this.ui.theme();
        int columns = columnCount(contentWidth);

        RenderUtil.textScaled(context, "INCLUDED", this.x, top, 0.85F,
                ColorUtil.withAlpha(theme.textSecondary, 210), false);
        top += RenderUtil.lineHeight() + 6.0F;
        top = renderCardGrid(context, activeBenefits(), top, contentWidth, columns, true);

        top += 10.0F;
        RenderUtil.textScaled(context, "COMING SOON", this.x, top, 0.85F,
                ColorUtil.withAlpha(theme.textSecondary, 210), false);
        top += RenderUtil.lineHeight() + 6.0F;
        top = renderCardGrid(context, comingSoon(), top, contentWidth, columns, false);
        return top;
    }

    private float renderCardGrid(DrawContext context, List<Benefit> benefits, float top,
                                 float contentWidth, int columns, boolean available) {
        Theme theme = this.ui.theme();
        float cardWidth = (contentWidth - (columns - 1) * CARD_GAP) / columns;

        for (int i = 0; i < benefits.size(); i++) {
            Benefit benefit = benefits.get(i);
            float cardX = this.x + (i % columns) * (cardWidth + CARD_GAP);
            float cardY = top + (i / columns) * (CARD_HEIGHT + CARD_GAP);

            RenderUtil.roundedRect(context, cardX, cardY, cardWidth, CARD_HEIGHT, 7.0F,
                    theme.surface);
            RenderUtil.roundedOutline(context, cardX, cardY, cardWidth, CARD_HEIGHT, 7.0F,
                    theme.border);

            int titleColor = available ? theme.textPrimary : theme.textDisabled;
            int budget = (int) (cardWidth - 26.0F);
            RenderUtil.textFlat(context, RenderUtil.truncate(benefit.title(), budget),
                    cardX + 9.0F, cardY + 9.0F, titleColor);

            // A tick or a clock, so the state is readable without the heading.
            RenderUtil.textRight(context, available ? "✔" : "◷",
                    cardX + cardWidth - 8.0F, cardY + 9.0F,
                    available ? theme.success : theme.textDisabled);

            RenderUtil.textScaled(context,
                    RenderUtil.truncate(benefit.detail(), (int) ((cardWidth - 18.0F) / 0.8F)),
                    cardX + 9.0F, cardY + 23.0F, 0.8F,
                    ColorUtil.withAlpha(theme.textSecondary, available ? 255 : 170), false);
        }
        return top + rows(benefits.size(), columns) * (CARD_HEIGHT + CARD_GAP);
    }

    private float rewardsHeight() {
        return 34.0F;
    }

    /**
     * The monthly rewards strip.
     *
     * <p>There are no rewards, and the service says so rather than the UI
     * guessing. When a backend starts delivering them this fills in without a
     * change here.</p>
     */
    private void renderRewards(DrawContext context, float top, float contentWidth) {
        Theme theme = this.ui.theme();
        top += 12.0F;
        RenderUtil.roundedRect(context, this.x, top, contentWidth, rewardsHeight() - 12.0F, 7.0F,
                theme.surface);
        RenderUtil.roundedOutline(context, this.x, top, contentWidth, rewardsHeight() - 12.0F, 7.0F,
                theme.border);

        String message = this.plus.rewards().hasAnyRewards()
                ? this.plus.rewards().unclaimed().size() + " reward(s) waiting"
                : "Monthly rewards arrive once Nebrel accounts are live.";
        RenderUtil.textScaled(context, "◷  " + message, this.x + 9.0F, top + 6.0F, 0.85F,
                theme.textDisabled, false);
    }

    // -- benefit model -------------------------------------------------------

    /** One row of the benefit list. */
    private record Benefit(String title, String detail) {
    }

    private List<Benefit> activeBenefits() {
        List<Benefit> result = new ArrayList<>();
        for (NebrelEntitlement entitlement : NebrelEntitlement.values()) {
            if (entitlement.implemented() && entitlement != NebrelEntitlement.NEBREL_PLUS) {
                result.add(new Benefit(entitlement.displayName(), detailFor(entitlement)));
            }
        }
        return result;
    }

    private List<Benefit> comingSoon() {
        List<Benefit> result = new ArrayList<>();
        for (NebrelEntitlement entitlement : NebrelEntitlement.values()) {
            if (!entitlement.implemented()) {
                result.add(new Benefit(entitlement.displayName(), detailFor(entitlement)));
            }
        }
        return result;
    }

    private static String detailFor(NebrelEntitlement entitlement) {
        return switch (entitlement) {
            case NEBREL_PLUS -> "Membership";
            case NEBREL_PLUS_BADGE -> "The N in front of your name";
            case NAMETAG_DESIGNER -> "Colours and animated effects";
            case ADDITIONAL_NAMETAG -> "A second line under your name";
            case FOUNDER_BADGE -> "For early supporters";
            case STAFF_BADGE -> "For the Nebrel team";
            case PARTNER_BADGE -> "For partnered servers";
            case CREATOR_BADGE -> "For content creators";
            case SHOP_DISCOUNT -> "Needs the shop";
            case FRIENDS_LIMIT_BONUS -> "Needs accounts";
            case SOCIAL_LIMIT_BONUS -> "Needs accounts";
            case HOSTED_WORLDS_LIMIT_BONUS -> "Needs hosted worlds";
            case MONTHLY_REWARDS -> "Cosmetics and emotes each month";
        };
    }

    // -- input ---------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.designerButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.preview.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return this.scroll.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.scroll.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        return this.scroll.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.scroll.mouseScrolled(mouseX, mouseY, amount);
    }

    public String tooltipAt(double mouseX, double mouseY) {
        return this.designerButton.isHovered(mouseX, mouseY)
                ? this.designerButton.tooltip()
                : null;
    }
}
