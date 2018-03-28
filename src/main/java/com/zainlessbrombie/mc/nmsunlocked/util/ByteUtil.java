package com.zainlessbrombie.mc.nmsunlocked.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ZainlessBrombie on 10.03.18 11:57.
 */
public class ByteUtil { // reading form stream and printing bytes to console
    public static byte[] readAllFromStream(InputStream inputStream) throws IOException { // i love java. most of the time.
        byte [] ret = new byte[inputStream.available()];
        int pointer = 0;
        int read;
        while((read = inputStream.read(ret,pointer,ret.length - pointer)) > 0) {
            pointer += read;
        }
        return ret;
    }

    public static synchronized void printBytes(byte[] toPrint) { //synchronized in order to avoid simultaneous System.out.println() calls
        StringBuilder builder = new StringBuilder(toPrint.length * 3);
        boolean redmode = false;
        for (byte aToPrint : toPrint) {
            if (validChar((char) aToPrint)) {
                if (!redmode) {
                    builder.append(ConsoleColors.RED_BRIGHT);
                    redmode = true;
                }
                builder.append((char) aToPrint);
            } else {
                if (redmode) {
                    builder.append(ConsoleColors.RESET);
                    redmode = false;
                }
                builder.append("[").append(aToPrint).append("]");
            }
        }
        if(redmode)
            builder.append(ConsoleColors.RESET);
        System.out.println(builder);
    }

    private static boolean validChar(char c) {
        int min = 0x20;
        int max = 0x7e;
        return c >= min && c <= max || c == 0xa;
    }
}
