package de.nebrel.client.module.impl.utility;

import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.event.ClientTickHook;
import de.nebrel.client.event.HudRenderHook;
import de.nebrel.client.hud.HudAnchor;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.setting.BooleanSetting;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.setting.NumberSetting;
import de.nebrel.client.setting.SettingSection;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Local, self-tracked goals.
 *
 * <p>Progress comes from what the client can already see about its own player:
 * distance travelled, jumps taken and time in a world. Nothing is fetched and
 * nothing is submitted, so the quests work in single player, on any server, and
 * offline.</p>
 *
 * <p>Progress is kept for the session. Persisting it would mean a fourth config
 * file whose contents are pure decoration, and losing it costs nothing.</p>
 */
public final class QuestsModule extends Module implements ClientTickHook, HudRenderHook {

    private static final int PANEL_WIDTH = 150;

    /** Which quests the overlay lists. */
    public enum Filter {
        ACTIVE("Active"),
        COMPLETED("Completed"),
        ALL("All");

        private final String display;

        Filter(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    private final List<Quest> quests = new ArrayList<>();

    public final BooleanSetting showOverlay;
    public final EnumSetting<Filter> filter;
    public final EnumSetting<HudAnchor> anchor;
    public final NumberSetting offsetX;
    public final NumberSetting offsetY;
    public final BooleanSetting showProgressBars;
    public final BooleanSetting notifyOnComplete;
    public final NumberSetting opacity;

    private double lastX;
    private double lastZ;
    private boolean hasLastPosition;
    private double walkedBlocks;
    private long sessionStartedAt;
    private boolean wasOnGround = true;
    private int jumpCount;

    public QuestsModule() {
        super("quests", "Quests", "Local goals tracked from your own play, nothing sent anywhere",
                ModuleCategory.UTILITY, "✓");

        this.showOverlay = bool("overlay", "Show Overlay", "List quests on the HUD", true);
        this.filter = choice("filter", "Show", "Which quests the overlay lists", Filter.ACTIVE);

        this.anchor = choice("anchor", "Anchor", "Screen corner to measure from",
                HudAnchor.MIDDLE_RIGHT, SettingSection.APPEARANCE);
        this.offsetX = number("x", "Offset X", "Horizontal offset from the anchor",
                -6.0D, -400.0D, 400.0D, 1.0D, SettingSection.APPEARANCE);
        this.offsetY = number("y", "Offset Y", "Vertical offset from the anchor",
                0.0D, -400.0D, 400.0D, 1.0D, SettingSection.APPEARANCE);

        this.showProgressBars = bool("bars", "Progress Bars",
                "Draw a bar under each objective", true, SettingSection.APPEARANCE);
        this.opacity = number("opacity", "Opacity", "Overlay transparency",
                0.95D, 0.2D, 1.0D, 0.05D, SettingSection.APPEARANCE);

        this.notifyOnComplete = bool("notify", "Completion Notification",
                "Show a toast when a quest completes", true, SettingSection.BEHAVIOR);

        buildDemoQuests();
    }

    /** The starter set. Replaceable later without touching the tracking code. */
    private void buildDemoQuests() {
        this.quests.add(new Quest("first_steps", "First Steps",
                "Get moving and see the world", "A sense of direction")
                .objective(Quest.Metric.DISTANCE_WALKED, 500));

        this.quests.add(new Quest("light_footed", "Light Footed",
                "Get some air", "Spring in your step")
                .objective(Quest.Metric.JUMPS, 100));

        this.quests.add(new Quest("settling_in", "Settling In",
                "Spend real time in a world", "A place that feels lived in")
                .objective(Quest.Metric.PLAY_TIME, 30)
                .objective(Quest.Metric.JUMPS, 250)
                .objective(Quest.Metric.DISTANCE_WALKED, 2000));
    }

    public List<Quest> quests() {
        return List.copyOf(this.quests);
    }

    @Override
    protected void onEnable() {
        this.sessionStartedAt = System.currentTimeMillis();
        this.hasLastPosition = false;
        this.wasOnGround = true;
        this.jumpCount = 0;
        this.walkedBlocks = 0.0D;
    }

    @Override
    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            this.hasLastPosition = false;
            return;
        }

        // Distance: integrate the per-tick horizontal delta.
        double x = player.getX();
        double z = player.getZ();
        if (this.hasLastPosition && !player.isSpectator()) {
            double dx = x - this.lastX;
            double dz = z - this.lastZ;
            double step = Math.sqrt(dx * dx + dz * dz);
            // Ignore teleports, which would otherwise complete a quest at once.
            if (step < 1.0D) {
                this.walkedBlocks += step;
            }
        }
        this.lastX = x;
        this.lastZ = z;
        this.hasLastPosition = true;

        // Jumps: the tick the player leaves the ground moving upward.
        boolean onGround = player.isOnGround();
        if (this.wasOnGround && !onGround && player.getVelocity().y > 0.0D) {
            this.jumpCount++;
        }
        this.wasOnGround = onGround;

        long minutes = (System.currentTimeMillis() - this.sessionStartedAt) / 60_000L;

        for (Quest quest : this.quests) {
            boolean wasComplete = quest.complete();

            quest.setProgress(Quest.Metric.DISTANCE_WALKED, (int) this.walkedBlocks);
            quest.setProgress(Quest.Metric.PLAY_TIME, (int) minutes);
            quest.setProgress(Quest.Metric.JUMPS, this.jumpCount);

            if (!wasComplete && quest.complete() && this.notifyOnComplete.get()) {
                NebrelClient.get().notifications().success("Quest complete", quest.name());
            }
        }
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (!this.showOverlay.get()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) {
            return;
        }

        List<Quest> visible = new ArrayList<>();
        for (Quest quest : this.quests) {
            boolean complete = quest.complete();
            if (this.filter.get() == Filter.ACTIVE && complete) {
                continue;
            }
            if (this.filter.get() == Filter.COMPLETED && !complete) {
                continue;
            }
            visible.add(quest);
        }
        if (visible.isEmpty()) {
            return;
        }

        int lineHeight = RenderUtil.lineHeight();
        float alpha = this.opacity.getFloat();
        var theme = NebrelClient.get().ui().theme();

        float height = 8.0F;
        for (Quest quest : visible) {
            height += lineHeight + 2.0F;
            height += quest.objectives().size()
                    * (lineHeight * 0.85F + (this.showProgressBars.get() ? 5.0F : 1.0F));
            height += 6.0F;
        }

        float screenWidth = client.getWindow().getScaledWidth();
        float screenHeight = client.getWindow().getScaledHeight();
        HudAnchor placement = this.anchor.get();
        float x = placement.resolveX(this.offsetX.getFloat(), screenWidth, PANEL_WIDTH);
        float y = placement.resolveY(this.offsetY.getFloat(), screenHeight, height);

        RenderUtil.roundedRect(context, x, y, PANEL_WIDTH, height, 7.0F,
                ColorUtil.fadeAlpha(theme.surface, alpha * 0.92F));
        RenderUtil.roundedOutline(context, x, y, PANEL_WIDTH, height, 7.0F,
                ColorUtil.fadeAlpha(theme.border, alpha));

        float cursorY = y + 5.0F;
        for (Quest quest : visible) {
            boolean complete = quest.complete();
            RenderUtil.textFlat(context,
                    RenderUtil.truncate((complete ? "✓ " : "") + quest.name(), PANEL_WIDTH - 16),
                    x + 8.0F, cursorY,
                    ColorUtil.fadeAlpha(complete ? theme.success : theme.textPrimary, alpha));
            cursorY += lineHeight + 2.0F;

            for (Quest.Objective objective : quest.objectives()) {
                RenderUtil.textScaled(context,
                        RenderUtil.truncate(objective.describe(), (int) ((PANEL_WIDTH - 20) / 0.85F)),
                        x + 10.0F, cursorY, 0.85F,
                        ColorUtil.fadeAlpha(objective.complete() ? theme.success : theme.textSecondary,
                                alpha),
                        false);
                cursorY += lineHeight * 0.85F;

                if (this.showProgressBars.get()) {
                    float barWidth = PANEL_WIDTH - 20.0F;
                    RenderUtil.roundedRect(context, x + 10.0F, cursorY + 1.0F, barWidth, 2.0F, 1.0F,
                            ColorUtil.fadeAlpha(theme.divider, alpha));
                    RenderUtil.roundedRect(context, x + 10.0F, cursorY + 1.0F,
                            barWidth * objective.fraction(), 2.0F, 1.0F,
                            ColorUtil.fadeAlpha(objective.complete() ? theme.success : theme.accent,
                                    alpha));
                    cursorY += 5.0F;
                } else {
                    cursorY += 1.0F;
                }
            }
            cursorY += 6.0F;
        }
    }
}
