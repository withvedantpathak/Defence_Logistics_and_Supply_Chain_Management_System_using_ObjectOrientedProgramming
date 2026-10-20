package com.dlscms.sim;

import com.dlscms.model.Priority;

/**
 * One timed request from a scenario file.
 *
 * @param id       scenario id such as S-0001
 * @param baseId   requesting base
 * @param supplyId requested supply
 * @param qty      units
 * @param priority urgency
 * @param hour     simulated hour at which the request arrives (day * 24 + hour of day)
 */
public record ScenarioRequest(String id, String baseId, String supplyId, int qty, Priority priority, long hour) { }
