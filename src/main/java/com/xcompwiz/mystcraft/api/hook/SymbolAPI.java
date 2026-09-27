package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol; import net.minecraft.resources.ResourceLocation; import java.util.List;
public interface SymbolAPI { void blacklistIdentifier(ResourceLocation identifier); List<IAgeSymbol> getAllRegisteredSymbols(); IAgeSymbol getSymbol(ResourceLocation identifier); String getSymbolOwner(ResourceLocation identifier); }
