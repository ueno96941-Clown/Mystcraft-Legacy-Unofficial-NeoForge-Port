package com.xcompwiz.mystcraft.api.impl;

import com.xcompwiz.mystcraft.api.APIInstanceProvider;
import com.xcompwiz.mystcraft.api.MystObjects;
import com.xcompwiz.mystcraft.api.client.ILinkPanelEffect;
import com.xcompwiz.mystcraft.api.hook.*;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiLinkPropertyRegistry;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiRenderContext;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiSymbolOverrides;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiSymbolView;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiWordRegistry;
import com.xcompwiz.mystcraft.api.instability.IInstabilityProvider;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import com.xcompwiz.mystcraft.api.symbol.BlockDescriptor;
import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol;
import com.xcompwiz.mystcraft.api.symbol.ModifierUtils;
import com.xcompwiz.mystcraft.api.util.Color;
import com.xcompwiz.mystcraft.api.util.ColorGradient;
import com.xcompwiz.mystcraft.api.word.DrawableWord;
import com.xcompwiz.mystcraft.api.word.WordData;
import com.xcompwiz.mystcraft.api.world.AgeDirector;
import com.xcompwiz.mystcraft.api.world.logic.IEnvironmentalEffect;
import com.xcompwiz.mystcraft.data.InkEffects;
import com.xcompwiz.mystcraft.grammar.GrammarExplorer;
import com.xcompwiz.mystcraft.grammar.GrammarRule;
import com.xcompwiz.mystcraft.grammar.GrammarRuleRegistry;
import com.xcompwiz.mystcraft.grammar.GrammarTree;
import com.xcompwiz.mystcraft.item.ItemPageContainer;
import com.xcompwiz.mystcraft.linking.LinkController;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.LegacySymbolId;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;
import com.xcompwiz.mystcraft.symbol.SymbolItemEconomy;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import com.xcompwiz.mystcraft.world.dimension.AgeDimensionKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Bridges the legacy public API contract onto the 1.21.1 runtime. */
public final class InternalAPI {
    private static final Map<String,Object> APIS = new LinkedHashMap<>();
    private static final Map<String,APIInstanceProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String,IInstabilityProvider> INSTABILITY = new LinkedHashMap<>();
    private static final List<ILinkPanelEffect> RENDER_EFFECTS = new ArrayList<>();
    private static boolean initialized;
    private static final Set<String> WARNED_COMPAT_STUBS = ConcurrentHashMap.newKeySet();

    private InternalAPI() {}

    public static synchronized void initAPI() {
        if (initialized) return;
        APIS.put("dimension", dimensionApi());
        APIS.put("grammar", grammarApi());
        APIS.put("instability", instabilityApi());
        APIS.put("instabilityfact", instabilityFactory());
        APIS.put("itemfact", itemFactory());
        APIS.put("linking", linkingApi());
        APIS.put("linkingprop", linkPropertyApi());
        APIS.put("page", pageApi());
        APIS.put("render", renderApi());
        APIS.put("symbol", symbolApi());
        APIS.put("symbolfact", symbolFactory());
        APIS.put("symbolvals", symbolValuesApi());
        APIS.put("word", wordApi());
        WordData.init((WordAPI) APIS.get("word"));
        MystObjects.entryPoint = InternalAPI::newProvider;
        initialized = true;
    }

    private static APIInstanceProvider newProvider() {
        // Modern FML no longer exposes a reliable active-container context at arbitrary API call sites.
        // Keep one provider per thread-context-neutral owner name; old ownerModid only affected symbol factory metadata.
        return PROVIDERS.computeIfAbsent("external", APIProviderImpl::new);
    }

    static boolean hasApi(String name) { initAPI(); return APIS.containsKey(name); }
    static Object api(String name) { initAPI(); return APIS.get(name); }
    public static List<ILinkPanelEffect> renderEffects() { synchronized (RENDER_EFFECTS) { return List.copyOf(RENDER_EFFECTS); } }

    private static void warnCompatStubOnce(String key, String message) {
        if (WARNED_COMPAT_STUBS.add(key)) {
            com.xcompwiz.mystcraft.Mystcraft.LOGGER.warn(message);
        }
    }

    private static MinecraftServer serverOrThrow() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("Mystcraft server API used without an active MinecraftServer");
        return server;
    }

    private static DimensionAPI dimensionApi() {
        return new DimensionAPI() {
            @Override public Collection<Integer> getAllAges() {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return List.of();
                return AgeRegistryData.get(server).all().stream().map(AgeRecord::ageUid).toList();
            }
            @Override public boolean isMystcraftAge(int dimId) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                return server != null && AgeRegistryData.get(server).byUid(dimId).isPresent();
            }
            @Override public int createAge() {
                MinecraftServer server = serverOrThrow();
                AgeRegistryData registry = AgeRegistryData.get(server);
                int uid = registry.allocateUid();
                long seed = server.overworld().getRandom().nextLong();
                AgeRecord record = new AgeRecord(uid, AgeDimensionKeys.levelKeyString(uid), "Age " + uid, seed, UUID.randomUUID());
                registry.put(record);
                AgeRegistryData.flush(server);
                return uid;
            }
        };
    }

    private static LinkingAPI linkingApi() {
        return new LinkingAPI() {
            @Override public boolean isLinkAllowed(Entity entity, ILinkInfo info) {
                if (entity == null || info == null || !(entity.level() instanceof ServerLevel source)) return false;
                return !NeoForge.EVENT_BUS.post(new com.xcompwiz.mystcraft.api.event.LinkEvent.LinkEventAllow(source, entity, info.clone())).isCanceled();
            }
            @Override public void linkEntity(Entity entity, ILinkInfo info) {
                if (entity != null && info != null && entity.level() instanceof ServerLevel source) LinkController.travelEntity(source, entity, info);
            }
            @Override public ILinkInfo createLinkInfoFromPosition(Level level, Entity location) {
                LinkInfoAdapter info = LinkInfoAdapter.detached(new CompoundTag());
                if (level == null) return info;
                MinecraftServer server = level.getServer();
                if (level.dimension() == Level.OVERWORLD) info.setDimensionUID(0);
                else if (level.dimension() == Level.NETHER) info.setDimensionUID(-1);
                else if (level.dimension() == Level.END) info.setDimensionUID(1);
                else if (server != null) AgeManager.resolveByLevel(server, level.dimension()).ifPresent(age -> {
                    info.setDimensionUID(age.ageUid());
                    info.setTargetUUID(age.uuid());
                    info.setDisplayName(age.ageName());
                });
                if (location != null) {
                    info.setSpawn(location.blockPosition());
                    info.setSpawnYaw(location.getYRot());
                }
                if ("???".equals(info.getDisplayName())) info.setDisplayName(level.dimension().location().toString());
                return info;
            }
            @Override public ILinkInfo createLinkInfo(CompoundTag tag) { return LinkInfoAdapter.detached(tag); }
        };
    }

    private static PageAPI pageApi() {
        return new PageAPI() {
            @Override public boolean hasLinkPanel(ItemStack page) { return Page.isLinkPanel(page); }
            @Override public Collection<String> getPageLinkProperties(ItemStack page) { return Page.getLinkProperties(page); }
            @Override public boolean isPageWritable(ItemStack page) { return Page.isBlank(page); }
            @Override public ResourceLocation getPageSymbol(ItemStack page) { return Page.getSymbol(page); }
            @Override public void setPageSymbol(ItemStack page, ResourceLocation symbol) { Page.setSymbol(page, symbol); }
        };
    }

    private static GrammarAPI grammarApi() {
        return new GrammarAPI() {
            @Override public void registerGrammarRule(ResourceLocation parent, Integer rank, ResourceLocation... args) {
                if (parent == null) return;
                List<String> rhs = new ArrayList<>();
                if (args != null) for (ResourceLocation arg : args) if (arg != null) rhs.add(arg.toString());
                GrammarRuleRegistry.registerExternalRule(new GrammarRule(parent.toString(), rhs, rank));
            }
            @Override public Collection<IAgeSymbol> getSymbolsExpandingToken(ResourceLocation token) {
                if (token == null) return List.of();
                LinkedHashSet<IAgeSymbol> result = new LinkedHashSet<>();
                for (GrammarRule rule : GrammarRuleRegistry.rulesFor(token.toString())) {
                    for (String value : rule.values()) {
                        ResourceLocation key = LegacySymbolId.canonicalKey(value);
                        ApiSymbolView view = key == null ? null : ApiSymbolView.resolve(key);
                        if (view != null) result.add(view);
                    }
                }
                return List.copyOf(result);
            }
            @Override public Collection<ResourceLocation> getTokensProducingToken(ResourceLocation token) {
                if (token == null) return List.of();
                LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
                for (GrammarRule rule : GrammarRuleRegistry.parentRulesFor(token.toString())) {
                    ResourceLocation key = LegacySymbolId.canonicalKey(rule.parent());
                    if (key != null) result.add(key);
                }
                return List.copyOf(result);
            }
            @Override public List<ResourceLocation> generateFromToken(ResourceLocation root, Random rand) {
                if (root == null) return List.of();
                return toKeys(GrammarExplorer.explore(root.toString(), rand == null ? new Random() : rand));
            }
            @Override public List<ResourceLocation> generateFromToken(ResourceLocation root, Random rand, List<ResourceLocation> written) {
                if (root == null) return List.of();
                Random use = rand == null ? new Random() : rand;
                GrammarTree tree = new GrammarTree(root.toString());
                List<String> terminals = written == null ? List.of() : written.stream().filter(Objects::nonNull).map(ResourceLocation::toString).toList();
                tree.parseTerminals(terminals, use);
                return toKeys(tree.getExpanded(use));
            }
            private List<ResourceLocation> toKeys(List<String> values) {
                ArrayList<ResourceLocation> result = new ArrayList<>();
                for (String value : values) {
                    ResourceLocation key = LegacySymbolId.canonicalKey(value);
                    if (key != null) result.add(key);
                }
                return List.copyOf(result);
            }
        };
    }

    private static ItemFactory itemFactory() {
        return new ItemFactory() {
            @Override public ItemStack buildPage() { return Page.createPage(); }
            @Override public ItemStack buildSymbolPage(ResourceLocation identifier) {
                String legacy = ApiSymbolView.legacyIdFor(identifier);
                return legacy == null ? Page.createSymbolPage(identifier) : Page.createSymbolPage(legacy);
            }
            @Override public ItemStack buildLinkPage(String... properties) {
                ItemStack page = Page.createLinkPage();
                if (properties != null) for (String property : properties) if (property != null) Page.addLinkProperty(page, property);
                return page;
            }
            @Override public ItemStack buildCollectionItem(String name, ResourceLocation... tokens) {
                LinkedHashSet<IAgeSymbol> symbols = new LinkedHashSet<>();
                GrammarAPI grammar = (GrammarAPI) APIS.get("grammar");
                if (tokens != null) for (ResourceLocation token : tokens) if (token != null) symbols.addAll(grammar.getSymbolsExpandingToken(token));
                List<IAgeSymbol> sorted = symbols.stream().sorted(Comparator.comparing(IAgeSymbol::getLocalizedName, String.CASE_INSENSITIVE_ORDER)).toList();
                ItemStack portfolio = new ItemStack(MystItems.PORTFOLIO.get());
                ItemPageContainer container = (ItemPageContainer) portfolio.getItem();
                container.setLegacyName(portfolio, name);
                ArrayList<ItemStack> pages = new ArrayList<>();
                for (IAgeSymbol symbol : sorted) {
                    String legacy = ApiSymbolView.legacyIdFor(symbol.getRegistryName());
                    pages.add(legacy == null ? Page.createSymbolPage(symbol.getRegistryName()) : Page.createSymbolPage(legacy));
                }
                container.setPages(portfolio, pages);
                return portfolio;
            }
            @Override public ItemStack buildCollectionItem(String name, ItemStack... pages) {
                ItemStack portfolio = new ItemStack(MystItems.PORTFOLIO.get());
                ItemPageContainer container = (ItemPageContainer) portfolio.getItem();
                container.setLegacyName(portfolio, name);
                ArrayList<ItemStack> accepted = new ArrayList<>();
                if (pages != null) for (ItemStack page : pages) if (page != null && page.is(MystItems.PAGE.get())) accepted.add(page.copyWithCount(1));
                container.setPages(portfolio, accepted);
                return portfolio;
            }
        };
    }

    private static LinkPropertyAPI linkPropertyApi() {
        return new LinkPropertyAPI() {
            @Override public void registerLinkProperty(String identifier, Color color) { ApiLinkPropertyRegistry.registerProperty(identifier, color); }
            @Override public Collection<String> getLinkProperties() {
                LinkedHashSet<String> result = new LinkedHashSet<>(List.of(
                        FLAG_INTRA_LINKING, FLAG_INTRA_LINKING_ONLY, FLAG_RELATIVE, FLAG_DISARM,
                        FLAG_MAINTAIN_MOMENTUM, FLAG_GENERATE_PLATFORM, FLAG_NATURAL, FLAG_EXTERNAL,
                        FLAG_OFFENSIVE, FLAG_TPCOMMAND, FLAG_FOLLOWING, PROP_SOUND));
                result.addAll(ApiLinkPropertyRegistry.colors().keySet());
                return List.copyOf(result);
            }
            @Override public Color getLinkPropertyColor(String identifier) { return new Color(InkEffects.getPropertyColor(identifier)); }
            @Override public ColorGradient getPropertiesGradient(Map<String, Float> properties) {
                ColorGradient gradient = new ColorGradient();
                float total = 0F;
                if (properties != null) for (var entry : properties.entrySet()) {
                    float probability = entry.getValue() == null ? 0F : entry.getValue();
                    if (probability < .001F) continue;
                    float interval = probability;
                    total += interval;
                    Color color = getLinkPropertyColor(entry.getKey());
                    if (interval > .3F) { gradient.pushColor(color, interval-.3F); interval=.3F; }
                    gradient.pushColor(color, interval);
                }
                if (total < .99F) {
                    float interval = 1F-total;
                    Color black = new Color(0F,0F,0F);
                    if (interval > .3F) { gradient.pushColor(black, interval-.3F); interval=.3F; }
                    gradient.pushColor(black, interval);
                }
                return gradient;
            }
            @Override public void addPropertyToItem(ItemStack stack, String property, float probability) { ApiLinkPropertyRegistry.add(stack, property, probability); }
            @Override public void addPropertyToItem(String name, String property, float probability) { ApiLinkPropertyRegistry.add(name, property, probability); }
            @Override public void addPropertyToItem(Item item, String property, float probability) { ApiLinkPropertyRegistry.add(item, property, probability); }
            @Override public Map<String, Float> getPropertiesForItem(ItemStack stack) {
                Map<String,Float> result = InkEffects.getItemEffects(stack);
                return result == null ? Map.of() : result;
            }
        };
    }

    private static SymbolAPI symbolApi() {
        return new SymbolAPI() {
            @Override public void blacklistIdentifier(ResourceLocation identifier) {
                String legacy = ApiSymbolView.legacyIdFor(identifier);
                if (legacy != null) SymbolAvailability.blacklist(legacy);
                GrammarRuleRegistry.rebuildForDynamicSymbols();
            }
            @Override public List<IAgeSymbol> getAllRegisteredSymbols() { return ApiSymbolView.all(); }
            @Override public IAgeSymbol getSymbol(ResourceLocation identifier) { return ApiSymbolView.resolve(identifier); }
            @Override public String getSymbolOwner(ResourceLocation identifier) {
                String legacy = ApiSymbolView.legacyIdFor(identifier);
                if (legacy == null) return null;
                int split = legacy.indexOf(':');
                return split < 0 ? "mystcraft" : legacy.substring(0, split);
            }
        };
    }

    private static SymbolValuesAPI symbolValuesApi() {
        return new SymbolValuesAPI() {
            private String id(IAgeSymbol symbol) {
                if (symbol == null) return null;
                if (symbol instanceof ApiSymbolView view) return view.legacyId();
                return ApiSymbolView.legacyIdFor(symbol.getRegistryName());
            }
            @Override public void setSymbolCardRank(IAgeSymbol symbol, int rank) { String id=id(symbol); if(id!=null) ApiSymbolOverrides.setCardRank(id,rank); }
            @Override public void setSymbolIsPurchasable(IAgeSymbol symbol, boolean flag) { String id=id(symbol); if(id!=null) ApiSymbolOverrides.setTradable(id,flag); }
            @Override public float getSymbolItemWeight(ResourceLocation identifier) { String id=ApiSymbolView.legacyIdFor(identifier); return id==null?0F:SymbolItemEconomy.itemWeight(id); }
            @Override public boolean getSymbolIsTradable(ResourceLocation identifier) {
                String id=ApiSymbolView.legacyIdFor(identifier);
                return id != null && ApiSymbolOverrides.tradable(id, SymbolItemEconomy.cardRank(id)>=0);
            }
            @Override public void setSymbolTradeItem(IAgeSymbol symbol, ItemStack stack) { setSymbolTradeItems(symbol,stack,ItemStack.EMPTY); }
            @Override public void setSymbolTradeItems(IAgeSymbol symbol, ItemStack primary, ItemStack secondary) { String id=id(symbol); if(id!=null) ApiSymbolOverrides.setTradeItems(id,primary,secondary); }
            @Override public List<ItemStack> getSymbolTradeItems(ResourceLocation identifier) {
                String id=ApiSymbolView.legacyIdFor(identifier);
                if(id==null) return List.of();
                List<ItemStack> custom=ApiSymbolOverrides.tradeItems(id);
                return custom.isEmpty() && getSymbolIsTradable(identifier) ? List.of(new ItemStack(Items.EMERALD)) : custom;
            }
        };
    }

    private static WordAPI wordApi() {
        return new WordAPI() {
            @Override public void registerWord(String name, DrawableWord word) { ApiWordRegistry.register(name,word); }
            @Override public void registerWord(String name, Integer[] components) { ApiWordRegistry.register(name,new DrawableWord(components)); }
        };
    }

    private static InstabilityAPI instabilityApi() {
        return new InstabilityAPI() {
            @Override public boolean registerInstability(String identifier, IInstabilityProvider provider, int activationcost) {
                if(identifier==null||identifier.isBlank()||provider==null||INSTABILITY.containsKey(identifier)) return false;
                INSTABILITY.put(identifier,provider);
                warnCompatStubOnce("instability-register",
                        "Legacy InstabilityAPI.registerInstability is registry-compatible only in this port; external providers are discoverable through the API but are not wired into the built-in 0.13.7.06 deck runtime yet.");
                return true;
            }
            @Override public void addCards(String deck, String identifier, int count) {
                warnCompatStubOnce("instability-addcards",
                        "Legacy InstabilityAPI.addCards is currently a compatibility no-op; external cards are not inserted into the built-in Instability decks.");
            }
            @Override public Collection<String> getAllInstabilityProviders() { return List.copyOf(INSTABILITY.keySet()); }
            @Override public IInstabilityProvider getInstabilityProvider(String identifier) { return INSTABILITY.get(identifier); }
        };
    }

    private static InstabilityFactory instabilityFactory() {
        return (effectclass, uselevel, itemCtorArgs) -> new IInstabilityProvider() {
            @Override public void addEffects(com.xcompwiz.mystcraft.api.instability.InstabilityDirector controller, Integer level) {
                warnCompatStubOnce("instability-factory",
                        "Legacy InstabilityFactory-created providers are compatibility stubs in this port; built-in 0.13.7.06 providers are live, external reflective effects are not instantiated.");
            }
            @Override public String toString(){return "CompatibilityInstabilityProvider["+(effectclass==null?"null":effectclass.getName())+"]";}
        };
    }

    private static SymbolFactory symbolFactory() {
        return new SymbolFactory() {
            @Override public IAgeSymbol createSymbol(String ownerModid, BlockState blockState, String thirdword, int rank, SymbolFactory.CategoryPair... categories) { return createSymbol(blockState,thirdword,rank,categories); }
            @Override public IAgeSymbol createSymbol(BlockState blockState, String thirdword, int rank, SymbolFactory.CategoryPair... categories) {
                if(blockState==null) return null;
                ResourceLocation blockId=BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                ResourceLocation registryName=ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "modmat_"+blockId.getPath()+"_0");
                return new IAgeSymbol() {
                    @Override public ResourceLocation getRegistryName(){return registryName;}
                    @Override public void registerLogic(AgeDirector controller,long seed){
                        BlockDescriptor descriptor=new BlockDescriptor(blockState);
                        if(categories!=null) for(SymbolFactory.CategoryPair pair:categories) if(pair!=null&&pair.category()!=null) descriptor.setUsable(pair.category(),true);
                        ModifierUtils.pushBlock(controller,descriptor);
                    }
                    @Override public int instabilityModifier(int count){return 0;}
                    @Override public boolean generatesConfigOption(){return false;}
                    @Override public String getLocalizedName(){return blockState.getBlock().getName().getString();}
                    @Override public String[] getPoem(){return new String[]{"Transform","Constraint",thirdword==null?"Material":thirdword,blockId.getPath()};}
                };
            }
        };
    }

    private static RenderAPI renderApi() {
        return new RenderAPI() {
            @Override public void registerRenderEffect(ILinkPanelEffect renderer) { if(renderer!=null) synchronized(RENDER_EFFECTS){RENDER_EFFECTS.add(renderer);} }
            @Override public void drawWord(float x,float y,float zLevel,float scale,String word) { var r=ApiRenderContext.current(); if(r!=null) r.drawWord(x,y,zLevel,scale,word); }
            @Override public void drawSymbol(float x,float y,float zLevel,float scale,ResourceLocation identifier) { var r=ApiRenderContext.current(); if(r!=null) r.drawSymbol(x,y,zLevel,scale,identifier); }
            @Override public void drawColorEye(float x,float y,float zLevel,float radius,Color color) { var r=ApiRenderContext.current(); if(r!=null) r.drawColorEye(x,y,zLevel,radius,color); }
        };
    }
}
