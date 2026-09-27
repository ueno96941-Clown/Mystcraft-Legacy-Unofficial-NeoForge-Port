package com.xcompwiz.mystcraft.api.symbol;
import net.minecraft.resources.ResourceLocation;
import java.util.Collection; import java.util.LinkedHashMap; import java.util.Map; import java.util.List;
public final class BlockCategory {
    private static final Map<ResourceLocation,BlockCategory> CATEGORIES=new LinkedHashMap<>();
    public static final BlockCategory ANY=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockAny"));
    public static final BlockCategory SOLID=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockSolid"));
    public static final BlockCategory FLUID=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockFluid"));
    public static final BlockCategory GAS=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockGas"));
    public static final BlockCategory TERRAIN=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockTerrain"));
    public static final BlockCategory STRUCTURE=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockStructure"));
    public static final BlockCategory ORGANIC=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockOrganic"));
    public static final BlockCategory CRYSTAL=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockCrystal"));
    public static final BlockCategory SEA=registerBlockCategory(ResourceLocation.fromNamespaceAndPath("mystcraft","BlockSea"));
    private final ResourceLocation name; private BlockCategory(ResourceLocation n){name=n;}
    public static synchronized BlockCategory registerBlockCategory(ResourceLocation n){return CATEGORIES.computeIfAbsent(n,BlockCategory::new);} public static BlockCategory getBlockCategory(ResourceLocation n){return CATEGORIES.get(n);} public static Collection<BlockCategory> getCategories(){return List.copyOf(CATEGORIES.values());}
    public ResourceLocation getName(){return name;} public ResourceLocation getGrammarBinding(){return name;}
}
