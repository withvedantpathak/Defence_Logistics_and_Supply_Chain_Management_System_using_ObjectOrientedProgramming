package com.dlscms.service;

import com.dlscms.exception.InvalidIdException;
import com.dlscms.exception.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic id-to-entity store (generics + collections). Ids are case-insensitive and kept sorted.
 *
 * @param <T> the entity type
 */
public final class Registry<T> {
    private final String kind;
    private final Function<T, String> idOf;
    private final Map<String, T> items = new TreeMap<>();

    /**
     * @param kind entity kind used in error messages, e.g. "base"
     * @param idOf function extracting the id of an entity
     */
    public Registry(String kind, Function<T, String> idOf) {
        this.kind = kind;
        this.idOf = idOf;
    }

    private static String norm(String id) {
        return id == null ? "" : id.trim().toUpperCase();
    }

    /**
     * Adds an entity.
     *
     * @param item the entity
     * @throws ValidationException if the id already exists
     */
    public void add(T item) throws ValidationException {
        String id = idOf.apply(item);
        if (items.containsKey(id)) {
            throw new ValidationException("Duplicate " + kind + " id " + id);
        }
        items.put(id, item);
    }

    /**
     * @param id entity id (any case)
     * @return the entity
     * @throws InvalidIdException if unknown
     */
    public T get(String id) throws InvalidIdException {
        T t = items.get(norm(id));
        if (t == null) {
            throw new InvalidIdException(kind, id == null ? "(none)" : id.trim());
        }
        return t;
    }

    /**
     * @param id entity id
     * @return true if known
     */
    public boolean contains(String id) {
        return items.containsKey(norm(id));
    }

    /**
     * Removes an entity.
     *
     * @param id entity id
     * @return the removed entity
     * @throws InvalidIdException if unknown
     */
    public T remove(String id) throws InvalidIdException {
        T t = get(id);
        items.remove(norm(id));
        return t;
    }

    /** @return all entities ordered by id (read-only) */
    public List<T> all() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }

    /**
     * @param p condition
     * @return entities satisfying the condition, ordered by id
     */
    public List<T> filter(Predicate<T> p) {
        List<T> out = new ArrayList<>();
        for (T t : items.values()) {
            if (p.test(t)) {
                out.add(t);
            }
        }
        return out;
    }

    /** @return number of entities */
    public int size() {
        return items.size();
    }
}
