package com.dlscms.model;

import com.dlscms.exception.ValidationException;
import com.dlscms.util.Distance;
import com.dlscms.util.Validator;
import java.util.Objects;

/** Immutable geographic location of a base (encapsulation: fields are private and final). */
public final class Location {
    private final String city;
    private final double lat;
    private final double lon;

    /**
     * @param city city label
     * @param lat  latitude in degrees (-90..90)
     * @param lon  longitude in degrees (-180..180)
     * @throws ValidationException if out of range or blank
     */
    public Location(String city, double lat, double lon) throws ValidationException {
        this.city = Validator.text(city, "city");
        this.lat = Validator.range(lat, -90, 90, "latitude");
        this.lon = Validator.range(lon, -180, 180, "longitude");
    }

    /** @return city label */
    public String city() {
        return city;
    }

    /** @return latitude in degrees */
    public double lat() {
        return lat;
    }

    /** @return longitude in degrees */
    public double lon() {
        return lon;
    }

    /**
     * @param other another location
     * @return straight-line distance in km
     */
    public double distanceKm(Location other) {
        return Distance.haversineKm(lat, lon, other.lat, other.lon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }
        Location l = (Location) o;
        return Double.compare(lat, l.lat) == 0 && Double.compare(lon, l.lon) == 0 && city.equals(l.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, lat, lon);
    }

    @Override
    public String toString() {
        return String.format("%s (%.2fN %.2fE)", city, lat, lon);
    }
}
