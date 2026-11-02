package civiltech.lab;

import civiltech.io.MiniJson;
import civiltech.terrain.TerrainGrid;
import java.util.Map;

/**
 * The terrain + land-cover grid file produced by tools/data-prep (vellore_hills_grid.json),
 * with conversions between grid cells and geographic coordinates (cell centres on a regular
 * latitude/longitude lattice, north row first).
 */
public final class GridData {

    public final String name;
    public final int rows;
    public final int cols;
    public final double west;
    public final double north;
    public final double cellDeg;
    public final double cellSizeX;
    public final double cellSizeY;
    public final double[] elevation;
    public final int[] landCover;
    public final double outletLat;
    public final double outletLon;

    private GridData(Map<String, Object> m) {
        name = (String) m.get("name");
        rows = ((Number) m.get("rows")).intValue();
        cols = ((Number) m.get("cols")).intValue();
        west = ((Number) m.get("west")).doubleValue();
        north = ((Number) m.get("north")).doubleValue();
        cellDeg = ((Number) m.get("cellDeg")).doubleValue();
        cellSizeX = ((Number) m.get("cellSizeX")).doubleValue();
        cellSizeY = ((Number) m.get("cellSizeY")).doubleValue();
        elevation = MiniJson.doubles(m.get("elevation"));
        landCover = MiniJson.ints(m.get("landCover"));
        Map<String, Object> o = MiniJson.object(m.get("defaultOutlet"));
        outletLat = ((Number) o.get("lat")).doubleValue();
        outletLon = ((Number) o.get("lon")).doubleValue();
    }

    public static GridData parse(String json) {
        return new GridData(MiniJson.object(MiniJson.parse(json)));
    }

    public TerrainGrid terrain() {
        return new TerrainGrid(rows, cols, cellSizeX, cellSizeY, elevation);
    }

    public int rowOfLat(double lat) { return (int) Math.floor((north - lat) / cellDeg); }
    public int colOfLon(double lon) { return (int) Math.floor((lon - west) / cellDeg); }
    public double latOfRow(double row) { return north - (row + 0.5) * cellDeg; }
    public double lonOfCol(double col) { return west + (col + 0.5) * cellDeg; }
}
