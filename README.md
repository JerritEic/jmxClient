# jmxClient
## Utility to read Minecraft server tick times using Java Management Extensions (JMX)

### jmxClient tool
- Given JMX url and port, continuously reads tick times to file. 
  Call using: _java -jar jmxClient.jar < JMX Url > < JMX Port > < Out Folder > < Duration >_

### jmxDummyServer
- Creates a dummy server to continuously create random tick time values. Useful for testing jmxClient and
JMX configuration. Call using: _java -jar jmxDummyServer.jar < Output to file > < Duration >_

### Testing
- Run either `runClientTest.sh` or `runClientTest.bat`.
  These run the dummy server for 40 seconds, logging all generated tick values to "actualTicks.csv," and the
  jmxClient for 10 seconds, which outputs all the retrieved ticks to "tick_log.csv."

### Used in Meterstick Benchmark
- See [Meterstick](https://github.com/JerritEic/Meterstick) 



