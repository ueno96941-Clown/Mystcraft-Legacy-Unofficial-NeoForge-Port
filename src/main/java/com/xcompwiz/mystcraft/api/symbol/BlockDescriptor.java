package com.xcompwiz.mystcraft.api.symbol;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap; import java.util.Map;
public final class BlockDescriptor {
    public final BlockState blockstate; private final Map<net.minecraft.resources.ResourceLocation,Boolean> usable=new HashMap<>();
    public BlockDescriptor(BlockState state){this.blockstate=state;} public BlockDescriptor(Block block){this(block.defaultBlockState());}
    public void setUsable(BlockCategory key,boolean flag){if(key==null||key==BlockCategory.ANY)return;usable.put(key.getName(),flag);} public boolean isUsable(BlockCategory key){if(key==null||key==BlockCategory.ANY)return true;return usable.getOrDefault(key.getName(),false);}
}
