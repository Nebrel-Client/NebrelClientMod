package de.nebrel.client.plus;

import de.nebrel.client.setting.SettingSection;

import java.util.List;

/**
 * The section names the nametag designer groups its settings under.
 *
 * <p>Setting sections are plain strings, so a feature can introduce its own
 * groups without touching the shared list. Nebrel+ has enough settings that the
 * generic "Appearance / Behavior" split stops helping: a user looking for the
 * badge gap wants a "Badge" heading, not to scan an Appearance section holding
 * badge, nametag and extra-line options at once.</p>
 *
 * <p>Each effect additionally gets a section named after itself, created in
 * {@code NametagEffect}, which appears only while that effect is switched on.
 * Those are not listed in {@link #DESIGNER_ORDER} — they sort after it, which
 * puts the tuning for whatever the user just enabled directly below the switch
 * list.</p>
 */
public final class PlusSections {

    public static final String BADGE = "Badge";
    public static final String NAMETAG = "Nametag";
    public static final String EFFECTS = "Effects";
    public static final String ADDITIONAL = "Additional Nametag";
    public static final String VISIBILITY = "Where It Shows";

    /** Display order for the designer's own sections. */
    public static final List<String> DESIGNER_ORDER = List.of(
            BADGE, NAMETAG, EFFECTS, ADDITIONAL, VISIBILITY, SettingSection.ADVANCED);

    private PlusSections() {
    }
}
