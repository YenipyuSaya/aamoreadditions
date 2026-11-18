package com.aamoreadditions.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

/** Read-only view of loot table data for the Farmer memory-mode seed lookup. */
@Mixin(LootTable.class)
public interface LootTableAccessor {
    @Accessor("pools") List<LootPool> aamoreadditions$pools();
}
