package com.zainlessbrombie.mc.crossclass;


import com.zainlessbrombie.mc.crossclass.data.PluginData;
import com.zainlessbrombie.reflect.Constant;
import com.zainlessbrombie.reflect.ConstantTable;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Created by mathis on 02.03.18 22:00.
 */
public class Main extends JavaPlugin {

    private Logger log = getLogger();

    private static Logger staticLog = Logger.getLogger("CrossClass");

    private static PluginStatus status = PluginStatus.NOT_LOADED;

    private static String versionString;

    private String pluginVersion;

    private static int classesChanged = -1;
    private static int port = 1234;

    private static List<String> prefixesToChange = new ArrayList<>();

    private static List<String> blockedPrefixes = new ArrayList<>();

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        staticLog.info("[CrossClass] Starting. If this message occurs after any other plugin has loaded, start this plugin as a javaagent");
        classesChanged = 0;
        instrumentation.addTransformer(
                (classLoader, name, aClass, protectionDomain, bytes) -> {
                    // saving performance. Also lambdas will have null as name and aClass
                    if (name == null || name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("sun/")) {
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
                                        System.out.println("[CrossClass] found version number '" + versionString + "'");
                                        break;
                                    }
                                }
                            } catch (Throwable t) {
                                staticLog.severe("[CrossClass] Could not read version number from stack. This is unusual.");
                                t.printStackTrace();
                                return null;
                            }
                        }

                        if (!isEligible(name))
                            return null;

                        try {
                            ConstantTable table = ConstantTable.readFrom(bytes);
                            Set<Integer> referencedByString = table.getConstantsOfType(Constant.StringConstant.class).map(Constant.StringConstant::getReferenceId).collect(Collectors.toSet());
                            class Holder {private int i;}
                            Holder h = new Holder();
                            table.getConstantsOfType(Constant.Utf8Constant.class)
                                    .filter(utf8Constant -> !referencedByString.contains(utf8Constant.getTableIndex()))
                                    .map(utf8Constant -> new T2<>(utf8Constant, new String(utf8Constant.getContent())))
                                    .filter(t2 -> {
                                        String toTest = t2.getO2();
                                        if (toTest.length() == 0)
                                            return false;
                                        toTest = t2.getO2().charAt(0) == 'L' ? t2.getO2().substring(1) : t2.getO2();
                                        return (toTest.startsWith("net/minecraft/server/v") || toTest.startsWith("org/bukkit/craftbukkit/v"));
                                    })
                                    .forEach(t2 -> {
                                        String newVersion = t2.getO2().replaceAll("(^[L]?(?:net/minecraft/server/)|(?:org/bukkit/craftbukkit/))(v[a-zA-Z0-9_]+)", "$1" + versionString); //could weekly builds contain other letters? Better safe than sorry
                                        if(!t2.getO2().equals(newVersion)) {
                                            t2.getO1().setContent(newVersion.getBytes());
                                            h.i++;
                                        }
                                    });
                            if(h.i != 0) {
                                classesChanged++;
                                SelfCommunication.updated++;
                            }
                            return table.recompile();
                        } catch (Throwable t) {
                            staticLog.severe("Could not modify " + name.replace('/', '.') + " because of " + t);
                            t.printStackTrace();
                        }
                        return null;
                    } catch (Throwable t) {
                        staticLog.severe("[CrossClass] An unknown error occurred: "+t);
                        staticLog.severe("[CrossClass] Please let the dev know about this");
                        t.printStackTrace();
                        return null;
                    }
                },true
        );
        //SelfCommunication.openSocket(port);
    }

    static PluginData getLocalPluginData() {
        return new PluginData(versionString,classesChanged);
    }


    private static boolean isEligible(String name) { //todo implement logarithmic solution
        for (String s : prefixesToChange) {
            if(name.startsWith(s))
                return true;
        }
        return false;
    }

    private static void loadConfig() {
        try {
            File pluginFolder = new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile(),"crossClass");
            pluginFolder.mkdir();
            File confFile = new File(pluginFolder,"conf.txt");
            List<String> configLines = Arrays.asList("#config file for CrossClass. All lines starting with 'prefix: ' (note the whitespace) add a qualifying prefix.",
                    "#if a class that is being loaded matches at least one of the listed prefixes, all non string occurrences of version strings will be changed.",
                    "#example:",
                    "#prefix: com.aplugin",
                    "#a line with only 'prefix: ' will match any and all classes. If you encounter no issues, you could leave it at that.",
                    "prefix: ",
                    "",
                    "#if you don't want a certain plugin to be changed, you can simply add it with prefixdeny. The prefixes java, javax and sun are always blocked for performance reasons",
                    "#prefixdeny: "
            );
            if(!confFile.exists())
                Files.write(confFile.toPath(),configLines,StandardOpenOption.CREATE);
            else
                configLines = Files.readAllLines(confFile.toPath());
            configLines.stream()
                    .filter(line -> line.startsWith("prefix: "))
                    .map(line -> line.substring("prefix: ".length()).replace('.','/'))
                    .forEach(prefixesToChange::add);
            configLines.stream()
                    .filter(line -> line.startsWith("prefixdeny :"))
                    .map(line -> line.substring("prefixdeny: ".length()).replace('.','/'))
                    .forEach(blockedPrefixes::add);
            staticLog.info("[CrossClass] loaded "+prefixesToChange.size()+" prefix_es");
            staticLog.info("[CrossClass] loaded "+blockedPrefixes.size()+" blocked prefix_es");
        } catch (URISyntaxException | IOException e) {
            staticLog.severe("[CrossClass] could not read config because of "+e);
            e.printStackTrace();
        }
    }

    public static synchronized void printBytes(byte[] toPrint) {
        //byte[] out = new byte[toPrint.length];
        //System.arraycopy(toPrint,0,out,0,toPrint.length);
        StringBuilder builder = new StringBuilder(toPrint.length * 3);
        boolean redmode = false;
        for (int i = 0; i < toPrint.length; i++) {
            if(validChar((char) toPrint[i])) {
                if(!redmode) {
                    builder.append(ConsoleColors.RED_BRIGHT);
                    redmode = true;
                }
                builder.append((char) toPrint[i]);
            }
            else {
                if(redmode) {
                    builder.append(ConsoleColors.RESET);
                    redmode = false;
                }
                builder.append("[").append(toPrint[i]).append("]");
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

    static {
        try {
            loadConfig();
            start();
        } catch (URISyntaxException e) {
            staticLog.severe("[CrossClass] This is weird. I got a "+e.getClass().getSimpleName()+" check if there are uncommon characters in your file path, like whitespaces");
            e.printStackTrace();
        }
    }


    public static void premain(String args,Instrumentation instrumentation) {
        staticLog.severe("Activating via premain");
        agentmain(args,instrumentation);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(pluginVersion == null)
            pluginVersion = getConfig().getString("version");
        sender.sendMessage(new String[]{
                ChatColor.BLUE+"###########> CrossClass <###########",
                ChatColor.BLUE+"CrossClass compatibility plugin. Redefines bytecode for all configured plugins. "+ChatColor.GRAY+"By ZainlessBrombie",
                ChatColor.BLUE+"Version: "+pluginVersion,
                ChatColor.BLUE+"["+prefixesToChange.size()+"] prefixes loaded from config.",
                ChatColor.BLUE+"["+blockedPrefixes.size()+"] prefixes are blocked.",
                ChatColor.BLUE + "Server version: " + SelfCommunication.versionString,
                ChatColor.BLUE + "[" + SelfCommunication.updated + "] classes have been edited and reassembled."
        });
        /*
        this was my response to the issue of different classloaders. Lets keep it around, just in case
        getServer().getScheduler().runTaskAsynchronously(this,() -> {
            try {
                PluginData data = SelfCommunication.poll();
                sender.sendMessage(new String[] {
                        ChatColor.BLUE + "Server version: " + data.getVersionString(),
                        ChatColor.BLUE + "[" + data.getClassesProcessed() + "] classes have been edited and reassembled."
                });
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED+"Uh oh umm... I seem to be unable to talk to myself. Error: "+e);
                e.printStackTrace();
            }
        });*/
        return true;
    }

    private static byte[] readAllFromStream(InputStream inputStream) throws IOException { // i love java. most of the time.
        byte [] ret = new byte[inputStream.available()];
        int pointer = 0;
        int read;
        while((read = inputStream.read(ret,pointer,ret.length - pointer)) > 0) {
            pointer += read;
        }
        return ret;
    }


    @Override
    public void onDisable() {
        super.onDisable();
        log.info("Disabling crossClass will NOT turn off its replacing capabilities");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        log.info("Enabled crossClass plugin. Current status of replacer: "+status+" ");
    }


    private static byte[] byteReplace(byte[] source, byte[] what, byte[] by) {
        byte[] ret = new byte[source.length];
        System.arraycopy(source,0,ret,0,source.length);
        for (int i = 0; i < ret.length; i++) {
            if(match(ret,what,i)) {
                System.arraycopy(by, 0, ret, i, by.length);
            }

        }
        return ret;
    }

    private static boolean match(byte[] where, byte[] what, int index) {
        for(int i = 0; i < what.length || i + index >= where.length;i++) {
            if(where[index + i] != what[i])
                return false;
        }
        return true;
    }


    private static void start() throws URISyntaxException {
        Logger log = Logger.getLogger("CrossClass");
        if (Main.class.getClassLoader().getClass().getSimpleName().equalsIgnoreCase("PluginClassLoader")) {
            log.info("[CrossClass] Loading in PluginClassLoader");
        } else {
            log.info("[CrossClass] Is not loading in PluginClassLoader, will not start from here! (this is normal, you should get this message once per startup)");
            return;
        }
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException e) {
            log.info("[CrossClass] Could not find lib, loading dynamically");

            File dir = new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile(),"crossClass");
            dir.mkdir();
            File jarFile = new File(dir,"tools.jar");
            if(!jarFile.exists()) {
                log.info("[CrossClass] Writing tools.jar");
                InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("tools.jar");
                byte[] raw;
                try {
                    raw = readAllFromStream(inputStream);
                    if (raw.length < 10000 || raw.length > 20000000) {
                        error();
                        throw new RuntimeException("Reading the tools.jar failed in a weird way: length was " + raw.length);
                    }
                } catch (IOException e1) {
                    log.severe("[CrossClass] Could not read tools.jar resource!");
                    e1.printStackTrace();
                    error();
                    return;
                }


                try {
                    Files.write(jarFile.toPath(), raw, StandardOpenOption.CREATE);
                } catch (IOException e1) {
                    log.severe("[CrossClass] Could not write tools.jar. Exiting.");
                    e1.printStackTrace();
                    error();
                    return;
                }
            }
            else
                log.info("tools.jar already present, not loading");

            URLClassLoader bukkitLoader;
            try {
                bukkitLoader = (URLClassLoader) Main.class.getClassLoader();
            } catch (ClassCastException castException) {
                log.severe("The bukkit class loader is not an instance of URLClassLoader. Jeez! What version of minecraft are we at? Greetings from the past i guess! Oh also this plugin won't work now, at least not this version.");
                error();
                throw castException;
            }
            try {
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
        ReplacerAgent.loadAgent(log);
        status = PluginStatus.RUNNING;
        log.info("[CrossClass] Loaded with "+Main.class.getClassLoader().getClass());
    }

    private static void error() {
        status = PluginStatus.ERROR;
    }


    private enum PluginStatus {
        NOT_LOADED, RUNNING, ERROR
    }

    private static class T2<TA,TB> {
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

    private class ConsoleColors {
        // Reset
        public static final String RESET = "\033[0m";  // Text Reset

        // Regular Colors
        public static final String BLACK = "\033[0;30m";   // BLACK
        public static final String RED = "\033[0;31m";     // RED
        public static final String GREEN = "\033[0;32m";   // GREEN
        public static final String YELLOW = "\033[0;33m";  // YELLOW
        public static final String BLUE = "\033[0;34m";    // BLUE
        public static final String PURPLE = "\033[0;35m";  // PURPLE
        public static final String CYAN = "\033[0;36m";    // CYAN
        public static final String WHITE = "\033[0;37m";   // WHITE

        // Bold
        public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
        public static final String RED_BOLD = "\033[1;31m";    // RED
        public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
        public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
        public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
        public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
        public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
        public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

        // Underline
        public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
        public static final String RED_UNDERLINED = "\033[4;31m";    // RED
        public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
        public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
        public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
        public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
        public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
        public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

        // Background
        public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
        public static final String RED_BACKGROUND = "\033[41m";    // RED
        public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
        public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
        public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
        public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
        public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
        public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE

        // High Intensity
        public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
        public static final String RED_BRIGHT = "\033[0;91m";    // RED
        public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
        public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
        public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
        public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
        public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
        public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE

        // Bold High Intensity
        public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
        public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
        public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
        public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
        public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
        public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
        public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
        public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

        // High Intensity backgrounds
        public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
        public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
        public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
        public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
        public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
        public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
        public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
        public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE
    }
}
