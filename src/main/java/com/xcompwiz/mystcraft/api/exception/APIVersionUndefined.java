package com.xcompwiz.mystcraft.api.exception;
public class APIVersionUndefined extends RuntimeException { public APIVersionUndefined(String api) { super("Mystcraft API version undefined: " + api); } }
