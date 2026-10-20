package com.dlscms.model;

import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.ValidationException;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;

/** One trip of one vehicle carrying one supply from a source base to a destination base. */
public final class Delivery implements Deliverable, Trackable, Persistable {
    /** CSV header of deliveries.csv. */
    public static final String HEADER = "id,request,source,dest,supply,qty,vehicle,status,planned_hour,"
            + "dispatched_hour,eta_hour,delivered_hour,distance_km,failure_reason";

    private final String id;
    private final String requestId;
    private final String sourceBaseId;
    private final String destBaseId;
    private final String supplyId;
    private final int qty;
    private final String vehicleId;
    private DeliveryStatus status;
    private final long plannedAtHour;
    private long dispatchedAtHour;
    private long etaHour;
    private long deliveredAtHour;
    private final double distanceKm;
    private String failureReason;

    /**
     * @param id              delivery id
     * @param requestId       request served
     * @param sourceBaseId    donor base
     * @param destBaseId      requesting base
     * @param supplyId        supply carried
     * @param qty             units carried
     * @param vehicleId       vehicle used
     * @param status          status
     * @param plannedAtHour   hour planned
     * @param dispatchedAtHour hour dispatched or -1
     * @param etaHour         estimated (or actual) arrival hour
     * @param deliveredAtHour hour delivered or -1
     * @param distanceKm      source to destination distance
     * @param failureReason   reason if failed, else empty
     * @throws ValidationException if invalid
     */
    public Delivery(String id, String requestId, String sourceBaseId, String destBaseId, String supplyId,
                    int qty, String vehicleId, DeliveryStatus status, long plannedAtHour,
                    long dispatchedAtHour, long etaHour, long deliveredAtHour, double distanceKm,
                    String failureReason) throws ValidationException {
        this.id = Validator.id(id, "delivery id");
        this.requestId = Validator.id(requestId, "request id");
        this.sourceBaseId = Validator.id(sourceBaseId, "source base");
        this.destBaseId = Validator.id(destBaseId, "destination base");
        this.supplyId = Validator.id(supplyId, "supply id");
        this.qty = Validator.positive(qty, "quantity");
        this.vehicleId = Validator.id(vehicleId, "vehicle id");
        if (status == null) {
            throw new ValidationException("delivery status is required");
        }
        this.status = status;
        this.plannedAtHour = plannedAtHour;
        this.dispatchedAtHour = dispatchedAtHour;
        this.etaHour = etaHour;
        this.deliveredAtHour = deliveredAtHour;
        this.distanceKm = distanceKm;
        this.failureReason = failureReason == null ? "" : failureReason;
    }

    /** @return delivery id */
    public String getId() {
        return id;
    }

    /** @return request id */
    public String getRequestId() {
        return requestId;
    }

    /** @return source base id */
    public String getSourceBaseId() {
        return sourceBaseId;
    }

    /** @return destination base id */
    public String getDestBaseId() {
        return destBaseId;
    }

    /** @return supply id */
    public String getSupplyId() {
        return supplyId;
    }

    /** @return units carried */
    public int getQty() {
        return qty;
    }

    /** @return vehicle id */
    public String getVehicleId() {
        return vehicleId;
    }

    /** @return status */
    public DeliveryStatus getStatus() {
        return status;
    }

    /** @return hour planned */
    public long getPlannedAtHour() {
        return plannedAtHour;
    }

    /** @return hour dispatched, or -1 */
    public long getDispatchedAtHour() {
        return dispatchedAtHour;
    }

    /** @return estimated arrival hour */
    public long getEtaHour() {
        return etaHour;
    }

    /** @return hour delivered, or -1 */
    public long getDeliveredAtHour() {
        return deliveredAtHour;
    }

    /** @return source to destination distance in km */
    public double getDistanceKm() {
        return distanceKm;
    }

    /** @return failure reason or empty text */
    public String getFailureReason() {
        return failureReason;
    }

    /** @return planned trip duration in hours (at least 1), fixed when the delivery is planned */
    public long durationHours() {
        return Math.max(1, etaHour - plannedAtHour);
    }

    @Override
    public void dispatch(long hour) throws InvalidStateTransitionException {
        if (status != DeliveryStatus.PLANNED) {
            throw new InvalidStateTransitionException("Delivery " + id + " is " + status + "; cannot dispatch");
        }
        long duration = durationHours();
        status = DeliveryStatus.IN_TRANSIT;
        dispatchedAtHour = hour;
        etaHour = hour + duration;
    }

    @Override
    public void complete(long hour) throws InvalidStateTransitionException {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new InvalidStateTransitionException("Delivery " + id + " is " + status + "; cannot complete");
        }
        status = DeliveryStatus.DELIVERED;
        deliveredAtHour = hour;
    }

    @Override
    public void fail(String reason, long hour) throws InvalidStateTransitionException {
        if (!status.isOpen()) {
            throw new InvalidStateTransitionException("Delivery " + id + " is " + status + "; cannot fail");
        }
        status = DeliveryStatus.FAILED;
        failureReason = reason == null ? "" : reason;
        deliveredAtHour = hour;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String statusLabel() {
        return status.name();
    }

    @Override
    public String locationLabel() {
        return sourceBaseId + "->" + destBaseId;
    }

    @Override
    public String csvHeader() {
        return HEADER;
    }

    @Override
    public String toCsvRow() {
        return Csv.join(id, requestId, sourceBaseId, destBaseId, supplyId, qty, vehicleId, status,
                plannedAtHour, dispatchedAtHour, etaHour, deliveredAtHour,
                String.format("%.1f", distanceKm), failureReason);
    }

    /**
     * Builds a delivery from CSV fields.
     *
     * @param f fields in header order
     * @return the delivery
     * @throws ValidationException if invalid
     */
    public static Delivery fromCsv(String[] f) throws ValidationException {
        DeliveryStatus st;
        try {
            st = DeliveryStatus.valueOf(f[7].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("unknown delivery status '" + f[7] + "'");
        }
        return new Delivery(f[0], f[1], f[2], f[3], f[4], Validator.parseInt(f[5], "qty"), f[6], st,
                Long.parseLong(f[8].trim()), Long.parseLong(f[9].trim()), Long.parseLong(f[10].trim()),
                Long.parseLong(f[11].trim()), Validator.parseDouble(f[12], "distance_km"),
                f.length > 13 ? f[13] : "");
    }
}
