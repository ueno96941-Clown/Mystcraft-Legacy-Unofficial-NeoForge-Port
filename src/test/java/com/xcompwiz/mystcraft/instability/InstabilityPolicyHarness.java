package com.xcompwiz.mystcraft.instability;
public final class InstabilityPolicyHarness {
 public static void main(String[] args) {
  if (InstabilityPolicy.runtimeEnabled()) throw new AssertionError("runtime instability enabled");
  if (InstabilityPolicy.effectiveScore(Integer.MAX_VALUE)!=0) throw new AssertionError("effective instability nonzero");
  if (InstabilityPolicy.allowProvider("mystcraft:anything")) throw new AssertionError("provider allowed");
  if (InstabilityPolicy.DECAY_ENABLED) throw new AssertionError("decay enabled");
  System.out.println("InstabilityPolicyHarness: PASS");
 }
}
