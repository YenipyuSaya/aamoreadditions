package com.aamoreadditions.upgrade;

import com.aamoreadditions.AAMoreAdditions;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Inactive (not rendered, not clickable, not a shift-click target) while the panel is closed. */
public class UpgradeSlot extends SlotItemHandler {
    /** Slot positions relative to the original GUI's top-left; above the existing 176x179 GUI. */
    public static final int Y = -32, SPACING = 18;
    /** Slots centred on the GUI (x=88): 5 slots -> 44 (unchanged), 2 slots -> 71. */
    public static int x0(int count) { return 88 - count * SPACING / 2 + 1; }
    private final AbstractContainerMenu menu;
    private final UpgradeSlots.Handler handler;

    public UpgradeSlot(AbstractContainerMenu menu, UpgradeSlots.Handler handler, int index) {
        super(handler, index, x0(handler.getSlots()) + index * SPACING,
                Y); // Same slot and click-area height for every machine, including Cross.
        this.menu = menu;
        this.handler = handler;
    }

    /** Empty-slot outline, block atlas sprite (items/ textures are stitched into the block atlas by vanilla). */
    public static final ResourceLocation EMPTY_ICON = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "item/empty_upgrade_slot");

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ICON);
    }

    /** Rejects duplicates and items whose lower tiers are missing (also applies to shift-click). */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return super.mayPlace(stack) && UpgradeRules.canInstall(handler, stack, getSlotIndex()) == UpgradeRules.Check.OK;
    }

    /** A prerequisite can't be taken out while a higher tier that needs it is installed. */
    @Override
    public boolean mayPickup(Player player) {
        return super.mayPickup(player) && (!handler.legal() || UpgradeRules.canRemove(handler.contents(), getSlotIndex()));
    }

    public UpgradeSlots.Handler handler() {
        return handler;
    }

    @Override
    public boolean isActive() {
        return ((UpgradeMenuState) menu).aamoreadditions$isUpgradeOpen();
    }
}
