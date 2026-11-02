package civiltech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import civiltech.hydrology.CurveNumberTable;
import civiltech.hydrology.HourlyRainfall;
import civiltech.lab.CatchmentModel;
import civiltech.lab.GridData;
import civiltech.lab.SimulationResult;
import civiltech.lab.SimulationSettings;
import civiltech.terrain.FlowNetwork;
import civiltech.terrain.PriorityFlood;
import civiltech.terrain.TerrainGrid;
import civiltech.terrain.Watershed;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class TerrainAndModelTest {

    /** V-shaped valley draining south: z = 10*|col - 5| + 2*(rows - row). */
    static TerrainGrid valley(int rows, int cols) {
        double[] z = new double[rows * cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                z[r * cols + c] = 10.0 * Math.abs(c - cols / 2) + 2.0 * (rows - r);
        return new TerrainGrid(rows, cols, 30, 30, z);
    }

    @Test
    void pitIsFilledAndDrains() {
        double[] z = new double[25];
        java.util.Arrays.fill(z, 10);
        z[12] = 1;   // centre pit
        z[2] = 5;    // spill point on the north edge
        TerrainGrid g = new TerrainGrid(5, 5, 1, 1, z);
        double[] f = PriorityFlood.fill(g);
        assertTrue(f[12] > 10 && f[12] < 10.000001, "pit raised to just above the surrounding 10 m rim");
        FlowNetwork net = FlowNetwork.build(g);
        int c = 12, steps = 0;
        while (net.receiver(c) != FlowNetwork.OUTSIDE && steps < 25) { c = net.receiver(c); steps++; }
        assertTrue(g.onEdge(c), "every cell reaches the edge");
    }

    @Test
    void valleyAccumulatesAlongAxis() {
        int rows = 20, cols = 11;
        TerrainGrid g = valley(rows, cols);
        FlowNetwork net = FlowNetwork.build(g);
        int outlet = g.index(rows - 1, cols / 2);
        assertEquals(rows * cols, net.accumulation(outlet), "whole grid drains to the valley outlet");
        Watershed ws = new Watershed(net, outlet);
        assertEquals(rows * cols, ws.cellCount());
        int snapped = net.snapToStream(rows - 6, 3, 2);   // window rows 12..16, cols 1..5
        assertEquals(g.index(rows - 4, cols / 2), snapped);
    }

    @Test
    void modelConservesMassOnSyntheticValley() {
        TerrainGrid g = valley(40, 21);
        int[] lc = new int[g.size()];
        for (int i = 0; i < lc.length; i++) lc[i] = (i % 3 == 0) ? 50 : 10;
        CatchmentModel model = new CatchmentModel(g, lc);
        model.delineate(39, 10, 1);
        double[] rain = new double[12];
        rain[2] = 20; rain[3] = 45; rain[4] = 30; rain[5] = 10;
        for (boolean slope : new boolean[] {false, true}) {
            SimulationSettings s = new SimulationSettings();
            s.slopeWeightedVelocity = slope;
            s.clarkStorageHours = 0.5;
            s.channelThresholdKm2 = 0.01;
            SimulationResult r = model.simulate(new HourlyRainfall(rain), s);
            assertEquals(r.excessVolumeM3, r.volumeScsM3, 1e-6 * r.excessVolumeM3);
            assertEquals(r.excessVolumeM3, r.volumeTimeAreaM3, 1e-6 * r.excessVolumeM3);
            assertEquals(r.excessVolumeM3, r.volumeClarkM3, 1e-3 * r.excessVolumeM3);
            assertTrue(r.peakClark <= r.peakTimeArea + 1e-9, "reservoir attenuates the peak");
            assertTrue(r.runoffCoefficient > 0 && r.runoffCoefficient < 1);
            // stream frames at the outlet reproduce the time-area hydrograph to within binning error
            int out = -1;
            for (int i = 0; i < r.channelCells.length; i++) if (r.channelCells[i] == model.watershed().outlet()) out = i;
            assertTrue(out >= 0);
            double peakFrames = 0;
            for (int f = 0; f < r.frameTimeHours.length; f++)
                peakFrames = Math.max(peakFrames, r.frameChannelFlow[f * r.channelCells.length + out]);
            assertTrue(Math.abs(peakFrames - r.peakTimeArea) / r.peakTimeArea < 0.15,
                    "frame peak " + peakFrames + " vs time-area peak " + r.peakTimeArea);
        }
    }

    @Test
    void velloreCatchmentFromOpenData() throws Exception {
        Path p = Paths.get("../../site/data/catchment/vellore_hills_grid.json");
        Assumptions.assumeTrue(Files.exists(p), "dataset not present");
        GridData d = GridData.parse(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        CatchmentModel model = new CatchmentModel(d.terrain(), d.landCover);
        Watershed ws = model.delineate(d.rowOfLat(d.outletLat), d.colOfLon(d.outletLon), 0);
        double[] cen = ws.centroidRowCol();
        System.out.printf("Vellore outlet r%d c%d (%.5f N %.5f E): area %.3f km2, cells %d, L %.0f m, drop %.1f m, S %.4f, "
                        + "centroid %.4f N %.4f E, max elev %.1f%n",
                d.rows > 0 ? model.grid().rowOf(ws.outlet()) : 0, model.grid().colOf(ws.outlet()),
                d.latOfRow(model.grid().rowOf(ws.outlet())), d.lonOfCol(model.grid().colOf(ws.outlet())),
                ws.areaSquareKilometres(), ws.cellCount(), ws.longestFlowPathMetres(), ws.longestPathDropMetres(),
                ws.longestPathSlope(), d.latOfRow(cen[0]), d.lonOfCol(cen[1]), ws.maxElevation());
        // values reproduced independently by tools/verification/crosscheck_terrain.py
        assertEquals(77, model.grid().rowOf(ws.outlet()));
        assertEquals(234, model.grid().colOf(ws.outlet()));
        assertEquals(38.650, ws.areaSquareKilometres(), 0.001);
        assertEquals(15414.4, ws.longestFlowPathMetres(), 0.1);
        assertEquals(205.9, ws.longestPathDropMetres(), 0.05);
        double[] rain = new double[48];
        for (int i = 10; i < 30; i++) rain[i] = 6;
        SimulationSettings s = new SimulationSettings();
        s.curveNumberByClass = CurveNumberTable.lookup(2);
        SimulationResult r = model.simulate(new HourlyRainfall(rain), s);
        System.out.println(r.summaryJson());
        assertEquals(r.excessVolumeM3, r.volumeTimeAreaM3, 1e-6 * r.excessVolumeM3);
    }
}
