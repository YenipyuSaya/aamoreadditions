package com.aamoreadditions.compat.jade;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.ModBlocks;
import com.aamoreadditions.upgrade.UpgradeDisplay;
import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.UpgradeStartup;
import com.aamoreadditions.upgrade.UpgradeType;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;

/** Server-authoritative icon/count groups; no client-side guesses about mode or activation. */
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
        var be = accessor.getBlockEntity();
        return (be != null && UpgradeSlots.supports(be))
                || accessor.getBlock() == ModBlocks.DECORATIVE_MACHINE_CASING.get()
                || accessor.getBlock() == ActuallyBlocks.EMPOWERER.get()
                || accessor.getBlock() == ActuallyBlocks.DISPLAY_STAND.get();
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!shouldRequestData(accessor)) return;
        var handler = UpgradeSlots.target(accessor.getLevel(), accessor.getPosition());
        if (handler == null) return;
        boolean dependency = UpgradeStartup.dependency();
        boolean legal = handler.legal();
        var active = handler.activeUpgrades();
        var installed = handler.contents().stream().filter(s -> !s.isEmpty()).toList();
        var visible = UpgradeDisplay.visible(installed, dependency, legal, stack -> {
            var type = UpgradeType.of(stack);
            return type != null && active.contains(type);
        });
        var groups = UpgradeDisplay.group(visible, ItemStack::getItem);
        var state = new CompoundTag();
        state.putBoolean("Dependency", dependency);
        state.putBoolean("Legal", legal);
        var list = new ListTag();
        for (var group : groups) {
            var entry = new CompoundTag();
            entry.put("Item", group.representative().copyWithCount(1).save(accessor.getLevel().registryAccess()));
            entry.putInt("Count", group.count());
            list.add(entry);
        }
        state.put("Groups", list);
        // Always send the state, including zero groups, so removal cannot leave a stale list.
        data.put(KEY, state);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) return;
        var state = data.getCompound(KEY);
        // Illegal machines hide the entire upgrade section, as configured by the owner.
        // In either case it is never correct to present installed items as currently active.
        if (!state.getBoolean("Legal")) return;
        var groups = new ArrayList<UpgradeDisplay.Group<ItemStack>>();
        var list = state.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            var entry = list.getCompound(i);
            var stack = ItemStack.parseOptional(accessor.getLevel().registryAccess(), entry.getCompound("Item"));
            int count = entry.getInt("Count");
            if (!stack.isEmpty() && count > 0 && count <= 10)
                groups.add(new UpgradeDisplay.Group<>(stack, count));
        }
        if (groups.isEmpty()) return;
        var helper = IElementHelper.get();
        tooltip.add(Component.translatable(state.getBoolean("Dependency")
                ? "tooltip.aamoreadditions.active_upgrades" : "tooltip.aamoreadditions.upgrades"));
        for (var row : UpgradeDisplay.rows(groups)) {
            boolean first = true;
            for (var group : row) {
                // Jade smallItem renders the real ItemStack model, without a stack-count overlay.
                var icon = helper.smallItem(group.representative());
                if (first) {
                    tooltip.add(icon);
                    first = false;
                } else {
                    tooltip.append(helper.spacer(6, 0));
                    tooltip.append(icon);
                }
                tooltip.append(helper.text(Component.literal("×" + group.count())));
            }
        }
    }
}
