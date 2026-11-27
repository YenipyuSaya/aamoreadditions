package com.aamoreadditions.digger;

import java.util.List;

/**
 * Pure, mode-independent exclusivity rules of the digger plugins (also checked in independent mode):
 * Silk Touch and Fortune exclude each other; at most one Silk Touch, one Block Placing plugin,
 * and one Range plugin (either the 3x3 or the 5x5, never both, never two).
 */
public final class DiggerRules {
    private DiggerRules() {}

    public enum Result { OK, CONFLICT, DUPLICATE }

    /** Can {@code adding} join {@code installed} (nulls ignored)? */
    public static Result canAdd(List<DiggerUpgrade> installed, DiggerUpgrade adding) {
        if (adding == null) return Result.OK;
        boolean silk = false, fortune = false, placer = false, range = false;
        for (DiggerUpgrade u : installed) {
            if (u == null) continue;
            silk |= u == DiggerUpgrade.SILK_TOUCH;
            fortune |= u.fortune();
            placer |= u == DiggerUpgrade.BLOCK_PLACING;
            range |= u.range();
        }
        if (adding == DiggerUpgrade.SILK_TOUCH && silk) return Result.DUPLICATE;
        if (adding == DiggerUpgrade.BLOCK_PLACING && placer) return Result.DUPLICATE;
        if (adding.range() && range) return Result.DUPLICATE;
        if ((adding == DiggerUpgrade.SILK_TOUCH && fortune) || (adding.fortune() && silk)) return Result.CONFLICT;
        return Result.OK;
    }

    /** A complete slot list is legal only without a Silk/Fortune pair and with at most one silk, one placer and one range plugin. */
    public static boolean valid(List<DiggerUpgrade> installed) {
        boolean fortune = false;
        int silks = 0, placers = 0, ranges = 0;
        for (DiggerUpgrade u : installed) {
            if (u == null) continue;
            fortune |= u.fortune();
            if (u == DiggerUpgrade.SILK_TOUCH) silks++;
            if (u == DiggerUpgrade.BLOCK_PLACING) placers++;
            if (u.range()) ranges++;
        }
        return !(silks > 0 && fortune) && silks <= 1 && placers <= 1 && ranges <= 1;
    }
}
