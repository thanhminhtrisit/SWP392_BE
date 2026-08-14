package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr.tesseract")
public class OcrProperties {

	private boolean enabled = false;

	private String executablePath;

	private String tessdataPath;

	private String language = "eng";

	private int timeoutSeconds = 60;

	private int pdfDpi = 200;

	private int pdfTextMinCharacters = 20;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}

	public String getTessdataPath() {
		return tessdataPath;
	}

	public void setTessdataPath(String tessdataPath) {
		this.tessdataPath = tessdataPath;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public int getPdfDpi() {
		return pdfDpi;
	}

	public void setPdfDpi(int pdfDpi) {
		this.pdfDpi = pdfDpi;
	}

	public int getPdfTextMinCharacters() {
		return pdfTextMinCharacters;
	}

	public void setPdfTextMinCharacters(int pdfTextMinCharacters) {
		this.pdfTextMinCharacters = pdfTextMinCharacters;
	}
}

