package com.se1908.group01.util;

public final class FileExtensionUtil {

	private FileExtensionUtil() {
	}

	public static String getExtensionLower(String filename) {
		if (filename == null) {
			return "";
		}
		var clean = filename.replace("\\", "/");
		var lastSlash = clean.lastIndexOf('/');
		var base = lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
		var lastDot = base.lastIndexOf('.');
		if (lastDot < 0 || lastDot == base.length() - 1) {
			return "";
		}
		return base.substring(lastDot + 1).toLowerCase();
	}
}

