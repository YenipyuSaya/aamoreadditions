package com.aamoreadditions.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/** Shared adapter called at the built-in behaviors' original getDrops invocation.
 * The final list reaches AA's canAddToSeeds/canAddToOutput before any harvest occurs.
 * The classification is intentionally restricted to the confirmed original crop list. */
public final class FarmerHarvestLoot {
    private FarmerHarvestLoot() {}

    /** Known original vanilla examples only, NOT a detector for arbitrary datapacks or mods. */
    public static boolean originalVanillaUnaffected(BlockState state) {
        return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.PUMPKIN) || state.is(Blocks.CACTUS);
    }

    public static List<ItemStack> roll(BlockState state, LootParams.Builder context, ServerLevel level,
                                       int fortuneLevel, boolean fortuneUnaffected,
                                       java.util.function.Function<LootParams.Builder,List<ItemStack>> original) {
        if (fortuneLevel < 0 || fortuneLevel > 4) throw new IllegalArgumentException("Invalid Farmer Fortune level");
        if (fortuneLevel == 0) return original.apply(context); // preserve AA's empty-tool baseline
        var tool = new ItemStack(Items.DIAMOND_HOE);
        tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE), fortuneLevel);
        var drops = original.apply(context.withParameter(LootContextParams.TOOL, tool));
        // Vanilla melon already limits slices to nine in its loot table. Never double it.
        if (fortuneLevel < 4 || !fortuneUnaffected || state.is(Blocks.MELON)) return drops;
        var doubled = new ArrayList<ItemStack>();
        for (var drop : drops) {
            if (drop.isEmpty()) continue;
            for (int count : FarmerUpgradeMath.doubledStackCounts(drop.getCount(), drop.getMaxStackSize()))
                doubled.add(drop.copyWithCount(count));
        }
        return doubled;
    }
}
