package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.render.animation.Animation;
import de.nebrel.client.render.animation.Easing;
import de.nebrel.client.setting.EnumSetting;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * A closed select box that opens into a list of options.
 *
 * <p>The list is drawn from {@link #renderOverlay}, which the parent calls after
 * every other component and outside any row clipping, so the popover is never
 * cut off by the panel it sits in. It flips above the button when there is not
 * enough room below.</p>
 *
 * @param <E> the enum being selected
 */
public final class DropdownComponent<E extends Enum<E>> extends Component {

    private static final float ROW_HEIGHT = 15.0F;
    private static final float MAX_LIST_HEIGHT = 120.0F;

    private final EnumSetting<E> setting;
    private final Animation openAmount;

    private boolean open;
    private float scrollOffset;

    public DropdownComponent(UiContext ui, EnumSetting<E> setting) {
        super(ui);
        this.setting = setting;
        this.openAmount = new Animation(0.0F, ui.duration(150L), Easing.EASE_OUT_CUBIC);
        this.height = 18.0F;
    }

    @Override
    public float preferredHeight() {
        return 18.0F;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        boolean hovered = isHovered(mouseX, mouseY) && enabled();

        int background = this.open ? theme.surfaceHover
                : (hovered ? theme.surfaceHover : theme.surfaceElevated);
        RenderUtil.roundedRect(context, this.x, this.y, this.width, this.height, 5.0F, background);
        RenderUtil.roundedOutline(context, this.x, this.y, this.width, this.height, 5.0F,
                this.open ? theme.accent : theme.border);

        String label = this.setting.displayValue();
        int textColor = enabled() ? theme.textPrimary : theme.textDisabled;
        RenderUtil.textFlat(context,
                RenderUtil.truncate(label, (int) (this.width - 22.0F)),
                this.x + 7.0F, this.y + (this.height - RenderUtil.lineHeight()) / 2.0F + 1.0F,
                textColor);

        // Chevron, rotated by drawing two short bars.
        drawChevron(context, this.x + this.width - 11.0F, this.y + this.height / 2.0F,
                this.openAmount.value(), theme.textSecondary);
    }

    private static void drawChevron(DrawContext context, float centerX, float centerY,
                                    float flip, int color) {
        // flip 0 points down, 1 points up.
        float direction = 1.0F - flip * 2.0F;
        for (int i = 0; i < 3; i++) {
            float offset = i;
            RenderUtil.rect(context, centerX - 3.0F + i, centerY - 1.0F + offset * direction * 0.9F,
                    1.0F, 1.0F, color);
            RenderUtil.rect(context, centerX + 3.0F - i, centerY - 1.0F + offset * direction * 0.9F,
                    1.0F, 1.0F, color);
        }
    }

    @Override
    public void renderOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
        float amount = this.openAmount.value();
        if (amount <= 0.01F) {
            return;
        }
        Theme theme = theme();
        List<E> options = this.setting.options();

        float fullHeight = Math.min(MAX_LIST_HEIGHT, options.size() * ROW_HEIGHT + 4.0F);
        float listHeight = fullHeight * amount;
        boolean above = flipUp(fullHeight);
        float listY = above ? this.y - listHeight - 2.0F : this.y + this.height + 2.0F;

        RenderUtil.shadow(context, this.x, listY, this.width, listHeight, 6.0F,
                ColorUtil.fadeAlpha(theme.shadow, amount), 4);
        RenderUtil.roundedRect(context, this.x, listY, this.width, listHeight, 6.0F,
                ColorUtil.fadeAlpha(theme.surfaceElevated, amount));
        RenderUtil.roundedOutline(context, this.x, listY, this.width, listHeight, 6.0F,
                ColorUtil.fadeAlpha(theme.border, amount));

        RenderUtil.pushClip(context, this.x, listY + 2.0F, this.width, Math.max(0.0F, listHeight - 4.0F));
        for (int i = 0; i < options.size(); i++) {
            E option = options.get(i);
            float rowY = listY + 2.0F + i * ROW_HEIGHT - this.scrollOffset;
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listHeight) {
                continue;
            }
            boolean selected = option == this.setting.get();
            boolean hovered = RenderUtil.hovered(mouseX, mouseY, this.x + 2.0F, rowY,
                    this.width - 4.0F, ROW_HEIGHT);

            if (selected) {
                RenderUtil.roundedRect(context, this.x + 2.0F, rowY, this.width - 4.0F, ROW_HEIGHT, 4.0F,
                        ColorUtil.fadeAlpha(theme.accentSoft, amount));
            } else if (hovered) {
                RenderUtil.roundedRect(context, this.x + 2.0F, rowY, this.width - 4.0F, ROW_HEIGHT, 4.0F,
                        ColorUtil.fadeAlpha(theme.surfaceHover, amount));
            }

            int color = selected ? theme.accent : theme.textSecondary;
            RenderUtil.textFlat(context,
                    RenderUtil.truncate(EnumSetting.label(option), (int) (this.width - 16.0F)),
                    this.x + 7.0F, rowY + (ROW_HEIGHT - RenderUtil.lineHeight()) / 2.0F + 1.0F,
                    ColorUtil.fadeAlpha(color, amount));
        }
        RenderUtil.popClip(context);
    }

    /** True when the list should open upward for lack of room below. */
    private boolean flipUp(float listHeight) {
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        return this.y + this.height + listHeight + 6.0F > screenHeight && this.y - listHeight - 2.0F > 0.0F;
    }

    private float listHeight() {
        return Math.min(MAX_LIST_HEIGHT, this.setting.options().size() * ROW_HEIGHT + 4.0F);
    }

    private float listTop() {
        float height = listHeight();
        return flipUp(height) ? this.y - height - 2.0F : this.y + this.height + 2.0F;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !enabled()) {
            return false;
        }
        if (this.open) {
            float height = listHeight();
            float top = listTop();
            if (RenderUtil.hovered(mouseX, mouseY, this.x, top, this.width, height)) {
                int index = (int) ((mouseY - top - 2.0F + this.scrollOffset) / ROW_HEIGHT);
                List<E> options = this.setting.options();
                if (index >= 0 && index < options.size()) {
                    this.setting.set(options.get(index));
                }
                closeOverlay();
                return true;
            }
            // A click anywhere else dismisses the list; the click is consumed so
            // it does not also toggle whatever was underneath.
            closeOverlay();
            return isHovered(mouseX, mouseY);
        }
        if (isHovered(mouseX, mouseY)) {
            this.open = true;
            this.scrollOffset = 0.0F;
            this.openAmount.animateTo(1.0F);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.open) {
            return false;
        }
        float height = listHeight();
        if (!RenderUtil.hovered(mouseX, mouseY, this.x, listTop(), this.width, height)) {
            return false;
        }
        float content = this.setting.options().size() * ROW_HEIGHT + 4.0F;
        float max = Math.max(0.0F, content - height);
        this.scrollOffset = Math.max(0.0F, Math.min(max, this.scrollOffset - (float) amount * ROW_HEIGHT));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // GLFW_KEY_ESCAPE closes the list rather than the whole screen.
        if (this.open && keyCode == 256) {
            closeOverlay();
            return true;
        }
        return false;
    }

    @Override
    public boolean hasOpenOverlay() {
        return this.open;
    }

    @Override
    public void closeOverlay() {
        if (this.open) {
            this.open = false;
            this.openAmount.animateTo(0.0F);
        }
    }

    @Override
    public String tooltip() {
        return this.setting.description().isEmpty() ? null : this.setting.description();
    }
}
