package com.steadybit.testcontainers.trafficcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TrafficControl {
    private static final Logger log = LoggerFactory.getLogger(TrafficControl.class);
    private static final Pattern COMMAND_FAILED_PATTERN = Pattern.compile("Command failed -:(\\d+)");

    public void add(RuleSet rules) {
        this.apply(Action.ADD, rules);
    }

    public void delete(RuleSet rules) {
        this.apply(Action.DELETE, rules);
    }

    private void apply(Action action, RuleSet rules) {
        if (rules.isEmpty()) {
            return;
        }

        try {
            String[] tcCommands = rules.render(action);
            if (log.isTraceEnabled()) {
                log.trace("Executing tc commands:\n{}", String.join("\n", tcCommands));
            }

            Result result = this.executeBatch(tcCommands);
            if (log.isTraceEnabled()) {
                log.trace("Executed tc commands (exitcode={}):\n{}", result.getExitcode(), result.getStdOut());
            }

            if (!result.isSuccessful()) {
                this.handleError(action, tcCommands, result);
            }
        } catch (Exception ex) {
            throw new TrafficControlException("Could not execute tcCommands", ex);
        }
    }

    public String dumpRules(String nic) {
        try {
            Result result = this.executeBatch("qdisc show dev " + nic, "filter show dev " + nic, "class show dev " + nic);
            return result.isSuccessful() ? result.getStdOut() : "failed: " + result.getStdOut();
        } catch (Exception e) {
            return "failed: " + e;
        }
    }

    private void handleError(Action action, String[] tcCommands, Result result) {
        List<String> notIgnored = new ArrayList<>(tcCommands.length);
        log.trace("TC-StdOut\n{}", result.stdOut);
        log.trace("TC-StdErr\n{}", result.stdErr);
        try (Scanner scanner = new Scanner(result.getStdErr())) {
            scanner.useDelimiter("\\n");

            while (scanner.hasNext()) {
                int ruleIndex = -1;
                String errorMessage = "";
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNext()) {
                    String line = scanner.next();
                    Matcher m = COMMAND_FAILED_PATTERN.matcher(line);
                    if (m.find()) {
                        ruleIndex = Integer.parseInt(m.group(1)) - 1;
                        errorMessage = sb.toString();
                        sb.setLength(0);
                        break;
                    } else {
                        if (sb.length() > 0) {
                            sb.append('\n');
                        }
                        sb.append(line);
                    }
                }

                String command = ruleIndex >= 0 && ruleIndex < tcCommands.length ? tcCommands[ruleIndex] : "<unknown>";

                if (Action.ADD.equals(action)) {
                    if (errorMessage.equalsIgnoreCase("Error: Exclusivity flag on, cannot modify.") || errorMessage.equalsIgnoreCase(
                            "RTNETLINK answers: File exists")) {
                        log.debug("Rule '{}' was not added. Error ignored: {}", command, errorMessage);
                        continue;
                    }
                } else if (Action.DELETE.equals(action)) {
                    log.debug("Rule '{}' was not deleted. Error ignored: {}", command, errorMessage);
                    continue;
                }

                notIgnored.add("'" + command + "' failed: " + errorMessage);
            }
        }
        if (!notIgnored.isEmpty()) {
            throw new TrafficControlException("Error when executing tc commands:\n" + String.join("\n", notIgnored));
        }
    }

    protected abstract Result executeBatch(String... tcCommands);

    public static class RuleSet {
        private final String nic;
        private final List<Rule> rules;

        public RuleSet(String nic) {
            this(nic, Collections.emptyList());
        }

        private RuleSet(String nic, Collection<Rule> rules) {
            this.nic = nic;
            this.rules = new ArrayList<>(rules);
        }

        public void qdiscRule(QDisc kind, String handle, String parent, String... options) {
            this.rules.add(new QDiscRule(kind, handle, parent, options));
        }

        public void classRule(QDisc kind, String classId, String parent, String... options) {
            this.rules.add(new ClassRule(kind, classId, parent, options));
        }

        public void filterRule(String parent, Protocol protocol, int prio, Filter kind, String... options) {
            this.rules.add(new FilterRule(parent, protocol, prio, kind, options));
        }

        private String[] render(Action action) {
            List<Rule> orderedRules = new ArrayList<>(this.rules);
            if (Action.DELETE.equals(action)) {
                Collections.reverse(orderedRules);
            }
            return orderedRules.stream().map(r -> r.render(this.nic, action)).toArray(String[]::new);
        }

        public List<Rule> getRules() {
            return Collections.unmodifiableList(this.rules);
        }

        public String getNic() {
            return this.nic;
        }

        @Override
        public String toString() {
            return "RuleSet{" + "nic='" + this.nic + '\'' + ", rules=" + this.rules + '}';
        }

        public boolean isEmpty() {
            return this.rules.isEmpty();
        }
    }

    public enum QDisc {
        PRIO, NETEM, HTB;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum Filter {
        U32;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum Protocol {
        IP, IP6;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private enum Action {
        ADD, DELETE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public static abstract class Rule {
        protected abstract String render(String nic, Action action);
    }

    public static class QDiscRule extends Rule {
        private final QDisc kind;
        private final String handle;
        private final String parent;
        private final String[] options;

        QDiscRule(QDisc kind, String handle, String parent, String[] options) {
            this.kind = kind;
            this.handle = handle;
            this.parent = parent;
            this.options = options;
        }

        @Override
        protected String render(String nic, Action action) {
            return "qdisc " + action + " dev " + nic + " " + (this.parent != null ? "parent " + this.parent : "root") + " handle " + this.handle + " "
                    + this.kind + (this.options != null ? " " + String.join(" ", this.options) : "");
        }
    }

    public static class ClassRule extends Rule {
        private final QDisc kind;
        private final String classid;
        private final String parent;
        private final String[] options;

        ClassRule(QDisc kind, String classid, String parent, String[] options) {
            this.kind = kind;
            this.classid = classid;
            this.parent = parent;
            this.options = options;
        }

        @Override
        protected String render(String nic, Action action) {
            return "class " + action + " dev " + nic + " parent " + this.parent + " classid " + this.classid + " " + this.kind + (this.options != null ?
                    " " + String.join(" ", this.options) : "");
        }
    }

    public static class FilterRule extends Rule {
        private final String parent;
        private final Protocol protocol;
        private final int prio;
        private final Filter kind;
        private final String[] options;

        FilterRule(String parent, Protocol protocol, int prio, Filter kind, String[] options) {
            this.parent = parent;
            this.protocol = protocol;
            this.prio = prio;
            this.kind = kind;
            this.options = options;
        }

        @Override
        protected String render(String nic, Action action) {
            return "filter " + action + " dev " + nic + " protocol " + this.protocol + " " + (this.parent != null ? "parent " + this.parent : "root") + " prio "
                    + this.prio + " " + this.kind + (this.options != null ? " " + String.join(" ", this.options) : "");

        }
    }

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
