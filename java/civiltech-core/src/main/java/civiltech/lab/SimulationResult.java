package civiltech.lab;

import civiltech.io.JsonWriter;

/** Everything a simulation produces. Arrays are indexed by computational step unless noted. */
public final class SimulationResult {

    // ----- catchment and method parameters
    public double areaKm2;
    public double weightedCurveNumber;
    public double longestFlowPathM;
    public double longestPathDropM;
    public double longestPathSlope;
    public double tcHours;
    public boolean tcFromKirpich;
    public double uniformVelocityMs;
    public double uhPeakTimeHours;
    public double uhPeakRatePerMm;
    public double uhVolumeCorrection;
    public double stepHours;

    // ----- time series (length = steps + 1, value at t = s * stepHours)
    public double[] timeHours;
    public double[] rainIntensityMmH;      // catchment rainfall intensity during the step that starts at t
    public double[] excessIntensityMmH;    // area-averaged excess intensity during the step that starts at t
    public double[] flowScs;               // m3/s, SCS unit hydrograph
    public double[] flowTimeArea;          // m3/s, distributed time-area translation
    public double[] flowClark;             // m3/s, time-area + linear reservoir (equals flowTimeArea if R = 0)

    // ----- hourly summary (length = hours of simulation)
    public double[] hourlyRainMm;
    public double[] hourlyExcessMm;
    public double[] hourlyMeanFlowScs;
    public double[] hourlyMeanFlowClark;

    // ----- totals
    public double totalRainMm;
    public double totalExcessMm;
    public double runoffCoefficient;
    public double peakScs, peakScsTimeHours, volumeScsM3;
    public double peakTimeArea, peakTimeAreaTimeHours, volumeTimeAreaM3;
    public double peakClark, peakClarkTimeHours, volumeClarkM3;
    public double excessVolumeM3;

    // ----- per-cell and animation data
    /** Curve number of each grid cell (0 outside the catchment). */
    public double[] cellCurveNumber;
    /** Travel time to the outlet in hours of each grid cell (NaN outside). */
    public double[] cellTravelTimeHours;
    /** Distinct curve numbers present in the catchment. */
    public double[] classCurveNumber;
    /** Fraction of catchment area for each distinct curve number. */
    public double[] classAreaFraction;
    /** Frame times in hours. */
    public double[] frameTimeHours;
    /** Cumulative excess (mm) per frame and class, flattened [frame * classes + class]. */
    public double[] frameClassCumulativeExcessMm;
    /** Grid indices of animated stream cells. */
    public int[] channelCells;
    /** Translation discharge (m3/s) per frame and stream cell, flattened [frame * channels + c]. */
    public double[] frameChannelFlow;

    public String summaryJson() {
        JsonWriter w = new JsonWriter().beginObject();
        writeSummary(w);
        return w.endObject().toString();
    }

    /** Series, hourly table and summary (no per-cell arrays). */
    public String toJson() {
        JsonWriter w = new JsonWriter().beginObject();
        writeSummary(w);
        w.array("timeHours", timeHours, 4)
         .array("rainIntensityMmH", rainIntensityMmH, 3)
         .array("excessIntensityMmH", excessIntensityMmH, 3)
         .array("flowScs", flowScs, 3)
         .array("flowTimeArea", flowTimeArea, 3)
         .array("flowClark", flowClark, 3)
         .array("hourlyRainMm", hourlyRainMm, 3)
         .array("hourlyExcessMm", hourlyExcessMm, 3)
         .array("hourlyMeanFlowScs", hourlyMeanFlowScs, 3)
         .array("hourlyMeanFlowClark", hourlyMeanFlowClark, 3)
         .array("classCurveNumber", classCurveNumber, 2)
         .array("classAreaFraction", classAreaFraction, 5)
         .array("frameTimeHours", frameTimeHours, 4);
        return w.endObject().toString();
    }

    private void writeSummary(JsonWriter w) {
        w.field("areaKm2", areaKm2)
         .field("weightedCurveNumber", weightedCurveNumber)
         .field("longestFlowPathM", longestFlowPathM)
         .field("longestPathDropM", longestPathDropM)
         .field("longestPathSlope", longestPathSlope)
         .field("tcHours", tcHours)
         .field("tcFromKirpich", tcFromKirpich)
         .field("uniformVelocityMs", uniformVelocityMs)
         .field("uhPeakTimeHours", uhPeakTimeHours)
         .field("uhPeakRatePerMm", uhPeakRatePerMm)
         .field("uhVolumeCorrection", uhVolumeCorrection)
         .field("stepHours", stepHours)
         .field("totalRainMm", totalRainMm)
         .field("totalExcessMm", totalExcessMm)
         .field("runoffCoefficient", runoffCoefficient)
         .field("excessVolumeM3", excessVolumeM3)
         .field("peakScs", peakScs).field("peakScsTimeHours", peakScsTimeHours).field("volumeScsM3", volumeScsM3)
         .field("peakTimeArea", peakTimeArea).field("peakTimeAreaTimeHours", peakTimeAreaTimeHours)
         .field("volumeTimeAreaM3", volumeTimeAreaM3)
         .field("peakClark", peakClark).field("peakClarkTimeHours", peakClarkTimeHours)
         .field("volumeClarkM3", volumeClarkM3)
         .field("channelCount", channelCells == null ? 0 : channelCells.length);
    }
}
