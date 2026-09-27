package com.xcompwiz.mystcraft.api.hook;
import java.util.Collection;
public interface DimensionAPI { Collection<Integer> getAllAges(); boolean isMystcraftAge(int dimId); int createAge(); }
