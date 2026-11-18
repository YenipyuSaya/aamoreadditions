package com.aamoreadditions.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** loot_table entries: a reference to another table, or an inline table. */
@Mixin(NestedLootTable.class)
public interface NestedLootTableAccessor {
    @Accessor("contents") Either<ResourceKey<LootTable>, LootTable> aamoreadditions$contents();
}
