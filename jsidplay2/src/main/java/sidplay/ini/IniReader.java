package sidplay.ini;

/*
 * Copyright (c) 2002 Stefan Matthias Aust.  All Rights Reserved.
 *
 * You are granted the right to use this code in a) GPL based projects in
 * which case this code shall be also protected by the GPL, or b) in other
 * projects as long as you make all modifications or extensions to this
 * code freely available, or c) make any other special agreement with the
 * copyright holder.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidutils.siddatabase.TimeConverter;

/**
 * This class can read properties files in Microsoft .ini file style and
 * provides an interface to read string, integer and boolean values. The .ini
 * files has the following structure:
 * 
 * <pre>
 * ; one type of comment
 * # another type of comment (will be removed in future)
 * [SectionNameWithNoSpaces]
 * Key string = whatever you want
 * </pre>
 * 
 * @author Antti S. Lankila
 */
public class IniReader {
	/**
	 * Commented line syntax.<BR>
	 * e.g. "; comment" or "# comment"
	 */
	private static final Pattern COMMENT = Pattern.compile("[;#].*");
	/**
	 * Section heading syntax<BR>
	 * e.g. "[Heading]"
	 */
	private static final Pattern SECTION_HEADING = Pattern.compile("\\[(\\w+)\\]");
	/**
	 * Key value syntax<BR>
	 * e.g. "Key String=Value String"
	 */
	private static final Pattern KEY_VALUE = Pattern.compile("(\\w+(?:\\s+\\w+)*)\\s*=\\s*(.*)");

	private final Map<String, Map<String, String>> sections = new LinkedHashMap<String, Map<String, String>>();

	private boolean dirty;

	public boolean isDirty() {
		return dirty;
	}

	public IniReader(final String pathname) throws IOException {
		this(new FileReader(pathname));
	}

	public IniReader(final InputStream input) throws IOException {
		this(new InputStreamReader(input));
	}

	public IniReader(final Reader input) throws IOException {
		initialize(new BufferedReader(input));
	}

	public String[] listSections() {
		return sections.keySet().toArray(new String[] {});
	}

	public String[] sectionProperties(String section) {
		Map<String, String> properties = sections.get(section);
		return properties.keySet().toArray(new String[] {});
	}

	private void initialize(final BufferedReader r) throws IOException {
		Map<String, String> section = null;
		StringBuilder comment = new StringBuilder();
		String line;

		while ((line = r.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty()) {
				continue;
			}

			Matcher m;

			m = COMMENT.matcher(line);
			if (m.matches()) {
				comment.append(line);
				comment.append(System.getProperty("line.separator"));
				continue;
			}

			m = SECTION_HEADING.matcher(line);
			if (m.matches()) {
				section = new LinkedHashMap<String, String>();
				sections.put(m.group(1), section);

				if (comment.length() != 0) {
					section.put("_", comment.toString());
					comment.delete(0, comment.length());
				}
				continue;
			}
			assert section != null;

			m = KEY_VALUE.matcher(line);
			if (m.matches()) {
				String key = m.group(1);
				section.put(key, m.group(2));
				if (comment.length() != 0) {
					section.put("_" + key, comment.toString());
					comment.delete(0, comment.length());
				}
				continue;
			}

			throw new RuntimeException(String.format("Unrecognized line in ini config: %s", line));
		}
	}

	public void save(String outputname) throws IOException {
		try (PrintWriter wr = new PrintWriter(new File(outputname))) {
			for (String section : sections.keySet()) {
				Map<String, String> unsaved = sections.get(section);
				if (unsaved.isEmpty()) {
					continue;
				}

				/* write section comment */
				if (unsaved.containsKey("_")) {
					wr.print(unsaved.get("_"));
				}
				// prevention to write empty sections (e.g. the default filter)
				if (section.length() > 0) {
					wr.println("[" + section + "]");

					for (String key : unsaved.keySet()) {
						if (key.startsWith("_")) {
							continue;
						}
						/* write key comment */
						if (unsaved.containsKey("_" + key)) {
							wr.print(unsaved.get("_" + key));
						}
						wr.println(key + "=" + unsaved.get(key));
					}

					wr.println("");
				}
			}
		}

		dirty = false;
	}

	public String getPropertyString(final String section, final String key, final String defaultValue) {
		final Map<String, String> map = sections.get(section);
		if (map != null) {
			final String value = map.get(key);
			if (value != null && value.length() > 0) {
				return value;
			}
		}
		return defaultValue;
	}

	public float getPropertyFloat(final String section, final String key, final float defaultValue) {
		final String s = getPropertyString(section, key, null);
		if (s != null) {
			return Float.parseFloat(s);
		}
		return defaultValue;
	}

	public int getPropertyInt(final String section, final String key, final int defaultValue) {
		final String s = getPropertyString(section, key, null);
		if (s != null && !s.equals("")) {
			return Integer.decode(s);
		}
		return defaultValue;
	}

	public boolean getPropertyBool(final String section, final String key, final boolean defaultValue) {
		final String s = getPropertyString(section, key, null);
		if (s != null) {
			return Boolean.valueOf(s);
		}
		return defaultValue;
	}

	public char getPropertyChar(final String section, final String key, final char defaultValue) {
		final String s = getPropertyString(section, key, null);
		if (s != null && s.length() > 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
			return s.substring(1, s.length() - 1).toCharArray()[0];
		}
		return defaultValue;
	}

	public int getPropertyTime(final String section, final String key, final int defaultValue) {
		final String s = getPropertyString(section, key, null);
		if (s != null) {
			return parseTime(s);
		}
		return defaultValue;
	}

	public <T extends Enum<T>> T getPropertyEnum(String section, String key, T defaultValue, Class<T> class1) {
		final String s = getPropertyString(section, key, null);
		if (s != null) {
			try {
				T value = (T) Enum.valueOf(class1, s.toUpperCase(Locale.US));
				return value;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		return defaultValue;
	}

	public void setProperty(String section, String key, Object value) {
		Map<String, String> settings = sections.get(section);
		if (settings == null) {
			sections.put(section, settings = new HashMap<String, String>());
		}
		String newValue = value instanceof Character ? "\'" + value + "\'"
				: value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value);

		String oldValue = null;
		if (settings.containsKey(key)) {
			oldValue = settings.get(key);
		}

		if (value == null) {
			settings.put(key, "");
			if (oldValue != null && !"".equals(oldValue)) {
				dirty = true;
			}
		} else {
			settings.put(key, newValue);
			if (!newValue.equals(oldValue)) {
				dirty = true;
			}
		}
	}

	private int parseTime(final String time) {
		int seconds = new TimeConverter().fromString(time).intValue();
		if (seconds == -1) {
			System.err.println("Invalid time, expected mm:ss (found " + time + ")");
		}
		return seconds;
	}

}