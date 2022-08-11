package client;

import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.*;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JmxClient {
    // Collect ticks every 2.5 seconds
    static int SAMPLING_INTERVAL = 2500;

    /* Entry point
     * Takes 4 optional arguments:
     *    - id: the RMI stub, specific for the tested server
     *    - port: the RMI port number
     *    - out_folder: the file location to output tick log
     *    - duration: max time to continue collecting ticks in milliseconds, defaults to max long
     */
    public static void main(String[] args) throws Exception {

        String portnum = "25585";
        String id = "net.minecraft.server:type=Server";
        String outFolder = "0";
        long timeToSample = Long.MAX_VALUE;
        if (args.length >= 1)
            id = args[0];
        if (args.length >= 2)
            portnum = args[1];
        if (args.length >= 3)
            outFolder = args[2];
        if (args.length >= 4)
            timeToSample =  Long.parseLong(args[3]);
        String filePath = outFolder + "/tick_log.csv";
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + portnum + "/jmxrmi");

        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        long[] curr = null;
        long[] prev = null;

        // Initialize the file to write tick time too
        String toWrite = "timestamp, tickTime,\n";
        FileWriter out = new FileWriter(filePath);
        out.write(toWrite);

        long endTime = System.currentTimeMillis() + timeToSample;
        // Begin sampling
        while (true) {
            long sampleStartTime = System.currentTimeMillis();
            if(sampleStartTime > endTime){
                System.out.println("sample duration expired.");
                break;
            }
            // Fetch 'val' at RMI stub with name 'tickTime'
            Object val = null;
            try {
                val = mbsc.getAttribute(new ObjectName(id), "tickTimes");
            } catch (InstanceNotFoundException e) {
                System.out.println("tickTimes not found, waiting...");
                Set<ObjectName> MBeans = mbsc.queryNames(null,null);
                for (ObjectName name : MBeans){
                    System.out.println(name);
                }
                Thread.sleep(1000L);
                continue;
            }
            // cast to correct type and check if successful
            long[] vals = (long[])val;
            if (vals.length != 100) {
                System.out.println("Error: Malformed tickTimes array.\n");
                break;
            }

            curr = vals;
            if (prev != null) {
                // Check against previously collected ticks, if exists
                long[] array = findNew(prev, curr);
                // Assign pseudo-timestamps to collected ticks and write to file
                if (array.length != 1) {
                    for (int x = 0; x < array.length; x++) {
                        toWrite = "%d,%d,\n".formatted(sampleStartTime + (50L * x), array[x]);
                        out.write(toWrite);
                    }
                    out.flush();
                }
            }
            prev = curr;
            Thread.sleep(SAMPLING_INTERVAL);
        }
        out.close();
    }

    /*
     *  Compare previous and current set of ticks to find block of continuous changed ticks
     */
    public static long[] findNew(long[] prev, long[] curr) {

        List<Integer> oldToNew = new ArrayList<Integer>();
        List<Integer> newToOld = new ArrayList<Integer>();
        // Find changes from old to new values
        for(int i =0; i < prev.length; i++){
            int j = (i + 1) % prev.length;
            if(prev[i] == curr[i] && prev[j] != curr[j]){
                oldToNew.add(j);
            }
            if(prev[i] != curr[i] && prev[j] == curr[j]){
                newToOld.add(j);
            }
        }
        
        if(oldToNew.size() == 1 && newToOld.size() ==1){
            return subArray(curr, oldToNew.get(0), newToOld.get(0));
        }
        // Else there is an extraneous value that is the same by chance as same spot in prev array,
        // should generally never happen
        System.out.println("Duplicate value detected in new ticks values!");
        // Find start and end to maximize new values (and minimize old ones)
        int minOld = Integer.MAX_VALUE;
        int maxNew = 0;
        int bestStart = -1;
        int bestEnd = -1;
        for(int start: oldToNew){
            for (int finish : newToOld){
                int i = start;
                int numOld = 0;
                int numNew = 0;
                while(i != finish){
                    if(prev[i] == curr[i]){numOld++;} else {numNew++;}
                    i = (i + 1) % curr.length;
                }
                // Lexicographic order numNew, numOld
                if(numNew > maxNew){
                    maxNew = numNew;
                    minOld = numOld;
                    bestStart = start;
                    bestEnd = finish;
                } else if(numNew == maxNew){
                    if(numOld <= minOld){
                        maxNew = numNew;
                        minOld = numOld;
                        bestStart = start;
                        bestEnd = finish;
                    }
                }
            }
        }
        return subArray(curr, bestStart, bestEnd);
    }

    // Copies array from indices start to finish, in circular fashion
    public static long[] subArray(long[] array, int start, int finish){
        int size = finish - start;
        if(start > finish){
            size = (array.length - start) + finish;
        }
        long[] temp = new long[size];
        for(int i =0; i < size; i++){
            int j = (start + i) % array.length;
            temp[i] = array[j];
        }
        return temp;
    }
}
