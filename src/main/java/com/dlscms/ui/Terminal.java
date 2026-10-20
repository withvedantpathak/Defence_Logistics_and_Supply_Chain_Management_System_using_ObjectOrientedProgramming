package com.dlscms.ui;

import com.dlscms.util.Style;
import com.dlscms.util.Table;
import java.util.List;

/** Text rendering helper: maps semantic styles to ANSI colours (or nothing in plain mode). */
public final class Terminal {
    /** Screen width in columns. */
    public static final int WIDTH = 100;
    private static final String ESC = "\u001b[";
    private static final String BASE = ESC + "0m" + ESC + "48;2;0;0;0m" + ESC + "38;2;255;176;0m";
    private static final String RESET = ESC + "0m";

    private final boolean ansi;

    /** @param ansi true for colour output, false for plain ASCII-safe output */
    public Terminal(boolean ansi) {
        this.ansi = ansi;
    }

    /** @return true if colours are enabled */
    public boolean isAnsi() {
        return ansi;
    }

    /**
     * @param text text to colour
     * @param s    semantic style
     * @return the text wrapped in escape codes (unchanged in plain mode)
     */
    public String paint(String text, Style s) {
        if (!ansi) {
            return text;
        }
        String code;
        switch (s) {
            case BOLD: code = ESC + "1;97m"; break;
            case ID: code = ESC + "96m"; break;
            case GOOD: code = ESC + "92m"; break;
            case BAD: code = ESC + "91m"; break;
            case WARN: code = ESC + "93m"; break;
            case DIM: code = ESC + "90m"; break;
            default: code = ""; break;
        }
        return code.isEmpty() ? text : code + text + BASE;
    }

    /** @return escape sequence that clears the screen and sets amber-on-black (empty in plain mode) */
    public String clear() {
        return ansi ? BASE + ESC + "2J" + ESC + "H" : "";
    }

    /** @return the terminating reset sequence (empty in plain mode) */
    public String end() {
        return ansi ? RESET : "";
    }

    /** @return a horizontal rule */
    public String rule() {
        return paint((ansi ? "─" : "-").repeat(WIDTH), Style.DIM);
    }

    /** @return the symbol for "below reorder level" */
    public String down() {
        return ansi ? "▼" : "v";
    }

    /** @return the symbol for "healthy" */
    public String up() {
        return ansi ? "▲" : "^";
    }

    /** @return the prompt arrow */
    public String arrow() {
        return ansi ? "▸" : ">";
    }

    /**
     * @param s text
     * @param w width
     * @return s padded on the right (or cut) to width w
     */
    public static String padRight(String s, int w) {
        return s.length() >= w ? s.substring(0, w) : s + " ".repeat(w - s.length());
    }

    /**
     * @param s text
     * @param w width
     * @return s padded on the left (or cut) to width w
     */
    public static String padLeft(String s, int w) {
        return s.length() >= w ? s.substring(0, w) : " ".repeat(w - s.length()) + s;
    }

    /**
     * Renders a table as aligned columns: numbers right-aligned, ids in cyan, header in white.
     *
     * @param t      table
     * @param indent left margin
     * @return rendered lines joined by newlines (a notice if the table is empty)
     */
    public String table(Table t, int indent) {
        List<String> h = t.headers();
        int n = h.size();
        int[] w = new int[n];
        for (int c = 0; c < n; c++) {
            w[c] = h.get(c).length();
            for (String[] r : t.rows()) {
                w[c] = Math.max(w[c], r[c].length());
            }
        }
        String pad = " ".repeat(indent);
        StringBuilder sb = new StringBuilder();
        sb.append(pad);
        for (int c = 0; c < n; c++) {
            String cell = Table.isNumeric(h.get(c)) || numericColumn(t, c) ? padLeft(h.get(c), w[c])
                    : padRight(h.get(c), w[c]);
            sb.append(paint(cell, Style.BOLD)).append("  ");
        }
        sb.append('\n');
        if (t.rows().isEmpty()) {
            return sb.append(pad).append(paint("(none)", Style.DIM)).toString();
        }
        for (String[] r : t.rows()) {
            sb.append(pad);
            for (int c = 0; c < n; c++) {
                boolean num = numericColumn(t, c);
                String cell = num ? padLeft(r[c], w[c]) : padRight(r[c], w[c]);
                Style st = t.styleOf(c, r[c]);
                if (st == Style.NORMAL && c == 0) {
                    st = Style.ID;
                }
                sb.append(paint(cell, st)).append("  ");
            }
            sb.append('\n');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static boolean numericColumn(Table t, int c) {
        if (t.rows().isEmpty()) {
            return false;
        }
        for (String[] r : t.rows()) {
            if (!r[c].isEmpty() && !Table.isNumeric(r[c])) {
                return false;
            }
        }
        return true;
    }
}
