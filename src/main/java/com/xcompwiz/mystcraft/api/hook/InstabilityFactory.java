package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.instability.IInstabilityProvider; import com.xcompwiz.mystcraft.api.world.logic.IEnvironmentalEffect;
public interface InstabilityFactory { IInstabilityProvider createProviderForEffect(Class<? extends IEnvironmentalEffect> effectclass,boolean uselevel,Object... itemCtorArgs); }
