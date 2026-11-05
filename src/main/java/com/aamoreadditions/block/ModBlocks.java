package com.aamoreadditions.block;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AAMoreAdditions.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AAMoreAdditions.MODID);

    public static final DeferredBlock<AlloyFurnaceBlock> ALLOY_FURNACE = BLOCKS.register("alloyfurnace", AlloyFurnaceBlock::new);
    public static final DeferredItem<BlockItem> ALLOY_FURNACE_ITEM = AAMoreAdditions.ITEMS.register("alloyfurnace", () -> new BlockItem(ALLOY_FURNACE.get(), new Item.Properties()));

    // Same stats as Actually Additions' Lava Factory Casing (stone copy, 1.5 hardness, 10 resistance, metal sound), decorative only.
    public static final DeferredBlock<net.minecraft.world.level.block.Block> DECORATIVE_MACHINE_CASING = BLOCKS.register("decorative_machine_casing",
            () -> new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties
                    .ofFullCopy(net.minecraft.world.level.block.Blocks.STONE).strength(1.5f, 10f)
                    .sound(net.minecraft.world.level.block.SoundType.METAL)));
    public static final DeferredItem<BlockItem> DECORATIVE_MACHINE_CASING_ITEM = AAMoreAdditions.ITEMS.register("decorative_machine_casing",
            () -> new BlockItem(DECORATIVE_MACHINE_CASING.get(), new Item.Properties()));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE_BE = BLOCK_ENTITIES.register("alloyfurnace",
            () -> BlockEntityType.Builder.of(AlloyFurnaceBlockEntity::new, ALLOY_FURNACE.get()).build(null));

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(ModBlocks::registerCapabilities);
    }

    /**
     * AA registers capabilities only for its OWN block entity types; our Alloy Furnace has its own type, so it must be
     * registered here (same getters AA uses: energy = its storage incl. maxReceive; items = AA inv with our acceptor/remover rules).
     */
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, ALLOY_FURNACE_BE.get(), (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ALLOY_FURNACE_BE.get(), (be, side) -> be.getItemHandler(side));
    }
}
