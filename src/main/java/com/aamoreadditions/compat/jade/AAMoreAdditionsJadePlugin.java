package com.aamoreadditions.compat.jade;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Discovered by Jade via @WailaPlugin; never referenced from the main mod class. */
@WailaPlugin
public class AAMoreAdditionsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        // Cross casings have no block entity. The provider filters native machines / Cross parts.
        registration.registerBlockDataProvider(UpgradeJadeProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(UpgradeJadeProvider.INSTANCE, Block.class);
    }
}
