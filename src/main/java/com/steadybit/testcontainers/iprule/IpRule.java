package com.steadybit.testcontainers.iprule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class IpRule {
    private static final Logger log = LoggerFactory.getLogger(IpRule.class);

    public void add(List<String[]> rules) {
        try {
            this.apply("add", rules);
        } catch (Exception e) {
            try {
                this.apply("del", rules);
            } catch (Exception nested) {
                e.addSuppressed(nested);
            }
            throw e;
        }
    }

    public void delete(List<String[]> rules) {
        List<String[]> orderedRules = new ArrayList<>(rules);
        Collections.reverse(orderedRules);
        this.apply("del", rules);
    }

    private void apply(String mode, List<String[]> rules) {
        if (rules.isEmpty()) {
            return;
        }

        try {
            String[] ipCommands = rules.stream().map(rule -> "rule " + mode + " " + String.join(" ", rule)).toArray(String[]::new);
            if (log.isTraceEnabled()) {
                log.trace("Executing ip commands:\n{}", String.join("\n", ipCommands));
            }

            Result result = this.executeBatch(ipCommands);
            if (log.isTraceEnabled()) {
                log.trace("Executed ip commands (exitcode={}):\n{}", result.getExitcode(), result.getStdOut());
            }

            if (!result.isSuccessful()) {
                throw new IpRuleException("Error when executing ip commands:\n" + String.join("\n", ipCommands));
            }
        } catch (Exception ex) {
            throw new IpRuleException("Could not execute tcCommands", ex);
        }
    }

    public String getCurrentRules() {
        try {
            return this.executeBatch("rule list").stdOut;
        } catch (Exception e) {
            log.warn("Failed to read ip rule list", e);
            return null;
        }
    }

    protected abstract Result executeBatch(String... command);

    protected static class Result {
        private final int exitcode;
        private final String stdOut;
        private final String stdErr;

        protected Result(int exitcode, String stdOut, String stdErr) {
            this.exitcode = exitcode;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }

        private boolean isSuccessful() {
            return this.exitcode == 0;
        }

        public int getExitcode() {
            return exitcode;
        }

        public String getStdOut() {
            return stdOut;
        }

        public String getStdErr() {
            return stdErr;
        }
    }
}
