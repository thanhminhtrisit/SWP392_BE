package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docling")
public class DoclingProperties {

	private boolean enabled;
	private String baseUrl = "http://localhost:5001";
	private String apiKey;
	private int timeoutSeconds = 300;
	private int maxTokens = 300;
	private boolean useMarkdownTables = true;
	private boolean mergePeers = true;
	private boolean fallbackEnabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public int getMaxTokens() {
		return maxTokens;
	}

	public void setMaxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
	}

	public boolean isUseMarkdownTables() {
		return useMarkdownTables;
	}

	public void setUseMarkdownTables(boolean useMarkdownTables) {
		this.useMarkdownTables = useMarkdownTables;
	}

	public boolean isMergePeers() {
		return mergePeers;
	}

	public void setMergePeers(boolean mergePeers) {
		this.mergePeers = mergePeers;
	}

	public boolean isFallbackEnabled() {
		return fallbackEnabled;
	}

	public void setFallbackEnabled(boolean fallbackEnabled) {
		this.fallbackEnabled = fallbackEnabled;
	}
}
