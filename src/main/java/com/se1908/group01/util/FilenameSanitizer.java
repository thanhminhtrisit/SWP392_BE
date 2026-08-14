package com.se1908.group01.util;

import java.text.Normalizer;

public final class FilenameSanitizer {

	private FilenameSanitizer() {
	}

	public static String sanitize(String filename) {
		if (filename == null || filename.isBlank()) {
			return "file";
		}

		var clean = filename.replace("\\", "/");
		var lastSlash = clean.lastIndexOf('/');
		var base = lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;

		base = Normalizer.normalize(base, Normalizer.Form.NFKC);
		base = base.replaceAll("[\\r\\n\\t]", " ").trim();
		base = base.replaceAll("[^A-Za-z0-9._ -]", "_");
		base = base.replaceAll("\\s+", " ");
		base = base.replaceAll("\\.+", ".");
		base = base.replaceAll("^\\.+", "");
		base = base.replaceAll("\\.$", "");

		if (base.isBlank()) {
			base = "file";
		}

		if (base.length() > 200) {
			base = base.substring(base.length() - 200);
		}

		return base;
	}
}

