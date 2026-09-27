package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.word.DrawableWord;
public interface WordAPI { void registerWord(String name,DrawableWord word); void registerWord(String name,Integer[] components); }
