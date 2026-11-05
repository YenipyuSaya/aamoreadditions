package com.aamoreadditions.effect;

import java.util.List;

/** THE single place deciding how the lines' multipliers combine. User decision: multiply. */
public final class EnergyCombine {
    private EnergyCombine() {}

    public static double combine(List<Double> mults) {
        double v = 1.0;
        for (double m : mults) v *= m;
        return v;
    }
}
