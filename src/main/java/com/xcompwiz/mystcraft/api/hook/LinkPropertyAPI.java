package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.util.Color; import com.xcompwiz.mystcraft.api.util.ColorGradient; import net.minecraft.world.item.Item; import net.minecraft.world.item.ItemStack; import java.util.*;
public interface LinkPropertyAPI {
 String FLAG_INTRA_LINKING="Intra Linking",FLAG_INTRA_LINKING_ONLY="Intra Linking Only",FLAG_RELATIVE="Relative",FLAG_DISARM="Disarm",FLAG_MAINTAIN_MOMENTUM="Maintain Momentum",FLAG_GENERATE_PLATFORM="Generate Platform",FLAG_NATURAL="Natural",FLAG_EXTERNAL="External",FLAG_OFFENSIVE="Offensive",FLAG_TPCOMMAND="Op-TP",FLAG_FOLLOWING="Following",PROP_SOUND="Sound";
 void registerLinkProperty(String identifier,Color color); Collection<String> getLinkProperties(); Color getLinkPropertyColor(String identifier); ColorGradient getPropertiesGradient(Map<String,Float> properties); void addPropertyToItem(ItemStack stack,String property,float probability); void addPropertyToItem(String name,String property,float probability); void addPropertyToItem(Item item,String property,float probability); Map<String,Float> getPropertiesForItem(ItemStack stack);
}
