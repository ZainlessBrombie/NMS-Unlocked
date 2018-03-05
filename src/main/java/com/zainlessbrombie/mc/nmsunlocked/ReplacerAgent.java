package com.zainlessbrombie.mc.nmsunlocked;


import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

/**
 * Created by mathis on 02.03.18 23:09.
 */
public class ReplacerAgent {

    static void loadAgent(Logger log) {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
        try {
            VirtualMachine virtualMachine = VirtualMachine.attach(pid);
            String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            virtualMachine.loadAgent(path);
            virtualMachine.detach();
            return;
        } catch (AttachNotSupportedException e) {
            log.severe("Could not attach -> plugin version compatibility not enabled");
            e.printStackTrace();
            throw new RuntimeException("Could not attach VM",e);
        } catch (IOException e) {
            log.severe("Could not attach because of IOException -> plugin version compatibility not enabled");
            if(!System.getProperty("java.version","[version_unknown]").startsWith("1")) {
                log.severe("If you are running version java 9 or above, you need to use the java argument -Djdk.attach.allowAttachSelf=true");
            }
            e.printStackTrace();
            throw new RuntimeException("Could not attach VM",e);
        } catch (AgentLoadException e) {
            log.severe("Could not attach because of AgentLoadException. This is unusual. -> plugin version compatibility not enabled");
            e.printStackTrace();
            throw new RuntimeException("Could not attach VM",e);
        } catch (AgentInitializationException e) {
            log.severe("Could not attach because of AgentInitializationException. This is unusual. -> plugin version compatibility not enabled");
            e.printStackTrace();
            throw new RuntimeException("Could not attach VM",e);
        }
    }
}
