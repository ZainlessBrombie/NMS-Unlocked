package com.zainlessbrombie.mc.nmsunlocked.data;

/**
 * Created by mathis on 03.03.18 22:01.
 */
public class PluginData { // currently not in use
    private String versionString;
    private int classesProcessed;

    public PluginData(String versionString, int classesProcessed) {
        this.versionString = versionString;
        this.classesProcessed = classesProcessed;
    }

    public String getVersionString() {
        return versionString;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    public int getClassesProcessed() {
        return classesProcessed;
    }

    public void setClassesProcessed(String classesProcessed) {
        this.classesProcessed = Integer.parseInt(classesProcessed);
    }
}
