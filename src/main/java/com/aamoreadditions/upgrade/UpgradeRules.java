package com.aamoreadditions.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Install/remove rules, shared by sneak-click, GUI slots, quick-move and client messages. */
public final class UpgradeRules {
    private UpgradeRules() {}

    public enum Check {
        OK, INVALID, DUPLICATE, MISSING_PREREQ;

        public Component message() {
            return switch (this) {
                case INVALID -> Component.translatable("message.aamoreadditions.upgrades.invalid_item").withStyle(ChatFormatting.RED);
                case DUPLICATE -> Component.translatable("message.aamoreadditions.upgrades.already_installed").withStyle(ChatFormatting.WHITE);
                case MISSING_PREREQ -> Component.translatable("message.aamoreadditions.upgrades.missing_prerequisite").withStyle(ChatFormatting.RED);
                default -> Component.empty();
            };
        }
    }

    private static Set<UpgradeType> installed(List<ItemStack> contents, int ignoreIndex) {
        Set<UpgradeType> set = EnumSet.noneOf(UpgradeType.class);
        for (int i = 0; i < contents.size(); i++) {
            if (i == ignoreIndex) continue;
            UpgradeType t = UpgradeType.of(contents.get(i));
            if (t != null) set.add(t);
        }
        return set;
    }

    /** Handler-aware check: also rejects upgrades this machine doesn't accept (energizer: efficiency only). */
    public static Check canInstall(UpgradeSlots.Handler h, ItemStack stack, int ignoreIndex) {
        if (stack.is(UpgradeSlots.UPGRADES) && !UpgradeSlots.allows(h.holder(), UpgradeType.of(stack))) return Check.INVALID;
        return canInstall(h.contents(), stack, ignoreIndex);
    }

    /** Can {@code stack} be installed, treating slot {@code ignoreIndex} as empty (-1 = none)? */
    public static Check canInstall(List<ItemStack> contents, ItemStack stack, int ignoreIndex) {
        if (!stack.is(UpgradeSlots.UPGRADES)) return Check.INVALID;
        for (int i = 0; i < contents.size(); i++) {
            if (i != ignoreIndex && ItemStack.isSameItem(contents.get(i), stack)) return Check.DUPLICATE;
        }
        UpgradeType type = UpgradeType.of(stack);
        if (type != null) {
            Set<UpgradeType> have = installed(contents, ignoreIndex);
            for (UpgradeType t : UpgradeType.values()) {
                if (t.line == type.line && t.tier < type.tier && !have.contains(t)) return Check.MISSING_PREREQ;
            }
        }
        return Check.OK;
    }

    /** Removal is blocked while a higher tier of the same line (which depends on it) is installed. */
    public static boolean canRemove(List<ItemStack> contents, int index) {
        UpgradeType type = UpgradeType.of(contents.get(index));
        if (type == null) return true;
        for (UpgradeType t : installed(contents, index)) {
            if (t.line == type.line && t.tier > type.tier) return false;
        }
        return true;
    }
}
