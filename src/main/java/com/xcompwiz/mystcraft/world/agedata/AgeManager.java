package com.xcompwiz.mystcraft.world.agedata;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.instability.AgeInstabilityScoreService;
import com.xcompwiz.mystcraft.instability.LegacyInstabilityPhase2Service;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.world.dimension.AgeDimensionKeys;
import com.xcompwiz.mystcraft.world.worldgen.AgeSymbolResolver;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reservation/finalization boundary for Ages before dynamic ServerLevel loading exists. */
public final class AgeManager {
    private AgeManager() {}

    public static Optional<AgeRecord> resolveExisting(MinecraftServer server, ItemStack book) {
        AgeRegistryData registry = AgeRegistryData.get(server);
        Integer uid = LinkOptions.getAgeUid(book);
        if (uid == null) uid = LinkOptions.getLegacyDimensionId(book);
        String key = LinkOptions.getDimensionKey(book);
        UUID uuid = LinkOptions.getTargetUuid(book);

        Optional<AgeRecord> byUid = uid == null ? Optional.empty() : registry.byUid(uid);
        Optional<AgeRecord> byKey = key == null || key.isBlank() ? Optional.empty() : registry.byDimensionKey(key);
        Optional<AgeRecord> byUuid = uuid == null ? Optional.empty() : registry.byUuid(uuid);

        AgeRecord candidate = byUid.orElseGet(() -> byKey.orElseGet(() -> byUuid.orElse(null)));
        if (candidate == null) return Optional.empty();

        // Any explicit identity already on the book must agree with the record.
        if (uid != null && candidate.ageUid() != uid) return Optional.empty();
        if (key != null && !key.isBlank() && !candidate.dimensionKey().equals(key)) return Optional.empty();
        if (uuid != null && !candidate.uuid().equals(uuid)) return Optional.empty();
        return Optional.of(candidate);
    }

    /**
     * Returns the persisted reservation represented by this book, if its
     * identity fields still agree with the global Age registry. This does not
     * imply that a ServerLevel for the Age currently exists.
     */
    public static Optional<AgeRecord> resolveReservation(MinecraftServer server, ItemStack book) {
        // CP279: Linking Books can target persisted Mystcraft Ages too.  A valid
        // DimensionKey/UID/UUID identity is enough to resolve the reservation even
        // when the live ServerLevel has been lazily unloaded.  Age establishment
        // remains restricted to Descriptive Books in establish().
        return resolveExisting(server, book);
    }

    /**
     * CP279 compatibility repair for Linking Books created before durable Mystcraft
     * Age identity was written by capturePosition().  DimensionKey-only books are
     * upgraded in place without changing their spawn, yaw, flags or properties.
     */
    public static boolean repairKnownAgeIdentity(MinecraftServer server, ItemStack book) {
        if (server == null || book == null || book.isEmpty()) return false;
        Optional<AgeRecord> resolved = resolveExisting(server, book);
        if (resolved.isEmpty()) return false;
        AgeRecord age = resolved.get();
        LinkOptions.setAgeUid(book, age.ageUid());
        LinkOptions.setLegacyDimensionId(book, age.ageUid());
        LinkOptions.setDimensionKey(book, age.dimensionKey());
        LinkOptions.setTargetUuid(book, age.uuid());
        return true;
    }

    /** Resolves a persisted Age from its live Level key. */
    public static Optional<AgeRecord> resolveByLevel(MinecraftServer server, ResourceKey<Level> levelKey) {
        if (server == null || levelKey == null) return Optional.empty();
        return AgeRegistryData.get(server).byDimensionKey(levelKey.location().toString());
    }

    /**
     * Finalizes a new Descriptive Book exactly once, or repairs missing identity
     * fields by reusing an already persisted matching Age.
     */
    public static AgeRecord establish(ServerPlayer player, ItemStack book) {
        MinecraftServer server = player.getServer();
        if (server == null) throw new IllegalStateException("Cannot establish an Age without a server");
        if (!(book.getItem() instanceof ItemAgebook agebook)) {
            throw new IllegalArgumentException("Only Descriptive Books can establish Ages");
        }

        Optional<AgeRecord> existing = resolveExisting(server, book);
        if (existing.isPresent()) {
            applyIdentity(book, existing.get());
            return existing.get();
        }

        // If the book already carries a complete-but-unknown identity, do not silently fork it.
        Integer explicitUid = LinkOptions.getAgeUid(book);
        if (explicitUid == null) explicitUid = LinkOptions.getLegacyDimensionId(book);
        String explicitKey = LinkOptions.getDimensionKey(book);
        UUID explicitUuid = LinkOptions.getTargetUuid(book);
        if (explicitUid != null || (explicitKey != null && !explicitKey.isBlank()) || explicitUuid != null) {
            throw new IllegalStateException("Descriptive Book contains an unknown or conflicting Age identity");
        }

        AgeRegistryData registry = AgeRegistryData.get(server);
        int uid = registry.allocateUid();
        String dimensionKey = AgeDimensionKeys.levelKeyString(uid);
        UUID uuid = UUID.randomUUID();

        String seedProperty = LinkOptions.getProperty(book, "Seed");
        long seed;
        if (seedProperty != null) {
            try {
                seed = Long.parseLong(seedProperty);
            } catch (NumberFormatException ex) {
                seed = server.overworld().getSeed() + new java.util.Random(uid).nextLong();
            }
        } else {
            seed = server.overworld().getSeed() + new java.util.Random(uid).nextLong();
        }

        String name = LinkOptions.getDisplayName(book);
        if (name == null || name.isBlank() || "???".equals(name)) name = "Age " + uid;

        AgeRecord record = new AgeRecord(uid, dimensionKey, name, seed, uuid);
        List<ItemStack> pages = agebook.getPageList(book, player.registryAccess());
        record.setPages(pages);
        record.setAuthors(agebook.getAuthors(book));

        List<String> symbols = new ArrayList<>();
        for (ItemStack page : pages) {
            String symbol = Page.getSymbolId(page);
            if (symbol != null) symbols.add(symbol);
        }
        record.setSymbols(symbols);
        List<String> effectiveSymbols = AgeSymbolResolver.resolve(seed, symbols);
        var instability = AgeInstabilityScoreService.staticScore(record);
        var phase2 = LegacyInstabilityPhase2Service.prepare(record, instability.recordedScore());
        if (Mystcraft.LOGGER.isDebugEnabled()) {
            Mystcraft.LOGGER.debug(
                    "Prepared Age {} '{}': pages={}, effectiveSymbols={}, instabilityScore={}, controllerScore={}, providers={}",
                    uid, name, pages.size(), effectiveSymbols.size(), instability.recordedScore(),
                    phase2.selection().controllerScore(), phase2.selection().providerLevels());
        }
        registry.put(record);
        // Age identity is global Overworld SavedData. Flush the reservation before
        // writing the identity back to the player's book so an unexpected stop
        // cannot leave a persisted book pointing at an Age record that never made
        // it to disk. An orphaned reservation is safe; a dangling book identity is
        // not.
        AgeRegistryData.flush(server);

        applyIdentity(book, record);
        LinkOptions.setProperty(book, "Seed", Long.toString(seed));
        return record;
    }


    private static void applyIdentity(ItemStack book, AgeRecord age) {
        LinkOptions.setAgeUid(book, age.ageUid());
        LinkOptions.setLegacyDimensionId(book, age.ageUid());
        LinkOptions.setDimensionKey(book, age.dimensionKey());
        LinkOptions.setTargetUuid(book, age.uuid());
        LinkOptions.setDisplayName(book, age.ageName());
        LinkOptions.setProperty(book, "Seed", Long.toString(age.seed()));
    }
}
