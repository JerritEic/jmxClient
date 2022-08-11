package client;

import java.io.FileWriter;
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
        /*
        * The JMX URI/URL system is confusing but well documented. For a succinct version see:
        *  https://stackoverflow.com/questions/2768087/explain-jmx-url
        */
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + portnum + "/jmxrmi");

        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        long[] curr = null;
        long[] prev = null;
        int currentTimestamp = 0;

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
                System.out.println("tickTimes not found, waiting...\n");
                // Queries and outputs all registered MBeans
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
                // Assign logical timestamps to collected ticks and write to file
                if (array.length != 1) {
                    for (int x = 0; x < array.length; x++) {
                        toWrite = "%d,%d,\n".formatted(currentTimestamp, array[x]);
                        currentTimestamp++;
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
        // Find all indices of changes from old to new values
        for(int i =0; i < prev.length; i++){
            int j = (i + 1) % prev.length;
            if(prev[i] == curr[i] && prev[j] != curr[j]){
                oldToNew.add(j);
            }
            if(prev[i] != curr[i] && prev[j] == curr[j]){
                newToOld.add(j);
            }
        }
        // Simplest case, there is a clear start and end to new values
        if(oldToNew.size() == 1 && newToOld.size() ==1){
            return subArray(curr, oldToNew.get(0), newToOld.get(0));
        }
        // Else there is an extraneous value that is the same by chance as same spot in prev array,
        // very unlikely to happen
        System.out.println("Duplicate value detected in new ticks values!");
        // Iterate start and finish indices to find combination that maximize new values (and minimize old ones)
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

    // Returns a subarray of array starting at index start and ending at finish. Finish may be less than start.
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

    //--------------- UTILITY FUNCTIONS ----------------------
    /*
     *  If value at index is same in prev and curr, set it to 0. As sampling frequency is 2.5 seconds and the full
     *  array of 100 ticks is 5 seconds worth of ticks, the new 50 ticks shouldn't overlap with the 50 previous
     *  new ticks.
     */
    public static long[] listDiff(long[] prev, long[] curr) {
        long[] diff = (long[])curr.clone();
        for (int i = 0; i < prev.length; i++) {
            if (curr[i] - prev[i] == 0L)
                diff[i] = 0L;
        }
        return diff;
    }

    /*
     * Return up to max number of values from array starting at index start
     */
    public static long[] getNonZero(long[] array, int start, int max) {
        long[] temp = new long[max];
        Arrays.fill(temp, -1L);
        int j = 0;
        for (int i = 0; i < max; i++) {
            j = start + i;
            if (j >= array.length)
                j = start + i - array.length;
            if (array[j] != 0L)
                temp[i] = array[j];
        }
        return temp;
    }
    public static void echo(String str) {
        System.out.print(str);
    }

    public static double findAverage(long[] array) {
        return Arrays.stream(array).average().orElse(Double.NaN);
    }

    public static void printArray(long[] array) {
        echo("[ ");
        for (int i = 0; i < array.length; i++)
            echo("" + array[i] + ", ");
        echo(" ]\n");
    }

    public static int findNumDiff(long[] first, long[] sec) {
        int counter = 0;
        for (int i = 0; i < first.length; i++) {
            if (first[i] != sec[i])
                counter++;
        }
        return counter;
    }
}
