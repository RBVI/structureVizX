package edu.ucsf.rbvi.structureVizX.internal.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides some simple utility methods for handling strings
 * 
 * @author scooter
 * 
 */
public class StringUtils {
	public static String[] tokenize(String line) {
		String tline = line.trim();
		List<String> list = new ArrayList<String>();

		// Split on spaces, but preserve quoted strings
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(tline);
		while (m.find()) {
			list.add(m.group(1).replace("\"",""));
		}

		return list.toArray(new String[1]);
	}
}
