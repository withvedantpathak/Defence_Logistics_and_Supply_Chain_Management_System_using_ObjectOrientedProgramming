package com.dlscms.model;

/** An entity that can be written as one CSV row. */
public interface Persistable {
    /** @return the CSV header line for this entity type */
    String csvHeader();

    /** @return this entity as one CSV line (no trailing newline) */
    String toCsvRow();
}
