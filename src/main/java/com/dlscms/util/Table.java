package com.dlscms.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Plain tabular data (headers + rows) with optional per-column styling hints.
 * Used by reports, screens and CSV export so all three show the same numbers.
 */
public final class Table {
    private final String title;
    private final List<String> headers;
    private final List<String[]> rows = new ArrayList<>();
    private final Map<Integer, Function<String, Style>> stylers = new HashMap<>();

    /**
     * @param title   table title (also used as the report name)
     * @param headers column headings
     */
    public Table(String title, String... headers) {
        this.title = title;
        this.headers = List.of(headers);
    }

    /**
     * Adds a row.
     *
     * @param cells one value per column
     * @return this table
     */
    public Table add(Object... cells) {
        String[] r = new String[headers.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = i < cells.length && cells[i] != null ? String.valueOf(cells[i]) : "";
        }
        rows.add(r);
        return this;
    }

    /**
     * Sets a function that picks the style of cells in a column.
     *
     * @param col    zero-based column
     * @param styler maps cell text to a style
     * @return this table
     */
    public Table style(int col, Function<String, Style> styler) {
        stylers.put(col, styler);
        return this;
    }

    /** @return the title */
    public String title() {
        return title;
    }

    /** @return column headings */
    public List<String> headers() {
        return headers;
    }

    /** @return the rows (read-only copy) */
    public List<String[]> rows() {
        return List.copyOf(rows);
    }

    /**
     * @param col zero-based column
     * @param text cell text
     * @return style for the cell
     */
    public Style styleOf(int col, String text) {
        Function<String, Style> f = stylers.get(col);
        return f == null ? Style.NORMAL : f.apply(text);
    }

    /** @return number of rows */
    public int size() {
        return rows.size();
    }

    /** @return the table as CSV text (header line then one line per row) */
    public String toCsv() {
        StringBuilder sb = new StringBuilder(Csv.join(headers.toArray()) + "\n");
        for (String[] r : rows) {
            sb.append(Csv.join((Object[]) r)).append('\n');
        }
        return sb.toString();
    }

    /** @return true when the cell text looks numeric (right-aligned on screen) */
    public static boolean isNumeric(String s) {
        return s.matches("[-+]?[0-9][0-9,]*(\\.[0-9]+)?%?h?");
    }
}
