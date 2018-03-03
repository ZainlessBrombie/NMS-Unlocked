package com.zainlessbrombie.mc.crossclass;

import net.minecraft.server.v1_11_R1.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;

/**
 * Created by mathis on 03.03.18 00:42.
 */
public class SampleLoad {
    static {
        System.out.println("LOADED SAMPLE");
        CraftPlayer player = null;
        if(player != null)
            player.chat(null);
    }

    public static void test123() {
        PacketPlayOutChat chat = new PacketPlayOutChat();
    }
}
