package civiltech.lab;

import civiltech.hydrology.HourlyRainfall;
import civiltech.hydrology.Kirpich;
import civiltech.hydrology.LinearReservoir;
import civiltech.hydrology.ScsCurveNumber;
import civiltech.hydrology.ScsUnitHydrograph;
import civiltech.terrain.FlowNetwork;
import civiltech.terrain.TerrainGrid;
import civiltech.terrain.Watershed;

/**
 * Educational event model of hourly rainfall-runoff for a catchment delineated from a DEM.
 *
 * <ol>
 *   <li>Terrain: Priority-Flood depression filling, D8 flow directions, flow accumulation.</li>
 *   <li>Catchment: all cells draining to the chosen outlet; longest flow path, slope, Kirpich Tc.</li>
 *   <li>Runoff generation: SCS Curve Number applied cell by cell (CN from land cover and soil group).</li>
 *   <li>Routing A: SCS curvilinear unit hydrograph of the whole catchment (lumped).</li>
 *   <li>Routing B: distributed time-area translation along D8 paths, travel times scaled to Tc,
 *       optionally followed by a Clark linear reservoir.</li>
 * </ol>
 *
 * This is a teaching model. It has not been calibrated or validated against observed discharge,
 * so its numbers illustrate the methods; they are not design or forecast values.
 */
public final class CatchmentModel {

    private final TerrainGrid grid;
    private final FlowNetwork network;
    private final int[] landCover;
    private Watershed watershed;

    public CatchmentModel(TerrainGrid grid, int[] landCover) {
        if (landCover.length != grid.size()) {
            throw new IllegalArgumentException("land cover must have one value per cell");
        }
        this.grid = grid;
        this.landCover = landCover.clone();
        this.network = FlowNetwork.build(grid);
    }

    public TerrainGrid grid() { return grid; }
    public FlowNetwork network() { return network; }
    public Watershed watershed() { return watershed; }
    public int landCover(int index) { return landCover[index]; }

    /** Delineates the catchment of the stream cell nearest to (row, col) within snapRadius cells. */
    public Watershed delineate(int row, int col, int snapRadius) {
        watershed = new Watershed(network, network.snapToStream(row, col, snapRadius));
        return watershed;
    }

    public SimulationResult simulate(HourlyRainfall rain, SimulationSettings set) {
        set.validate();
        if (watershed == null) {
            throw new IllegalStateException("delineate a catchment first");
        }
        final Watershed ws = watershed;
        final int n = grid.size();
        final double cellArea = grid.cellArea();
        SimulationResult res = new SimulationResult();

        // ---------- members and curve numbers
        int[] cells = new int[ws.cellCount()];
        int m = 0;
        for (int i = 0; i < n; i++) {
            if (ws.contains(i)) {
                cells[m++] = i;
            }
        }
        double[] cellCn = new double[n];
        double[] distinct = new double[16];
        int nClass = 0;
        int[] classOf = new int[n];
        double cnSum = 0;
        for (int k : cells) {
            int lc = landCover[k];
            double cn = (lc >= 0 && lc < set.curveNumberByClass.length) ? set.curveNumberByClass[lc] : 0.0;
            if (!(cn > 0.0 && cn <= 100.0)) {
                throw new IllegalArgumentException("no valid curve number for land-cover class " + lc);
            }
            cellCn[k] = cn;
            cnSum += cn;
            int c = 0;
            while (c < nClass && distinct[c] != cn) {
                c++;
            }
            if (c == nClass) {
                if (nClass == distinct.length) {
                    distinct = java.util.Arrays.copyOf(distinct, nClass * 2);
                }
                distinct[nClass++] = cn;
            }
            classOf[k] = c;
        }
        double[] classCn = java.util.Arrays.copyOf(distinct, nClass);
        double[] classFrac = new double[nClass];
        for (int k : cells) {
            classFrac[classOf[k]] += 1.0 / cells.length;
        }
        double[] retention = new double[nClass];
        for (int c = 0; c < nClass; c++) {
            retention[c] = ScsCurveNumber.retentionMm(classCn[c]);
        }

        res.areaKm2 = ws.areaSquareKilometres();
        res.weightedCurveNumber = cnSum / cells.length;
        res.cellCurveNumber = cellCn;
        res.classCurveNumber = classCn;
        res.classAreaFraction = classFrac;

        // ---------- geometry and time of concentration
        res.longestFlowPathM = ws.longestFlowPathMetres();
        res.longestPathDropM = ws.longestPathDropMetres();
        res.longestPathSlope = ws.longestPathSlope();
        if (set.tcOverrideHours > 0) {
            res.tcHours = set.tcOverrideHours;
            res.tcFromKirpich = false;
        } else {
            if (res.longestFlowPathM <= 0) {
                throw new IllegalStateException("catchment is a single cell; choose another outlet or set Tc");
            }
            res.tcHours = Kirpich.hours(res.longestFlowPathM, res.longestPathSlope);
            res.tcFromKirpich = true;
        }
        final double tc = res.tcHours;
        res.uniformVelocityMs = res.longestFlowPathM > 0 ? res.longestFlowPathM / (tc * 3600.0) : 0.0;

        // ---------- travel times to the outlet
        double[] travel = new double[n];
        java.util.Arrays.fill(travel, Double.NaN);
        if (!set.slopeWeightedVelocity || res.longestFlowPathM <= 0) {
            double l = Math.max(res.longestFlowPathM, 1e-9);
            for (int k : cells) {
                travel[k] = ws.flowLength(k) / l * tc;
            }
        } else {
            int[] order = network.upstreamFirstOrder();
            double[] raw = new double[n];
            double maxRaw = 0;
            for (int p = n - 1; p >= 0; p--) {            // downstream first
                int c = order[p];
                if (!ws.contains(c) || c == ws.outlet()) {
                    continue;
                }
                int r = network.receiver(c);
                double step = network.stepLength(c);
                double slope = Math.max(set.minimumStepSlope, (grid.elevation(c) - grid.elevation(r)) / step);
                raw[c] = raw[r] + step / Math.sqrt(slope);
                maxRaw = Math.max(maxRaw, raw[c]);
            }
            for (int k : cells) {
                travel[k] = maxRaw > 0 ? raw[k] / maxRaw * tc : 0.0;
            }
        }
        double maxTravel = 0;
        for (int k : cells) {
            maxTravel = Math.max(maxTravel, travel[k]);
        }
        res.cellTravelTimeHours = travel;

        // ---------- time axis
        final double dt = set.stepMinutes / 60.0;
        final int perHour = 60 / set.stepMinutes;
        ScsUnitHydrograph uh = new ScsUnitHydrograph(res.areaKm2, tc, dt);
        res.uhPeakTimeHours = uh.peakTimeHours();
        res.uhPeakRatePerMm = uh.peakRate();
        res.uhVolumeCorrection = uh.volumeCorrection();
        res.stepHours = dt;
        double tail = Math.max(5.0 * uh.peakTimeHours(), maxTravel + 6.0 * set.clarkStorageHours) + 1.0;
        int hoursTotal = rain.hours() + (int) Math.ceil(tail);
        int steps = hoursTotal * perHour;
        res.timeHours = new double[steps + 1];
        for (int s = 0; s <= steps; s++) {
            res.timeHours[s] = s * dt;
        }

        // ---------- runoff generation (cumulative excess per class at the step boundaries)
        double[][] cum = new double[nClass][steps + 1];
        for (int c = 0; c < nClass; c++) {
            for (int s = 0; s <= steps; s++) {
                cum[c][s] = excess(rain.cumulative(s * dt), retention[c]);
            }
        }
        double[] excessStep = new double[steps];
        res.rainIntensityMmH = new double[steps + 1];
        res.excessIntensityMmH = new double[steps + 1];
        for (int s = 0; s < steps; s++) {
            double e = 0;
            for (int c = 0; c < nClass; c++) {
                e += classFrac[c] * (cum[c][s + 1] - cum[c][s]);
            }
            excessStep[s] = e;
            res.rainIntensityMmH[s] = rain.intensity(s * dt + dt / 2.0);
            res.excessIntensityMmH[s] = e / dt;
        }
        res.totalRainMm = rain.total();
        double totalExcess = 0;
        for (int c = 0; c < nClass; c++) {
            totalExcess += classFrac[c] * cum[c][steps];
        }
        res.totalExcessMm = totalExcess;
        res.runoffCoefficient = res.totalRainMm > 0 ? totalExcess / res.totalRainMm : 0.0;
        res.excessVolumeM3 = totalExcess / 1000.0 * res.areaKm2 * 1.0e6;

        // ---------- routing A: SCS unit hydrograph
        res.flowScs = uh.convolve(excessStep, steps + 1);

        // ---------- routing B: time-area translation (centred step averages, mass conservative)
        double toFlow = cellArea / 1000.0 / (dt * 3600.0);
        double[] qta = new double[steps + 1];
        double rainEnd = rain.hours();
        for (int k : cells) {
            double tk = travel[k];
            double sk = retention[classOf[k]];
            int s0 = Math.max(0, (int) Math.floor((tk - dt) / dt));
            int s1 = Math.min(steps, (int) Math.ceil((tk + rainEnd + dt) / dt));
            double prev = excess(rain.cumulative(s0 * dt - dt / 2.0 - tk), sk);
            for (int s = s0; s <= s1; s++) {
                double next = excess(rain.cumulative(s * dt + dt / 2.0 - tk), sk);
                qta[s] += (next - prev) * toFlow;
                prev = next;
            }
        }
        res.flowTimeArea = qta;
        res.flowClark = LinearReservoir.route(qta, set.clarkStorageHours, dt);

        double[] pk = peak(res.flowScs, dt);
        res.peakScs = pk[0]; res.peakScsTimeHours = pk[1]; res.volumeScsM3 = volume(res.flowScs, dt);
        pk = peak(res.flowTimeArea, dt);
        res.peakTimeArea = pk[0]; res.peakTimeAreaTimeHours = pk[1]; res.volumeTimeAreaM3 = volume(qta, dt);
        pk = peak(res.flowClark, dt);
        res.peakClark = pk[0]; res.peakClarkTimeHours = pk[1]; res.volumeClarkM3 = volume(res.flowClark, dt);

        // ---------- hourly table
        res.hourlyRainMm = new double[hoursTotal];
        res.hourlyExcessMm = new double[hoursTotal];
        res.hourlyMeanFlowScs = new double[hoursTotal];
        res.hourlyMeanFlowClark = new double[hoursTotal];
        for (int h = 0; h < hoursTotal; h++) {
            res.hourlyRainMm[h] = h < rain.hours() ? rain.depth(h) : 0.0;
            int a = h * perHour, b = a + perHour;
            double e = 0;
            for (int s = a; s < b; s++) {
                e += excessStep[s];
            }
            res.hourlyExcessMm[h] = e;
            res.hourlyMeanFlowScs[h] = trapezoidMean(res.flowScs, a, b);
            res.hourlyMeanFlowClark[h] = trapezoidMean(res.flowClark, a, b);
        }

        // ---------- animation frames
        int frameStep = set.frameMinutes / set.stepMinutes;
        int frames = steps / frameStep + 1;
        res.frameTimeHours = new double[frames];
        res.frameClassCumulativeExcessMm = new double[frames * nClass];
        for (int f = 0; f < frames; f++) {
            res.frameTimeHours[f] = f * frameStep * dt;
            for (int c = 0; c < nClass; c++) {
                res.frameClassCumulativeExcessMm[f * nClass + c] = cum[c][f * frameStep];
            }
        }
        buildChannelFrames(res, set, cells, classOf, travel, retention, rain, dt, steps, frameStep, frames,
                maxTravel, toFlow);
        return res;
    }

    /** Stream-cell discharge by time-area translation, using travel-time bins one step wide. */
    private void buildChannelFrames(SimulationResult res, SimulationSettings set, int[] cells, int[] classOf,
                                    double[] travel, double[] retention, HourlyRainfall rain, double dt,
                                    int steps, int frameStep, int frames, double maxTravel, double toFlow) {
        Watershed ws = watershed;
        int n = grid.size();
        int threshold = (int) Math.ceil(set.channelThresholdKm2 * 1.0e6 / grid.cellArea());
        int[] channelIndex = new int[n];
        java.util.Arrays.fill(channelIndex, -1);
        int nCh = 0;
        for (int k : cells) {
            if (network.accumulation(k) >= Math.max(1, threshold)) {
                channelIndex[k] = nCh++;
            }
        }
        int[] channels = new int[nCh];
        for (int k : cells) {
            if (channelIndex[k] >= 0) {
                channels[channelIndex[k]] = k;
            }
        }
        res.channelCells = channels;
        int nClass = retention.length;
        int nBins = (int) Math.ceil(maxTravel / dt) + 2;
        double[][] hist = new double[nCh][];
        for (int k : cells) {
            int j = k;
            while (true) {
                int ch = channelIndex[j];
                if (ch >= 0) {
                    if (hist[ch] == null) {
                        hist[ch] = new double[nClass * nBins];
                    }
                    int b = (int) Math.round((travel[k] - travel[j]) / dt);
                    hist[ch][classOf[k] * nBins + Math.max(0, Math.min(nBins - 1, b))] += 1.0;
                }
                if (j == ws.outlet()) {
                    break;
                }
                j = network.receiver(j);
            }
        }
        // increments of cumulative excess over [s*dt - dt/2, s*dt + dt/2], for s = -nBins .. steps
        double[][] inc = new double[nClass][steps + nBins + 1];
        for (int c = 0; c < nClass; c++) {
            double prev = excess(rain.cumulative(-nBins * dt - dt / 2.0), retention[c]);
            for (int s = -nBins; s <= steps; s++) {
                double next = excess(rain.cumulative(s * dt + dt / 2.0), retention[c]);
                inc[c][s + nBins] = next - prev;
                prev = next;
            }
        }
        double[] flow = new double[frames * nCh];
        for (int ch = 0; ch < nCh; ch++) {
            double[] h = hist[ch];
            for (int c = 0; c < nClass; c++) {
                for (int b = 0; b < nBins; b++) {
                    double count = h[c * nBins + b];
                    if (count == 0) {
                        continue;
                    }
                    double w = count * toFlow;
                    double[] ic = inc[c];
                    for (int f = 0; f < frames; f++) {
                        int s = f * frameStep - b + nBins;
                        if (s >= 0) {
                            flow[f * nCh + ch] += w * ic[s];
                        }
                    }
                }
            }
        }
        res.frameChannelFlow = flow;
    }

    private static double excess(double p, double retention) {
        double ia = ScsCurveNumber.LAMBDA * retention;
        if (p <= ia) {
            return 0.0;
        }
        double pe = p - ia;
        return pe * pe / (pe + retention);
    }

    private static double[] peak(double[] q, double dt) {
        int best = 0;
        for (int i = 1; i < q.length; i++) {
            if (q[i] > q[best]) {
                best = i;
            }
        }
        return new double[] {q[best], best * dt};
    }

    private static double volume(double[] q, double dt) {
        double v = 0;
        for (double x : q) {
            v += x;
        }
        return v * dt * 3600.0;
    }

    private static double trapezoidMean(double[] q, int a, int b) {
        double s = 0.5 * (q[a] + q[b]);
        for (int i = a + 1; i < b; i++) {
            s += q[i];
        }
        return s / (b - a);
    }
}
