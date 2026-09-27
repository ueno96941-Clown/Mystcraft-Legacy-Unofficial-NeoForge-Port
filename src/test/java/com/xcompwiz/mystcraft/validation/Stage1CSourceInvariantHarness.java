package com.xcompwiz.mystcraft.validation;
import java.nio.file.*;
import java.util.*;
public final class Stage1CSourceInvariantHarness {
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args.length==0?".":args[0]);
  List<String> errors=new ArrayList<>();
  try(var paths=Files.walk(root.resolve("src/main/java"))) {
   paths.filter(p->p.toString().endsWith(".java")).forEach(p->{
    try {
     String rel=root.relativize(p).toString().replace('\\','/');
     String text=Files.readString(p);
     if(!rel.contains("/client/") && text.contains("com.xcompwiz.mystcraft.client"))
      errors.add("common->client reference: "+rel);
     String low=p.getFileName().toString().toLowerCase(Locale.ROOT);
     if(low.contains("decay")) errors.add("Decay source file: "+rel);
    } catch(Exception e){ throw new RuntimeException(e); }
   });
  }
  try(var paths=Files.walk(root.resolve("src/main/resources"))) {
   paths.filter(Files::isRegularFile).forEach(p->{
    String rel=root.relativize(p).toString().replace('\\','/');
    if(p.getFileName().toString().toLowerCase(Locale.ROOT).contains("decay"))
     errors.add("Decay resource file: "+rel);
   });
  }
  Path planner=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeWorldgenPlanner.java");
  String plannerText=Files.readString(planner);
  if(!plannerText.contains("validateMandatoryControllers(effectiveAnalysis)"))
   errors.add("AgeWorldgenPlanner no longer enforces mandatory controller postcondition");

  Path grammar=root.resolve("src/main/java/com/xcompwiz/mystcraft/grammar/BuiltinLegacyGrammar.java");
  String grammarText=Files.readString(grammar);
  String mandatoryRoot="List.of(\"mystcraft:TerrainGen\", \"mystcraft:BiomeController\", \"mystcraft:Weather\", \"mystcraft:Lighting\"";
  if(!grammarText.contains(mandatoryRoot))
   errors.add("Legacy Age root mandatory controller order changed");

  Path resolver=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeSymbolResolver.java");
  String resolverText=Files.readString(resolver);
  if(!resolverText.contains("tree.parseTerminals(requested, new Random(ageSeed))")
    || !resolverText.contains("tree.getExpanded(new Random(ageSeed))"))
   errors.add("Legacy grammar two-Random seed boundary changed");

  try(var paths=Files.walk(root.resolve("src/main/java/com/xcompwiz/mystcraft/client"))) {
   paths.filter(p->p.toString().endsWith(".java")).forEach(p->{
    try {
     String text=Files.readString(p);
     if(text.contains("registerFluidType") && text.contains("WATER_TYPE"))
      errors.add("unsafe duplicate vanilla WATER_TYPE client extension registration: "+root.relativize(p));
    } catch(Exception e){ throw new RuntimeException(e); }
   });
  }

  Path worldManager=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeWorldManager.java");
  String worldManagerText=Files.readString(worldManager);
  if(!worldManagerText.contains("Runtime Age capture must run on the server thread"))
   errors.add("shutdown runtime capture lost server-thread invariant");
  if(!worldManagerText.contains("if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) continue;"))
   errors.add("captureAllLoaded no longer scopes capture to Mystcraft dimensions");
  if(!worldManagerText.contains("ForcedChunkManager.hasForcedChunks(level)"))
   errors.add("runtime Age unload must honor combined vanilla/NeoForge persistent forced chunks");
  if(worldManagerText.contains("!level.getForcedChunks().isEmpty()"))
   errors.add("runtime Age unload regressed to vanilla-only forced chunk detection");
  if(!worldManagerText.contains("server.perWorldTickTimes.remove(level.dimension())"))
   errors.add("runtime Age unload no longer prunes NeoForge per-dimension tick history");
  String accessTransformerText=Files.readString(root.resolve("src/main/resources/META-INF/accesstransformer.cfg"));
  if(!accessTransformerText.contains("public net.minecraft.server.MinecraftServer perWorldTickTimes"))
   errors.add("runtime Age tick-history cleanup lost its NeoForge 21.1.x access transformer");

  Path runtimeInstaller=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeServerLevelInstaller.java");
  String runtimeInstallerText=Files.readString(runtimeInstaller);
  if(!runtimeInstallerText.contains("AgePersistentForcedChunkRuntime.reinstate(level)"))
   errors.add("runtime Age install no longer replays persistent forced chunks after startup");
  if(!runtimeInstallerText.contains("NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));") ||
     runtimeInstallerText.indexOf("NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));") > runtimeInstallerText.indexOf("AgePersistentForcedChunkRuntime.reinstate(level)"))
   errors.add("runtime Age forced-ticket replay must follow LevelEvent.Load like NeoForge startup");
  int runtimeSeedInstall=runtimeInstallerText.indexOf("AgeWorldgenSeedRuntime.install(level, age.seed())");
  int runtimeStructureInstall=runtimeInstallerText.indexOf("AgeStructureStateInstaller.install(");
  int runtimeLoadPost=runtimeInstallerText.indexOf("NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));");
  int runtimeSpawnResolve=runtimeInstallerText.indexOf("AgeRuntimeStateSync.ensureInitialSpawn(server, age, level)");
  if(runtimeSeedInstall<0 || runtimeStructureInstall<0 || runtimeLoadPost<0 || runtimeSpawnResolve<0 ||
     !(runtimeSeedInstall < runtimeLoadPost && runtimeStructureInstall < runtimeLoadPost && runtimeLoadPost < runtimeSpawnResolve))
   errors.add("runtime Age first-chunk order must remain seed/structure install -> LevelEvent.Load -> spawn chunk resolution");
  if(!runtimeInstallerText.contains("NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));"))
   errors.add("post-Load runtime install rollback must emit a compensating LevelEvent.Unload");
  if(!runtimeInstallerText.contains("overworld.getRandomSequences()"))
   errors.add("runtime Age ServerLevel must share Overworld RandomSequences like vanilla secondary dimensions");
  if(!runtimeInstallerText.contains("Util.backgroundExecutor()"))
   errors.add("runtime Age ServerLevel must use Minecraft vanilla background worker executor contract");
  if(runtimeInstallerText.contains("ServerLevel level = new ServerLevel(\n                server,\n                server,"))
   errors.add("runtime Age ServerLevel regressed to using MinecraftServer as its chunk worker executor");

  String vanillaBridgeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeVanillaWorldgenBridgeResolver.java"));
  if(vanillaBridgeText.contains("minecraft:lake_lava_surface") || vanillaBridgeText.contains("minecraft:lake_lava_underground"))
   errors.add("Legacy Lakes must not also inject modern lava-lake PlacedFeatures (double population)");
  if(!vanillaBridgeText.contains("case SURFACE_LAKES, DEEP_LAKES -> custom.add(entry.kind())"))
   errors.add("surface/deep lakes must remain on the direct Legacy population path");
  if(!vanillaBridgeText.contains("case DUNGEONS ->") || !vanillaBridgeText.contains("custom.add(entry.kind())") ||
     vanillaBridgeText.contains("minecraft:monster_room") || vanillaBridgeText.contains("minecraft:monster_room_deep"))
   errors.add("Dungeons must remain on the direct Legacy population path without modern monster-room injection");
  String legacyStructureSpecsText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacyStructureSetSpecs.java"));
  String structureSelectionText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStructureSetSelection.java"));
  if(!legacyStructureSpecsText.contains("VILLAGE_SPACING = 32") ||
     !legacyStructureSpecsText.contains("VILLAGE_SEPARATION = 8") ||
     !legacyStructureSpecsText.contains("VILLAGE_SALT = 10387312") ||
     !legacyStructureSpecsText.contains("minecraft:village_plains") ||
     !legacyStructureSpecsText.contains("minecraft:village_desert") ||
     !legacyStructureSpecsText.contains("minecraft:village_savanna") ||
     !legacyStructureSpecsText.contains("minecraft:village_taiga") ||
     legacyStructureSpecsText.contains("minecraft:village_snowy"))
   errors.add("Legacy Village StructureSet adapter drifted from 1.12 32/8/salt/style contract");
  String mineshaftMathText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacyMineshaftPlacementMath.java"));
  String mineshaftPlacementText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyMineshaftPlacement.java"));
  if(!structureSelectionText.contains("legacyMineshafts(structures)") ||
     !structureSelectionText.contains("new AgeLegacyMineshaftPlacement()") ||
     !mineshaftPlacementText.contains("extends RandomSpreadStructurePlacement") ||
     !mineshaftPlacementText.contains("AgeLegacyMineshaftPlacementMath.isPlacementChunk") ||
     !mineshaftMathText.contains("random.nextDouble() < CHANCE") ||
     !mineshaftMathText.contains("random.nextInt(80) < Math.max(Math.abs(chunkX), Math.abs(chunkZ))") ||
     !mineshaftMathText.contains("random.nextInt();"))
   errors.add("Mineshaft placement drifted from 1.12 MapGenBase + MapGenMineshaft candidate contract");
  if(!structureSelectionText.contains("legacyVillages(structures)") ||
     !structureSelectionText.contains("new RandomSpreadStructurePlacement(") ||
     !structureSelectionText.contains("AgeLegacyStructureSetSpecs.VILLAGE_SPACING") ||
     !structureSelectionText.contains("Holder.direct(new StructureSet(entries, placement))"))
   errors.add("Age-local Village StructureSet is no longer synthesized from the Legacy placement contract");
  if(!structureSelectionText.contains("legacyNetherFortressOnly(structures)") ||
     !structureSelectionText.contains("AgeLegacyStructureSetSpecs.NETHER_FORTRESS_STRUCTURE") ||
     !structureSelectionText.contains("Holder.direct(new StructureSet(fortress, new AgeLegacyNetherFortressPlacement()))"))
   errors.add("NetherFort Symbol must use fortress-only with the Legacy MapGenNetherBridge placement adapter");
  String fortressPlacementText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyNetherFortressPlacement.java"));
  String fortressMathText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacyNetherFortressPlacementMath.java"));
  if(!fortressPlacementText.contains("FrequencyReductionMethod.LEGACY_TYPE_1") ||
     !fortressPlacementText.contains("1.0F / 3.0F") ||
     !fortressPlacementText.contains("REGION_SIZE = 16") ||
     !fortressPlacementText.contains("AgeLegacyNetherFortressPlacementMath.candidate") ||
     !fortressMathText.contains("queryChunkX >> 4") ||
     !fortressMathText.contains("queryChunkZ >> 4") ||
     !fortressMathText.contains("random.nextInt(3) == 0") ||
     !fortressMathText.contains("+ 4 + random.nextInt(8)"))
   errors.add("NetherFort placement drifted from 1.12 16-region/1-in-3/+4..11 contract");
  if(structureSelectionText.contains("minecraft:bastion_remnant"))
   errors.add("NetherFort StructureSet adapter must never admit Bastion Remnants");
  String legacyPopulationText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationFeature.java"));
  String legacyPlacedText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java"));
  String legacyBatchText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java"));
  if(!legacyPlacedText.contains("case DUNGEONS, SPIKES, OBELISKS, CRYSTAL_FORMATION, DEEP_LAKES, SURFACE_LAKES"))
   errors.add("Dungeons are no longer installed as a direct Legacy runtime population feature");
  String densePolicyText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/DenseOresPolicy.java"));
  if(!densePolicyText.contains("MAX_EFFECTIVE_PAGES = 3") ||
     !densePolicyText.contains("case 1 -> 2L") ||
     !densePolicyText.contains("case 2 -> 3L") ||
     !densePolicyText.contains("default -> 5L") ||
     !densePolicyText.contains("extraBlocksForLayer"))
   errors.add("CP291 Dense Ores must remain 0=1x, 1=2x, 2=3x, 3+=5x with three effective pages");
  if(!legacyPopulationText.contains("snapshotDenseOreLayerData") ||
     !legacyPopulationText.contains("extraBlocksForLayer(ores.size(), hosts.size(), densePages)") ||
     !legacyPopulationText.contains("same Y") ||
     !legacyPopulationText.contains("for (int page = 0; page < densePages; ++page)"))
   errors.add("CP260 Prosperous Ores must preserve source-Y/mod-ore behavior and bounded host-capacity placement");
  if(!legacyPlacedText.contains("new AgeLegacyPopulationBatchFeature(ordered)") ||
     !legacyPlacedText.contains("return List.of(Holder.direct(new PlacedFeature") ||
     !legacyPlacedText.contains("Collapse the authored page count at the first"))
   errors.add("direct Mystcraft population ports must remain one ordered shared-RNG batch, including collapsed DenseOres page count at its first Symbol position");
  if(!legacyBatchText.contains("for (int index = 0; index < orderedSteps.size(); ++index)") ||
     !legacyBatchText.contains("any |= step.feature().place(context)") || legacyBatchText.contains("any ||="))
   errors.add("Legacy population batch must execute ordinary children sequentially on the same context/random stream without accidental short-circuiting");
  if(!legacyPlacedText.contains("case VILLAGES") || !legacyPlacedText.contains("legacyStructure(entry.kind())") ||
     !legacyBatchText.contains("step.kind() == AgeFeatureKind.VILLAGES && placed") ||
     !legacyBatchText.contains("AgeLegacyStructurePopulationBridge.place") ||
     !legacyBatchText.contains("return any;"))
   errors.add("Legacy Village IPopulate placement/success must short-circuit later population at the Village Symbol position");
  String structurePopulationText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructurePopulationBridge.java"));
  if(!structurePopulationText.contains("int minX = chunkBlockX + 8") ||
     !structurePopulationText.contains("int maxX = minX + 15") ||
     !structurePopulationText.contains("int minZ = chunkBlockZ + 8") ||
     !structurePopulationText.contains("int maxZ = minZ + 15") ||
     !structurePopulationText.contains("path.startsWith(\"village_\")") ||
     !structurePopulationText.contains("structures.startsForStructure") ||
     !structurePopulationText.contains("structureBox.intersects(populationBox)") ||
     !structurePopulationText.contains("start.placeInChunk("))
   errors.add("Legacy MapGenStructure population bridge lost shifted-box query/placement contract");
  String legacyPopulationSeedText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacyPopulationSeedMath.java"));
  if(!legacyPopulationSeedText.contains("random.nextLong() / 2L * 2L + 1L") ||
     !legacyPopulationSeedText.contains("(long) chunkX * xMul + (long) chunkZ * zMul ^ ageSeed"))
   errors.add("Legacy ChunkProviderMyst population boundary seed math drifted");
  String fissureRuntimeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStarFissureRuntime.java"));
  if(!fissureRuntimeText.contains("AgeLegacyPopulationSeedMath.populationBoundarySeed") ||
     fissureRuntimeText.contains("341873128712L") || fissureRuntimeText.contains("132897987541L"))
   errors.add("Star Fissure isolated runtime must use the exact Legacy population boundary seed instead of invented salts");
  if(!legacyPopulationText.contains("case DUNGEONS -> placeDungeons(context, chunkX, chunkZ)") ||
     !legacyPopulationText.contains("for (int attempt = 0; attempt < 8; ++attempt)") ||
     !legacyPopulationText.contains("chunkX + random.nextInt(16) + 8") ||
     !legacyPopulationText.contains("random.nextInt(256)") ||
     !legacyPopulationText.contains("chunkZ + random.nextInt(16) + 8") ||
     !legacyPopulationText.contains("generateLegacyDungeon(parent.level(), random") ||
     legacyPopulationText.contains("Feature.MONSTER_ROOM.place"))
   errors.add("Legacy dungeon population lost its direct 1.12 8-attempt/+8-window/0..255 kernel");
  if(!legacyPopulationText.contains("openings < 1 || openings > 5") ||
     !legacyPopulationText.contains("dy == -1 && random.nextInt(4) != 0") ||
     !legacyPopulationText.contains("for (int chest = 0; chest < 2; ++chest)") ||
     !legacyPopulationText.contains("for (int tryIndex = 0; tryIndex < 3; ++tryIndex)") ||
     !legacyPopulationText.contains("BuiltInLootTables.SIMPLE_DUNGEON") ||
     !legacyPopulationText.contains("EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SPIDER"))
   errors.add("Legacy dungeon room/chest/loot/spawner semantics drifted from 1.12 WorldGenDungeons");
  String generationInstallerText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeChunkGeneratorGenerationSettingsInstaller.java"));
  String generationOverlayText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeBiomeGenerationSettingsOverlay.java"));
  if(!generationInstallerText.contains("legacyPopulationFeatures =") ||
     !generationInstallerText.contains("AgeLegacyPopulationPlacedFeatures.create(featurePlan)") ||
     !generationInstallerText.contains("legacyPopulationFeatures,"))
   errors.add("Legacy population PlacedFeatures must be constructed once and shared across all Age biome overlays");
  if(generationOverlayText.contains("AgeLegacyPopulationPlacedFeatures.create(featurePlan)") ||
     !generationOverlayText.contains("LEGACY_POPULATION_STEP = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal()") ||
     !generationOverlayText.contains("builder.addFeature(LEGACY_POPULATION_STEP, feature)"))
   errors.add("Legacy population features must use shared holders in a post-vanilla decoration step");

  String forcedRuntimeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgePersistentForcedChunkRuntime.java"));
  if(!forcedRuntimeText.contains("ForcedChunksSavedData.factory()") ||
     !forcedRuntimeText.contains("updateChunkForced(new ChunkPos(packed), true)") ||
     !forcedRuntimeText.contains("ForcedChunkManager.reinstatePersistentChunks(level, data)"))
   errors.add("runtime Age persistent forced-chunk replay lost vanilla/NeoForge startup parity");

  String borderBridgeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeWorldBorderBridge.java"));
  if(!borderBridgeText.contains("target.applySettings(source.createSettings())") ||
     !borderBridgeText.contains("new BorderChangeListener.DelegateBorderChangeListener(target)") ||
     !borderBridgeText.contains("binding.source().removeListener(binding.listener())"))
   errors.add("runtime Age border bridge lost vanilla secondary-dimension snapshot/delegate lifecycle");
  if(!runtimeInstallerText.contains("AgeRuntimeWorldBorderBridge.attach(overworld, level)"))
   errors.add("runtime Age install no longer attaches Overworld border delegation");
  if(runtimeInstallerText.indexOf("AgeRuntimeWorldBorderBridge.attach(overworld, level)") > runtimeInstallerText.indexOf("NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));"))
   errors.add("runtime Age world-border bridge must be attached before LevelEvent.Load");
  String lifecycleText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeLifecycleEvents.java"));
  if(!lifecycleText.contains("onLevelUnload(LevelEvent.Unload event)") ||
     !lifecycleText.contains("AgeRuntimeWorldBorderBridge.detach(level)") ||
     !lifecycleText.contains("AgeRuntimeWorldBorderBridge.clearAll()"))
   errors.add("runtime Age world-border listeners are not detached on unload/server stop");
  String registryDataText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/agedata/AgeRegistryData.java"));
  if(!registryDataText.contains("server.overworld().getDataStorage().save()") ||
     !registryDataText.contains("Mystcraft Age registry flush must run on the server thread"))
   errors.add("global Age SavedData lost explicit Overworld durability flush/server-thread guard");
  String runtimeStateText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeStateSync.java"));
  if(!runtimeStateText.contains("captureAndFlush(") || !runtimeStateText.contains("AgeRegistryData.flush(server)"))
   errors.add("runtime Age state sync lost durable capture/flush boundary");
  int fissureSave=runtimeStateText.indexOf("level.save(null, true, false)");
  int fissureGeneratedMarker=runtimeStateText.indexOf("age.setStarFissureGenerated(true)");
  int fissureProcessedMarker=runtimeStateText.indexOf("age.setStarFissureProcessed(true)");
  int fissureFlush=runtimeStateText.indexOf("AgeRegistryData.flush(server)", fissureProcessedMarker);
  if(fissureSave<0 || fissureGeneratedMarker<0 || fissureProcessedMarker<0 || fissureFlush<0 ||
     !(fissureSave < fissureGeneratedMarker && fissureGeneratedMarker < fissureProcessedMarker && fissureProcessedMarker < fissureFlush))
   errors.add("Star Fissure generated commit order must remain Age chunk save -> generated/processed markers -> Overworld SavedData flush");
  String spawnResolverText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeSpawnResolver.java"));
  if(!spawnResolverText.contains("ChunkStatus.CARVERS") || spawnResolverText.contains("getHeightmapPos("))
   errors.add("initial Age spawn must resolve before FEATURES without forcing a FULL heightmap lookup");
  String fissureBatchText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java"));
  String fissurePlacedText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java"));
  String fissureBridgeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStarFissurePopulationBridge.java"));
  String fissureRuntimeI66Text=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStarFissureRuntime.java"));
  String ageRecordText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/agedata/AgeRecord.java"));
  if(!fissurePlacedText.contains("starFissureSentinel()") ||
     !fissureBatchText.contains("AgeStarFissurePopulationBridge.stageIfSpawnChunk") ||
     !fissureBatchText.contains("AgeStarFissurePopulationBridge.suppressIfSpawnChunk"))
   errors.add("Star Fissure lost ordered spawn-chunk population/short-circuit staging");
  if(!fissureBridgeText.contains("ConcurrentHashMap") ||
     !fissureRuntimeI66Text.contains("AgeStarFissurePopulationBridge.take") ||
     !fissureRuntimeI66Text.contains("level.getChunk(spawnChunkX, spawnChunkZ)"))
   errors.add("Star Fissure async FEATURES -> server-thread application bridge is incomplete");
  if(!ageRecordText.contains("StarFissureProcessed") ||
     !lifecycleText.contains("AgeStarFissurePopulationBridge.clearAll()") ||
     !lifecycleText.contains("AgeStarFissurePopulationBridge.clear(level.dimension())"))
   errors.add("Star Fissure processed/suppressed durability or staging cleanup contract lost");
  if(!fissurePlacedText.contains("case MINESHAFTS, STRONGHOLDS, NETHER_FORTRESS") ||
     !fissurePlacedText.contains("legacyStructure(entry.kind())") ||
     !structurePopulationText.contains("case MINESHAFTS ->") ||
     !structurePopulationText.contains("case STRONGHOLDS ->") ||
     !structurePopulationText.contains("case NETHER_FORTRESS ->"))
   errors.add("Legacy structure Symbols must place their prepared StructureStarts in authored IPopulate order");

  if(!lifecycleText.contains("AgeRuntimeStateSync.captureAndFlush(server, age, level, age.visited())"))
   errors.add("Mystcraft LevelEvent.Save must flush AgeRegistryData after capture in the same save cycle");
  if(!lifecycleText.contains("AgeRegistryData.flush(server)") ||
     !lifecycleText.contains("Captured and flushed runtime state"))
   errors.add("ServerStopping must flush captured Age runtime state before vanilla shutdown save/close");
  int stoppingCapture=lifecycleText.indexOf("AgeRuntimeWorldManager.captureAllLoaded(server)");
  int stoppingFlush=lifecycleText.indexOf("AgeRegistryData.flush(server)");
  if(stoppingCapture<0 || stoppingFlush<0 || stoppingCapture > stoppingFlush)
   errors.add("ServerStopping durability order must remain capture-all -> Overworld SavedData flush");

  String runtimeLevelText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/MystcraftRuntimeServerLevel.java"));
  if(!runtimeLevelText.contains("public long getSeed()") ||
     !runtimeLevelText.contains("CONSTRUCTION_SEED") ||
     !runtimeLevelText.contains("return mystcraftAgeSeed"))
   errors.add("runtime Mystcraft ServerLevel no longer exposes the durable Age seed to WorldGenRegion");
  String seedRuntimeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeWorldgenSeedRuntime.java"));
  if(seedRuntimeText.contains("RandomState.create(") ||
     seedRuntimeText.contains("chunkMap.randomState =") ||
     !seedRuntimeText.contains("chunkSource.getGeneratorState().getLevelSeed() != ageSeed") ||
     !seedRuntimeText.contains("level.structureCheck.randomState = randomState") ||
     !seedRuntimeText.contains("level.structureCheck.seed = ageSeed"))
   errors.add("runtime Age seed contract must preserve constructor-born ChunkMap RandomState and only repair StructureCheck");
  if(!runtimeInstallerText.contains("MystcraftRuntimeServerLevel.create(") ||
     !runtimeInstallerText.contains("AgeWorldgenSeedRuntime.install(level, age.seed())"))
   errors.add("runtime Age install no longer binds ServerLevel/worldgen internals to the Age seed");
  int seedInstall=runtimeInstallerText.indexOf("AgeWorldgenSeedRuntime.install(level, age.seed())");
  int structureInstall=runtimeInstallerText.indexOf("AgeStructureStateInstaller.install(");
  if(seedInstall<0 || structureInstall<0 || seedInstall > structureInstall)
   errors.add("Age RandomState must be rebound before Age-local StructureSet state is created");
  int structurePlacementInstall=runtimeInstallerText.indexOf("AgeLegacyStructurePlacementRuntime.install(level, age.seed())");
  if(structurePlacementInstall<0 || structureInstall<0 || runtimeLoadPost<0 ||
     !(structureInstall < structurePlacementInstall && structurePlacementInstall < runtimeLoadPost))
   errors.add("Legacy structure auto-placement suppression must install after Age StructureState and before LevelEvent.Load/first chunk");
  String structurePlacementRuntimeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructurePlacementRuntime.java"));
  if(!structurePlacementRuntimeText.contains("super(level, new WorldOptions(ageSeed, true, false), structureCheck)") ||
     !structurePlacementRuntimeText.contains("this.suppressedWorldgenOptions = new WorldOptions(ageSeed, false, false)") ||
     !structurePlacementRuntimeText.contains("public StructureManager forWorldGenRegion(WorldGenRegion region)") ||
     !structurePlacementRuntimeText.contains("level.structureManager = new PopulationAwareStructureManager("))
   errors.add("runtime Age must preserve top-level structure visibility while suppressing only WorldGenRegion automatic placement");
  String structureStateText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStructureStateInstaller.java"));
  if(structureStateText.contains("ChunkGeneratorStructureState.createForFlat(") ||
     !structureStateText.contains("new ChunkGeneratorStructureState(") ||
     !structureStateText.contains("ageSeed,\n                ageSeed,") ||
     !structureStateText.contains("state.ensureStructuresGenerated()") ||
     !structureStateText.contains("AgeLegacyStrongholdRingInstaller.replaceRings(") ||
     structureStateText.indexOf("state.ensureStructuresGenerated()") > structureStateText.indexOf("AgeLegacyStrongholdRingInstaller.replaceRings(") ||
     structureStateText.indexOf("AgeLegacyStrongholdRingInstaller.replaceRings(") > structureStateText.indexOf("chunkSource.chunkMap.chunkGeneratorState = state"))
   errors.add("Age-local StructureSet state lost Age-seeded Legacy stronghold ring/eager initialization contract");
  String strongholdRingText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStrongholdRingInstaller.java"));
  if(!strongholdRingText.contains("new LegacyRandomSource(ageSeed)") ||
     strongholdRingText.contains("random.fork()") ||
     !strongholdRingText.contains("preferred::contains,\n                random,") ||
     !strongholdRingText.contains("state.ringPositions.put(") ||
     !strongholdRingText.contains("BIOME_SEARCH_RADIUS = 112"))
   errors.add("Stronghold rings must retain the 1.12 sequential Java-Random ring/biome-search stream");
  String strongholdSelectionText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStructureSetSelection.java"));
  String strongholdEligibilityText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacyStrongholdBiomeEligibility.java"));
  if(!strongholdSelectionText.contains("if (STRONGHOLDS.equals(resourceId))") ||
     !strongholdSelectionText.contains("legacyStrongholds(structures, biomes)") ||
     !strongholdSelectionText.contains("new ConcentricRingsStructurePlacement(") ||
     !strongholdEligibilityText.contains("legacyBaseHeight > 0.0D") ||
     !strongholdSelectionText.contains("LegacyBiomeSymbolRegistry.values()") ||
     !strongholdSelectionText.contains("definition.legacyBaseHeight()") ||
     strongholdEligibilityText.contains("STRONGHOLD_BIASED_TO"))
   errors.add("Stronghold preferred biomes must come from the retained 1.12 baseHeight > 0 contract, not the modern tag");
  if(accessTransformerText.contains("public-f net.minecraft.server.level.ChunkMap randomState") ||
     !accessTransformerText.contains("public net.minecraft.world.level.chunk.ChunkGeneratorStructureState <init>(Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeSource;JJLjava/util/List;)V") ||
     !accessTransformerText.contains("public net.minecraft.server.level.ServerLevel structureCheck") ||
     !accessTransformerText.contains("public-f net.minecraft.server.level.ServerLevel structureManager") ||
     !accessTransformerText.contains("public net.minecraft.world.level.chunk.ChunkGeneratorStructureState ringPositions") ||
     !accessTransformerText.contains("public-f net.minecraft.world.level.levelgen.structure.StructureCheck randomState") ||
     !accessTransformerText.contains("public-f net.minecraft.world.level.levelgen.structure.StructureCheck seed"))
   errors.add("runtime Age per-seed worldgen access-transformer contract is incomplete or over-broad");

  String environmentResolverText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeEnvironmentResolver.java"));
  if(!environmentResolverText.contains("boolean pvpEnabled = true"))
   errors.add("project policy requires PvP OFF in every Mystcraft Age");

  Path rainBridge=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeRainRenderBridge.java");
  Path snowBridge=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeSnowRenderBridge.java");
  String rainBridgeText=Files.readString(rainBridge), snowBridgeText=Files.readString(snowBridge);
  if(!rainBridgeText.contains("-gz/len*.5D") || !rainBridgeText.contains("gx/len*.5D"))
   errors.add("forced-rain renderer lost Legacy radial quad geometry");
  if(!snowBridgeText.contains("*.3F+.5F") || snowBridgeText.contains("dx*dx+dz*dz>radius*radius"))
   errors.add("forced-snow renderer lost Legacy square coverage/alpha falloff");

  Path packedLight=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeWeatherPackedLightBridge.java");
  String packedLightText=Files.readString(packedLight);
  if(!packedLightText.contains("(combinedLight * 3 + LEGACY_FULL_BRIGHT) / 4"))
   errors.add("forced-snow packed-light bridge lost Legacy 3/4 + 1/4 full-bright formula");
  if(!packedLightText.contains("return combinedLight;"))
   errors.add("forced-rain packed-light bridge no longer preserves combined light");

  String rainRendererText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeRainRenderer.java"));
  String snowRendererText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeSnowRenderer.java"));
  if(!rainRendererText.contains("DefaultVertexFormat.PARTICLE") || !rainRendererText.contains("GameRenderer::getParticleShader") || !rainRendererText.contains(".setLight(light)"))
   errors.add("forced-rain renderer no longer carries packed light through the 1.21.1 particle vertex path");
  if(!snowRendererText.contains("DefaultVertexFormat.PARTICLE") || !snowRendererText.contains("GameRenderer::getParticleShader") || !snowRendererText.contains("AgeWeatherPackedLightBridge.snowPacked") || !snowRendererText.contains(".setLight(light)"))
   errors.add("forced-snow renderer no longer applies Legacy packed-light brightening through the 1.21.1 particle vertex path");
  if(!rainRendererText.contains("lightTexture().turnOnLightLayer()") || !snowRendererText.contains("lightTexture().turnOnLightLayer()"))
   errors.add("forced weather renderers must enable the lightmap layer for PARTICLE vertices");
  if(!rainRendererText.contains("Tesselator.getInstance().begin(VertexFormat.Mode.QUADS") ||
     !rainRendererText.contains("DefaultVertexFormat.PARTICLE") ||
     !snowRendererText.contains("Tesselator.getInstance().begin(VertexFormat.Mode.QUADS") ||
     !snowRendererText.contains("DefaultVertexFormat.PARTICLE"))
   errors.add("forced weather renderer buffer creation drifted from Minecraft 1.21.1 Tesselator/BufferBuilder API");
  if(rainRendererText.contains("buildOrThrow()") || snowRendererText.contains("buildOrThrow()") ||
     !rainRendererText.contains("MeshData mesh = b.build()") || !snowRendererText.contains("MeshData mesh = b.build()"))
   errors.add("climate-filtered weather renderers must tolerate an empty per-type vertex buffer");
  String climateResolverText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeLegacyPrecipitationClimateResolver.java"));
  String temperatureMathText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacyBiomeTemperatureMath.java"));
  String precipitationBridgeText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgePrecipitationBridge.java"));
  String dimensionEffectsText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeDimensionSpecialEffects.java"));
  String precipitationTickerText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgePrecipitationTicker.java"));
  if(!temperatureMathText.contains("LEGACY_HEIGHT_THRESHOLD = 64") ||
     !temperatureMathText.contains("temperatureNoise * 4.0D") ||
     !temperatureMathText.contains("* 0.05F / 30.0F") ||
     !climateResolverText.contains("new LegacyRandomSource(1234L)") ||
     !climateResolverText.contains("AgeLegacyBiomeTemperatureMath.adjusted") ||
     !precipitationBridgeText.contains("precipitationAtControlledTemperature") ||
     !rainRendererText.contains("AgeLegacyPrecipitationClimateResolver.precipitation") ||
     !snowRendererText.contains("AgeLegacyPrecipitationClimateResolver.precipitation") ||
     !dimensionEffectsText.contains("AgeRainRenderer.render") ||
     !dimensionEffectsText.contains("AgeSnowRenderer.render") ||
     !precipitationTickerText.contains("AgeLegacyPrecipitationClimateResolver.precipitation"))
   errors.add("forced weather lost the 1.12 per-column base-temperature/altitude precipitation split");

  Path cloudRenderer=root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeCloudRenderer.java");
  String cloudRendererText=Files.readString(cloudRenderer);
  if(!cloudRendererText.contains("AgeCloudRenderLegacyMath.MIN") || !cloudRendererText.contains("AgeCloudRenderLegacyMath.UV_SCALE"))
   errors.add("custom cloud renderer lost Legacy fast-cloud grid/UV contract");
  if(!cloudRendererText.contains("GraphicsStatus.FAST") || !cloudRendererText.contains("renderFancy(") || !cloudRendererText.contains("AgeCloudFancyLegacyMath.TOP_EPSILON"))
   errors.add("custom cloud renderer lost Legacy fancy-cloud geometry selection/contract");

  Path skyRenderer=root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeSkyRenderer.java");
  String skyRendererText=Files.readString(skyRenderer);
  if(!skyRendererText.contains("Camera camera, float partialTick"))
   errors.add("AgeSkyRenderer must receive partialTick explicitly");
  Path dimEffects=root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeDimensionSpecialEffects.java");
  String dimEffectsText=Files.readString(dimEffects);
  if(!dimEffectsText.contains("Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,") ||
     !dimEffectsText.contains("boolean isFoggy, Runnable setupFog"))
   errors.add("renderSky drifted from NeoForge 21.1.x IDimensionSpecialEffectsExtension signature");
  if(!dimEffectsText.contains("PoseStack poseStack,double camX,double camY,double camZ,") ||
     !dimEffectsText.contains("Matrix4f modelViewMatrix,Matrix4f projectionMatrix"))
   errors.add("renderClouds drifted from NeoForge 21.1.x IDimensionSpecialEffectsExtension signature");
  if(!dimEffectsText.contains("float partialTick,LightTexture lightTexture,double camX,double camY,double camZ"))
   errors.add("renderSnowAndRain drifted from NeoForge 21.1.x IDimensionSpecialEffectsExtension signature");
  if(!dimEffectsText.contains("float partialTicks, float skyDarken,") ||
     !dimEffectsText.contains("float blockLightRedFlicker, float skyLight, int pixelX, int pixelY,") ||
     !dimEffectsText.contains("Vector3f colors"))
   errors.add("adjustLightmapColors drifted from NeoForge 21.1.x IDimensionSpecialEffectsExtension signature");
  if(dimEffectsText.contains("RenderSystem.getProjectionMatrix()"))
   errors.add("21.1.x renderSky must use the projection matrix supplied by the extension hook");
  if(!dimEffectsText.contains("camera, partialTick, level.getRainLevel(partialTick), level.getThunderLevel(partialTick), setupFog, entry.ageSeed()"))
   errors.add("renderSky must forward partialTick and live weather strengths to AgeSkyRenderer");
  if(!skyRendererText.contains("AgeSkyWeatherTintBridge.apply(skyColor,rainStrength,thunderStrength)"))
   errors.add("custom ColorSky lost Legacy rain/thunder attenuation");

  Path skyBridge=root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeSkyRenderBridge.java");
  String skyBridgeText=Files.readString(skyBridge);
  if(!skyBridgeText.contains("case RAINBOW") || !skyBridgeText.contains("1F,0F,0,new float[0]"))
   errors.add("rainbow must remain rain-independent like Legacy SymbolDoodadRainbow");

  String weatherDistanceText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeWeatherRenderDistanceContract.java"));
  if(!weatherDistanceText.contains("FAST_RADIUS = 5") || !weatherDistanceText.contains("FANCY_RADIUS = 10"))
   errors.add("weather renderer lost Legacy fast=5/fancy=10 radius contract");
  if(!dimEffectsText.contains("graphicsMode().get()!=GraphicsStatus.FAST"))
   errors.add("weather renderer no longer selects Legacy radius from graphics mode");

  String precipTickText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgePrecipitationTickBridge.java"));
  if(!precipTickText.contains("100.0F*strength*strength") || !precipTickText.contains("312987231L"))
   errors.add("precipitation tick bridge lost Legacy splash count/seed math");
  if(!precipTickText.contains("nextInt(LEGACY_RADIUS)-random.nextInt(LEGACY_RADIUS)"))
   errors.add("precipitation tick bridge lost Legacy triangular +/-10 offset sampling");
  if(!precipTickText.contains("particleSetting==1") || !precipTickText.contains("count>>1") || !precipTickText.contains("particleSetting>=2"))
   errors.add("precipitation tick bridge lost Legacy particle-setting reduction contract");
  String precipTickerText=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgePrecipitationTicker.java"));
  if(precipTickerText.contains("ParticleTypes.SNOWFLAKE"))
   errors.add("snow splash particles are not a Legacy addRainParticles behavior");
  if(!precipTickerText.contains("options.particles().get()") || !precipTickerText.contains("ParticleStatus.MINIMAL") || !precipTickerText.contains("ParticleStatus.DECREASED"))
   errors.add("precipitation ticker no longer maps Minecraft 1.21.1 particle quality into Legacy splash counts");
  if(!precipTickerText.contains("rainSoundCounter++") || !precipTickerText.contains("WEATHER_RAIN_ABOVE"))
   errors.add("precipitation ticker lost Legacy stateful rain sound cadence");
  if(!precipTickerText.contains("if (!Minecraft.useFancyGraphics()) strength /= 2.0F") ||
     !precipTickerText.contains("random.nextDouble()") ||
     !precipTickerText.contains("state.getShape(level, below)") ||
     !precipTickerText.contains("state.is(Blocks.MAGMA_BLOCK)") ||
     !precipTickerText.contains("AgePrecipitationSurfaceLegacyMath.rainParticleY") ||
     !precipTickerText.contains("AgePrecipitationSurfaceLegacyMath.rainSoundY") ||
     !precipTickerText.contains("AgePrecipitationSurfaceLegacyMath.hotSurfaceSmokeY"))
   errors.add("precipitation ticker lost 1.12 fancy-strength/double-RNG/block-shape/magma surface placement fidelity");

  String rainRenderer=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeRainRenderer.java"));
  String snowRenderer=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/client/AgeSnowRenderer.java"));
  if(!rainRenderer.contains("AgeWeatherColumnBoundsLegacyMath.lowerY") || !rainRenderer.contains("AgeWeatherColumnBoundsLegacyMath.lightSampleY")) errors.add("Rain renderer must use Legacy vertical bounds/light sample math");
  if(!snowRenderer.contains("AgeWeatherColumnBoundsLegacyMath.upperY") || !snowRenderer.contains("AgeWeatherColumnBoundsLegacyMath.lightSampleY")) errors.add("Snow renderer must use Legacy vertical bounds/light sample math");
  if(rainRenderer.contains("surface+20") || snowRenderer.contains("surface+20")) errors.add("Approximate surface+20 weather clamp must not return");

  if(!errors.isEmpty()) throw new AssertionError(String.join("\n",errors));
  System.out.println("Stage1CSourceInvariantHarness: PASS");
 }
}