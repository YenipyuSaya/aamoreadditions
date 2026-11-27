package com.aamoreadditions.booster;

import java.util.List;

/**
 * Consecutive boosters stacked upward from a machine, stopped by a non-booster or the configured cap.
 *
 * <p>Overload: a special tier (Transfinite or Dimensional) that is connected — adjacent in this
 * counted column — to another special or to any lower tier. A non-booster gap separates stacks,
 * so a special below a gap and a vanilla booster above it is not connected. A lone special
 * stands alone and is not an overload.
 */
public record BoosterScan(List<BoosterTier> layers, boolean overloaded) {

    public static BoosterScan of(List<BoosterTier> layers) {
        int specials = 0;
        for (BoosterTier t : layers) if (t.special()) specials++;
        boolean overloaded = specials > 0 && (specials > 1 || layers.size() > specials);
        return new BoosterScan(List.copyOf(layers), overloaded);
    }

    public enum Binding {
        /** Ordinary distance check against {@link #multiplier(int, int, boolean) multiplied} range. */
        RANGED,
        /** Same dimension, no distance check, both chunks must be loaded. */
        TRANSFINITE,
        /** Any dimension or coordinate, both chunks must be loaded. */
        DIMENSIONAL,
        /** Overload: binding is disabled; the stack explodes. */
        DISABLED
    }

    /**
     * @param specialsAsAdvanced when true (Vertical Digger), Transfinite and Dimensional have no
     *        special binding and each counts as an Advanced multiplier; overload is still evaluated.
     */
    public Binding binding(boolean specialsAsAdvanced) {
        if (overloaded) return Binding.DISABLED;
        if (specialsAsAdvanced) return Binding.RANGED;
        if (layers.size() == 1 && layers.getFirst() == BoosterTier.TRANSFINITE) return Binding.TRANSFINITE;
        if (layers.size() == 1 && layers.getFirst() == BoosterTier.DIMENSIONAL) return Binding.DIMENSIONAL;
        return Binding.RANGED;
    }

    /**
     * Product of per-layer multipliers, clamped to {@link Integer#MAX_VALUE}. Specials contribute
     * the Advanced multiplier when {@code specialsAsAdvanced} is true. A special cannot legally
     * appear in a ranged mixed stack (that is an overload).
     */
    public int multiplier(int aaMultiplier, int advancedMultiplier, boolean specialsAsAdvanced) {
        long m = 1L;
        for (BoosterTier t : layers) {
            int factor = switch (t) {
                case VANILLA -> aaMultiplier;
                case ADVANCED, TRANSFINITE, DIMENSIONAL -> advancedMultiplier;
            };
            m = Math.min(Integer.MAX_VALUE, m * (long) Math.max(1, factor));
        }
        return (int) m;
    }

    public int signature() {
        int h = overloaded ? 1 : 0;
        for (BoosterTier t : layers) h = 31 * h + t.ordinal();
        return h;
    }
}
