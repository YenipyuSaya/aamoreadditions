package com.aamoreadditions;

import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.UpgradeType;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AAMoreAdditions.MODID)
public class AAMoreAdditions {
    public static final String MODID = "aamoreadditions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final net.neoforged.neoforge.registries.DeferredItem<Item> UPGRADE_WRENCH =
            ITEMS.register("upgrade_wrench", () -> new com.aamoreadditions.item.UpgradeWrenchItem(new Item.Properties().stacksTo(1)));

    // Upgrade items, one per UpgradeType (no effects yet).
    static {
        for (UpgradeType t : UpgradeType.values()) {
            t.bind(ITEMS.registerSimpleItem(t.id, new Item.Properties().stacksTo(16)));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aamoreadditions"))
            .icon(() -> com.aamoreadditions.block.ModBlocks.ALLOY_FURNACE_ITEM.get().getDefaultInstance())
            .displayItems((params, out) -> {
                out.accept(com.aamoreadditions.block.ModBlocks.ALLOY_FURNACE_ITEM.get());
                out.accept(com.aamoreadditions.block.ModBlocks.DECORATIVE_MACHINE_CASING_ITEM.get());
                for (UpgradeType t : UpgradeType.values()) out.accept(t.item());
                out.accept(UPGRADE_WRENCH.get());
            })
            .build());

    public AAMoreAdditions(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, com.aamoreadditions.effect.UpgradeConfig.SPEC);
        com.aamoreadditions.block.ModBlocks.register(modBus);
        com.aamoreadditions.sound.ModSounds.register(modBus);
        com.aamoreadditions.menu.ModMenus.register(modBus);
        com.aamoreadditions.recipe.ModRecipes.register(modBus);
        com.aamoreadditions.item.WrenchRecord.COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        UpgradeSlots.ATTACHMENTS.register(modBus);
        if (Boolean.getBoolean("aamoreadditions.verifyMixins")) {
            // Dev check: force-load mixin targets so a broken injection fails at startup.
            for (String c : new String[]{"inventory.CrusherContainer", "inventory.ContainerFurnaceDouble", "inventory.ContainerCoalGenerator", "inventory.ContainerOilGenerator", "blocks.base.BlockContainerBase", "tile.TileEntityCrusher", "tile.TileEntityPoweredFurnace", "tile.TileEntityCoalGenerator", "tile.TileEntityOilGenerator", "tile.TileEntityHeatCollector", "tile.TileEntityLeafGenerator"}) {
                try { Class.forName("de.ellpeck.actuallyadditions.mod." + c); } catch (ClassNotFoundException e) { throw new RuntimeException(e); }
            }
            LOGGER.info("aamoreadditions: upgrade-slot mixin targets loaded OK");
        }
        // Fail-safe check: the generator hooks rely on these AA fields (require = 0 injections skip silently otherwise).
        for (String f : new String[]{"TileEntityCoalGenerator#maxBurnTime", "TileEntityOilGenerator#maxBurnTime", "TileEntityOilGenerator#currentEnergyProduce"}) {
            String[] p = f.split("#");
            try {
                Class.forName("de.ellpeck.actuallyadditions.mod.tile." + p[0], false, AAMoreAdditions.class.getClassLoader()).getField(p[1]);
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("aamoreadditions: AA field {} not found; generator upgrade effects for it are disabled.", f);
            }
        }
    }
}
