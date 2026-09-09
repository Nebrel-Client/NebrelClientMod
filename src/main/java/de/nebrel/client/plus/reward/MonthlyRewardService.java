package de.nebrel.client.plus.reward;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Holds the monthly rewards a member has been granted.
 *
 * <p>Empty by design. Rewards are cosmetics and emotes, and both need a backend
 * to grant them and content that does not exist yet; a placeholder list here
 * would show members rewards they can never claim.</p>
 *
 * <p>So this reports honestly that there is nothing yet, and the Nebrel+ page
 * says "coming soon" rather than showing an empty grid that looks broken. When
 * a backend starts supplying rewards, {@link #setRewards} is the only entry
 * point it needs.</p>
 */
public final class MonthlyRewardService {

    private final List<MonthlyReward> rewards = new ArrayList<>();

    /** Replaces the known rewards. Called by a backend provider later. */
    public void setRewards(List<MonthlyReward> newRewards) {
        this.rewards.clear();
        if (newRewards != null) {
            this.rewards.addAll(newRewards);
        }
    }

    public List<MonthlyReward> all() {
        return List.copyOf(this.rewards);
    }

    /** The current month as {@code YYYY-MM}. */
    public static String currentPeriod() {
        return YearMonth.now().toString();
    }

    /** This month's reward of a given type, if one has been granted. */
    public Optional<MonthlyReward> current(MonthlyReward.Type type) {
        String period = currentPeriod();
        for (MonthlyReward reward : this.rewards) {
            if (reward.type() == type && period.equals(reward.period())) {
                return Optional.of(reward);
            }
        }
        return Optional.empty();
    }

    public List<MonthlyReward> unclaimed() {
        List<MonthlyReward> result = new ArrayList<>();
        for (MonthlyReward reward : this.rewards) {
            if (!reward.claimed()) {
                result.add(reward);
            }
        }
        return result;
    }

    /**
     * Whether any reward has been granted at all.
     *
     * <p>False today, which is what the UI keys its "coming soon" state off
     * rather than hard-coding the same assumption in the screen.</p>
     */
    public boolean hasAnyRewards() {
        return !this.rewards.isEmpty();
    }
}
