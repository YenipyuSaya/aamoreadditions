package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerCoalGenerator;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerFurnaceDouble;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerOilGenerator;
import de.ellpeck.actuallyadditions.mod.inventory.CrusherContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {CrusherContainer.class, ContainerFurnaceDouble.class, ContainerCoalGenerator.class, ContainerOilGenerator.class, de.ellpeck.actuallyadditions.mod.inventory.ContainerEnergizer.class, de.ellpeck.actuallyadditions.mod.inventory.ContainerFarmer.class}, remap = false)
public class UpgradeQuickMoveMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$quickMoveUpgrades(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if((Object)this instanceof de.ellpeck.actuallyadditions.mod.inventory.ContainerFarmer farmer){
            ItemStack ring=com.aamoreadditions.farmer.FarmerRingMenus.quickMove(farmer,player,index);
            if(ring!=null){cir.setReturnValue(ring);return;}
        }
        ItemStack result = UpgradeMenus.quickMove((AbstractContainerMenu) (Object) this, player, index);
        if (result != null) cir.setReturnValue(result);
    }
}
