package com.dlscms.service;

import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/** Creates and manages supply requests. */
public final class RequestService {
    private final World world;

    /** @param world the shared network state */
    public RequestService(World world) {
        this.world = world;
    }

    /**
     * Creates a NEW request.
     *
     * @param baseId   requesting base
     * @param supplyId requested supply
     * @param qty      units
     * @param priority urgency
     * @return the new request
     * @throws LogisticsException if ids are unknown or qty is not positive
     */
    public SupplyRequest create(String baseId, String supplyId, int qty, Priority priority)
            throws LogisticsException {
        String b = world.bases.get(baseId).getId();
        String s = world.supplies.get(supplyId).getId();
        int before = world.requestIds.peekNumber();
        SupplyRequest r;
        try {
            r = new SupplyRequest(world.requestIds.next(), b, s, qty, 0, priority,
                    RequestStatus.NEW, world.clock.now());
            world.requests.add(r);
        } catch (LogisticsException e) {
            world.requestIds.setNext(before);        // a refused request must not burn an id
            throw e;
        }
        world.setDirty(true);
        return r;
    }

    /**
     * @param id request id
     * @return the request
     * @throws LogisticsException if unknown
     */
    public SupplyRequest get(String id) throws LogisticsException {
        return world.requests.get(id);
    }

    /**
     * @param id request id
     * @return the approved request
     * @throws LogisticsException if unknown or not NEW
     */
    public SupplyRequest approve(String id) throws LogisticsException {
        SupplyRequest r = get(id);
        r.approve();
        world.setDirty(true);
        return r;
    }

    /**
     * @param id request id
     * @return the rejected request
     * @throws LogisticsException if unknown or not rejectable
     */
    public SupplyRequest reject(String id) throws LogisticsException {
        SupplyRequest r = get(id);
        r.reject();
        world.setDirty(true);
        return r;
    }

    /**
     * @param id request id
     * @return the cancelled request
     * @throws LogisticsException if unknown or not cancellable
     */
    public SupplyRequest cancel(String id) throws LogisticsException {
        SupplyRequest r = get(id);
        r.cancel();
        world.setDirty(true);
        return r;
    }

    /** @return all requests, most urgent and oldest first */
    public List<SupplyRequest> sorted() {
        List<SupplyRequest> out = new ArrayList<>(world.requests.all());
        java.util.Collections.sort(out);
        return out;
    }

    /**
     * Requests that still need deliveries planned, in the order they should be served.
     * Uses a {@link PriorityQueue} ordered by {@link SupplyRequest#compareTo}.
     *
     * @return actionable requests, most urgent first
     */
    public List<SupplyRequest> actionable() {
        PriorityQueue<SupplyRequest> q = new PriorityQueue<>();
        for (SupplyRequest r : world.requests.all()) {
            boolean open = r.getStatus() == RequestStatus.APPROVED || r.getStatus() == RequestStatus.FULFILLING;
            if (open && r.remaining() > 0) {
                q.add(r);
            }
        }
        List<SupplyRequest> out = new ArrayList<>();
        while (!q.isEmpty()) {
            out.add(q.poll());
        }
        return out;
    }
}
