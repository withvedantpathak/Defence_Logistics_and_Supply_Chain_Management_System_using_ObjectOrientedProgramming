package com.dlscms.sim;

/**
 * Metrics of one simulation run (one row of the raw results file).
 *
 * @param scale              dataset scale name
 * @param seed               dataset seed
 * @param sourceStrategy     donor strategy name
 * @param vehicleStrategy    vehicle strategy name
 * @param requests           requests injected
 * @param fulfilled          requests completely delivered by the end of the horizon
 * @param stockLimited       unfulfilled requests whose remaining units exceed all spare stock at other bases at the
 *                           end of the run (donors keep their reorder level)
 * @param vehicleLimited     unfulfilled requests with enough spare stock that are still waiting for a vehicle
 * @param inFlight           unfulfilled requests with enough spare stock whose remaining units are on the road
 * @param fulfilmentRate     fulfilled / requests
 * @param unitFillRate       delivered units / requested units
 * @param avgLeadHours       mean creation-to-completion hours over fulfilled requests (NaN if none)
 * @param avgLeadCritical    same for CRITICAL requests (NaN if none)
 * @param criticalWithin24h  share of CRITICAL requests completed within 24 h of arrival
 * @param utilisation        share of vehicle-hours spent dispatched
 * @param deliveries         delivered trips
 * @param totalKm            sum of haul distances of delivered trips
 * @param endHour            simulated hour at which the run stopped
 * @param wallMillis         wall-clock milliseconds of the simulation itself
 * @param usedMemoryMb       approximate heap in use at the end (MB)
 */
public record SimResult(String scale, long seed, String sourceStrategy, String vehicleStrategy, int requests,
                        int fulfilled, int stockLimited, int vehicleLimited, int inFlight, double fulfilmentRate, double unitFillRate, double avgLeadHours,
                        double avgLeadCritical, double criticalWithin24h, double utilisation, int deliveries,
                        double totalKm, long endHour, long wallMillis, long usedMemoryMb) {

    /** @return CSV header matching {@link #toCsvRow()} */
    public static String csvHeader() {
        return "scale,seed,source_strategy,vehicle_strategy,requests,fulfilled,stock_limited,vehicle_limited,in_flight,fulfilment_rate,unit_fill_rate,"
                + "avg_lead_h,avg_lead_critical_h,critical_within_24h,utilisation,deliveries,total_km,end_hour,"
                + "wall_ms,used_mb";
    }

    /** @return this result as one CSV line (fixed decimals so identical runs give identical text, except timing) */
    public String toCsvRow() {
        return String.join(",", scale, String.valueOf(seed), sourceStrategy, vehicleStrategy,
                String.valueOf(requests), String.valueOf(fulfilled), String.valueOf(stockLimited), String.valueOf(vehicleLimited),
                String.valueOf(inFlight), f(fulfilmentRate), f(unitFillRate),
                f(avgLeadHours), f(avgLeadCritical), f(criticalWithin24h), f(utilisation),
                String.valueOf(deliveries), String.format("%.1f", totalKm), String.valueOf(endHour),
                String.valueOf(wallMillis), String.valueOf(usedMemoryMb));
    }

    private static String f(double v) {
        return Double.isNaN(v) ? "" : String.format("%.4f", v);
    }
}
