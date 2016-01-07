# SmokeTester
Java8 based generic smoke test framework

Overview:

This was developed from an older integration testing framework that was hardcoded to take an (XML) list of URL's 
(and request attributes/parameters) make HTTP(s) calls, and then validate the response against a list of criteria to 
producer a PASS/FAIL indication.

Now based on the Strategy design pattern, the user supplied a Set of Strategy classes to a Context, which (each element) is 
executed (possibly in parallel) and then a list of SmokeTesResult's (one per Strategy) is returned.

This makes this framework very generic, any testing logic (optionally to be run in parallel), will inplement the 
SmokeTestStrategy and returns a SmokeTestResult.



