package com.gist.mathis.service.ingester;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;

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
	
	public static String extractTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("text/html")) {
            return (String) message.getContent();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    return (String) part.getContent();
                }
            }
        }
        return "";
    }

	public static String getAddressesAsString(Address[] addresses) {
        if (addresses == null) return "";
        List<String> result = new ArrayList<>();
        for (Address a : addresses) result.add(((InternetAddress)a).getAddress());
        return String.join(",", result);
    }
}
