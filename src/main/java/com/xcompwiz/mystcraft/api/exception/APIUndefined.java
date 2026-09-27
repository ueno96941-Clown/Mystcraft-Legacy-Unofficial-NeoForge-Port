package com.xcompwiz.mystcraft.api.exception;
public class APIUndefined extends RuntimeException { public APIUndefined(String api) { super("Mystcraft API undefined: " + api); } }
