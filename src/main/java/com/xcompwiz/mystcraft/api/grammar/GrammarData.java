package com.xcompwiz.mystcraft.api.grammar;

import com.xcompwiz.mystcraft.api.MystObjects;
import com.xcompwiz.mystcraft.api.symbol.BlockCategory;
import net.minecraft.resources.ResourceLocation;

/** Stable grammar-token names exposed by the 0.13.7.06 API. */
public final class GrammarData {
    public static final ResourceLocation BIOME=asMyst("Biome"), BIOME_LIST=asMyst("Biomes"), BIOMECONTROLLER=asMyst("BiomeController"),
            LIGHTING=asMyst("Lighting"), WEATHER=asMyst("Weather"), TERRAIN=asMyst("TerrainGen"), VISUAL_EFFECT=asMyst("Visual"),
            FEATURE_SMALL=asMyst("FeatureSmall"), FEATURE_MEDIUM=asMyst("FeatureMedium"), FEATURE_LARGE=asMyst("FeatureLarge"),
            EFFECT=asMyst("Effect"), SUN=asMyst("Sun"), MOON=asMyst("Moon"), STARFIELD=asMyst("Starfield"), DOODAD=asMyst("Doodad"),
            SUNSET_UNCOMMON=asMyst("SunsetUncommon"), SUNSET=asMyst("Sunset"), ANGLE_SEQ=asMyst("Angle"), PERIOD_SEQ=asMyst("Period"),
            PHASE_SEQ=asMyst("Phase"), COLOR_SEQ=asMyst("Color"), GRADIENT_SEQ=asMyst("Gradient"), ANGLE_BASIC=asMyst("AngleBasic"),
            PERIOD_BASIC=asMyst("PeriodBasic"), PHASE_BASIC=asMyst("PhaseBasic"), COLOR_BASIC=asMyst("ColorBasic"), GRADIENT_BASIC=asMyst("GradientBasic");
    @Deprecated public static final ResourceLocation TERRAINALT=FEATURE_LARGE;
    @Deprecated public static final ResourceLocation POPULATOR=FEATURE_MEDIUM;
    public static final ResourceLocation BLOCK_TERRAIN=BlockCategory.TERRAIN.getGrammarBinding(), BLOCK_SOLID=BlockCategory.SOLID.getGrammarBinding(),
            BLOCK_STRUCTURE=BlockCategory.STRUCTURE.getGrammarBinding(), BLOCK_ORGANIC=BlockCategory.ORGANIC.getGrammarBinding(),
            BLOCK_CRYSTAL=BlockCategory.CRYSTAL.getGrammarBinding(), BLOCK_SEA=BlockCategory.SEA.getGrammarBinding(),
            BLOCK_FLUID=BlockCategory.FLUID.getGrammarBinding(), BLOCK_GAS=BlockCategory.GAS.getGrammarBinding(), BLOCK_ANY=BlockCategory.ANY.getGrammarBinding();
    private static ResourceLocation asMyst(String path){return ResourceLocation.fromNamespaceAndPath(MystObjects.MystcraftModId,path);}
    private GrammarData(){}
}
