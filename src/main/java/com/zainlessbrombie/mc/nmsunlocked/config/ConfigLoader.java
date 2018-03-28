package com.zainlessbrombie.mc.nmsunlocked.config;

import com.zainlessbrombie.mc.nmsunlocked.Main;
import com.zainlessbrombie.mc.nmsunlocked.SelfCommunication;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by ZainlessBrombie on 10.03.18 12:14.
 */
public class ConfigLoader {

    private static Logger staticLog = Logger.getLogger("NMSUnlocked");

    /**
     * This method must not be called from a PluginClassLoader loaded class before being called from the parent, or else SelfCommunication will be loaded for the PluginClassLoader and the parent class loader separately.
     */
    public static void loadConfig() {
        try {
            File pluginFolder = new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile(),"nmsUnlocked");
            pluginFolder.mkdir();
            File confFile = new File(pluginFolder,"conf.txt");
            List<String> configLines = Arrays.asList("#config file for NMSUnlocked. All lines starting with 'prefix: ' (note the whitespace) add a qualifying prefix.",
                    "#if a class that is being loaded matches at least one of the listed prefixes, all non string occurrences of version strings will be changed.",
                    "#example:",
                    "#prefix: com.aplugin",
                    "#a line with only 'prefix: ' will match any and all classes. If you encounter no issues, you could leave it at that.",
                    "prefix: ",
                    "",
                    "#if you don't want a certain plugin to be changed, you can simply add it with prefixdeny. The prefixes java, javax, sun, org.bukkit and net.minecraft are always blocked for performance reasons (also, why should they be changed ;) )",
                    "#prefixdeny: "
            ); // default config
            if(!confFile.exists())
                Files.write(confFile.toPath(),configLines, StandardOpenOption.CREATE);
            else
                configLines = Files.readAllLines(confFile.toPath());
            synchronized (SelfCommunication.lock) {
                SelfCommunication.prefixesToBlock.clear();
                SelfCommunication.prefixesToChange.clear();
                configLines.stream()
                        .filter(line -> line.startsWith("prefix: "))
                        .map(line -> line.substring("prefix: ".length()).replace('.', '/'))
                        .forEach(SelfCommunication.prefixesToChange::add);
                configLines.stream()
                        .filter(line -> line.startsWith("prefixdeny :"))
                        .map(line -> line.substring("prefixdeny: ".length()).replace('.', '/'))
                        .forEach(SelfCommunication.prefixesToBlock::add);
            }
            staticLog.info("[NMSUnlocked] loaded "+SelfCommunication.prefixesToChange.size()+" prefix_es");
            staticLog.info("[NMSUnlocked] loaded "+SelfCommunication.prefixesToBlock.size()+" blocked prefix_es");
        } catch (URISyntaxException | IOException e) {
            staticLog.severe("[NMSUnlocked] could not read config because of "+e);
            e.printStackTrace();
        }
    }
}
