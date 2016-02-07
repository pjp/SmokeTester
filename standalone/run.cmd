set DOS_CLASSPATH=../target/SmokerTester-1.0-SNAPSHOT.jar;log4j-1.2.17.jar

java -cp %DOS_CLASSPATH% -Dlog4j.configuration=file:log4j.xml com.pearceful.util.ShellScriptListProcessor scripts-dos.txt %1 %2
