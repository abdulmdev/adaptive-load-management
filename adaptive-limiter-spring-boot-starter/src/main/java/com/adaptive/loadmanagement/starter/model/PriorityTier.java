package com.adaptive.loadmanagement.starter.model;

/**
 * Traffic Priority Tiers modeled after Uber's load management framework.
 * Lower ordinal / lower numerical priority value represents higher criticality.
 */
public enum PriorityTier {
    P0(0, "CRITICAL", 1.00),     // Payments, Checkout, Location Dispatch (Never shed unless catastrophe)
    P1(1, "ESSENTIAL", 0.90),    // Order status tracking, Driver profile (Shed at Load > 0.90)
    P2(2, "NORMAL", 0.80),       // Search, Reviews, History (Shed at Load > 0.80)
    P3(3, "BACKGROUND", 0.65);   // Marketing banners, Analytics, Recommendations (Shed at Load > 0.65)

    private final int level;
    private final String description;
    private final double maxAllowedLoadFactor;

    PriorityTier(int level, String description, double maxAllowedLoadFactor) {
        this.level = level;
        this.description = description;
        this.maxAllowedLoadFactor = maxAllowedLoadFactor;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public double getMaxAllowedLoadFactor() {
        return maxAllowedLoadFactor;
    }

    public static PriorityTier fromHeaderOrDefault(String headerVal) {
        if (headerVal == null || headerVal.trim().isEmpty()) {
            return P2; // Default to Normal priority if header omitted
        }
        try {
            return PriorityTier.valueOf(headerVal.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return P2;
        }
    }
}
