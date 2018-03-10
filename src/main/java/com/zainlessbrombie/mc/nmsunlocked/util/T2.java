package com.zainlessbrombie.mc.nmsunlocked.util;

/**
 * Created by mathis on 10.03.18 11:57.
 */
public class T2<TA,TB> {
    private TA o1;
    private TB o2;

    public T2(TA o1, TB o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    public TA getO1() {
        return o1;
    }

    public void setO1(TA o1) {
        this.o1 = o1;
    }

    public TB getO2() {
        return o2;
    }

    public void setO2(TB o2) {
        this.o2 = o2;
    }
}
