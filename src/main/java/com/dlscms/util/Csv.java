package com.dlscms.util;

import java.util.ArrayList;
import java.util.List;

/** Minimal RFC-4180 style CSV helpers (commas, quotes and newlines inside quoted fields). */
public final class Csv {
    private Csv() { }

    /**
     * Joins values into one CSV line, quoting where needed.
     *
     * @param cols values (null becomes empty)
     * @return the CSV line without trailing newline
     */
    public static String join(Object... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cols[i] == null ? "" : String.valueOf(cols[i])));
        }
        return sb.toString();
    }

    /**
     * Escapes one field.
     *
     * @param s raw text
     * @return text, quoted if it contains a comma, quote or newline
     */
    public static String escape(String s) {
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    /**
     * Splits one CSV line into fields.
     *
     * @param line the line
     * @return the fields
     */
    public static String[] split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
