@ECHO OFF
start "server" java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=25585 -Dcom.sun.management.jmxremote.ssl=false -jar .\target\jmxDummyServer.jar actualticks.csv 60000
java -jar ./target/jmxClient.jar -id com.jerrit.Meterstick:type=DummyServer -port 25585 -out . -dur 10000
pause