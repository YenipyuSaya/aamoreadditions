package com.aamoreadditions.digger;

import com.aamoreadditions.upgrade.UpgradeType;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * AA drill upgrades accepted by the Vertical Digger's upgrade slots (and only there).
 * Speed I-III are aliases of the AAMA speed line, Fortune I/II of the AAMA efficiency line
 * (same activation, prerequisites, energy/capacity data and Farmer fortune levels).
 * Silk Touch, Block Placing and Range (3x3 / 5x5) are standalone plugins without a line alias.
 */
public enum DiggerUpgrade {
    SPEED_1(() -> ActuallyItems.DRILL_UPGRADE_SPEED.get(), UpgradeType.SPEED_1),
    SPEED_2(() -> ActuallyItems.DRILL_UPGRADE_SPEED_II.get(), UpgradeType.SPEED_2),
    SPEED_3(() -> ActuallyItems.DRILL_UPGRADE_SPEED_III.get(), UpgradeType.SPEED_3),
    FORTUNE_1(() -> ActuallyItems.DRILL_UPGRADE_FORTUNE.get(), UpgradeType.EFFICIENCY_1),
    FORTUNE_2(() -> ActuallyItems.DRILL_UPGRADE_FORTUNE_II.get(), UpgradeType.EFFICIENCY_2),
    SILK_TOUCH(() -> ActuallyItems.DRILL_UPGRADE_SILK_TOUCH.get(), null),
    BLOCK_PLACING(() -> ActuallyItems.DRILL_UPGRADE_BLOCK_PLACING.get(), null),
    RANGE_3(() -> ActuallyItems.DRILL_UPGRADE_THREE_BY_THREE.get(), null),
    RANGE_5(() -> ActuallyItems.DRILL_UPGRADE_FIVE_BY_FIVE.get(), null);

    private final Supplier<Item> item;
    /** AAMA line/tier this drill upgrade behaves as; null = standalone plugin. */
    public final UpgradeType alias;

    DiggerUpgrade(Supplier<Item> item, UpgradeType alias) {
        this.item = item;
        this.alias = alias;
    }

    public Item item() { return item.get(); }

    public boolean fortune() { return this == FORTUNE_1 || this == FORTUNE_2; }

    /** Either AA drill area augment (3x3 or 5x5); at most one may be installed. */
    public boolean range() { return this == RANGE_3 || this == RANGE_5; }

    public static DiggerUpgrade of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (DiggerUpgrade u : values()) if (stack.is(u.item())) return u;
        return null;
    }

    /** Kinds of a slot list, null for empty or foreign stacks. */
    public static java.util.List<DiggerUpgrade> kinds(java.util.List<ItemStack> contents, int ignoreIndex) {
        var list = new java.util.ArrayList<DiggerUpgrade>(contents.size());
        for (int i = 0; i < contents.size(); i++) list.add(i == ignoreIndex ? null : of(contents.get(i)));
        return list;
    }
}
