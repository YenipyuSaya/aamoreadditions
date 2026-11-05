package com.aamoreadditions.compat.jade;

import de.ellpeck.actuallyadditions.mod.blocks.base.BlockContainerBase;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Discovered by Jade via @WailaPlugin; never referenced from the main mod class. */
@WailaPlugin
public class AAMoreAdditionsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        // All 7 supported machines extend BlockContainerBase; providers filter with UpgradeSlots.supports().
        registration.registerBlockDataProvider(UpgradeJadeProvider.INSTANCE, BlockContainerBase.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(UpgradeJadeProvider.INSTANCE, BlockContainerBase.class);
    }
}
