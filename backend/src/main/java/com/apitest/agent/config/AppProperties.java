package com.apitest.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Testing testing = new Testing();
    private Demo demo = new Demo();

    public Testing getTesting() { return testing; }
    public void setTesting(Testing testing) { this.testing = testing; }
    public Demo getDemo() { return demo; }
    public void setDemo(Demo demo) { this.demo = demo; }

    public static class Testing {
        private int maxConcurrency = 5;
        private int timeoutSeconds = 10;
        private boolean demoMode = true;
        private List<String> allowedHosts = new ArrayList<>();
        private long maxOpenApiFileSizeBytes = 1_048_576L;
        private int maxEndpoints = 100;
        private int maxGeneratedTests = 100;
        private int maxResponseBodyBytes = 10_240;

        public int getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(int v) { this.maxConcurrency = v; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
        public boolean isDemoMode() { return demoMode; }
        public void setDemoMode(boolean v) { this.demoMode = v; }
        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> v) { this.allowedHosts = v; }
        public long getMaxOpenApiFileSizeBytes() { return maxOpenApiFileSizeBytes; }
        public void setMaxOpenApiFileSizeBytes(long v) { this.maxOpenApiFileSizeBytes = v; }
        public int getMaxEndpoints() { return maxEndpoints; }
        public void setMaxEndpoints(int v) { this.maxEndpoints = v; }
        public int getMaxGeneratedTests() { return maxGeneratedTests; }
        public void setMaxGeneratedTests(int v) { this.maxGeneratedTests = v; }
        public int getMaxResponseBodyBytes() { return maxResponseBodyBytes; }
        public void setMaxResponseBodyBytes(int v) { this.maxResponseBodyBytes = v; }
    }

    public static class Demo {
        private boolean defectsEnabled = true;
        public boolean isDefectsEnabled() { return defectsEnabled; }
        public void setDefectsEnabled(boolean v) { this.defectsEnabled = v; }
    }
}
