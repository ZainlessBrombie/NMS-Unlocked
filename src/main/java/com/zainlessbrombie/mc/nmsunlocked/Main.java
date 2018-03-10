package com.zainlessbrombie.mc.nmsunlocked;


import com.zainlessbrombie.mc.nmsunlocked.config.ConfigLoader;
import com.zainlessbrombie.mc.nmsunlocked.util.T2;
import com.zainlessbrombie.reflect.Constant;
import com.zainlessbrombie.reflect.ConstantTable;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.zainlessbrombie.mc.nmsunlocked.util.ByteUtil.readAllFromStream;


/**
 * Copyright ZainlessBrombie 2018
 * Do not copy or redistribute.
 */
public class Main extends JavaPlugin {

    private static String DO_NOT_COPY="Thank you :) <3"; // for decompiling

    private Logger log = getLogger();

    private static Logger staticLog = Logger.getLogger("NMSUnlocked");

    private static PluginStatus status = PluginStatus.NOT_LOADED;

    private static String versionString;

    private String pluginVersion;



    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        staticLog.info("[NMSUnlocked] Starting. If this message occurs after any other plugin has loaded, start this plugin as a javaagent");
        instrumentation.addTransformer(
                (classLoader, name, aClass, protectionDomain, bytes) -> {
                    // saving performance. Also lambdas will have null as name and aClass
                    if (name != null && (name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("sun/") || name.startsWith("net/minecraft") || name.startsWith("org/bukkit"))) {
                        return null;
                    }

                    try {

                        // catching version string
                        if (versionString == null) {
                            try {
                                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                                for (int i = 1; i < 6; i++) { //check only the first five elements
                                    if (stack[stack.length - i].getClassName().startsWith("net.minecraft.server.")) {
                                        String preVersionString = stack[stack.length - i].getClassName().substring("net.minecraft.server.".length());
                                        versionString = preVersionString.substring(0, preVersionString.indexOf("."));
                                        SelfCommunication.versionString = versionString;
                                        System.out.println("[NMSUnlocked] found version number '" + versionString + "'");
                                        break;
                                    }
                                }
                            } catch (Throwable t) {
                                staticLog.severe("[NMSUnlocked] Could not read version number from stack. This is unusual.");
                                t.printStackTrace();
                                return null;
                            }
                        }


                        try {
                            ConstantTable table = null;
                            if(name == null) {
                                table = ConstantTable.readFrom(bytes);
                                name = table.getSelf().getName().getContentString();
                            }
                            if (!isEligible(name))
                                return null;
                            if(table == null)
                                table = ConstantTable.readFrom(bytes);
                            Set<Integer> referencedByString = table.getConstantsOfType(Constant.StringConstant.class).map(Constant.StringConstant::getReferenceId).collect(Collectors.toSet());
                            class Holder {private int i;}
                            Holder h = new Holder();
                            table.getConstantsOfType(Constant.Utf8Constant.class)
                                    .filter(utf8Constant -> !referencedByString.contains(utf8Constant.getTableIndex()))
                                    .map(utf8Constant -> new T2<>(utf8Constant, new String(utf8Constant.getContent())))
                                    .filter(t2 -> {
                                        String toTest = t2.getO2();
                                        return (toTest.contains("net/minecraft/server/v") || toTest.contains("org/bukkit/craftbukkit/v"));
                                    })
                                    .forEach(t2 -> {
                                        String newVersion = t2.getO2().replaceAll("((?:net/minecraft/server/)|(?:org/bukkit/craftbukkit/))(v[a-zA-Z0-9_]+)", "$1" + versionString); //could weekly builds contain other letters? Better safe than sorry
                                        if(!t2.getO2().equals(newVersion)) {
                                            t2.getO1().setContent(newVersion.getBytes());
                                            h.i++;
                                        }
                                    });
                            if(h.i != 0) {
                                synchronized (SelfCommunication.modified) {
                                    SelfCommunication.updated++;
                                    SelfCommunication.modified.add(name);
                                }
                            }
                            return table.recompile();
                        } catch (Throwable t) {
                            staticLog.severe("[NMSUnlocked] Could not modify " + (name == null ? "[UNKNOWN LAMBDA]" : name.replace('/', '.')) + " because of " + t);
                            t.printStackTrace();
                        }
                        return null;
                    } catch (Throwable t) {
                        staticLog.severe("[NMSUnlocked] An unknown error occurred: "+t);
                        staticLog.severe("[NMSUnlocked] Please let the dev know about this");
                        t.printStackTrace();
                        return null;
                    }
                },true
        );
    }



    private static boolean isEligible(String name) { //todo implement logarithmic solution
        synchronized (SelfCommunication.lock) {
            for(String s : SelfCommunication.prefixesToBlock)
                if(name.startsWith(s))
                    return false;
            for (String s : SelfCommunication.prefixesToChange)
                if(name.startsWith(s))
                    return true;
            return false;
        }
    }





    // ############# INIT ############# //

    static {
        try {
            Logger log = Logger.getLogger("NMSUnlocked");
            if (Main.class.getClassLoader().getClass().getSimpleName().equalsIgnoreCase("PluginClassLoader")) {
                log.info("[NMSUnlocked] Loading in PluginClassLoader");
                start();
            } else {
                ConfigLoader.loadConfig();
                log.info("[NMSUnlocked] Is not loading in PluginClassLoader, will not start from here. (this is normal, you should get this message once per startup)");
            }
        } catch (URISyntaxException e) {
            staticLog.severe("[NMSUnlocked] This is weird. I got a "+e.getClass().getSimpleName()+" check if there are uncommon characters in your file path, like whitespaces");
            e.printStackTrace();
        }
    }


    // started via java -javaagent:
    public static void premain(String args,Instrumentation instrumentation) {
        staticLog.info("Activating via premain (Java Agent)");
        agentmain(args,instrumentation);
    }


    // status command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(pluginVersion == null)
            pluginVersion = getConfig().getString("version");

        // Send for normal user
        if(!sender.hasPermission("nmsunlocked.status")) {
            if (args.length == 0)
                sender.sendMessage(new String[]{
                        "",
                        ChatColor.AQUA + "[==============>" + ChatColor.YELLOW + " NMSUnlocked " + ChatColor.AQUA + "==============>]",
                        ChatColor.AQUA + " > NMSUnlocked compatibility plugin! " + ChatColor.GRAY + "By ZainlessBrombie.",
                        ChatColor.AQUA + " > " + ChatColor.WHITE + "Forget version incompatibility:",
                        ChatColor.AQUA + " > " + ChatColor.WHITE + "Reassembles class code for " + ChatColor.BLUE + ChatColor.BOLD + "a" + ChatColor.DARK_GREEN + ChatColor.BOLD + "l" + ChatColor.DARK_RED + ChatColor.BOLD + "l " + ChatColor.RESET + "configured plugins. ",
                        ChatColor.YELLOW + " > You are seeing the reduced output as you do not have",
                        ChatColor.YELLOW + " > the nmsunlocked.status permission",
                        ChatColor.AQUA + "___________________________________________",
                        ""
                });
            else
                sender.sendMessage(ChatColor.RED+"You don't have permission (nmsunlocked.status)");
            return true;
        }

        // Has permission. List.
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("list")) {
                sender.sendMessage(ChatColor.BLUE+"==========================================");
                sender.sendMessage(ChatColor.BLUE+"The following classes have been modified:");
                synchronized (SelfCommunication.modified) {
                    SelfCommunication.modified.forEach(str -> sender.sendMessage(" + "+str));
                }
                sender.sendMessage(ChatColor.BLUE+"==========================================");
            } else {
                sender.sendMessage(ChatColor.BLUE+"usage: /nmsunlocked list");
            }
        } else { // Status decorated for the console
            String wrench = new String(new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x94, (byte) 0xA7});
            String decor = (sender instanceof ConsoleCommandSender) ? " \u2699 " + wrench + " \u2699  " : " ";
            sender.sendMessage(new String[]{
                    "",
                    ChatColor.AQUA + "[==============>" + ChatColor.YELLOW + decor + "NMSUnlocked" + decor + ChatColor.AQUA + "==============>]",
                    //"Gear 1 "+'\u2699',
                    ChatColor.AQUA + " > NMSUnlocked compatibility plugin! " + ChatColor.GRAY + "By ZainlessBrombie.",
                    ChatColor.AQUA + " > " + ChatColor.WHITE + "Forget version incompatibility:",
                    ChatColor.AQUA + " > " + ChatColor.WHITE + "Reassembles class code for " + ChatColor.BLUE + ChatColor.BOLD + "a" + ChatColor.DARK_GREEN + ChatColor.BOLD + "l" + ChatColor.DARK_RED + ChatColor.BOLD + "l " + ChatColor.RESET + "configured plugins. ",
                    ChatColor.AQUA + " > Version: " + ChatColor.WHITE + getDescription().getVersion(),
                    ChatColor.AQUA + " > Server version: \"" + ChatColor.WHITE + SelfCommunication.versionString + ChatColor.AQUA + "\"",
                    ChatColor.AQUA + " > " + ChatColor.YELLOW + "[" + SelfCommunication.prefixesToChange.size() + "]" + ChatColor.WHITE + " prefixes loaded from config.",
                    ChatColor.AQUA + " > " + ChatColor.YELLOW + "[" + SelfCommunication.prefixesToBlock.size() + "]" + ChatColor.WHITE + " prefixes are blocked.",
                    ChatColor.AQUA + " > " + ChatColor.YELLOW + "[" + SelfCommunication.updated + "]" + ChatColor.WHITE + " classes have been edited and reassembled."
            });
            if (SelfCommunication.prefixesToBlock.size() == 0 && SelfCommunication.prefixesToChange.contains(""))
                sender.sendMessage(ChatColor.GOLD + " > All plugins are being reassembled! =)");
            sender.sendMessage(new String[]{
                    ChatColor.AQUA + "___________________________________________",
                    ""
            });
        }

        return true;
    }




    @Override
    public void onDisable() {
        super.onDisable();
        log.warning("Disabling NMSUnlocked plugin will NOT turn off its replacing capabilities. The config will be reloaded on next pluginStart, however plugins will only be fully affected by that config change if they are manually unloaded and loaded again (not to be confused with reenabled)");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        log.info("Enabled NMSUnlocked plugin. Current status of replacer: "+status+" ");
        ConfigLoader.loadConfig();
        log.info("Loaded config.");
    }


    /**
     * Register the Agent. Call only from PluginClassLoader loaded class.
     */
    private static void start() throws URISyntaxException {
        Logger log = Logger.getLogger("NMSUnlocked");
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine"); //test if the tools.jar is already loaded (unusual)
        } catch (ClassNotFoundException e) {
            log.info("[NMSUnlocked] Could not find lib, loading dynamically");

            File dir = new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile(),"nmsUnlocked");
            dir.mkdir(); //The plugin's personal dir
            String systemType = System.getProperty("os.name").toLowerCase();
            if(systemType.contains("linux"))
                systemType = "linux";
            else if(systemType.contains("windows"))
                systemType = "windows";
            else
                systemType = "osx";

            File jarFile = new File(dir,"tools.jar");

            if(!jarFile.exists()) { //write tools.jar if not already present
                if(systemType.equals("osx"))
                    throw new RuntimeException("OSX IS CURRENTLY NOT SUPPORTED - SORRY. You can place your own tools.jar in the plugins/nmsUnlocked folder, it should work");
                log.info("[NMSUnlocked] Writing tools.jar for system type "+systemType);
                InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("tools/"+systemType+"/tools.jar");
                byte[] raw;
                try {
                    raw = readAllFromStream(inputStream);
                    if (raw.length < 10000 || raw.length > 20000000) {
                        error();
                        throw new RuntimeException("Reading the tools.jar failed in a weird way: length was " + raw.length);
                    }
                } catch (IOException e1) {
                    log.severe("[NMSUnlocked] Could not read tools.jar resource!");
                    e1.printStackTrace();
                    error();
                    return;
                }


                try {
                    Files.write(jarFile.toPath(), raw, StandardOpenOption.CREATE);
                } catch (IOException e1) {
                    log.severe("[NMSUnlocked] Could not write tools.jar. Exiting.");
                    e1.printStackTrace();
                    error();
                    return;
                }
            }
            else
                log.info("[NMSUnlocked] tools.jar already present, not loading");


            URLClassLoader bukkitLoader;
            try {
                bukkitLoader = (URLClassLoader) Main.class.getClassLoader();
            } catch (ClassCastException castException) {
                // I sincerely hope this will never happen (and it probably won't)
                log.severe("[NMSUnlocked] The bukkit class loader is not an instance of URLClassLoader. Jeez! What version of minecraft are we at? Greetings from the past I guess! Oh also this plugin won't work now, at least not this version.");
                error();
                throw castException;
            }
            try {
                // add the tools.jar
                Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                m.invoke(bukkitLoader,new File(dir,"tools.jar").toURI().toURL());
            } catch (NoSuchMethodException | IllegalAccessException | MalformedURLException | InvocationTargetException e1) {
                log.severe("An error has occurred trying to load a required jar. Exiting.");
                e1.printStackTrace();
                error();
                return;
            }
        }
        status = PluginStatus.ERROR;
        ReplacerAgent.loadAgent(log); // has to be in separate class, as the classes used there are loaded only in this method
        status = PluginStatus.RUNNING;
        log.info("[NMSUnlocked] Loaded with "+Main.class.getClassLoader().getClass());
    }

    private static void error() {
        status = PluginStatus.ERROR;
    }


    private enum PluginStatus {
        NOT_LOADED, RUNNING, ERROR
    }




}
