package com.xcompwiz.mystcraft.api.world.logic;
import com.xcompwiz.mystcraft.api.util.Color; import net.minecraft.world.entity.Entity; import net.minecraft.world.level.biome.Biome;
public interface IDynamicColorProvider { String CLOUD="cloud",FOG="fog",SKY="sky"; Color getColor(Entity entity,Biome biome,float time,float celestialAngle,float partialTick); }
