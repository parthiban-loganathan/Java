package civiltech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import civiltech.hydrology.CurveNumberTable;
import civiltech.hydrology.HourlyRainfall;
import civiltech.hydrology.Kirpich;
import civiltech.hydrology.LinearReservoir;
import civiltech.hydrology.ScsCurveNumber;
import civiltech.hydrology.ScsUnitHydrograph;
import org.junit.jupiter.api.Test;

class HydrologyTest {

    @Test
    void curveNumberRunoffMatchesHandCalculation() {
        // CN 80: S = 25400/80 - 254 = 63.5 mm, Ia = 12.7 mm; P = 100 mm -> Q = 87.3^2 / 150.8
        assertEquals(63.5, ScsCurveNumber.retentionMm(80), 1e-12);
        assertEquals(87.3 * 87.3 / 150.8, ScsCurveNumber.cumulativeRunoffMm(100, 80), 1e-12);
        assertEquals(0.0, ScsCurveNumber.cumulativeRunoffMm(12.7, 80), 0.0);
        assertEquals(42.0, ScsCurveNumber.cumulativeRunoffMm(42.0, 100), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> ScsCurveNumber.retentionMm(0));
    }

    @Test
    void runoffFractionIsDerivativeOfRunoff() {
        double h = 1e-6;
        for (double p : new double[] {20, 50, 120, 300}) {
            double numeric = (ScsCurveNumber.cumulativeRunoffMm(p + h, 75) - ScsCurveNumber.cumulativeRunoffMm(p - h, 75)) / (2 * h);
            assertEquals(numeric, ScsCurveNumber.runoffFraction(p, 75), 1e-6);
        }
    }

    @Test
    void kirpichMatchesFormula() {
        // 0.0195 * 1000^0.77 * 0.01^-0.385 = 23.44 min
        assertEquals(23.44, Kirpich.minutes(1000, 0.01), 0.01);
    }

    @Test
    void tr55TableValues() {
        int c = CurveNumberTable.soilGroupIndex("C");
        assertEquals(73, CurveNumberTable.curveNumber(10, c));   // woods, fair, C
        assertEquals(90, CurveNumberTable.curveNumber(50, c));   // residential 1/8 acre, C
        assertEquals(85, CurveNumberTable.curveNumber(40, c));   // row crops SR good, C
        assertEquals(94, CurveNumberTable.curveNumber(60, 3));   // fallow bare soil, D
    }

    @Test
    void unitHydrographHoldsOneMillimetre() {
        double area = 38.0, dt = 10.0 / 60.0;
        ScsUnitHydrograph uh = new ScsUnitHydrograph(area, 2.0, dt);
        double vol = 0;
        for (double q : uh.ordinates()) vol += q * dt * 3600;
        assertEquals(area * 1000.0, vol, 1e-6);
        assertEquals(dt / 2 + 1.2, uh.peakTimeHours(), 1e-12);
        assertTrue(Math.abs(uh.volumeCorrection() - 1.0) < 0.03, "table integrates to ~1 unit");
        double[] q = uh.convolve(new double[] {2.0, 0, 0}, 60);
        double v2 = 0;
        for (double x : q) v2 += x * dt * 3600;
        assertEquals(2 * area * 1000.0, v2, 1e-6);
    }

    @Test
    void dimensionlessTableEndpoints() {
        assertEquals(1.0, ScsUnitHydrograph.dimensionless(1.0), 1e-12);
        assertEquals(0.0, ScsUnitHydrograph.dimensionless(5.0), 1e-12);
        assertEquals((0.47 + 0.66) / 2, ScsUnitHydrograph.dimensionless(0.55), 1e-12);
    }

    @Test
    void linearReservoirConservesMassAndReachesSteadyState() {
        double dt = 0.1;
        double[] in = new double[2000];
        for (int i = 1; i < 300; i++) in[i] = 5.0;
        double[] out = LinearReservoir.route(in, 2.0, dt);
        double vi = 0, vo = 0;
        for (int i = 0; i < in.length; i++) { vi += in[i]; vo += out[i]; }
        assertEquals(vi, vo, 1e-6 * vi);
        assertEquals(5.0, out[299], 0.01);
    }

    @Test
    void rainfallCsvParsing() {
        HourlyRainfall r = HourlyRainfall.fromCsv("# comment\ntime,precip_mm\n2024-11-30 00:30,1.5\n2024-11-30 01:30,2.5\n");
        assertEquals(2, r.hours());
        assertEquals(4.0, r.total(), 1e-12);
        assertEquals(1.5 + 2.5 * 0.5, r.cumulative(1.5), 1e-12);
        assertEquals(2.5, r.intensity(1.2), 0.0);
    }
}
