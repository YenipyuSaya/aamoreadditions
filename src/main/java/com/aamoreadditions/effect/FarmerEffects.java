package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.UpgradeLogic;
import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.UpgradeStartup;
import com.aamoreadditions.upgrade.UpgradeType;
import de.ellpeck.actuallyadditions.api.internal.IFarmer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import java.util.function.Function;

/** One runtime adapter: behaviors, storage and info consume the same confirmed policy. */
public final class FarmerEffects {
    private FarmerEffects() {}
    public static FarmerUpgradeMath.Effects evaluate(List<UpgradeType> active) {
        return FarmerUpgradeMath.evaluate(active.stream().map(t->new UpgradeLogic.Plugin(t.line.name(),t.tier)).toList(),UpgradeStartup.dependency());
    }
    public static FarmerUpgradeMath.Effects evaluate(TileEntityFarmer tile) {
        return evaluate(UpgradeSlots.get(tile).activeUpgrades());
    }
    public static boolean canWork(IFarmer farmer) {
        return !(farmer instanceof TileEntityFarmer tile) || !com.aamoreadditions.upgrade.UpgradeRuntime.blocked(tile);
    }
    public static int cost(IFarmer farmer,int original) {
        return farmer instanceof TileEntityFarmer tile ? FarmerUpgradeMath.operationCost(original,evaluate(tile).energy()) : original;
    }
    public static int fortune(TileEntityFarmer tile) {
        return UpgradeSlots.get(tile).legal() ? evaluate(tile).fortune() : 0;
    }
    public static List<ItemStack> harvest(BlockState state,LootParams.Builder context,IFarmer farmer,
                                          Function<LootParams.Builder,List<ItemStack>> original) {
        if (!(farmer instanceof TileEntityFarmer tile) || !(tile.getLevel() instanceof ServerLevel level))
            return original.apply(context);
        return FarmerHarvestLoot.roll(state,context,level,fortune(tile),
                FarmerHarvestLoot.originalVanillaUnaffected(state),original);
    }
}
