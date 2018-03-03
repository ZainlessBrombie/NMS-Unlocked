package com.zainlessbrombie.mc.crossclass;


import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;


/**
 * Created by mathis on 02.03.18 22:00.
 */
public class Main extends JavaPlugin {

    private Logger log = getLogger();

    byte[] versOld = new byte[] {118,49,95,49,49,95,82,49};
    byte[] versNew = new byte[] {118,49,95,49,49,95,82,49};

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("\n########INSTRUMENTATION########\n");
        instrumentation.addTransformer((classLoader, name, aClass, protectionDomain, bytes) -> {
                    if(!name.startsWith("com/zainlessbrombie"))
                        return null;
                    try {
                        CtClass beingLoaded = ClassPool.getDefault().makeClass(new ByteArrayInputStream(bytes));
                        CtMethod method = beingLoaded.getMethods()[0];

                        System.out.println("############ load ############ " + name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    byte[] ret = byteReplace(bytes,"v1_11_R1".getBytes(),"v1_12_R1".getBytes());
                    System.out.println(new String(ret));
                    return ret;
                },true
        );
    }


    public static void premain(String args,Instrumentation instrumentation) {
        System.out.println("Activating via premain");
        agentmain(args,instrumentation);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        log.info("Loading crossClass");
        log.info("Version string is "+getServer().getClass());
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException e) {
            log.info("Could not find lib, loading dynamically");
            File dir = new File(getFile().getParentFile(),"crossClass");
            dir.mkdir();
            File jarFile = new File(dir,"tools.jar");
            if(!jarFile.exists()) {
                log.info("Writing tools.jar");
                InputStream inputStream = getClassLoader().getResourceAsStream("tools.jar");
                byte[] raw;
                try {
                    raw = readAllFromStream(inputStream);
                    if (raw.length < 10000 || raw.length > 20000000) {
                        throw new RuntimeException("Reading the tools.jar failed in a weird way: length was " + raw.length);
                    }
                } catch (IOException e1) {
                    log.severe("Could not read tools.jar resource!");
                    e1.printStackTrace();
                    super.getPluginLoader().disablePlugin(this);
                    return;
                }


                try {
                    Files.write(jarFile.toPath(), raw, StandardOpenOption.CREATE);
                } catch (IOException e1) {
                    log.severe("Could not write tools.jar. Exiting.");
                    e1.printStackTrace();
                    getPluginLoader().disablePlugin(this);
                    return;
                }
            }
            else
                log.info("tools.jar already present, not loading");

            URLClassLoader bukkitLoader;
            try {
                bukkitLoader = (URLClassLoader) getClassLoader();
            } catch (ClassCastException castException) {
                log.severe("The bukkit class loader is not an instance of URLClassLoader. Jeez! What version of minecraft are we at? Greetings from the past i guess! Oh also this plugin won't work now, at least not this version.");
                throw castException;
            }
            try {
                Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                m.invoke(bukkitLoader,new File(dir,"tools.jar").toURI().toURL());

            } catch (NoSuchMethodException | IllegalAccessException | MalformedURLException | InvocationTargetException e1) {
                log.severe("An error has occurred trying to load a required jar. Exiting.");
                e1.printStackTrace();
                getPluginLoader().disablePlugin(this);
                return;
            }
        }
        ReplacerAgent.loadAgent(log);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        System.out.println("OnCommand");
        try {
            Class.forName("com.zainlessbrombie.mc.crossclass.SampleLoad");
        } catch (ClassNotFoundException e) {
            System.out.println("fail");
        }
        //SampleLoad.test123();
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
        log.info("Enabled crossClass plugin");
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
}
