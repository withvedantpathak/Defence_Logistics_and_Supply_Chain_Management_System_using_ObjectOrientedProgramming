package com.dlscms.model;

import com.dlscms.exception.ValidationException;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;

/** A defence logistics base (node) that stores supplies and hosts vehicles. */
public final class Base implements Persistable {
    /** CSV header of bases.csv. */
    public static final String HEADER = "id,name,city,lat,lon,capacity_units";

    private final String id;
    private String name;
    private final Location location;
    private int capacityUnits;

    /**
     * @param id            unique id such as PUN
     * @param name          display name
     * @param location      where the base is
     * @param capacityUnits maximum total stock units
     * @throws ValidationException if any value is invalid
     */
    public Base(String id, String name, Location location, int capacityUnits) throws ValidationException {
        this.id = Validator.id(id, "base id");
        this.name = Validator.text(name, "base name");
        if (location == null) {
            throw new ValidationException("base location is required");
        }
        this.location = location;
        this.capacityUnits = Validator.positive(capacityUnits, "storage capacity");
    }

    /** @return base id */
    public String getId() {
        return id;
    }

    /** @return display name */
    public String getName() {
        return name;
    }

    /**
     * @param name new display name
     * @throws ValidationException if blank
     */
    public void setName(String name) throws ValidationException {
        this.name = Validator.text(name, "base name");
    }

    /** @return the location */
    public Location getLocation() {
        return location;
    }

    /** @return maximum total stock units */
    public int getCapacityUnits() {
        return capacityUnits;
    }

    /**
     * @param capacityUnits new capacity
     * @throws ValidationException if not positive
     */
    public void setCapacityUnits(int capacityUnits) throws ValidationException {
        this.capacityUnits = Validator.positive(capacityUnits, "storage capacity");
    }

    /**
     * @param other another base
     * @return straight-line distance in km
     */
    public double distanceKm(Base other) {
        return location.distanceKm(other.location);
    }

    @Override
    public String csvHeader() {
        return HEADER;
    }

    @Override
    public String toCsvRow() {
        return Csv.join(id, name, location.city(), String.format("%.2f", location.lat()),
                String.format("%.2f", location.lon()), capacityUnits);
    }

    /**
     * Builds a base from CSV fields.
     *
     * @param f fields in header order
     * @return the base
     * @throws ValidationException if any field is invalid
     */
    public static Base fromCsv(String[] f) throws ValidationException {
        Location loc = new Location(f[2], Validator.parseDouble(f[3], "lat"), Validator.parseDouble(f[4], "lon"));
        return new Base(f[0], f[1], loc, Validator.parseInt(f[5], "capacity_units"));
    }

    @Override
    public String toString() {
        return id + " " + name;
    }
}
