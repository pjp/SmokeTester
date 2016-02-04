package com.pearceful.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * Created by ppearce on 2016-02-04.
 *
 * Read a file that contains commands or shell scripts (one per line) to execute
 * in parallel. Example file is scripts.txt
 *
 * As per the shell, lines starting with # as ignored
 *
 */
public class ShellScriptListProcessor {
    public static void main(String[] args) {
        int exitStatus      = 0;
        int threadPoolSize  = 10;
        int timeoutSeconds  = 600;

        if(args.length < 1) {
            System.err.println("Need a file name to read as input");
            System.exit(1);
        }

        Path path = Paths.get(args[0]);

        Set<SmokeTestStrategy> shellScripts = new CopyOnWriteArraySet<>();

        /////////////////////////////////////////
        // Process each non comment or blank line
        try(Stream<String> lines =
                    Files.lines(path)
                            .filter(s -> s.trim().length() > 0)
                            .filter(s -> !s.startsWith("#"))) {

            lines.map(line -> { return new ShellScriptProcessor(line); })
                    .forEach(shellScripts::add);

            //////////////////////////////
            // Run the scripts in parallel
            List<SmokeTestResult> results =
                    SmokeTestContext.runSmokeTests(
                            shellScripts, threadPoolSize, timeoutSeconds);

            ///////////////////////////////////
            // Display the result for each test
            int failedCount = 0;
            int passedCount = 0;

            for(SmokeTestResult result : results) {
                if(result.getState().equals(SmokeTestResult.STATE.USER_PASS)) {
                    passedCount++;
                } else {
                    failedCount++;
                }

                System.out.println(result.getMessage());
            }

            //////////
            // Summary
            System.out.printf("SUMMARY: There were %d pass(es) and %d failure(s)", passedCount, failedCount);

            ////////////////////////////////////////////////
            // Indicate if there was a failure to the caller
            if(failedCount > 0) {
                exitStatus = 4;
            }
        } catch (IOException e) {
            System.err.println(e);
            exitStatus = 2;
        } catch (SmokeTestException e) {
            System.err.println(e);
            exitStatus = 3;
        }

        System.exit(exitStatus);
    }
}
