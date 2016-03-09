UNIX_CLASSPATH=../target/SmokerTester-1.2.jar:log4j-1.2.17.jar

java -cp $UNIX_CLASSPATH -Dlog4j.configuration=file:log4j.xml com.pearceful.util.standalone.JsonConfigProcessor sample-conf.json $1 $2