package com.xcompwiz.mystcraft.api.impl;

import com.xcompwiz.mystcraft.api.APIInstanceProvider;
import com.xcompwiz.mystcraft.api.exception.APIUndefined;
import com.xcompwiz.mystcraft.api.exception.APIVersionRemoved;
import com.xcompwiz.mystcraft.api.exception.APIVersionUndefined;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime provider for the 0.13.7.06 API-v1 surface. */
public final class APIProviderImpl implements APIInstanceProvider {
    private final String ownerMod;
    private final Map<String,Object> instances = new LinkedHashMap<>();

    public APIProviderImpl(String ownerMod) { this.ownerMod = ownerMod == null ? "unknown" : ownerMod; }
    public String getOwnerMod() { return ownerMod; }

    @Override
    public synchronized Object getAPIInstance(String api) throws APIUndefined, APIVersionUndefined, APIVersionRemoved {
        Object existing = instances.get(api);
        if (existing != null) return existing;
        if (api == null) throw new APIUndefined("null");
        int split = api.lastIndexOf('-');
        if (split <= 0 || split == api.length()-1) throw new APIUndefined(api);
        String name = api.substring(0, split);
        final int version;
        try { version = Integer.parseInt(api.substring(split+1)); }
        catch (NumberFormatException bad) { throw new APIUndefined(api); }
        if (!InternalAPI.hasApi(name)) throw new APIUndefined(name);
        if (version != 1) throw new APIVersionUndefined(api);
        Object created = InternalAPI.api(name);
        if (created == null) throw new APIVersionRemoved(api);
        instances.put(api, created);
        return created;
    }
}
