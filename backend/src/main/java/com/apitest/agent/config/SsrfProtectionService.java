package com.apitest.agent.config;

import com.apitest.agent.exception.SsrfViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.*;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SsrfProtectionService {

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            Pattern.compile("^169\\.254\\..*"),
            Pattern.compile(".*169\\.254\\.169\\.254.*") // AWS/GCP cloud metadata
    );

    private final AppProperties appProperties;

    public SsrfProtectionService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void validateTargetUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new SsrfViolationException("Target URL cannot be empty");
        }

        try {
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            String host = url.getHost();

            if (host == null || host.isBlank()) {
                throw new SsrfViolationException("Invalid target URL: missing host");
            }

            // Always block cloud metadata endpoints
            for (Pattern pattern : BLOCKED_PATTERNS) {
                if (pattern.matcher(host).matches()) {
                    throw new SsrfViolationException("Access to cloud metadata address is blocked: " + host);
                }
            }

            AppProperties.Testing testing = appProperties.getTesting();
            List<String> allowedHosts = testing.getAllowedHosts();

            // If allowedHosts is configured, restrict to only those hosts
            if (allowedHosts != null && !allowedHosts.isEmpty()) {
                boolean allowed = allowedHosts.stream().anyMatch(h ->
                        h.equalsIgnoreCase(host) || host.toLowerCase().endsWith("." + h.toLowerCase())
                );
                if (!allowed) {
                    throw new SsrfViolationException("Host '" + host + "' is not in allowed hosts list: " + allowedHosts);
                }
            }

        } catch (MalformedURLException | URISyntaxException e) {
            throw new SsrfViolationException("Invalid target URL: " + e.getMessage());
        }
    }
}
