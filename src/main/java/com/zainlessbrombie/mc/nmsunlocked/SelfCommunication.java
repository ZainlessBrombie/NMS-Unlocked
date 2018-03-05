package com.zainlessbrombie.mc.nmsunlocked;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mathis on 03.03.18 21:26.
 */
public class SelfCommunication { //they are not in the same classloader

    public static int updated = 0; //=> ^ has to be public ^
    public static String versionString;

    public static final Object lock = new Object();

    public static final List<String> prefixesToChange = new ArrayList<>();
    public static final List<String> prefixesToBlock = new ArrayList<>();

    public final static List<String> modified = new ArrayList<>();

}
