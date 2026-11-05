package com.aamoreadditions.compat.jade;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.upgrade.UpgradeSlots;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

/**
 * Syncs the 5 upgrade slots (the attachment is not synced to clients on its own) and shows
 * "升级：[icon] name, [icon] name". Nothing is shown when no upgrade is installed.
 * Config toggle uid aamoreadditions:upgrades (lang: config.jade.plugin_aamoreadditions.upgrades).
 */
public enum UpgradeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "upgrades");
    private static final String KEY = "aamoreadditions_upgrades";

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        return be != null && UpgradeSlots.supports(be);
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be == null || !UpgradeSlots.supports(be) || !be.hasData(UpgradeSlots.UPGRADE_INV)) return;
        UpgradeSlots.Handler h = UpgradeSlots.get(be);
        ListTag list = new ListTag();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) list.add(s.save(accessor.getLevel().registryAccess()));
        }
        if (!list.isEmpty()) data.put(KEY, list);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity be = accessor.getBlockEntity();
        if (be == null || !UpgradeSlots.supports(be)) return;
        CompoundTag data = accessor.getServerData();
        if (!data.contains(KEY, Tag.TAG_LIST)) return;
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        IElementHelper helper = IElementHelper.get();
        boolean first = true;
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.parseOptional(accessor.getLevel().registryAccess(), list.getCompound(i));
            if (s.isEmpty()) continue;
            if (first) {
                tooltip.add(helper.text(Component.translatable("tooltip.aamoreadditions.upgrades")));
                first = false;
            } else {
                tooltip.append(helper.text(Component.literal(", ")));
            }
            tooltip.append(helper.smallItem(s));
            tooltip.append(helper.text(s.getHoverName()));
        }
    }
}
