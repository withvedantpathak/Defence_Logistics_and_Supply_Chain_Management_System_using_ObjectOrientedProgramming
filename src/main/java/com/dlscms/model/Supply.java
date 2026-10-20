package com.dlscms.model;

import com.dlscms.exception.ValidationException;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;

/** A catalogue item (e.g. MED-KIT) that can be stored and transported. */
public final class Supply implements Persistable {
    /** CSV header of supplies.csv. */
    public static final String HEADER = "id,name,category,unit_weight_kg";

    private final String id;
    private final String name;
    private final SupplyCategory category;
    private final double unitWeightKg;

    /**
     * @param id           unique id such as MED-KIT
     * @param name         display name
     * @param category     category
     * @param unitWeightKg weight of one unit in kg
     * @throws ValidationException if any value is invalid
     */
    public Supply(String id, String name, SupplyCategory category, double unitWeightKg)
            throws ValidationException {
        this.id = Validator.id(id, "supply id");
        this.name = Validator.text(name, "supply name");
        if (category == null) {
            throw new ValidationException("supply category is required");
        }
        this.category = category;
        this.unitWeightKg = Validator.positive(unitWeightKg, "unit weight");
    }

    /** @return supply id */
    public String getId() {
        return id;
    }

    /** @return display name */
    public String getName() {
        return name;
    }

    /** @return category */
    public SupplyCategory getCategory() {
        return category;
    }

    /** @return weight of one unit in kg */
    public double getUnitWeightKg() {
        return unitWeightKg;
    }

    @Override
    public String csvHeader() {
        return HEADER;
    }

    @Override
    public String toCsvRow() {
        return Csv.join(id, name, category, String.format("%.1f", unitWeightKg));
    }

    /**
     * Builds a supply from CSV fields.
     *
     * @param f fields in header order
     * @return the supply
     * @throws ValidationException if invalid
     */
    public static Supply fromCsv(String[] f) throws ValidationException {
        SupplyCategory c;
        try {
            c = SupplyCategory.valueOf(f[2].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("unknown supply category '" + f[2] + "'");
        }
        return new Supply(f[0], f[1], c, Validator.parseDouble(f[3], "unit_weight_kg"));
    }

    @Override
    public String toString() {
        return id;
    }
}
