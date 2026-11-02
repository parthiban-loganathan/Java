package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests check the Java against hand calculations. Run them with: mvn test */
class RunoffCalculatorTest {

    @Test
    void runoffMatchesHandCalculation() {
        // CN 80: S = 25400/80 - 254 = 63.5 mm, Ia = 12.7 mm.
        // P = 100 mm: Q = (100 - 12.7)^2 / (100 - 12.7 + 63.5) = 7621.29 / 150.8 = 50.539 mm
        SubCatchment s = new SubCatchment("test", 12.9, 79.2, 2.0, 80);
        assertEquals(63.5, s.retentionMm(), 1e-9);
        assertEquals(7621.29 / 150.8, s.cumulativeRunoffMm(100), 1e-9);
        assertEquals(0.0, s.cumulativeRunoffMm(12.7), 0.0);
        // 1 mm over 2 km^2 = 2000 m^3
        assertEquals(2000.0, s.volumeM3(1.0), 1e-9);
    }

    @Test
    void hourlyRunoffAddsUpToEventRunoff() {
        SubCatchment s = new SubCatchment("test", 12.9, 79.2, 1.0, 75);
        double[] rain = {5, 20, 35, 10, 0, 2};
        double sum = 0;
        for (double q : s.hourlyRunoffMm(rain)) sum += q;
        assertEquals(s.cumulativeRunoffMm(72), sum, 1e-9);
    }

    @Test
    void csvReadingAndJson() {
        String subs = "name,centroid_lat,centroid_lon,area_km2,curve_number\nA,12.9,79.2,1.5,80\nB,12.8,79.1,2.5,70\n";
        String rain = "# comment\ntime,precip_mm\n1,10\n2,40\n3,50\n";
        String json = RunoffCalculator.calculateJson(subs, rain);
        assertTrue(json.contains("\"totalRainMm\":100.0"));
        assertTrue(json.contains("\"totalAreaKm2\":4.0"));
        assertThrows(IllegalArgumentException.class, () -> RunoffCalculator.readRainfall("x\n1\n-2\n"));
        assertTrue(WebApi.calculate("bad", rain).startsWith("{\"error\""));
    }
}
