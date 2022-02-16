package jmxDummyServer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class DummyServer extends Thread implements DummyServerMBean{
    private boolean notStopped = true;
    private boolean toFile;
    private FileWriter out;
    private long [] ticks = new long[100];

    DummyServer(boolean toFile) throws IOException {
        Arrays.fill(ticks, 0L);
        this.toFile = toFile;
        if(toFile){
            out = new FileWriter("actualTicks.csv");
            out.write("actualTick\n");
        }
    }

    @Override
    public void run(){
        try {
            startTicks();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    // Generate tick values
    void startTicks() throws InterruptedException, IOException {
        // By default, picks longs in range of 10 to 50 ms, e.g. normal Minecraft tick times
        int index = 0;
        while(notStopped){
            long nextVal = ThreadLocalRandom.current().nextLong(10000000,50000000);
            Thread.sleep(nextVal / 1000000); // Simulate tick operations
            ticks[index] = nextVal; // Record tick duration
            if(toFile){
                out.write(nextVal + "\n");
                out.flush();
            }
            index = ( index + 1 ) % 100;
            Thread.sleep(( 50000000 - nextVal) / 1000000); // Wait for next tick to start
        }
        if(toFile){
            out.close();
        }
    }

    @Override
    public long[] gettickTimes() {
        return ticks;
    }

    @Override
    public void finish(){
        notStopped = false;
        System.out.println("Stopped");
    }


}
