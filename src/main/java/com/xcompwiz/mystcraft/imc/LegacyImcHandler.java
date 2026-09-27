package com.xcompwiz.mystcraft.imc;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.grammar.GrammarRuleRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;

import java.util.Locale;
import java.util.Map;

/** NeoForge bridge for the five public IMC keys accepted by Mystcraft 0.13.7.06. */
public final class LegacyImcHandler {
    private LegacyImcHandler() {}

    public static void process(InterModProcessEvent event) {
        event.getIMCStream().forEach(LegacyImcHandler::processOne);
    }

    private static void processOne(InterModComms.IMCMessage message) {
        String key = message.method() == null ? "" : message.method().toLowerCase(Locale.ROOT);
        Object value;
        try {
            value = message.messageSupplier().get();
        } catch (Throwable t) {
            Mystcraft.LOGGER.error("Failed to obtain IMC '{}' payload from [{}]", key, message.senderModId(), t);
            return;
        }
        try {
            switch (key) {
                case "blacklist" -> blacklistSymbol(message.senderModId(), value);
                case "blacklistfluid" -> blacklistFluid(message.senderModId(), value);
                case "fluidsymboldata" -> fluidSymbolData(message.senderModId(), value);
                case "blockinstability" -> ignoredInstability(message.senderModId(), key);
                case "meteorblock" -> ignoredInstability(message.senderModId(), key);
                default -> Mystcraft.LOGGER.warn("Unrecognized Mystcraft IMC '{}' from [{}]", key, message.senderModId());
            }
        } catch (Throwable t) {
            Mystcraft.LOGGER.error("Failed to process Mystcraft IMC '{}' from [{}]", key, message.senderModId(), t);
        }
    }

    private static void blacklistSymbol(String sender, Object payload) {
        String id = asString(payload);
        if (id == null || id.isBlank()) return;
        if (SymbolAvailability.blacklist(id)) {
            GrammarRuleRegistry.rebuildForDynamicSymbols();
            Mystcraft.LOGGER.info("Mystcraft IMC: [{}] blacklisted symbol {}", sender, id);
        }
    }

    private static void blacklistFluid(String sender, Object payload) {
        String value = asString(payload);
        if (value == null || value.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            // Old FluidRegistry names were frequently bare names.  Treat those as
            // sender-owned first, which matches the old BlockName ownership rule.
            id = ResourceLocation.tryBuild(sender, value);
        }
        if (id == null) return;
        LegacyMaterialSymbolRegistry.blacklistRuntimeFluid(id);
        LegacyMaterialSymbolRegistry.bootstrapRuntimeFluids();
        GrammarRuleRegistry.rebuildForDynamicSymbols();
        Mystcraft.LOGGER.info("Mystcraft IMC: [{}] blacklisted fluid {}", sender, id);
    }

    private static void fluidSymbolData(String sender, Object payload) {
        FluidSymbolPayload p = FluidSymbolPayload.from(payload, sender);
        if (p == null || p.fluidId() == null) return;
        LegacyMaterialSymbolRegistry.configureRuntimeFluid(
                p.fluidId(), p.seaBanned(), p.cardRank(), p.grammarRank(), p.factor1(), p.factor2());
        com.xcompwiz.mystcraft.instability.LegacyInstabilityBlockManager.invalidateCache();
        LegacyMaterialSymbolRegistry.bootstrapRuntimeFluids();
        GrammarRuleRegistry.rebuildForDynamicSymbols();
        Mystcraft.LOGGER.info("Mystcraft IMC: [{}] updated fluid symbol data for {}", sender, p.fluidId());
    }

    private static void ignoredInstability(String sender, String key) {
        // CP299 restores all built-in active 0.13.7.06 Instability providers. These external
        // compatibility-extension messages remain a no-op until their modern registry contracts
        // are ported; accepting them prevents legacy integrations from failing hard.
        Mystcraft.LOGGER.info("Mystcraft IMC: [{}] sent '{}'; accepted as compatibility no-op (built-in Instability is LIVE; external extension not yet ported)", sender, key);
    }

    private static String asString(Object value) {
        if (value instanceof String s) return s;
        if (value instanceof ResourceLocation id) return id.toString();
        return null;
    }

    private record FluidSymbolPayload(ResourceLocation fluidId, Boolean seaBanned, Integer cardRank, Integer grammarRank, Float factor1, Float factor2) {
        static FluidSymbolPayload from(Object value, String sender) {
            if (value instanceof CompoundTag tag) {
                String raw = tag.contains("fluidname") ? tag.getString("fluidname") : "";
                ResourceLocation id = parseFluid(raw, sender);
                Boolean sea = tag.contains("seabanned") ? tag.getBoolean("seabanned") : null;
                Integer card = tag.contains("cardrank") ? tag.getInt("cardrank") : null;
                Integer grammar = tag.contains("grammarrank") ? tag.getInt("grammarrank") : null;
                Float factor1 = tag.contains("factor1") ? tag.getFloat("factor1") : null;
                Float factor2 = tag.contains("factor2") ? tag.getFloat("factor2") : null;
                return new FluidSymbolPayload(id, sea, card, grammar, factor1, factor2);
            }
            if (value instanceof Map<?, ?> map) {
                Object rawObj = map.get("fluidname");
                ResourceLocation id = parseFluid(rawObj == null ? "" : rawObj.toString(), sender);
                Boolean sea = map.get("seabanned") instanceof Boolean b ? b : null;
                Integer card = asInt(map.get("cardrank"));
                Integer grammar = asInt(map.get("grammarrank"));
                Float factor1 = asFloat(map.get("factor1"));
                Float factor2 = asFloat(map.get("factor2"));
                return new FluidSymbolPayload(id, sea, card, grammar, factor1, factor2);
            }
            return null;
        }

        private static Integer asInt(Object value) {
            return value instanceof Number n ? n.intValue() : null;
        }

        private static Float asFloat(Object value) {
            return value instanceof Number n ? n.floatValue() : null;
        }

        private static ResourceLocation parseFluid(String raw, String sender) {
            if (raw == null || raw.isBlank()) return null;
            ResourceLocation parsed = ResourceLocation.tryParse(raw);
            if (parsed != null && raw.indexOf(':') >= 0) return parsed;
            ResourceLocation senderOwned = ResourceLocation.tryBuild(sender, raw);
            return senderOwned != null ? senderOwned : parsed;
        }
    }
}
