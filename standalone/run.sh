UNIX_CLASSPATH=../target/SmokerTester-1.2.jar:log4j-1.2.17.jar:gson-2.6.2.jar

java -cp $UNIX_CLASSPATH -Dlog4j.configuration=file:log4j.xml com.pearceful.util.standalone.TextLineConfigProcessor scripts.txt $1 $2