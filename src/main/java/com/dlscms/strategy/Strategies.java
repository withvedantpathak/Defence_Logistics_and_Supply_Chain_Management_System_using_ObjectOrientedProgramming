package com.dlscms.strategy;

import com.dlscms.exception.ValidationException;
import java.util.List;

/** Factory that turns strategy names into strategy objects. */
public final class Strategies {
    /** Valid source-selection strategy names. */
    public static final List<String> SOURCE_NAMES = List.of("NEAREST", "SURPLUS", "BALANCED");
    /** Valid vehicle-assignment strategy names. */
    public static final List<String> VEHICLE_NAMES = List.of("SMALLEST", "FASTEST");

    private Strategies() { }

    /**
     * @param name NEAREST, SURPLUS or BALANCED
     * @return the strategy
     * @throws ValidationException if unknown
     */
    public static SourceSelectionStrategy source(String name) throws ValidationException {
        switch (name == null ? "" : name.trim().toUpperCase()) {
            case "NEAREST":
                return new NearestFirst();
            case "SURPLUS":
                return new MostSurplus();
            case "BALANCED":
                return new Balanced();
            default:
                throw new ValidationException("Unknown source strategy '" + name + "'; use " + SOURCE_NAMES);
        }
    }

    /**
     * @param name SMALLEST or FASTEST
     * @return the strategy
     * @throws ValidationException if unknown
     */
    public static VehicleAssignmentStrategy vehicle(String name) throws ValidationException {
        switch (name == null ? "" : name.trim().toUpperCase()) {
            case "SMALLEST":
                return new SmallestFit();
            case "FASTEST":
                return new FastestFit();
            default:
                throw new ValidationException("Unknown vehicle strategy '" + name + "'; use " + VEHICLE_NAMES);
        }
    }
}
