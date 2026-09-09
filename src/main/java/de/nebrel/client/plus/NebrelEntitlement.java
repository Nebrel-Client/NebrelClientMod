package de.nebrel.client.plus;

/**
 * A capability a player may hold.
 *
 * <p>Entitlements exist so that nothing in the client ever asks "is this player
 * premium". A boolean like that spreads: every feature grows its own check, the
 * checks drift apart, and adding a second tier later means touching all of
 * them. Asking for a specific capability instead keeps each feature honest
 * about what it actually requires, and lets the answer come from somewhere else
 * entirely once a backend exists.</p>
 *
 * <p>Some of these are deliberately not implemented yet. They are listed so the
 * shape is fixed now rather than retrofitted, and
 * {@link #implemented()} says which is which so the UI can label the rest
 * honestly instead of showing a switch that does nothing.</p>
 */
public enum NebrelEntitlement {

    /** Membership itself. Everything else is normally granted alongside it. */
    NEBREL_PLUS("Nebrel+", true),

    /** The N badge in front of the player's name. */
    NEBREL_PLUS_BADGE("Nebrel+ Badge", true),

    /** Access to the nametag designer and its effects. */
    NAMETAG_DESIGNER("Nametag Designer", true),

    /** The second nametag line. */
    ADDITIONAL_NAMETAG("Additional Nametag", true),

    // -- reserved -----------------------------------------------------------
    // Shape fixed now, behaviour deliberately absent. Nothing reads these yet
    // beyond the UI, which labels them as not available.

    FOUNDER_BADGE("Founder Badge", false),
    STAFF_BADGE("Staff Badge", false),
    PARTNER_BADGE("Partner Badge", false),
    CREATOR_BADGE("Creator Badge", false),

    /** A discount the shop would apply. The shop does not exist. */
    SHOP_DISCOUNT("Shop Discount", false),

    /** A raised friend cap. The number belongs to the backend, not here. */
    FRIENDS_LIMIT_BONUS("Extended Friends List", false),

    /** A raised cap for the social feature. */
    SOCIAL_LIMIT_BONUS("Extended Social Limits", false),

    /** A raised cap for hosted worlds. */
    HOSTED_WORLDS_LIMIT_BONUS("Extended Hosted Worlds", false),

    /** The monthly cosmetic and emote drops. */
    MONTHLY_REWARDS("Monthly Rewards", false);

    private final String displayName;
    private final boolean implemented;

    NebrelEntitlement(String displayName, boolean implemented) {
        this.displayName = displayName;
        this.implemented = implemented;
    }

    public String displayName() {
        return this.displayName;
    }

    /**
     * Whether holding this entitlement currently changes anything.
     *
     * <p>False means the entitlement is a placeholder for a feature that does
     * not exist yet. The UI shows those as "Coming soon" rather than as a
     * benefit the member already has.</p>
     */
    public boolean implemented() {
        return this.implemented;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
