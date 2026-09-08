package de.nebrel.client.gui.theme;

/**
 * The built-in palettes.
 *
 * <p>Nebrel Dark avoids pure black on purpose: the panel sits on a very dark
 * blue-grey and stacks three further surface levels above it, so cards and
 * popovers read as layers rather than as outlined boxes. Nebrel Light mirrors
 * the same structure inverted.</p>
 *
 * <p>The default accent is a violet used only for state (active nav row, an on
 * toggle, slider fill, focus ring). Large areas stay neutral.</p>
 */
public final class Themes {

    /** Default accent: Nebrel violet. */
    public static final int DEFAULT_ACCENT = 0xFF8B5CF6;

    public static final Theme DARK = Theme.builder("nebrel_dark", "Nebrel Dark", true)
            .background(0xFF0E0E13)
            .surface(0xFF16161D)
            .surfaceElevated(0xFF1E1E27)
            .surfaceHover(0xFF272733)
            .textPrimary(0xFFECECF2)
            .textSecondary(0xFF9A9AAB)
            .textDisabled(0xFF5A5A69)
            .accent(DEFAULT_ACCENT)
            .accentHover(0xFF9F79F8)
            .accentPressed(0xFF7546D6)
            .accentSoft(0x268B5CF6)
            .border(0xFF2A2A36)
            .divider(0xFF22222C)
            .success(0xFF3DD68C)
            .warning(0xFFF5B547)
            .error(0xFFF2555A)
            .scrim(0xA607070B)
            .shadow(0x66000000)
            .build();

    public static final Theme LIGHT = Theme.builder("nebrel_light", "Nebrel Light", false)
            .background(0xFFF4F4F8)
            .surface(0xFFFFFFFF)
            .surfaceElevated(0xFFFAFAFD)
            .surfaceHover(0xFFEDEDF3)
            .textPrimary(0xFF17171F)
            .textSecondary(0xFF61616F)
            .textDisabled(0xFFA2A2B0)
            .accent(DEFAULT_ACCENT)
            .accentHover(0xFF9F79F8)
            .accentPressed(0xFF6D3FCC)
            .accentSoft(0x1E8B5CF6)
            .border(0xFFDFDFE8)
            .divider(0xFFEBEBF1)
            .success(0xFF16A46B)
            .warning(0xFFC98207)
            .error(0xFFD93B41)
            .scrim(0x8C1A1A22)
            .shadow(0x2E101018)
            .build();

    private Themes() {
    }

    /** All built-in themes, in the order the theme picker shows them. */
    public static Theme[] all() {
        return new Theme[]{DARK, LIGHT};
    }

    /** Looks a theme up by id, falling back to {@link #DARK}. */
    public static Theme byId(String id) {
        for (Theme theme : all()) {
            if (theme.id().equals(id)) {
                return theme;
            }
        }
        return DARK;
    }
}
