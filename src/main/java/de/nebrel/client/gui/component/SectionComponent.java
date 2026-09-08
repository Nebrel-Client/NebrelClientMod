package de.nebrel.client.gui.component;

import de.nebrel.client.gui.UiContext;
import de.nebrel.client.gui.theme.Theme;
import de.nebrel.client.render.RenderUtil;
import de.nebrel.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/**
 * A section heading inside a settings list.
 *
 * <p>Small uppercase label with a rule running to the right edge, so groups
 * read as groups without needing a box around each one.</p>
 */
public final class SectionComponent extends Component {

    private static final float HEIGHT = 20.0F;

    private final String label;

    public SectionComponent(UiContext ui, String label) {
        super(ui);
        this.label = label == null ? "" : label.toUpperCase(Locale.ROOT);
        this.height = HEIGHT;
    }

    @Override
    public float preferredHeight() {
        return HEIGHT;
    }

    public String label() {
        return this.label;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible()) {
            return;
        }
        Theme theme = theme();
        float baseline = this.y + HEIGHT - RenderUtil.lineHeight() - 2.0F;

        float labelWidth = RenderUtil.textWidthScaled(this.label, 0.85F);
        RenderUtil.textScaled(context, this.label, this.x, baseline + 1.0F, 0.85F,
                ColorUtil.withAlpha(theme.textSecondary, 210), false);

        float ruleX = this.x + labelWidth + 8.0F;
        float ruleWidth = this.x + this.width - ruleX;
        if (ruleWidth > 4.0F) {
            RenderUtil.rect(context, ruleX, baseline + RenderUtil.lineHeight() / 2.0F - 1.0F,
                    ruleWidth, 1.0F, ColorUtil.withAlpha(theme.divider, 190));
        }
    }
}
