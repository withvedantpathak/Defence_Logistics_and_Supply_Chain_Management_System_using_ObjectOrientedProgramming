package com.dlscms.model;

import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.ValidationException;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;

/** A base's request for supplies. Ordered by priority (high first), then age, then id. */
public final class SupplyRequest implements Persistable, Comparable<SupplyRequest> {
    /** CSV header of requests.csv. */
    public static final String HEADER =
            "id,requester_base,supply,qty,fulfilled_qty,priority,status,created_hour";

    private final String id;
    private final String requesterBaseId;
    private final String supplyId;
    private final int qty;
    private int fulfilledQty;
    private int plannedQty; // units in PLANNED / IN_TRANSIT deliveries; rebuilt from deliveries on load
    private final Priority priority;
    private RequestStatus status;
    private final long createdAtHour;

    /**
     * @param id              request id
     * @param requesterBaseId destination base
     * @param supplyId        requested supply
     * @param qty             units requested
     * @param fulfilledQty    units already delivered
     * @param priority        urgency
     * @param status          current status
     * @param createdAtHour   simulated hour of creation
     * @throws ValidationException if invalid
     */
    public SupplyRequest(String id, String requesterBaseId, String supplyId, int qty, int fulfilledQty,
                         Priority priority, RequestStatus status, long createdAtHour) throws ValidationException {
        this.id = Validator.id(id, "request id");
        this.requesterBaseId = Validator.id(requesterBaseId, "base id");
        this.supplyId = Validator.id(supplyId, "supply id");
        this.qty = Validator.positive(qty, "quantity");
        this.fulfilledQty = Validator.nonNegative(fulfilledQty, "fulfilled quantity");
        if (fulfilledQty > qty) {
            throw new ValidationException("fulfilled quantity exceeds requested quantity");
        }
        if (priority == null || status == null) {
            throw new ValidationException("priority and status are required");
        }
        this.priority = priority;
        this.status = status;
        this.createdAtHour = createdAtHour;
    }

    /** @return request id */
    public String getId() {
        return id;
    }

    /** @return destination base id */
    public String getRequesterBaseId() {
        return requesterBaseId;
    }

    /** @return supply id */
    public String getSupplyId() {
        return supplyId;
    }

    /** @return units requested */
    public int getQty() {
        return qty;
    }

    /** @return units delivered so far */
    public int getFulfilledQty() {
        return fulfilledQty;
    }

    /** @return units on planned or in-transit deliveries */
    public int getPlannedQty() {
        return plannedQty;
    }

    /** @param planned units on open deliveries (used when rebuilding state after loading) */
    public void setPlannedQty(int planned) {
        this.plannedQty = planned;
    }

    /** @return urgency */
    public Priority getPriority() {
        return priority;
    }

    /** @return lifecycle status */
    public RequestStatus getStatus() {
        return status;
    }

    /** @return hour of creation */
    public long getCreatedAtHour() {
        return createdAtHour;
    }

    /** @return units still to be planned */
    public int remaining() {
        return qty - fulfilledQty - plannedQty;
    }

    private void require(boolean ok, String action) throws InvalidStateTransitionException {
        if (!ok) {
            throw new InvalidStateTransitionException("Request " + id + " is " + status + "; cannot " + action);
        }
    }

    /** @throws InvalidStateTransitionException unless NEW */
    public void approve() throws InvalidStateTransitionException {
        require(status == RequestStatus.NEW, "approve");
        status = RequestStatus.APPROVED;
    }

    /** @throws InvalidStateTransitionException unless NEW or APPROVED with nothing planned or delivered */
    public void reject() throws InvalidStateTransitionException {
        require((status == RequestStatus.NEW || status == RequestStatus.APPROVED)
                && plannedQty == 0 && fulfilledQty == 0, "reject");
        status = RequestStatus.REJECTED;
    }

    /** @throws InvalidStateTransitionException unless NEW or APPROVED with nothing planned or delivered */
    public void cancel() throws InvalidStateTransitionException {
        require((status == RequestStatus.NEW || status == RequestStatus.APPROVED)
                && plannedQty == 0 && fulfilledQty == 0, "cancel (fail its deliveries first)");
        status = RequestStatus.CANCELLED;
    }

    /**
     * A delivery was planned for this request.
     *
     * @param n units
     * @throws InvalidStateTransitionException unless APPROVED or FULFILLING
     */
    public void onPlanned(int n) throws InvalidStateTransitionException {
        require(status == RequestStatus.APPROVED || status == RequestStatus.FULFILLING, "plan deliveries");
        plannedQty += n;
        status = RequestStatus.FULFILLING;
    }

    /**
     * A delivery arrived.
     *
     * @param n units
     */
    public void onDelivered(int n) {
        plannedQty -= n;
        fulfilledQty += n;
        if (fulfilledQty >= qty) {
            status = RequestStatus.FULFILLED;
        } else if (plannedQty == 0) {
            status = RequestStatus.APPROVED;
        }
    }

    /**
     * A delivery failed.
     *
     * @param n units
     */
    public void onFailed(int n) {
        plannedQty -= n;
        if (plannedQty == 0 && status == RequestStatus.FULFILLING) {
            status = RequestStatus.APPROVED;
        }
    }

    @Override
    public int compareTo(SupplyRequest o) {
        int c = Integer.compare(o.priority.rank(), priority.rank());
        if (c != 0) {
            return c;
        }
        c = Long.compare(createdAtHour, o.createdAtHour);
        return c != 0 ? c : id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SupplyRequest && ((SupplyRequest) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String csvHeader() {
        return HEADER;
    }

    @Override
    public String toCsvRow() {
        return Csv.join(id, requesterBaseId, supplyId, qty, fulfilledQty, priority, status, createdAtHour);
    }

    /**
     * Builds a request from CSV fields.
     *
     * @param f fields in header order
     * @return the request
     * @throws ValidationException if invalid
     */
    public static SupplyRequest fromCsv(String[] f) throws ValidationException {
        RequestStatus st;
        try {
            st = RequestStatus.valueOf(f[6].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("unknown request status '" + f[6] + "'");
        }
        return new SupplyRequest(f[0], f[1], f[2], Validator.parseInt(f[3], "qty"),
                Validator.parseInt(f[4], "fulfilled_qty"), Priority.parse(f[5]), st,
                Long.parseLong(f[7].trim()));
    }
}
