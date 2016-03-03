set DOS_CLASSPATH=../target/SmokerTester-1.0.jar;log4j-1.2.17.jar;gson-2.6.2.jar

java -cp %DOS_CLASSPATH% -Dlog4j.configuration=file:log4j.xml com.pearceful.util.standalone.ShellScriptListProcessor scripts.txt %1 %2
