package com.dlscms.persistence;

import com.dlscms.exception.LogisticsException;

/**
 * Converts one CSV row into an entity.
 *
 * @param <T> entity type
 */
@FunctionalInterface
public interface RowParser<T> {
    /**
     * @param fields the row's fields
     * @return the entity
     * @throws LogisticsException if the row is invalid
     */
    T parse(String[] fields) throws LogisticsException;
}
