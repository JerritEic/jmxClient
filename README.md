# jmxClient
## Utility to read Minecraft server tick times using Java Management Extensions (JMX)

### jmxClient tool
- Given JMX url and port, continuously reads tick times to file. 
  Call using: _java -jar jmxClient.jar < JMX Url > < JMX Port > < Out Folder > < Duration >_

### jmxDummyServer
- Creates a dummy server to continuously create random tick time values. Useful for testing jmxClient and
JMX configuration. Call using: _java -jar jmxDummyServer.jar < Output to file > < Duration >_

### Used in Meterstick Benchmark
- See [Meterstick](https://github.com/JerritEic/Meterstick) 



