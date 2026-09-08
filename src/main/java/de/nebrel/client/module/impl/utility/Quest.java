package de.nebrel.client.module.impl.utility;

import java.util.ArrayList;
import java.util.List;

/**
 * A local quest: a name, a reward and the objectives that complete it.
 *
 * <p>Entirely client side. Progress is counted from things the client already
 * observes about its own player, and nothing is reported anywhere.</p>
 */
public final class Quest {

    /**
     * What a single objective measures.
     *
     * <p>Every metric here is something the client can observe about its own
     * player without asking the server. The block-break count deliberately is
     * not one of them: the client's statistic handler is only populated on a
     * server once the stats screen has been opened, so a quest built on it
     * would sit at zero for most players.</p>
     */
    public enum Metric {
        JUMPS("Jumps"),
        DISTANCE_WALKED("Blocks walked"),
        PLAY_TIME("Minutes played");

        private final String display;

        Metric(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    /** One measurable step of a quest. */
    public static final class Objective {
        private final Metric metric;
        private final int target;
        private int progress;

        public Objective(Metric metric, int target) {
            this.metric = metric;
            this.target = Math.max(1, target);
        }

        public Metric metric() {
            return this.metric;
        }

        public int target() {
            return this.target;
        }

        public int progress() {
            return Math.min(this.progress, this.target);
        }

        public boolean complete() {
            return this.progress >= this.target;
        }

        public float fraction() {
            return Math.min(1.0F, this.progress / (float) this.target);
        }

        /** Adds to the running count. Never decreases. */
        public void advance(int amount) {
            if (amount > 0) {
                this.progress += amount;
            }
        }

        /** Sets an absolute value, for metrics that are already cumulative. */
        public void setProgress(int value) {
            this.progress = Math.max(this.progress, value);
        }

        void reset() {
            this.progress = 0;
        }

        public String describe() {
            return this.metric.toString() + ": " + progress() + " / " + this.target;
        }
    }

    private final String id;
    private final String name;
    private final String description;
    private final String reward;
    private final List<Objective> objectives = new ArrayList<>();

    private boolean claimed;

    public Quest(String id, String name, String description, String reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.reward = reward;
    }

    public Quest objective(Metric metric, int target) {
        this.objectives.add(new Objective(metric, target));
        return this;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String description() {
        return this.description;
    }

    public String reward() {
        return this.reward;
    }

    public List<Objective> objectives() {
        return List.copyOf(this.objectives);
    }

    /** True once every objective is met. */
    public boolean complete() {
        for (Objective objective : this.objectives) {
            if (!objective.complete()) {
                return false;
            }
        }
        return !this.objectives.isEmpty();
    }

    /** Overall progress across the objectives, 0.0-1.0. */
    public float fraction() {
        if (this.objectives.isEmpty()) {
            return 0.0F;
        }
        float total = 0.0F;
        for (Objective objective : this.objectives) {
            total += objective.fraction();
        }
        return total / this.objectives.size();
    }

    public boolean claimed() {
        return this.claimed;
    }

    public void setClaimed(boolean value) {
        this.claimed = value;
    }

    public void reset() {
        for (Objective objective : this.objectives) {
            objective.reset();
        }
        this.claimed = false;
    }

    /** Feeds an increment to every objective watching this metric. */
    public void advance(Metric metric, int amount) {
        for (Objective objective : this.objectives) {
            if (objective.metric() == metric) {
                objective.advance(amount);
            }
        }
    }

    /** Feeds an absolute value to every objective watching this metric. */
    public void setProgress(Metric metric, int value) {
        for (Objective objective : this.objectives) {
            if (objective.metric() == metric) {
                objective.setProgress(value);
            }
        }
    }
}
