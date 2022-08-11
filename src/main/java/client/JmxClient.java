package client;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class JmxClient {
    // Collect ticks every 2.5 seconds
    static int SAMPLING_INTERVAL = 2500;

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("help", false, "print this message");
        options.addOption("ip", true, "Minecraft node IP address");
        options.addOption("port", true, "Minecraft node port number");
        options.addOption("id", true, "ID of the JMX resource to access");
        options.addOption("out", true, "output directory");
        options.addOption("dur", true, "sample duration");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ant", options);
            System.exit(0);
        }

        String ip = cmd.getOptionValue("ip", "");
        String portnum = cmd.getOptionValue("port", "25585");
        String id = cmd.getOptionValue("id", "net.minecraft.server:type=Server");
        Path filePath = Paths.get(cmd.getOptionValue("out", "."), "tick_log.csv");
        long timeToSample = cmd.hasOption("dur") ? Long.parseLong(cmd.getOptionValue("dur")) :
            Long.MAX_VALUE;

        JMXServiceURL url = new JMXServiceURL(MessageFormat.format(
            "service:jmx:rmi:///jndi/rmi://{0}:{1}/jmxrmi", ip, portnum));

        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        long[] curr = null;
        long[] prev = null;

        // Initialize the file to write tick time too
        String toWrite = "timestamp, tickTime,\n";
        FileWriter out = new FileWriter(filePath.toFile());
        out.write(toWrite);

        long currentTime = System.currentTimeMillis();
        // Compute end time (currentTime + timeToSample), but protect against wrap around
        long endTime = timeToSample > (Long.MAX_VALUE - currentTime) ? Long.MAX_VALUE : currentTime
            + timeToSample;
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
