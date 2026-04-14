package com.gist.mathis.service.processor;

public class ProcessorUtil {
	public static String cleanText(String inputText) {
		// Remove or mask phone numbers (international and local formats)
		String withoutPhoneNumbers = inputText.replaceAll(
				// Regex for phone numbers: +XX XXX XXX XXX, XXX-XXX-XXXX, etc.
				"\\+?\\d{1,3}[-\\s]?\\(?\\d{1,4}\\)?[-\\s]?\\d{1,4}[-\\s]?\\d{1,9}", "[PHONE_NUMBER]");

		// Remove or mask email addresses
		String withoutEmails = withoutPhoneNumbers.replaceAll(
				// Regex for emails: user@domain.com
				"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[EMAIL]");

		// Remove or mask physical addresses (simplified)
		String withoutAddresses = withoutEmails.replaceAll(
				// Regex for addresses: 123 Main St, Via Roma 123, etc.
				"\\b\\d{1,5}\\s+([a-zA-Z]+\\s*)+\\b", "[ADDRESS]");

		// Remove or mask Italian fiscal codes (Codice Fiscale)
		String withoutFiscalCodes = withoutAddresses.replaceAll(
				// Regex for Italian fiscal code: 16 characters, letters and numbers
				"\\b[A-Za-z]{6}\\d{2}[A-Za-z]\\d{2}[A-Za-z]\\d{3}[A-Za-z]\\b", "[FISCAL_CODE]");

		// Remove or mask credit card numbers
		String withoutCreditCards = withoutFiscalCodes.replaceAll(
				// Regex for credit card numbers: 16 digits, possibly separated by spaces or
				// hyphens
				"\\b(?:\\d[ -]*?){13,16}\\b", "[CREDIT_CARD]");

		// Remove or mask IP addresses (IPv4)
		String withoutIPs = withoutCreditCards.replaceAll(
				// Regex for IPv4: XXX.XXX.XXX.XXX
				"\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "[IP_ADDRESS]");

		// Remove or mask dates of birth (DD/MM/YYYY or YYYY-MM-DD)
		String withoutDates = withoutIPs.replaceAll(
				// Regex for dates: DD/MM/YYYY or YYYY-MM-DD
				"\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b", "[DATE]");

		// Remove or mask usernames (e.g., @username)
		String withoutUsernames = withoutDates.replaceAll(
				// Regex for usernames: @username
				"@[A-Za-z0-9_]+", "[USERNAME]");

		return withoutUsernames;
	}

	public static void main(String[] args) {
		// Example text with various personal data
		String sampleText = "My phone is +39 123 4567890, email me at example@domain.com. "
				+ "I live at 123 Main St, Rome. My fiscal code is RSSMRA80A01H501X. "
				+ "My credit card is 1234 5678 9012 3456. My IP is 192.168.1.1. "
				+ "Born on 01/01/1980. Follow me @myusername.";

		String cleanedText = cleanText(sampleText);
		System.out.println(cleanedText);
	}
}
