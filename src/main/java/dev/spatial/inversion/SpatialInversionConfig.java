package dev.spatial.inversion;

/**
 * Gameplay balance constants and configuration limits.
 */
public final class SpatialInversionConfig {
    private SpatialInversionConfig() {}

    // Spatial Link
    public static final double LINK_MAX_PLACE_RANGE = 24.0;
    public static final double LINK_MAX_BETWEEN_ANCHORS = 20.0;
    public static final int LINK_DURATION_TICKS = 160; // 8 seconds
    public static final int LINK_COOLDOWN_TICKS = 200; // 10 seconds

    // Vector Redirect
    public static final int REDIRECT_DURATION_TICKS = 60; // 3 seconds
    public static final int REDIRECT_COOLDOWN_TICKS = 320; // 16 seconds

    // Spatial Exchange
    public static final double EXCHANGE_RANGE = 18.0;
    public static final int EXCHANGE_COOLDOWN_TICKS = 440; // 22 seconds

    // Distortion Field
    public static final double DISTORTION_RADIUS = 6.0;
    public static final int DISTORTION_DURATION_TICKS = 100; // 5 seconds
    public static final int DISTORTION_COOLDOWN_TICKS = 560; // 28 seconds

    // Inversion Strike
    public static final double STRIKE_RANGE = 24.0;
    public static final float STRIKE_BASE_DAMAGE = 18.0f;
    public static final float STRIKE_PROFICIENCY_BONUS = 10.0f;
    public static final int STRIKE_COOLDOWN_TICKS = 300; // 15 seconds

    // Fold Step
    public static final double FOLD_MAX_DISTANCE = 16.0;
    public static final int FOLD_COOLDOWN_TICKS = 240; // 12 seconds

    // Spatial Cage
    public static final double CAGE_RADIUS = 8.0;
    public static final int CAGE_DURATION_TICKS = 120; // 6 seconds
    public static final int CAGE_COOLDOWN_TICKS = 760; // 38 seconds

    // Absolute Inversion (Ultimate)
    public static final double ABSOLUTE_MAX_RANGE = 30.0;
    public static final int ABSOLUTE_DURATION_TICKS = 100; // 5 seconds
    public static final int ABSOLUTE_CAPACITY = 12; // Redirects up to 12 incoming attacks/projectiles
    public static final int ABSOLUTE_COOLDOWN_TICKS = 2400; // 120 seconds

    // General Safety Limits
    public static final int MAX_PROJECTILE_HOPS = 6;
}
