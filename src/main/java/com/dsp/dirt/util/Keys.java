package com.dsp.dirt.util;

public final class Keys {
    private Keys() {}

    // Count types
    public static final String T  = "T";   // (p,slot,w)
    public static final String PS = "PS";  // (p,slot,*) total
    public static final String SW = "SW";  // (*,slot,w) total
    public static final String S  = "S";   // (*,slot,*) total

    // Slots
    public static final String X = "X";
    public static final String Y = "Y";

    // Encoding: join with tab for Text keys
    public static String k3(String a, String b, String c) { return a + "\t" + b + "\t" + c; }
    public static String k4(String a, String b, String c, String d) { return a + "\t" + b + "\t" + c + "\t" + d; }
}
