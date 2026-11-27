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

    public static final DeferredBlock<PhantomBoosterBlock> ADVANCED_PHANTOM_BOOSTER =
            BLOCKS.register("advanced_phantom_booster", PhantomBoosterBlock::new);
    public static final DeferredItem<BlockItem> ADVANCED_PHANTOM_BOOSTER_ITEM =
            AAMoreAdditions.ITEMS.register("advanced_phantom_booster",
                    () -> new BlockItem(ADVANCED_PHANTOM_BOOSTER.get(), new Item.Properties()));

    public static final DeferredBlock<PhantomBoosterBlock> TRANSFINITE_PHANTOM_BOOSTER =
            BLOCKS.register("transfinite_phantom_booster", PhantomBoosterBlock::new);
    public static final DeferredItem<BlockItem> TRANSFINITE_PHANTOM_BOOSTER_ITEM =
            AAMoreAdditions.ITEMS.register("transfinite_phantom_booster",
                    () -> new BlockItem(TRANSFINITE_PHANTOM_BOOSTER.get(), new Item.Properties()));

    public static final DeferredBlock<PhantomBoosterBlock> DIMENSIONAL_PHANTOM_BOOSTER =
            BLOCKS.register("dimensional_phantom_booster", DimensionalPhantomBoosterBlock::new);
    public static final DeferredItem<BlockItem> DIMENSIONAL_PHANTOM_BOOSTER_ITEM =
            AAMoreAdditions.ITEMS.register("dimensional_phantom_booster",
                    () -> new BlockItem(DIMENSIONAL_PHANTOM_BOOSTER.get(), new Item.Properties()));


    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE_BE = BLOCK_ENTITIES.register("alloyfurnace",
            () -> BlockEntityType.Builder.of(AlloyFurnaceBlockEntity::new, ALLOY_FURNACE.get()).build(null));

    /** Data-less block entity of the Dimensional Phantom Booster, only used to attach the client effect renderer. */
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalPhantomBoosterBlockEntity>> DIMENSIONAL_PHANTOM_BOOSTER_BE =
            BLOCK_ENTITIES.register("dimensional_phantom_booster",
                    () -> BlockEntityType.Builder.of(DimensionalPhantomBoosterBlockEntity::new, DIMENSIONAL_PHANTOM_BOOSTER.get()).build(null));

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
