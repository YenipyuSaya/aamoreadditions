package com.aamoreadditions.menu;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AAMoreAdditions.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<AlloyFurnaceMenu>> ALLOY_FURNACE =
            MENUS.register("alloyfurnace", () -> IMenuTypeExtension.create(AlloyFurnaceMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CasingCrossMenu>> CASING_CROSS =
            MENUS.register("casing_cross", () -> IMenuTypeExtension.create(CasingCrossMenu::fromNetwork));

    private ModMenus() {}

    public static void register(IEventBus bus) { MENUS.register(bus); }
}
