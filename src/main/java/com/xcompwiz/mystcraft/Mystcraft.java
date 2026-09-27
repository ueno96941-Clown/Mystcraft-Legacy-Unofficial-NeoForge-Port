package com.xcompwiz.mystcraft;

import com.xcompwiz.mystcraft.api.impl.InternalAPI;
import com.xcompwiz.mystcraft.advancement.MystCriteriaTriggers;

import com.mojang.logging.LogUtils;
import com.xcompwiz.mystcraft.network.MystNetwork;
import com.xcompwiz.mystcraft.registry.MystBiomeSources;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import com.xcompwiz.mystcraft.registry.MystCreativeTabs;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystFluids;
import com.xcompwiz.mystcraft.registry.MystEntities;
import com.xcompwiz.mystcraft.fluid.MystFluidCapabilities;
import com.xcompwiz.mystcraft.imc.LegacyImcHandler;
import com.xcompwiz.mystcraft.registry.MystMenus;
import com.xcompwiz.mystcraft.registry.MystRecipes;
import com.xcompwiz.mystcraft.registry.MystSounds;
import com.xcompwiz.mystcraft.registry.MystVillagers;
import com.xcompwiz.mystcraft.symbol.SymbolRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Mystcraft.MOD_ID)
public final class Mystcraft {
    public static final String MOD_ID = "mystcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Mystcraft(IEventBus modEventBus, ModContainer modContainer) {
        MystBiomeSources.register(modEventBus);
        MystBlocks.register(modEventBus);
        MystItems.register(modEventBus);
        MystFluids.register(modEventBus);
        MystEntities.register(modEventBus);
        MystBlockEntities.register(modEventBus);
        MystMenus.register(modEventBus);
        MystRecipes.register(modEventBus);
        MystSounds.register(modEventBus);
        MystCriteriaTriggers.register(modEventBus);
        MystVillagers.register(modEventBus);
        MystCreativeTabs.register(modEventBus);
        SymbolRegistry.bootstrapLegacyBuiltins();
        InternalAPI.initAPI();
        modEventBus.addListener(MystNetwork::registerPayloads);
        modEventBus.addListener(MystFluidCapabilities::register);
        modEventBus.addListener(LegacyImcHandler::process);
        LOGGER.info("Mystcraft Legacy port bootstrap loaded: target 0.13.7.06 semantics on Minecraft 1.21.1");
    }

}
