package server;

public interface DummyServerMBean {
    // Circular array of tick times in nanoseconds
    public long[] gettickTimes();
    // Utility to stop the dummy server remotely
    public void finish();
}
