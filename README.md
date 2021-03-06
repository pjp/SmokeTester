# SmokeTester
## Java8 based generic smoke test framework

### Overview:

This was developed from an older integration testing framework that was hardcoded to take an (XML) list of URL's 
(and request attributes/parameters) make HTTP(s) calls, and then validate the response against a list of criteria to 
producer a PASS/FAIL indication.

Now based on the Strategy design pattern, the user supplied a Set of Strategy classes to a Context, which (each element) is 
executed (possibly in parallel) and then a list of SmokeTesResult's (one per Strategy) is returned.

This makes this framework very generic, any testing logic (optionally to be run in parallel), will implement the 
SmokeTestStrategy interface (or extends the *BaseSmokeTestStrategy* class) and return a SmokeTestResult.

See *SmokeTestContext.runSmokeTests* static method and the *SmokeTestContextTest* class

### Sample Implementations ###

For complete running sample implementations, see:-

1) *TestLineConfigProcessor* class and the scripts.txt/run.sh/run.cmd files in the standalone package and directory.

2) Newly added *JsonConfigProcessor* class and the sample-conf.json/jrun.sh/jrun.cmd files