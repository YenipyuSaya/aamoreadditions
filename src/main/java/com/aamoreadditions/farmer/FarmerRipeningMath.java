package com.aamoreadditions.farmer;

import com.aamoreadditions.effect.FarmerUpgradeMath;
import com.aamoreadditions.upgrade.UpgradeLogic;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.*;

/** Independent ring policy; shared activation and unpenalized Farmer speed remain authoritative. */
public final class FarmerRipeningMath {
    public static final int COST=300;
    public record Plan(int period,int draws) {}
    private FarmerRipeningMath() {}
    public static Plan evaluate(List<UpgradeLogic.Plugin> installed,boolean dependency) {
        double speed=FarmerUpgradeMath.evaluate(installed,dependency).speed();
        var active=UpgradeLogic.active(installed,dependency,UpgradeLogic.Plugin::line,UpgradeLogic.Plugin::tier);
        BigDecimal attempts=BigDecimal.valueOf(45);
        for(var p:active) if(p.line().equals("SPEED")) attempts=attempts.multiply(new BigDecimal(switch(p.tier()) {
            case 1 -> "0.9"; case 2 -> "0.8"; case 3 -> "0.75"; default -> throw new IllegalArgumentException("Speed tier");
        }));
        return new Plan(Math.max(10,(int)Math.ceil(30/speed)),Math.max(25,attempts.setScale(0,RoundingMode.HALF_UP).intValueExact()));
    }
    /** Fixed draw budget, with replacement. A stale/now-mature draw is skipped, not rerolled.
     * Callback performs exactly one real randomTick, returning true even if no growth occurred. */
    public static int run(int draws,int targets,IntSupplier energy,IntUnaryOperator select,
                          IntPredicate tick,IntConsumer spend) {
        int performed=0;
        for(int n=0;n<draws && targets>0 && energy.getAsInt()>=COST;n++) {
            if(tick.test(select.applyAsInt(targets))){spend.accept(COST);performed++;}
        }
        return performed;
    }
    public static boolean arrowActive(boolean ring,int machineEnergy){return ring && machineEnergy>0;}
}
