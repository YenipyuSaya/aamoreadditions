package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.UpgradeStartup;
import com.aamoreadditions.upgrade.UpgradeType;
import java.util.List;

/** Runtime and menu Info adapter; only the Cross's own three editable coefficients are read. */
public final class CrossEffects {
    private CrossEffects() {}
    public static double speed(List<UpgradeType> installed) {
        return CrossUpgradeMath.speed(installed.stream().filter(t -> t.line == UpgradeType.Line.SPEED)
                .map(t -> t.tier).toList(), UpgradeStartup.dependency(), UpgradeConfig::crossSpeed);
    }
}
