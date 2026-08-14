package com.se1908.group01.service;

import com.se1908.group01.config.OcrProperties;
import com.se1908.group01.util.FileExtensionUtil;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
/**
 * Extract text từ image đã upload hoặc trang PDF được render khi text trực tiếp không đủ.
 */
public class OcrService {

	private final OcrProperties properties;

	public OcrService(OcrProperties properties) {
		this.properties = properties;
	}

	public boolean isEnabled() {
		return properties.isEnabled() && StringUtils.hasText(properties.getExecutablePath());
	}

	public String extractText(MultipartFile file) throws IOException {
		// Image upload đi vào pipeline ingestion dưới dạng các text segment từ OCR.
		if (!isEnabled()) {
			return "";
		}

		var ext = FileExtensionUtil.getExtensionLower(file.getOriginalFilename());
		var suffix = StringUtils.hasText(ext) ? "." + ext : ".img";
		var tempInput = Files.createTempFile("ai-study-ocr-", suffix);
		try (var in = file.getInputStream()) {
			Files.copy(in, tempInput, StandardCopyOption.REPLACE_EXISTING);
			return runTesseract(tempInput);
		} finally {
			Files.deleteIfExists(tempInput);
		}
	}

	public String extractText(BufferedImage image) throws IOException {
		if (!isEnabled() || image == null) {
			return "";
		}

		var tempInput = Files.createTempFile("ai-study-ocr-page-", ".png");
		try {
			ImageIO.write(image, "png", tempInput.toFile());
			return runTesseract(tempInput);
		} finally {
			Files.deleteIfExists(tempInput);
		}
	}

	private String runTesseract(Path inputPath) throws IOException {
		var tempOutput = Files.createTempFile("ai-study-ocr-output-", ".txt");
		List<String> command = new ArrayList<>();
		command.add(properties.getExecutablePath());
		command.add(inputPath.toAbsolutePath().toString());
		command.add("stdout");
		command.add("-l");
		command.add(StringUtils.hasText(properties.getLanguage()) ? properties.getLanguage() : "eng");

		if (StringUtils.hasText(properties.getTessdataPath())) {
			command.add("--tessdata-dir");
			command.add(properties.getTessdataPath());
		}

		var process = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.redirectOutput(tempOutput.toFile())
				.start();

		try {
			var completed = process.waitFor(Math.max(1, properties.getTimeoutSeconds()), TimeUnit.SECONDS);
			var output = Files.readString(tempOutput, StandardCharsets.UTF_8);
			if (!completed) {
				process.destroyForcibly();
				throw new IOException("Tesseract OCR timed out");
			}
			if (process.exitValue() != 0) {
				throw new IOException("Tesseract OCR failed: " + output.trim());
			}
			return output.trim();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Tesseract OCR was interrupted", ex);
		} finally {
			Files.deleteIfExists(tempOutput);
		}
	}
}
