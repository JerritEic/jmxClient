package client;

public enum Option {
    IP("ip", "IP", ""),
    PORT("port", "PORT", "25564"),
    ID("id", "ID", "net.minecraft.server:type=Server"),
    OUT("out", "OUTPUT_DIR", "."),
    DUR("dur", "DURATION", Long.toString(Long.MAX_VALUE)),
    ;

    private final String name;
    private final String environmentVariable;
    private final String defaultValue;

    Option(String name, String environmentVariable, String defaultValue) {
        this.name = name;
        this.environmentVariable = environmentVariable;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.name;
    }

    public String getDefault() {
        String envValue = System.getenv(environmentVariable);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }
}
