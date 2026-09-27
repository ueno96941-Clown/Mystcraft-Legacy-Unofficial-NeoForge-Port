package com.xcompwiz.mystcraft.api;

import com.xcompwiz.mystcraft.api.exception.APIUndefined;
import com.xcompwiz.mystcraft.api.exception.APIVersionRemoved;
import com.xcompwiz.mystcraft.api.exception.APIVersionUndefined;

/** Compatibility entry point retained from the 0.13.7.06 public API. */
public interface APIInstanceProvider {
    interface EntryPoint { APIInstanceProvider getProviderInstance(); }
    Object getAPIInstance(String api) throws APIUndefined, APIVersionUndefined, APIVersionRemoved;
}
