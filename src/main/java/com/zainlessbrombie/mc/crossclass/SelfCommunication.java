package com.zainlessbrombie.mc.crossclass;

import com.zainlessbrombie.mc.crossclass.data.PluginData;
import com.zainlessbrombie.util.StringListSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created by mathis on 03.03.18 21:26.
 */
public class SelfCommunication { //they are not in the same classloader
    public static final int BREAK = 3;

    public static int updated = 0;
    public static String versionString;



    /*static void openSocket(int port) {
        Thread t = new Thread(() -> {
            try {
                ServerSocket incoming = new ServerSocket(port);
                int read;
                while(true) {
                    Socket socket = incoming.accept();
                    if(!socket.getInetAddress().getHostName().equals("localhost")) {
                        socket.shutdownOutput();
                        socket.close();
                        continue;
                    }
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();
                    while((read = in.read()) != -1) {
                        if(read == BREAK)
                            return;
                        byte[] toSend = StringListSerializer.parseMapToString(packData()).getBytes();

                        out.write(new byte[]{123, (byte) (toSend.length >> 8), (byte) toSend.length});
                        out.write(toSend);
                    }
                }
            } catch (IOException e) {
                System.out.println("[CrossClass] failed self communication: "+e);
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }*/


    /*static Map<String,String> packData() {
        Map<String,String> ret = new HashMap<>();
        PluginData data = Main.getLocalPluginData();
        ret.put("processed", String.valueOf(data.getClassesProcessed()));
        ret.put("version",data.getVersionString());
        return ret;
    }


    static Socket toSelf;

    public static synchronized PluginData poll() throws IOException {
        if(toSelf == null)
            toSelf = new Socket("localhost",1234);
        toSelf.getOutputStream().write(1);
        InputStream in = toSelf.getInputStream();
        int a;
        if((a = in.read()) != 123) {
            toSelf.getOutputStream().write(BREAK);
            toSelf = null;
            throw new RuntimeException("An error occurred: "+a+" is not a valid first message char");
        }
        int l = in.read() * 0x100 + in.read();
        byte[] input = new byte[l];
        int ptr = 0;
        while((a = (ptr += in.read(input,ptr,input.length - ptr))) < input.length && a > 0);
        Map<String,String> returned = StringListSerializer.parseStringToMap(new String(input));
        return new PluginData(returned.get("version"), Integer.parseInt(returned.get("processed")));
    }*/
}
