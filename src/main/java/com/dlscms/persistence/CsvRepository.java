package com.dlscms.persistence;

import com.dlscms.exception.DataFormatException;
import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Persistable;
import com.dlscms.util.Csv;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic CSV file repository for any {@link Persistable} entity type (generics + file handling).
 *
 * @param <T> entity type
 */
public final class CsvRepository<T extends Persistable> {
    private final String fileName;
    private final String header;
    private final RowParser<T> parser;

    /**
     * @param fileName file name inside the data folder, e.g. "bases.csv"
     * @param header   expected header line
     * @param parser   row parser
     */
    public CsvRepository(String fileName, String header, RowParser<T> parser) {
        this.fileName = fileName;
        this.header = header;
        this.parser = parser;
    }

    /**
     * Writes all items atomically (temporary file, then move).
     *
     * @param dir   target folder (created if missing)
     * @param items entities to write
     * @throws DataFormatException if the file cannot be written
     */
    public void save(Path dir, Collection<T> items) throws DataFormatException {
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Path tmp = dir.resolve(fileName + ".tmp");
            StringBuilder sb = new StringBuilder(header).append('\n');
            for (T t : items) {
                sb.append(t.toCsvRow()).append('\n');
            }
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DataFormatException("Cannot write " + fileName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads all items.
     *
     * @param dir      source folder
     * @param optional if true a missing file gives an empty list instead of an error
     * @return the entities in file order
     * @throws DataFormatException if the file is missing (and not optional), has a wrong header, a wrong
     *                             column count, or a row fails validation; the message names file and line
     */
    public List<T> load(Path dir, boolean optional) throws DataFormatException {
        Path file = dir.resolve(fileName);
        if (!Files.exists(file)) {
            if (optional) {
                return new ArrayList<>();
            }
            throw new DataFormatException("Missing data file " + fileName + " in " + dir);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataFormatException("Cannot read " + fileName + ": " + e.getMessage(), e);
        }
        if (lines.isEmpty() || !stripBom(lines.get(0)).trim().equals(header)) {
            throw new DataFormatException("Bad header in " + fileName + " (expected: " + header + ")");
        }
        int columns = Csv.split(header).length;
        List<T> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] f = Csv.split(line);
            if (f.length != columns) {
                throw new DataFormatException("Data format error in " + fileName + " line " + (i + 1)
                        + ": expected " + columns + " columns, found " + f.length);
            }
            try {
                out.add(parser.parse(f));
            } catch (LogisticsException | RuntimeException e) {
                throw new DataFormatException("Data format error in " + fileName + " line " + (i + 1)
                        + ": " + e.getMessage(), e);
            }
        }
        return out;
    }

    private static String stripBom(String s) {
        return s.startsWith("﻿") ? s.substring(1) : s;
    }
}
