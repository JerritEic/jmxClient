package jmxDummyServer;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/*
* Creates a dummy server that registers an MBean with a tickTimes array in the same way that Minecraft servers do.
*  Takes 2 optional arguments:
*    - write to file: whether to write generated tick values to file
*    - duration: max time to continue creating ticks in milliseconds, defaults to max long
*/
public class jmxDummyManager {
    public static void main(String[] args)
            throws Exception {
        boolean writeToFile = true;
        long duration = Long.MAX_VALUE;
        if (args.length >= 1)
            if(args[0].equals("false")){
                writeToFile = false;
            }
        if (args.length >= 2)
            duration = Long.parseLong(args[1]);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.jerrit.Meterstick:type=DummyServer");
        DummyServer serverThread = new DummyServer(writeToFile);
        mbs.registerMBean(serverThread, name);

        // Begin tick creation and wait the duration before stopping it
        System.out.println("Starting tick generation");
        serverThread.start();
        System.out.println("Waiting...");
        Thread.sleep(duration);
        System.out.println("Stopping tick generation");
        serverThread.finish();
    }
}
