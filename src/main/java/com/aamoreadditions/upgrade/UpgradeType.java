package com.aamoreadditions.upgrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

/**
 * Single source of truth for upgrade items: line + tier (effects hook in here later).
 * Tier N of a line requires all tiers 1..N-1 of the same line to be installed.
 */
public enum UpgradeType {
    SPEED_1("speed_upgrade_machine_1", Line.SPEED, 1),
    SPEED_2("speed_upgrade_machine_2", Line.SPEED, 2),
    SPEED_3("speed_upgrade_machine_3", Line.SPEED, 3),
    EFFICIENCY_1("efficiency_upgrade_machine_1", Line.EFFICIENCY, 1),
    EFFICIENCY_2("efficiency_upgrade_machine_2", Line.EFFICIENCY, 2);

    public enum Line { SPEED, EFFICIENCY }

    public final String id;
    public final Line line;
    public final int tier;
    private Supplier<? extends ItemLike> item;

    UpgradeType(String id, Line line, int tier) {
        this.id = id;
        this.line = line;
        this.tier = tier;
    }

    /** Called once from item registration. */
    public void bind(Supplier<? extends ItemLike> item) {
        this.item = item;
    }

    public Item item() {
        return item.get().asItem();
    }

    public static UpgradeType of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (UpgradeType t : values()) if (t.item != null && stack.is(t.item())) return t;
        return null;
    }
}
