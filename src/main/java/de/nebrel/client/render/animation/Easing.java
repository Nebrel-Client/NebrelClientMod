package de.nebrel.client.render.animation;

/**
 * Easing curves used by every animated element in the client.
 *
 * <p>All functions map [0, 1] onto [0, 1] (the back/overshoot curves briefly
 * leave that range on purpose).</p>
 */
public enum Easing {

    LINEAR("Linear") {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    EASE_OUT("Ease Out") {
        @Override
        public float apply(float t) {
            return 1.0F - (1.0F - t) * (1.0F - t);
        }
    },
    EASE_OUT_CUBIC("Ease Out Cubic") {
        @Override
        public float apply(float t) {
            float inv = 1.0F - t;
            return 1.0F - inv * inv * inv;
        }
    },
    EASE_IN_OUT("Ease In Out") {
        @Override
        public float apply(float t) {
            return t < 0.5F
                    ? 2.0F * t * t
                    : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0D) / 2.0F;
        }
    },
    SMOOTHSTEP("Smoothstep") {
        @Override
        public float apply(float t) {
            return t * t * (3.0F - 2.0F * t);
        }
    },
    SMOOTHERSTEP("Smootherstep") {
        @Override
        public float apply(float t) {
            return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
        }
    },
    EASE_OUT_BACK("Ease Out Back") {
        @Override
        public float apply(float t) {
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            float inv = t - 1.0F;
            return 1.0F + c3 * inv * inv * inv + c1 * inv * inv;
        }
    },
    EASE_OUT_EXPO("Ease Out Expo") {
        @Override
        public float apply(float t) {
            return t >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * t);
        }
    };

    private final String displayName;

    Easing(String displayName) {
        this.displayName = displayName;
    }

    public abstract float apply(float t);

    public String displayName() {
        return this.displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
