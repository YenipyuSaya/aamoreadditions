package com.aamoreadditions.effect;

/** Shared code defaults, not another config namespace. Farmer deliberately uses these
 * defaults rather than coupling itself to one particular machine's user overrides. */
public final class ProcessorUpgradeDefaults {
    private ProcessorUpgradeDefaults() {}
    public record Coefficients(double speed, double energy, double capacity) {}
    public static Coefficients speed(int tier) {
        return switch(tier) {
            case 1 -> new Coefficients(2,1.5,1);
            case 2 -> new Coefficients(3,2,1);
            case 3 -> new Coefficients(5,2.5,1);
            default -> throw new IllegalArgumentException("Unknown speed tier");
        };
    }
    public static Coefficients efficiency(int tier) {
        return switch(tier) {
            case 1 -> new Coefficients(1,0.8,1.5);
            case 2 -> new Coefficients(1,0.5,2);
            default -> throw new IllegalArgumentException("Unknown efficiency tier");
        };
    }
}
