package com.gist.mathis.service.ingester;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IngesterUtil {
	public static String extractEmail(String text) {
		Matcher matcher = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").matcher(text);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	public static String extractWebsite(String text) {
		Matcher matcher = Pattern.compile("(https?://)?(www\\.[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(text);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}
}
