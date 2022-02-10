package com.steadybit.testcontainers.iprule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class IpRule {
    private static final Logger log = LoggerFactory.getLogger(IpRule.class);

    public void add(List<String[]> rules) {
        List<String> failed = new ArrayList<>();
        List<String[]> added = new ArrayList<>();
        this.apply("add", rules, added, failed);

        if (!failed.isEmpty()) {
            this.apply("del", added, new ArrayList<>(), new ArrayList<>());
            throw new IpRuleException("Error adding ip rules " + failed);
        }
    }

    public void delete(List<String[]> rules) {
        List<String> failed = new ArrayList<>();
        List<String[]> deleted = new ArrayList<>();
        this.apply("del", rules, deleted, failed);

        if (!failed.isEmpty()) {
            throw new IpRuleException("Error deleting ip rules " + failed);
        }
    }

    private void apply(String mode, List<String[]> rules, List<String[]> success, List<String> failed) {
        for (String[] rule : rules) {
            try {
                this.execute((String[]) ArrayUtils.addAll(new String[] { mode }, rule));
                success.add(rule);
            } catch (Exception ex) {
                String ruleStr = String.join(" ", rule);
                if (log.isDebugEnabled()) {
                    log.warn("Error {} ip rule {}: {}", mode, ruleStr, ex.getMessage());
                } else {
                    log.warn("Error {} ip rule {}", mode, ruleStr, ex);
                }
                failed.add(ruleStr);
            }
        }
    }

    public String getCurrentRules() {
        try {
            return this.execute("list");
        } catch (Exception e) {
            log.warn("Failed to read ip rule list", e);
            return null;
        }
    }

    protected abstract String execute(String... command);
}
