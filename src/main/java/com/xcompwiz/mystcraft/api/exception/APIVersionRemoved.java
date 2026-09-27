package com.xcompwiz.mystcraft.api.exception;
public class APIVersionRemoved extends RuntimeException { public APIVersionRemoved(String api) { super("Mystcraft API version removed: " + api); } }
