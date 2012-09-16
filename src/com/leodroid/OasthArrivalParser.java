package com.leodroid;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.leodroid.model.BusLineArrival;

public class OasthArrivalParser {
	public final static String patternDesktopLinesArrival = "<td>([0-9]*\\w{0,2})</td><td>([^:]*):[ ]*([^<]*)</td><td>([0-9]*)</td>";
	public final static String patternMobileLinesArrival = "<b class=\"bl\">([0-9]*\\w{0,2})</b>. *([^<]*)</span><span class=\"sp2\">Ξ‘Ξ¦Ξ™Ξ�Ξ— Ξ£Ξ• <span class=\"bl\"><b> *([0-9]*)' *</b>";
	public final static String patternDesktopLineArrival = "<td>([0-9]*)</td><td>([0-9]*) *minutes</td>";
	public final static String patternMobileLineArrival = "class=\"sp1\">([0-9]*)</span><span class=\"sp2\">Ξ‘Ξ¦Ξ™Ξ�Ξ— Ξ£Ξ• <span class=\"sptime\"> *([0-9]*)' *</span>";
	public final static String patternTextualPos = "class=\"sp1\" >Όχημα: ([0-9]*)</span><span class=\"sp2\">ΒΡΙΣΚΕΤΑΙ ΠΕΡΙΠΟΥ <span class=\"bl\">([0-9]*)</span> ΜΕΤΡΑ <span class=\"bl\"> *([^<]*) *</span> ΑΠΟ ΤΗ ΣΤΑΣΗ <span class=\"bl\">([^<]*)</span>";
	public final static String replacementDesktopLinesArrival = "$1,$2,$3,$4";
	public final static String replacementMobileLinesArrival = "$1,$2,$3";
	public final static String replacementLineArrival = "$1,$2";
	private final static String e = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

	public static ArrayList<BusLineArrival> parseMobileLines(String response) {
		return parse(response, patternMobileLinesArrival,
				replacementMobileLinesArrival);
	}

	public static ArrayList<BusLineArrival> parseMobileLine(String response) {
		return parse(response, patternMobileLineArrival, replacementLineArrival);
	}

	public static ArrayList<BusLineArrival> parseDesktopLine(String response) {
		return parse(response, patternDesktopLineArrival,
				replacementLineArrival);
	}

	public static ArrayList<BusLineArrival> parseDesktopLines(String response) {
		return parse(response, patternDesktopLinesArrival,
				replacementDesktopLinesArrival);
	}

	public static ArrayList<BusLineArrival> parse(String response,
			String pattern, String replacement) {
		ArrayList<BusLineArrival> bla = new ArrayList<BusLineArrival>();

		// In case you would like to ignore case sensitivity you could use this
		// statement
		// Pattern pattern = Pattern.compile("\\s+", Pattern.CASE_INSENSITIVE);
		Matcher matcher = Pattern.compile(pattern).matcher(response);

		// Check all occurance
		while (matcher.find()) {
			bla.add(new BusLineArrival(matcher.group().replaceAll(pattern,
					replacement)));
		}
		return bla;
	}

	public static ArrayList<String> parseToArrayList(String response,
			String pattern, String replacement) {
		ArrayList<String> bla = new ArrayList<String>();

		// In case you would like to ignore case sensitivity you could use this
		// statement
		// Pattern pattern = Pattern.compile("\\s+", Pattern.CASE_INSENSITIVE);
		Matcher matcher = Pattern.compile(pattern).matcher(response);

		// Check all occurance
		while (matcher.find()) {
			bla.add(matcher.group().replaceAll(pattern, replacement));
		}
		return bla;
	}

	public static String parseS(String response, String pattern,
			String replacement) {
		StringBuilder bla = new StringBuilder();

		// In case you would like to ignore case sensitivity you could use this
		// statement
		// Pattern pattern = Pattern.compile("\\s+", Pattern.CASE_INSENSITIVE);
		Matcher matcher = Pattern.compile(pattern).matcher(response);

		// Check all occurance
		while (matcher.find()) {
			bla.append(matcher.group().replaceAll(pattern, replacement))
					.append("\n");
		}
		return bla.toString();
	}

	public static String parseMobilePosition(String response) {
		String z = response + "";
		z = z.replaceAll("(?i)grZNn", "=");
		z = decodeCoordinates(z);
		z = z.replace('!', '3');
		z = decodeCoordinates(z);
		return z;
	}

	private static String decodeCoordinates(String a) {

		StringBuilder b = new StringBuilder("");
		char c, chr2, chr3;
		int d, enc2, enc3, enc4;
		int i = 0;
		a = a.replaceAll("[^A-Za-z0-9+/=]", "");
		while (i < a.length()) {
			d = e.indexOf(a.charAt(i++));
			enc2 = e.indexOf(a.charAt(i++));
			enc3 = e.indexOf(a.charAt(i++));
			enc4 = e.indexOf(a.charAt(i++));
			c = (char) ((d << 2) | (enc2 >> 4));
			chr2 = (char) (((enc2 & 15) << 4) | (enc3 >> 2));
			chr3 = (char) (((enc3 & 3) << 6) | enc4);
			b.append(c);
			if (enc3 != 64) {
				b.append(chr2);
			}
			if (enc4 != 64) {
				b.append(chr3);
			}
		}
		b = g(b);
		return b.toString();
	}

	private static StringBuilder g(StringBuilder a) {
		StringBuilder b = new StringBuilder("");
		int i = 0;
		char c;
		char c2;
		char c3;

		while (i < a.length()) {
			c = a.charAt(i);
			if (c < 128) {
				b.append(c);
				i++;
			} else if ((c > 191) && (c < 224)) {
				c2 = a.charAt(i + 1);
				b.append((char) (((c & 31) << 6) | (c2 & 63)));
				i += 2;
			} else {
				c2 = a.charAt(i + 1);
				c3 = a.charAt(i + 2);
				b.append((char) (((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63)));
				i += 3;
			}
		}
		return b;
	}

}
