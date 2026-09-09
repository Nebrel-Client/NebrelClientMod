package de.nebrel.client.plus.reward;

/**
 * One monthly drop.
 *
 * <p>The model exists so the UI has something real to lay out and so the
 * backend contract is fixed. No reward content ships: inventing a cosmetic that
 * does not exist would put an item in the member's list that can never be
 * granted.</p>
 *
 * @param id          stable identifier
 * @param type        what kind of thing it is
 * @param displayName human readable name
 * @param period      the month it belongs to, as {@code YYYY-MM}
 * @param claimed     whether the member has taken it
 */
public record MonthlyReward(
        String id,
        Type type,
        String displayName,
        String period,
        boolean claimed) {

    /** What a reward grants. */
    public enum Type {
        COSMETIC("Cosmetic"),
        EMOTE("Emote");

        private final String display;

        Type(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    public MonthlyReward withClaimed(boolean value) {
        return new MonthlyReward(this.id, this.type, this.displayName, this.period, value);
    }
}
