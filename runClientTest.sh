java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=25585 -Dcom.sun.management.jmxremote.ssl=false -jar ./target/jmxDummyServer.jar actualTicks.csv 40000 &
java -jar ./target/jmxClient.jar com.jerrit.Meterstick:type=DummyServer 25585 . 10000