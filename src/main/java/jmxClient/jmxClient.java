package jmxClient;

import java.io.FileWriter;
import java.util.Arrays;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class jmxClient {
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
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + id  +":"+ portnum + "/jmxrmi");

        JMXConnector jmxc = JMXConnectorFactory.connect(url);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        long[] curr = null;
        long[] prev = null;

        // Initialize the file to write tick time too
        String to_write = "timestamp, tickTime,\n";
        FileWriter out = new FileWriter(filePath);
        out.write(to_write);

        long endTime = System.currentTimeMillis() + timeToSample;

        // Begin sampling
        while (true) {
            long sampleStartTime = System.currentTimeMillis();
            if(sampleStartTime > endTime){
                echo("sample duration expired.");
                break;
            }
            // Fetch 'val' at RMI stub with name 'tickTime'
            Object val = null;
            try {
                val = mbsc.getAttribute(new ObjectName(id), "tickTimes");
            } catch (InstanceNotFoundException e) {
                echo("tickTimes not found, waiting...");
                Thread.sleep(1000L);
                continue;
            }
            // cast to correct type and check if successful
            long[] vals = (long[])val;
            if (vals.length != 100) {
                echo("Error: Malformed tickTimes array.\n");
                break;
            }

            curr = vals;
            if (prev != null) {
                // Check against previously collected ticks, if exists
                long[] array = findNew(prev, curr, 50);
                // Assign pseudo-timestamps to collected ticks and write to file
                if (array.length != 1) {
                    for (int x = 0; x < array.length; x++) {
                        to_write = "" + sampleStartTime + 50L * x + "," + array[x] + ",\n";
                        out.write(to_write);
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
     *  Compare previous and current set of ticks to find the beginning of the range of new ticks
     */
    public static long[] findNew(long[] prev, long[] curr, int max) {
        long[] diff = listDiff(prev, curr);
        int farthest = -1;
        // Farthest is index of last non-zero value that is right after a zero value
        for (int i = 0; i < diff.length; i++) {
            if (diff[i] == 0L)
                // Edge case of switch exactly at diff.length-1
                if (i + 1 == diff.length) {
                    if (diff[0] != 0L)
                        farthest = 0;
                } else if (diff[i + 1] != 0L) {
                    farthest = i + 1;
                }
        }
        // All values zero
        if (farthest == -1) {
            echo("Error: No change in tickTime array.\n");
            printArray(prev);
            printArray(curr);
            return new long[1];
        }
        return getNonZero(diff, farthest, max);
    }

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

    /* Return up to max values from array starting at index start
     *
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

    //--------------- UTILITY FUNCTIONS ----------------------
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
