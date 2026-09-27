package com.xcompwiz.mystcraft.api.symbol;
import com.xcompwiz.mystcraft.api.world.AgeDirector;
import net.minecraft.resources.ResourceLocation;
/** Modern source-compatible equivalent of the 0.13.7.06 Age Symbol contract. */
public interface IAgeSymbol {
    ResourceLocation getRegistryName();
    void registerLogic(AgeDirector controller,long seed);
    int instabilityModifier(int count);
    boolean generatesConfigOption();
    String getLocalizedName();
    String[] getPoem();
    /** Legacy Forge-registry compatibility helper retained for source/API parity. */
    default Class<IAgeSymbol> getRegistryType() { return IAgeSymbol.class; }
}
