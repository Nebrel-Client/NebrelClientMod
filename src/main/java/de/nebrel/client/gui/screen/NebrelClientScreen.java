package de.nebrel.client.gui.screen;

import de.nebrel.client.config.ClientSettings;
import de.nebrel.client.core.NebrelClient;
import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.component.IconButtonComponent;
import de.nebrel.client.gui.component.ModalComponent;
import de.nebrel.client.gui.component.ModuleCardComponent;
import de.nebrel.client.gui.component.ScrollContainer;
import de.nebrel.client.gui.component.SearchComponent;
import de.nebrel.client.gui.component.ToggleComponent;
import de.nebrel.client.gui.component.TooltipComponent;
import de.nebrel.client.gui.layout.NametagDesignerView;
import de.nebrel.client.gui.layout.PlusPageView;
import de.nebrel.client.gui.layout.SettingsListView;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.module.Module;
import de.nebrel.client.module.ModuleCategory;
import de.nebrel.client.module.ModuleManager;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.render.icon.Icons;
import de.nebrel.client.render.icon.PixelIcon;
import de.nebrel.client.setting.Setting;
import de.nebrel.client.util.ColorUtil;
import de.nebrel.client.util.NebrelMath;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game client menu.
 *
 * <p>Five views share one window: the module grid, a module's settings, the
 * client's own settings, the Nebrel+ page and the nametag designer. Switching
 * between them slides and cross-fades inside the panel rather than pushing a new
 * screen, so the frame stays put and the transition reads as navigation rather
 * than a jump.</p>
 */
public final class NebrelClientScreen extends Screen {

    private enum View {
        MODULES,
        MODULE_SETTINGS,
        CLIENT_SETTINGS,
        PLUS,
        PLUS_DESIGNER
    }

    // Layout constants, in unscaled GUI pixels before the menu scale is applied.
    private static final float BASE_WIDTH = 560.0F;
    private static final float BASE_HEIGHT = 344.0F;
    private static final float SIDEBAR_WIDTH = 124.0F;
    private static final float HEADER_HEIGHT = 42.0F;
    private static final float PANEL_RADIUS = 12.0F;
    private static final float CONTENT_PADDING = 14.0F;
    private static final float CARD_MIN_WIDTH = 196.0F;
    private static final float CARD_GAP = 8.0F;
    private static final float NAV_ROW_HEIGHT = 22.0F;
    private static final float NAV_GAP = 2.0F;

    private final NebrelClient nebrel;
    private final UiContext ui;
    private final ModuleManager modules;

    private final Animation openAmount;
    private final Animation viewShift;
    private final Animation navHighlightY;
    private final Animation navHighlightHeight;

    private final TooltipComponent tooltip;
    private final ModalComponent modal;
    private final SearchComponent search;
    private final ScrollContainer moduleScroll;
    private final SettingsListView settingsList;
    private final PlusPageView plusPage;
    private final NametagDesignerView designer;
    private final IconButtonComponent themeButton;
    private final IconButtonComponent hudEditorButton;
    private final IconButtonComponent resetButton;
    private final IconButtonComponent backButton;
    private final ToggleComponent headerToggle;

    private final List<ModuleCardComponent> cards = new ArrayList<>();

    private View view = View.MODULES;
    private ModuleCategory category = ModuleCategory.ALL;
    private Module openModule;
    private String query = "";
    private boolean closing;
    private boolean navHighlightPlaced;

    // Panel geometry, recomputed on every resize.
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;

    public NebrelClientScreen() {
        super(Text.literal("Nebrel Client"));
        this.nebrel = NebrelClient.get();
        this.ui = this.nebrel.ui();
        this.modules = this.nebrel.modules();

        this.openAmount = new Animation(0.0F, this.ui.duration(220L), Easing.EASE_OUT_CUBIC);
        this.viewShift = new Animation(0.0F, this.ui.duration(200L), Easing.EASE_OUT_CUBIC);
        this.navHighlightY = new Animation(0.0F, this.ui.duration(180L), Easing.EASE_OUT_CUBIC);
        this.navHighlightHeight = new Animation(NAV_ROW_HEIGHT, this.ui.duration(180L),
                Easing.EASE_OUT_CUBIC);

        this.tooltip = new TooltipComponent(this.ui);
        this.modal = new ModalComponent(this.ui);
        this.search = new SearchComponent(this.ui, this::onQueryChanged);
        this.moduleScroll = new ScrollContainer(this.ui);
        this.settingsList = new SettingsListView(this.ui);
        this.plusPage = new PlusPageView(this.ui, this.nebrel.plus(), this::openDesigner);
        this.designer = new NametagDesignerView(this.ui, this.nebrel.plus(),
                this::onKeybindCapture);

        this.themeButton = new IconButtonComponent(this.ui, "◐",
                () -> this.nebrel.themes().cycleTheme())
                .tooltip("Cycle Dark, Light and Glass themes");
        this.hudEditorButton = new IconButtonComponent(this.ui, "▤",
                () -> this.client.setScreen(new HudEditorScreen(this)))
                .tooltip("Open the HUD editor");
        this.resetButton = new IconButtonComponent(this.ui, "↺", this::confirmReset)
                .tooltip("Reset these settings to their defaults");
        this.backButton = new IconButtonComponent(this.ui, "‹", this::goBack)
                .tooltip("Back to the module list");
        this.headerToggle = new ToggleComponent(this.ui,
                () -> this.openModule != null && this.openModule.enabled(),
                value -> {
                    if (this.openModule != null) {
                        this.modules.setEnabled(this.openModule, value);
                    }
                });
    }

    // -- lifecycle -----------------------------------------------------------

    @Override
    protected void init() {
        this.openAmount.set(0.0F);
        this.openAmount.animateTo(1.0F);
        this.navHighlightPlaced = false;
        recomputeLayout();
        rebuildCards();
    }

    @Override
    public boolean shouldPause() {
        // The world keeps running behind the menu, as it does in a client menu.
        return false;
    }

    @Override
    public void removed() {
        this.nebrel.keybinds().setCapturing(false);
        // The menu is where most settings are changed, so flush on the way out
        // rather than waiting for the debounce.
        this.nebrel.config().saveNow();
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        recomputeLayout();
        rebuildCards();
    }

    private void recomputeLayout() {
        float scale = this.ui.settings().menuScale.getFloat();
        this.panelWidth = Math.min(this.width - 32.0F, BASE_WIDTH * scale);
        this.panelHeight = Math.min(this.height - 32.0F, BASE_HEIGHT * scale);
        // Very small windows: fall back to almost the whole screen.
        this.panelWidth = Math.max(this.panelWidth, Math.min(this.width - 8.0F, 320.0F));
        this.panelHeight = Math.max(this.panelHeight, Math.min(this.height - 8.0F, 200.0F));
        this.panelX = (this.width - this.panelWidth) / 2.0F;
        this.panelY = (this.height - this.panelHeight) / 2.0F;
    }

    private float sidebarWidth() {
        // Collapse the sidebar labels on narrow windows rather than clipping them.
        return this.panelWidth < 420.0F ? 40.0F : SIDEBAR_WIDTH;
    }

    private boolean sidebarCollapsed() {
        return sidebarWidth() < 60.0F;
    }

    private float contentLeft() {
        return this.panelX + sidebarWidth();
    }

    private float contentWidth() {
        return this.panelWidth - sidebarWidth();
    }

    // -- module list ---------------------------------------------------------

    private void onQueryChanged(String newQuery) {
        this.query = newQuery;
        rebuildCards();
        this.moduleScroll.reset();
    }

    private List<Module> currentModules() {
        // A non-empty query searches everything, because a user typing "fog"
        // wants No Fog whichever category happens to be selected.
        if (!this.query.isBlank()) {
            return this.modules.search(this.query);
        }
        return this.modules.getByCategory(this.category);
    }

    private void rebuildCards() {
        this.cards.clear();
        for (Module module : currentModules()) {
            this.cards.add(new ModuleCardComponent(this.ui, module, this.modules, this::openModule));
        }
    }

    private int columnCount() {
        float available = contentWidth() - CONTENT_PADDING * 2.0F;
        return Math.max(1, (int) ((available + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP)));
    }

    // -- navigation ----------------------------------------------------------

    private void openModule(Module module) {
        this.openModule = module;
        this.settingsList.setSettings(module.settings(), this::onKeybindCapture);
        this.settingsList.resetScroll();
        this.headerToggle.syncImmediately();
        switchView(View.MODULE_SETTINGS);
    }

    private void openClientSettings() {
        this.openModule = null;
        List<Setting<?>> settings = new ArrayList<>(this.ui.settings().settings());
        this.settingsList.setSettings(settings, this::onKeybindCapture);
        this.settingsList.resetScroll();
        switchView(View.CLIENT_SETTINGS);
    }

    private void openPlus() {
        this.openModule = null;
        this.plusPage.resetScroll();
        switchView(View.PLUS);
    }

    private void openDesigner() {
        this.designer.resetScroll();
        switchView(View.PLUS_DESIGNER);
    }

    private void goBack() {
        // The designer was opened from the Nebrel+ page, so back returns there
        // rather than jumping all the way out to the module grid.
        if (this.view == View.PLUS_DESIGNER) {
            switchView(View.PLUS);
            return;
        }
        this.openModule = null;
        switchView(View.MODULES);
    }

    private void switchView(View target) {
        if (this.view == target) {
            return;
        }
        this.settingsList.closeOverlays();
        this.designer.closeOverlays();
        this.tooltip.dismiss();
        this.view = target;
        // Slide the incoming view in from the side it conceptually comes from.
        this.viewShift.set(target == View.MODULES ? -1.0F : 1.0F);
        this.viewShift.animateTo(0.0F);
    }

    private void onKeybindCapture(boolean capturing) {
        this.nebrel.keybinds().setCapturing(capturing);
    }

    private void confirmReset() {
        if (this.view == View.MODULE_SETTINGS && this.openModule != null) {
            Module target = this.openModule;
            this.modal.show("Reset " + target.name() + "?",
                    "Every setting for this module goes back to its default. "
                            + "This cannot be undone.",
                    "Reset",
                    () -> {
                        target.resetSettings();
                        this.nebrel.config().markDirty();
                        this.nebrel.notifications().info(target.name(), "Settings reset to defaults.");
                    });
        } else if (this.view == View.PLUS_DESIGNER) {
            this.modal.show("Reset Nebrel+ styling?",
                    "Badge style, nametag colours, every effect and the additional "
                            + "nametag go back to their defaults. This cannot be undone.",
                    "Reset",
                    () -> {
                        this.designer.resetAll();
                        this.nebrel.plus().invalidateCaches();
                        this.nebrel.config().markDirty();
                        this.nebrel.notifications().info("Nebrel+",
                                "Styling reset to defaults.");
                    });
        } else if (this.view == View.CLIENT_SETTINGS) {
            this.modal.show("Reset client settings?",
                    "Menu key, theme options and animation preferences go back to "
                            + "their defaults. Module settings are not affected.",
                    "Reset",
                    () -> {
                        for (Setting<?> setting : this.ui.settings().settings()) {
                            setting.reset();
                        }
                        this.nebrel.themes().resetAccent();
                        this.nebrel.config().markDirty();
                        this.nebrel.notifications().info("Client settings",
                                "Reset to defaults.");
                    });
        }
    }

    // -- rendering -----------------------------------------------------------

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientSettings settings = this.ui.settings();
        float amount = this.openAmount.value();
        float strength = settings.backgroundStrength.getFloat();

        switch (settings.backgroundStyle.get()) {
            case NONE -> {
                // Nothing behind the panel; the world stays fully visible.
            }
            case BLUR -> {
                // Defer to the game's own menu backdrop, then add a light scrim
                // so panel text keeps its contrast over bright scenes.
                super.renderBackground(context, mouseX, mouseY, delta);
                RenderUtil.rect(context, 0.0F, 0.0F, this.width, this.height,
                        ColorUtil.fadeAlpha(this.ui.theme().scrim,
                                amount * NebrelMath.clamp(strength * 0.35F, 0.0F, 1.0F)));
            }
            case DIM -> RenderUtil.rect(context, 0.0F, 0.0F, this.width, this.height,
                    ColorUtil.fadeAlpha(this.ui.theme().scrim,
                            amount * NebrelMath.clamp(strength * 0.85F, 0.0F, 1.0F)));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float amount = this.openAmount.value();
        if (this.closing && this.openAmount.finished()) {
            this.client.setScreen(null);
            return;
        }

        renderBackground(context, mouseX, mouseY, delta);

        Theme theme = this.ui.theme();
        MatrixStack matrices = context.getMatrices();

        // The panel scales up from 96% and rises slightly as it opens.
        float scale = 0.96F + 0.04F * amount;
        float centerX = this.panelX + this.panelWidth / 2.0F;
        float centerY = this.panelY + this.panelHeight / 2.0F;

        matrices.push();
        matrices.translate(centerX, centerY, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        matrices.translate(-centerX, -centerY + (1.0F - amount) * 10.0F, 0.0F);

        RenderUtil.shadow(context, this.panelX, this.panelY, this.panelWidth, this.panelHeight,
                PANEL_RADIUS, ColorUtil.fadeAlpha(theme.shadow, amount), 6);
        RenderUtil.roundedRect(context, this.panelX, this.panelY, this.panelWidth, this.panelHeight,
                PANEL_RADIUS, ColorUtil.fadeAlpha(theme.background, amount));
        RenderUtil.roundedOutline(context, this.panelX, this.panelY, this.panelWidth, this.panelHeight,
                PANEL_RADIUS, ColorUtil.fadeAlpha(theme.border, amount));

        renderSidebar(context, mouseX, mouseY, delta, amount);
        renderContent(context, mouseX, mouseY, delta, amount);

        matrices.pop();

        this.tooltip.request(tooltipUnder(mouseX, mouseY), mouseX, mouseY);
        this.tooltip.render(context);
        this.modal.render(context, mouseX, mouseY, delta);
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta, float amount) {
        Theme theme = this.ui.theme();
        float sidebar = sidebarWidth();
        boolean collapsed = sidebarCollapsed();

        // Sidebar sits on the surface layer so it reads as a distinct column.
        RenderUtil.roundedRect(context, this.panelX, this.panelY, sidebar, this.panelHeight,
                PANEL_RADIUS, ColorUtil.fadeAlpha(theme.surface, amount));
        // Square off the inner edge, which the rounded rect would otherwise curve.
        RenderUtil.rect(context, this.panelX + sidebar - PANEL_RADIUS, this.panelY,
                PANEL_RADIUS, this.panelHeight, ColorUtil.fadeAlpha(theme.surface, amount));
        RenderUtil.rect(context, this.panelX + sidebar, this.panelY + 1.0F, 1.0F, this.panelHeight - 2.0F,
                ColorUtil.fadeAlpha(theme.divider, amount));

        // Brand.
        float brandY = this.panelY + 15.0F;
        if (collapsed) {
            RenderUtil.textCentered(context, "N", this.panelX + sidebar / 2.0F, brandY,
                    ColorUtil.fadeAlpha(theme.accent, amount));
        } else {
            RenderUtil.textFlat(context, "Nebrel", this.panelX + 14.0F, brandY,
                    ColorUtil.fadeAlpha(theme.textPrimary, amount));
            RenderUtil.textFlat(context, "Client",
                    this.panelX + 14.0F + RenderUtil.textWidth("Nebrel "), brandY,
                    ColorUtil.fadeAlpha(theme.accent, amount));
        }

        List<ModuleCategory> entries = navEntries();
        float cursorY = this.panelY + HEADER_HEIGHT;

        // Sliding highlight behind the active row.
        int activeIndex = entries.indexOf(this.category);
        if (activeIndex >= 0) {
            float targetY = cursorY + navIndexOffset(entries, activeIndex);
            if (!this.navHighlightPlaced) {
                this.navHighlightY.set(targetY);
                this.navHighlightHeight.set(NAV_ROW_HEIGHT);
                this.navHighlightPlaced = true;
            } else {
                this.navHighlightY.animateTo(targetY);
            }
            RenderUtil.roundedRect(context, this.panelX + 6.0F, this.navHighlightY.value(),
                    sidebar - 12.0F, this.navHighlightHeight.value(), 6.0F,
                    ColorUtil.fadeAlpha(theme.accentSoft, amount));
            RenderUtil.roundedRect(context, this.panelX + 6.0F, this.navHighlightY.value() + 4.0F,
                    2.0F, this.navHighlightHeight.value() - 8.0F, 1.0F,
                    ColorUtil.fadeAlpha(theme.accent, amount));
        }

        for (int i = 0; i < entries.size(); i++) {
            ModuleCategory entry = entries.get(i);
            float rowY = cursorY + navIndexOffset(entries, i);
            boolean active = entry == this.category;
            boolean hovered = RenderUtil.hovered(mouseX, mouseY, this.panelX + 6.0F, rowY,
                    sidebar - 12.0F, NAV_ROW_HEIGHT);

            if (hovered && !active) {
                RenderUtil.roundedRect(context, this.panelX + 6.0F, rowY, sidebar - 12.0F,
                        NAV_ROW_HEIGHT, 6.0F, ColorUtil.fadeAlpha(theme.surfaceHover, amount * 0.9F));
            }

            int color = active ? theme.accent : (hovered ? theme.textPrimary : theme.textSecondary);
            color = ColorUtil.fadeAlpha(color, amount);
            float textY = rowY + (NAV_ROW_HEIGHT - RenderUtil.lineHeight()) / 2.0F + 1.0F;

            PixelIcon bitmap = iconFor(entry);
            if (collapsed) {
                if (bitmap != null) {
                    float iconSize = RenderUtil.lineHeight();
                    RenderUtil.icon(context, bitmap, this.panelX + sidebar / 2.0F - iconSize / 2.0F,
                            textY, iconSize, color);
                } else {
                    RenderUtil.textCentered(context, entry.icon(), this.panelX + sidebar / 2.0F,
                            textY, color);
                }
            } else {
                if (bitmap != null) {
                    RenderUtil.icon(context, bitmap, this.panelX + 15.0F, textY,
                            RenderUtil.lineHeight(), color);
                } else {
                    RenderUtil.textFlat(context, entry.icon(), this.panelX + 15.0F, textY, color);
                }
                RenderUtil.textFlat(context, entry.displayName(), this.panelX + 30.0F, textY, color);

                int count = entry == ModuleCategory.FAVORITES
                        ? this.modules.getFavorites().size()
                        : this.modules.getByCategory(entry).size();
                if (count > 0) {
                    RenderUtil.textScaled(context, String.valueOf(count),
                            this.panelX + sidebar - 18.0F, textY + 1.0F, 0.8F,
                            ColorUtil.fadeAlpha(theme.textDisabled, amount), false);
                }
            }
        }

        // Nebrel+ and client settings are pinned to the bottom, below the
        // categories, because they are destinations rather than filters.
        float plusY = plusRowY();
        boolean plusActive = this.view == View.PLUS || this.view == View.PLUS_DESIGNER;
        boolean plusHovered = RenderUtil.hovered(mouseX, mouseY, this.panelX + 6.0F, plusY,
                sidebar - 12.0F, NAV_ROW_HEIGHT);
        if (plusActive || plusHovered) {
            RenderUtil.roundedRect(context, this.panelX + 6.0F, plusY, sidebar - 12.0F,
                    NAV_ROW_HEIGHT, 6.0F,
                    ColorUtil.fadeAlpha(plusActive ? theme.accentSoft : theme.surfaceHover, amount));
        }
        // The Nebrel+ row keeps the accent even when it is not selected: it is
        // the one entry in the list that names a product rather than a view.
        int plusColor = ColorUtil.fadeAlpha(
                plusActive || plusHovered ? theme.accent : ColorUtil.withAlpha(theme.accent, 200),
                amount);
        float plusTextY = plusY + (NAV_ROW_HEIGHT - RenderUtil.lineHeight()) / 2.0F + 1.0F;
        if (collapsed) {
            RenderUtil.textCentered(context, "✦", this.panelX + sidebar / 2.0F, plusTextY, plusColor);
        } else {
            RenderUtil.textFlat(context, "✦", this.panelX + 15.0F, plusTextY, plusColor);
            RenderUtil.textFlat(context, "Nebrel+", this.panelX + 30.0F, plusTextY, plusColor);
            if (this.nebrel.plus().localIsPlus()) {
                RenderUtil.textScaled(context, "ON", this.panelX + sidebar - 20.0F,
                        plusTextY + 1.0F, 0.8F,
                        ColorUtil.fadeAlpha(theme.success, amount), false);
            }
        }

        float settingsY = settingsRowY();
        boolean settingsActive = this.view == View.CLIENT_SETTINGS;
        boolean settingsHovered = RenderUtil.hovered(mouseX, mouseY, this.panelX + 6.0F, settingsY,
                sidebar - 12.0F, NAV_ROW_HEIGHT);
        if (settingsActive || settingsHovered) {
            RenderUtil.roundedRect(context, this.panelX + 6.0F, settingsY, sidebar - 12.0F,
                    NAV_ROW_HEIGHT, 6.0F,
                    ColorUtil.fadeAlpha(settingsActive ? theme.accentSoft : theme.surfaceHover, amount));
        }
        int settingsColor = ColorUtil.fadeAlpha(
                settingsActive ? theme.accent : (settingsHovered ? theme.textPrimary : theme.textSecondary),
                amount);
        float settingsTextY = settingsY + (NAV_ROW_HEIGHT - RenderUtil.lineHeight()) / 2.0F + 1.0F;
        float settingsIconSize = RenderUtil.lineHeight();
        if (collapsed) {
            RenderUtil.icon(context, Icons.SETTINGS, this.panelX + sidebar / 2.0F - settingsIconSize / 2.0F,
                    settingsTextY, settingsIconSize, settingsColor);
        } else {
            RenderUtil.icon(context, Icons.SETTINGS, this.panelX + 15.0F, settingsTextY,
                    settingsIconSize, settingsColor);
            RenderUtil.textFlat(context, "Settings", this.panelX + 30.0F, settingsTextY, settingsColor);
        }
    }

    /**
     * The hand-drawn icon for a category, or {@code null} to keep the
     * category's own single glyph. Only {@code MISC} has no bitmap — nothing
     * uses that category yet, so it was not worth drawing one blind.
     */
    private static PixelIcon iconFor(ModuleCategory category) {
        return switch (category) {
            case ALL -> Icons.ALL;
            case FAVORITES -> Icons.FAVORITES;
            case HUD -> Icons.HUD;
            case VISUAL -> Icons.VISUAL;
            case PLAYER -> Icons.PLAYER;
            case WORLD -> Icons.WORLD;
            case RENDER -> Icons.RENDER;
            case UTILITY -> Icons.UTILITY;
            case MISC -> null;
        };
    }

    private float settingsRowY() {
        return this.panelY + this.panelHeight - NAV_ROW_HEIGHT - 10.0F;
    }

    private float plusRowY() {
        return settingsRowY() - NAV_ROW_HEIGHT - NAV_GAP;
    }

    /** Vertical offset of nav row {@code index}, including the group gap. */
    private float navIndexOffset(List<ModuleCategory> entries, int index) {
        float offset = 0.0F;
        for (int i = 0; i < index; i++) {
            offset += NAV_ROW_HEIGHT + NAV_GAP;
            // A wider gap separates the two virtual entries from the categories.
            if (entries.get(i) == ModuleCategory.FAVORITES) {
                offset += 8.0F;
            }
        }
        return offset;
    }

    private List<ModuleCategory> navEntries() {
        List<ModuleCategory> entries = new ArrayList<>();
        entries.add(ModuleCategory.ALL);
        entries.add(ModuleCategory.FAVORITES);
        for (ModuleCategory candidate : ModuleCategory.values()) {
            if (candidate.real()) {
                entries.add(candidate);
            }
        }
        return entries;
    }

    private void renderContent(DrawContext context, int mouseX, int mouseY, float delta, float amount) {
        float shift = this.viewShift.value() * 18.0F;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(shift, 0.0F, 0.0F);

        switch (this.view) {
            case MODULES -> renderModuleView(context, mouseX, mouseY, delta, amount);
            case MODULE_SETTINGS -> renderModuleSettings(context, mouseX, mouseY, delta, amount);
            case CLIENT_SETTINGS -> renderClientSettings(context, mouseX, mouseY, delta, amount);
            case PLUS -> renderPlusPage(context, mouseX, mouseY, delta, amount);
            case PLUS_DESIGNER -> renderDesigner(context, mouseX, mouseY, delta, amount);
        }

        matrices.pop();
    }

    private void renderModuleView(DrawContext context, int mouseX, int mouseY, float delta, float amount) {
        Theme theme = this.ui.theme();
        float left = contentLeft();
        float width = contentWidth();

        // Header: title on the left, search on the right.
        float headerY = this.panelY + 12.0F;
        String title = this.query.isBlank() ? this.category.displayName() : "Search";
        RenderUtil.textFlat(context, title, left + CONTENT_PADDING, headerY + 4.0F,
                ColorUtil.fadeAlpha(theme.textPrimary, amount));

        float buttonSize = 20.0F;
        float rightEdge = left + width - CONTENT_PADDING;

        this.themeButton.setBounds(rightEdge - buttonSize, headerY, buttonSize, buttonSize);
        this.themeButton.render(context, mouseX, mouseY, delta);

        this.hudEditorButton.setBounds(rightEdge - buttonSize * 2.0F - 4.0F, headerY,
                buttonSize, buttonSize);
        this.hudEditorButton.render(context, mouseX, mouseY, delta);

        float searchWidth = Math.min(180.0F, width - CONTENT_PADDING * 2.0F
                - RenderUtil.textWidth(title) - buttonSize * 2.0F - 24.0F);
        if (searchWidth > 70.0F) {
            this.search.setBounds(rightEdge - buttonSize * 2.0F - 12.0F - searchWidth,
                    headerY, searchWidth, buttonSize);
            this.search.setVisible(true);
            this.search.render(context, mouseX, mouseY, delta);
        } else {
            this.search.setVisible(false);
        }

        RenderUtil.rect(context, left + 1.0F, this.panelY + HEADER_HEIGHT - 6.0F, width - 2.0F, 1.0F,
                ColorUtil.fadeAlpha(theme.divider, amount));

        // Card grid.
        float gridTop = this.panelY + HEADER_HEIGHT;
        float gridHeight = this.panelHeight - HEADER_HEIGHT - 8.0F;
        this.moduleScroll.setBounds(left, gridTop, width, gridHeight);

        int columns = columnCount();
        float usable = width - CONTENT_PADDING * 2.0F - (this.moduleScroll.scrollable() ? 10.0F : 0.0F);
        float cardWidth = (usable - (columns - 1) * CARD_GAP) / columns;
        int rows = (int) Math.ceil(this.cards.size() / (float) columns);
        float contentHeight = rows * (ModuleCardComponent.HEIGHT + CARD_GAP) + CONTENT_PADDING;
        this.moduleScroll.setContentHeight(contentHeight);
        float offset = this.moduleScroll.offset();

        this.moduleScroll.begin(context);
        for (int i = 0; i < this.cards.size(); i++) {
            int column = i % columns;
            int rowIndex = i / columns;
            float cardX = left + CONTENT_PADDING + column * (cardWidth + CARD_GAP);
            float cardY = gridTop + 4.0F + rowIndex * (ModuleCardComponent.HEIGHT + CARD_GAP) - offset;

            ModuleCardComponent card = this.cards.get(i);
            card.setBounds(cardX, cardY, cardWidth, ModuleCardComponent.HEIGHT);
            if (cardY + ModuleCardComponent.HEIGHT < gridTop || cardY > gridTop + gridHeight) {
                continue;
            }
            card.render(context, mouseX, mouseY, delta);
        }
        this.moduleScroll.end(context);
        this.moduleScroll.render(context, mouseX, mouseY, delta);

        if (this.cards.isEmpty()) {
            String message = this.query.isBlank()
                    ? "Nothing here yet"
                    : "No modules match \"" + this.query + "\"";
            RenderUtil.textCentered(context, message, left + width / 2.0F,
                    gridTop + gridHeight / 2.0F - RenderUtil.lineHeight(),
                    ColorUtil.fadeAlpha(theme.textDisabled, amount));
            if (this.category == ModuleCategory.FAVORITES && this.query.isBlank()) {
                RenderUtil.textScaledCentered(context,
                        "Click the star on a module card to add it here",
                        left + width / 2.0F, gridTop + gridHeight / 2.0F + 2.0F, 0.85F,
                        ColorUtil.fadeAlpha(theme.textDisabled, amount));
            }
        }
    }

    private void renderModuleSettings(DrawContext context, int mouseX, int mouseY, float delta,
                                      float amount) {
        if (this.openModule == null) {
            goBack();
            return;
        }
        Theme theme = this.ui.theme();
        float left = contentLeft();
        float width = contentWidth();
        float headerY = this.panelY + 12.0F;

        this.backButton.setBounds(left + CONTENT_PADDING - 4.0F, headerY, 20.0F, 20.0F);
        this.backButton.render(context, mouseX, mouseY, delta);

        float textX = left + CONTENT_PADDING + 22.0F;
        float rightEdge = left + width - CONTENT_PADDING;

        this.headerToggle.setBounds(rightEdge - ToggleComponent.TRACK_WIDTH,
                headerY + 3.0F, ToggleComponent.TRACK_WIDTH, ToggleComponent.TRACK_HEIGHT);
        this.headerToggle.render(context, mouseX, mouseY, delta);

        this.resetButton.setBounds(rightEdge - ToggleComponent.TRACK_WIDTH - 26.0F, headerY,
                20.0F, 20.0F);
        this.resetButton.render(context, mouseX, mouseY, delta);

        int budget = (int) Math.max(30.0F, rightEdge - textX - ToggleComponent.TRACK_WIDTH - 34.0F);
        RenderUtil.textFlat(context,
                this.openModule.icon() + "  " + RenderUtil.truncate(this.openModule.name(), budget),
                textX, headerY + 1.0F, ColorUtil.fadeAlpha(theme.textPrimary, amount));
        if (!this.openModule.description().isEmpty()) {
            RenderUtil.textScaled(context,
                    RenderUtil.truncate(this.openModule.description(), (int) (budget / 0.85F)),
                    textX, headerY + RenderUtil.lineHeight() + 2.0F, 0.85F,
                    ColorUtil.fadeAlpha(theme.textSecondary, amount), false);
        }

        RenderUtil.rect(context, left + 1.0F, this.panelY + HEADER_HEIGHT - 6.0F, width - 2.0F, 1.0F,
                ColorUtil.fadeAlpha(theme.divider, amount));

        this.settingsList.setBounds(left + CONTENT_PADDING, this.panelY + HEADER_HEIGHT,
                width - CONTENT_PADDING * 2.0F, this.panelHeight - HEADER_HEIGHT - 10.0F);
        this.settingsList.render(context, mouseX, mouseY, delta);
    }

    private void renderClientSettings(DrawContext context, int mouseX, int mouseY, float delta,
                                      float amount) {
        Theme theme = this.ui.theme();
        float left = contentLeft();
        float width = contentWidth();
        float headerY = this.panelY + 12.0F;

        this.backButton.setBounds(left + CONTENT_PADDING - 4.0F, headerY, 20.0F, 20.0F);
        this.backButton.render(context, mouseX, mouseY, delta);

        RenderUtil.textFlat(context, "⚙  Client Settings", left + CONTENT_PADDING + 22.0F,
                headerY + 1.0F, ColorUtil.fadeAlpha(theme.textPrimary, amount));
        RenderUtil.textScaled(context, "Menu, theme and interface preferences",
                left + CONTENT_PADDING + 22.0F, headerY + RenderUtil.lineHeight() + 2.0F, 0.85F,
                ColorUtil.fadeAlpha(theme.textSecondary, amount), false);

        float rightEdge = left + width - CONTENT_PADDING;
        this.resetButton.setBounds(rightEdge - 20.0F, headerY, 20.0F, 20.0F);
        this.resetButton.render(context, mouseX, mouseY, delta);

        RenderUtil.rect(context, left + 1.0F, this.panelY + HEADER_HEIGHT - 6.0F, width - 2.0F, 1.0F,
                ColorUtil.fadeAlpha(theme.divider, amount));

        float listTop = this.panelY + HEADER_HEIGHT;
        float accentRowHeight = 30.0F;
        renderAccentRow(context, left + CONTENT_PADDING, listTop, width - CONTENT_PADDING * 2.0F,
                mouseX, mouseY, amount);

        this.settingsList.setBounds(left + CONTENT_PADDING, listTop + accentRowHeight,
                width - CONTENT_PADDING * 2.0F,
                this.panelHeight - HEADER_HEIGHT - accentRowHeight - 10.0F);
        this.settingsList.render(context, mouseX, mouseY, delta);
    }

    private void renderPlusPage(DrawContext context, int mouseX, int mouseY, float delta,
                                float amount) {
        float left = contentLeft();
        float width = contentWidth();
        renderSubHeader(context, "Nebrel+", "Membership, badge and nametag styling",
                false, amount, mouseX, mouseY, delta);

        this.plusPage.setBounds(left + CONTENT_PADDING, this.panelY + HEADER_HEIGHT,
                width - CONTENT_PADDING * 2.0F, this.panelHeight - HEADER_HEIGHT - 10.0F);
        this.plusPage.render(context, mouseX, mouseY, delta);
    }

    private void renderDesigner(DrawContext context, int mouseX, int mouseY, float delta,
                                float amount) {
        float left = contentLeft();
        float width = contentWidth();
        renderSubHeader(context, "✎  Nametag Designer",
                "Changes apply live, above your head and in the preview",
                true, amount, mouseX, mouseY, delta);

        this.designer.setBounds(left + CONTENT_PADDING, this.panelY + HEADER_HEIGHT,
                width - CONTENT_PADDING * 2.0F, this.panelHeight - HEADER_HEIGHT - 10.0F);
        this.designer.render(context, mouseX, mouseY, delta);
    }

    /**
     * The back arrow, a title, a subtitle and the divider.
     *
     * <p>Shared by the views that are entered from somewhere else, so they line
     * up with the module settings header rather than each inventing its own.</p>
     */
    private void renderSubHeader(DrawContext context, String title, String subtitle,
                                 boolean withReset, float amount,
                                 int mouseX, int mouseY, float delta) {
        Theme theme = this.ui.theme();
        float left = contentLeft();
        float width = contentWidth();
        float headerY = this.panelY + 12.0F;

        this.backButton.setBounds(left + CONTENT_PADDING - 4.0F, headerY, 20.0F, 20.0F);
        this.backButton.render(context, mouseX, mouseY, delta);

        float textX = left + CONTENT_PADDING + 22.0F;
        if (withReset) {
            this.resetButton.setBounds(left + width - CONTENT_PADDING - 20.0F, headerY,
                    20.0F, 20.0F);
            this.resetButton.render(context, mouseX, mouseY, delta);
        }

        RenderUtil.textFlat(context, title, textX, headerY + 1.0F,
                ColorUtil.fadeAlpha(theme.textPrimary, amount));
        RenderUtil.textScaled(context, subtitle, textX, headerY + RenderUtil.lineHeight() + 2.0F,
                0.85F, ColorUtil.fadeAlpha(theme.textSecondary, amount), false);

        RenderUtil.rect(context, left + 1.0F, this.panelY + HEADER_HEIGHT - 6.0F, width - 2.0F,
                1.0F, ColorUtil.fadeAlpha(theme.divider, amount));
    }

    /** Accent swatches, shown above the client settings list. */
    private void renderAccentRow(DrawContext context, float x, float y, float width,
                                 int mouseX, int mouseY, float amount) {
        Theme theme = this.ui.theme();
        RenderUtil.textScaled(context, "ACCENT", x, y + 2.0F, 0.85F,
                ColorUtil.fadeAlpha(ColorUtil.withAlpha(theme.textSecondary, 210), amount), false);

        float swatchSize = 14.0F;
        float startX = x + RenderUtil.textWidthScaled("ACCENT", 0.85F) + 10.0F;
        int[] presets = accentPresets();
        for (int i = 0; i < presets.length; i++) {
            float swatchX = startX + i * (swatchSize + 5.0F);
            if (swatchX + swatchSize > x + width) {
                break;
            }
            boolean selected = presets[i] == this.nebrel.themes().accent();
            boolean hovered = RenderUtil.hovered(mouseX, mouseY, swatchX, y, swatchSize, swatchSize);
            RenderUtil.roundedRect(context, swatchX, y, swatchSize, swatchSize, 4.0F,
                    ColorUtil.fadeAlpha(presets[i], amount));
            if (selected || hovered) {
                RenderUtil.roundedOutline(context, swatchX - 2.0F, y - 2.0F,
                        swatchSize + 4.0F, swatchSize + 4.0F, 5.0F,
                        ColorUtil.fadeAlpha(selected ? theme.textPrimary : theme.border, amount));
            }
        }
    }

    private static int[] accentPresets() {
        return new int[]{
                0xFF8B5CF6, 0xFF3B82F6, 0xFF06B6D4, 0xFF3DD68C,
                0xFFF5B547, 0xFFF2555A, 0xFFEC6BC8, 0xFF94A3B8
        };
    }

    // -- tooltips ------------------------------------------------------------

    private String tooltipUnder(int mouseX, int mouseY) {
        if (this.modal.open()) {
            return null;
        }
        if (this.view == View.MODULES) {
            if (this.themeButton.isHovered(mouseX, mouseY)) {
                return this.themeButton.tooltip();
            }
            if (this.hudEditorButton.isHovered(mouseX, mouseY)) {
                return this.hudEditorButton.tooltip();
            }
            for (ModuleCardComponent card : this.cards) {
                if (card.isHovered(mouseX, mouseY)) {
                    return card.tooltip();
                }
            }
            return null;
        }
        if (this.backButton.isHovered(mouseX, mouseY)) {
            return this.backButton.tooltip();
        }
        if (this.resetButton.isHovered(mouseX, mouseY)) {
            return this.resetButton.tooltip();
        }
        if (this.view == View.PLUS) {
            return this.plusPage.tooltipAt(mouseX, mouseY);
        }
        if (this.view == View.PLUS_DESIGNER) {
            return this.designer.tooltipAt(mouseX, mouseY);
        }
        return this.settingsList.tooltipAt(mouseX, mouseY);
    }

    // -- input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modal.open()) {
            return this.modal.mouseClicked(mouseX, mouseY, button);
        }

        if (handleSidebarClick(mouseX, mouseY, button)) {
            return true;
        }

        switch (this.view) {
            case MODULES -> {
                if (this.search.visible() && this.search.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.themeButton.mouseClicked(mouseX, mouseY, button)
                        || this.hudEditorButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.moduleScroll.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                for (ModuleCardComponent card : this.cards) {
                    if (card.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
            case MODULE_SETTINGS -> {
                if (this.backButton.mouseClicked(mouseX, mouseY, button)
                        || this.resetButton.mouseClicked(mouseX, mouseY, button)
                        || this.headerToggle.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.settingsList.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            case CLIENT_SETTINGS -> {
                if (this.backButton.mouseClicked(mouseX, mouseY, button)
                        || this.resetButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (handleAccentClick(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.settingsList.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            case PLUS -> {
                if (this.backButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.plusPage.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            case PLUS_DESIGNER -> {
                if (this.backButton.mouseClicked(mouseX, mouseY, button)
                        || this.resetButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.designer.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSidebarClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        float sidebar = sidebarWidth();
        if (mouseX < this.panelX || mouseX > this.panelX + sidebar) {
            return false;
        }

        List<ModuleCategory> entries = navEntries();
        float cursorY = this.panelY + HEADER_HEIGHT;
        for (int i = 0; i < entries.size(); i++) {
            float rowY = cursorY + navIndexOffset(entries, i);
            if (RenderUtil.hovered(mouseX, mouseY, this.panelX + 6.0F, rowY,
                    sidebar - 12.0F, NAV_ROW_HEIGHT)) {
                this.category = entries.get(i);
                this.search.clear();
                this.query = "";
                rebuildCards();
                this.moduleScroll.reset();
                switchView(View.MODULES);
                return true;
            }
        }

        if (RenderUtil.hovered(mouseX, mouseY, this.panelX + 6.0F, plusRowY(),
                sidebar - 12.0F, NAV_ROW_HEIGHT)) {
            openPlus();
            return true;
        }
        if (RenderUtil.hovered(mouseX, mouseY, this.panelX + 6.0F, settingsRowY(),
                sidebar - 12.0F, NAV_ROW_HEIGHT)) {
            openClientSettings();
            return true;
        }
        return false;
    }

    private boolean handleAccentClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        float x = contentLeft() + CONTENT_PADDING;
        float y = this.panelY + HEADER_HEIGHT;
        float startX = x + RenderUtil.textWidthScaled("ACCENT", 0.85F) + 10.0F;
        int[] presets = accentPresets();
        for (int i = 0; i < presets.length; i++) {
            float swatchX = startX + i * 19.0F;
            if (RenderUtil.hovered(mouseX, mouseY, swatchX, y, 14.0F, 14.0F)) {
                this.nebrel.themes().setAccent(presets[i]);
                this.nebrel.config().markDirty();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.moduleScroll.mouseReleased(mouseX, mouseY, button);
        this.settingsList.mouseReleased(mouseX, mouseY, button);
        this.plusPage.mouseReleased(mouseX, mouseY, button);
        this.designer.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.modal.open()) {
            return true;
        }
        switch (this.view) {
            case MODULES -> {
                if (this.moduleScroll.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
                if (this.search.visible()
                        && this.search.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
            case PLUS -> {
                if (this.plusPage.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
            case PLUS_DESIGNER -> {
                if (this.designer.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
            default -> {
                if (this.settingsList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.modal.open()) {
            return true;
        }
        switch (this.view) {
            case MODULES -> {
                if (this.moduleScroll.mouseScrolled(mouseX, mouseY, vertical)) {
                    return true;
                }
            }
            case PLUS -> {
                if (this.plusPage.mouseScrolled(mouseX, mouseY, vertical)) {
                    return true;
                }
            }
            case PLUS_DESIGNER -> {
                if (this.designer.mouseScrolled(mouseX, mouseY, vertical)) {
                    return true;
                }
            }
            default -> {
                if (this.settingsList.mouseScrolled(mouseX, mouseY, vertical)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.modal.open()) {
            return this.modal.keyPressed(keyCode, scanCode, modifiers);
        }

        // A capturing keybind row or an open popover takes the key first.
        if (this.view == View.PLUS_DESIGNER
                && this.designer.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.view == View.MODULE_SETTINGS || this.view == View.CLIENT_SETTINGS) {
            if (this.settingsList.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (this.view == View.MODULES && this.search.visible()
                && this.search.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 256) { // Escape
            if (this.view != View.MODULES) {
                goBack();
            } else {
                beginClose();
            }
            return true;
        }
        // The menu key closes the menu again, which is what a toggle should do.
        if (keyCode == this.ui.settings().menuKey.get() && this.view == View.MODULES
                && !this.search.focused()) {
            beginClose();
            return true;
        }
        // Ctrl+F, or just typing, jumps to the search box.
        if (this.view == View.MODULES && keyCode == 70 && (modifiers & 0x2) != 0) {
            this.search.focus();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.modal.open()) {
            return true;
        }
        if (this.view == View.MODULES) {
            if (this.search.visible() && this.search.charTyped(chr, modifiers)) {
                return true;
            }
            // Typing a printable character anywhere in the grid starts a search.
            if (this.search.visible() && !this.search.focused() && chr > ' ') {
                this.search.focus();
                return this.search.charTyped(chr, modifiers);
            }
            return false;
        }
        if (this.view == View.PLUS_DESIGNER) {
            return this.designer.charTyped(chr, modifiers);
        }
        if (this.view == View.PLUS) {
            return false;
        }
        return this.settingsList.charTyped(chr, modifiers);
    }

    /** Plays the close animation, then drops the screen. */
    private void beginClose() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        this.settingsList.closeOverlays();
        this.designer.closeOverlays();
        this.openAmount.animateTo(0.0F);
        if (!this.ui.animationsEnabled()) {
            this.client.setScreen(null);
        }
    }

    @Override
    public void close() {
        beginClose();
    }
}
