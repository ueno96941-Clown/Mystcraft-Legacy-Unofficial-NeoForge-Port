package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.instability.IInstabilityProvider; import java.util.Collection;
public interface InstabilityAPI { boolean registerInstability(String identifier,IInstabilityProvider provider,int activationcost); void addCards(String deck,String identifier,int count); Collection<String> getAllInstabilityProviders(); IInstabilityProvider getInstabilityProvider(String identifier); }
