package civiltech.terrain;

/**
 * The catchment (watershed) draining to one outlet cell, with the flow-path geometry needed by
 * rainfall-runoff methods.
 *
 * <p>Membership and flow length are computed in one downstream-first sweep: a cell belongs to the
 * catchment when its receiver does, and its flow length to the outlet equals the receiver's flow
 * length plus the D8 step between them. O(N).
 */
public final class Watershed {

    private final FlowNetwork network;
    private final int outlet;
    private final boolean[] member;
    private final double[] flowLength;
    private final int cellCount;
    private final int farthestCell;

    public Watershed(FlowNetwork network, int outlet) {
        TerrainGrid g = network.grid();
        if (outlet < 0 || outlet >= g.size()) {
            throw new IllegalArgumentException("outlet index out of range");
        }
        this.network = network;
        this.outlet = outlet;
        int n = g.size();
        member = new boolean[n];
        flowLength = new double[n];
        java.util.Arrays.fill(flowLength, Double.NaN);
        member[outlet] = true;
        flowLength[outlet] = 0.0;
        int[] order = network.upstreamFirstOrderShared();
        int count = 0;
        int far = outlet;
        for (int p = n - 1; p >= 0; p--) {          // downstream first
            int c = order[p];
            if (c != outlet) {
                int r = network.receiver(c);
                if (r == FlowNetwork.OUTSIDE || !member[r]) {
                    continue;
                }
                member[c] = true;
                flowLength[c] = flowLength[r] + network.stepLength(c);
            }
            count++;
            if (flowLength[c] > flowLength[far]) {
                far = c;
            }
        }
        cellCount = count;
        farthestCell = far;
    }

    public FlowNetwork network() { return network; }
    public int outlet() { return outlet; }
    public boolean contains(int index) { return member[index]; }
    public int cellCount() { return cellCount; }
    public double areaSquareMetres() { return cellCount * network.grid().cellArea(); }
    public double areaSquareKilometres() { return areaSquareMetres() / 1.0e6; }

    /** D8 flow length from a member cell to the outlet in metres (NaN for non-members). */
    public double flowLength(int index) { return flowLength[index]; }

    /** Cell with the longest flow path to the outlet (the hydraulically most remote point). */
    public int farthestCell() { return farthestCell; }
    public double longestFlowPathMetres() { return flowLength[farthestCell]; }

    /**
     * Elevation drop along the longest flow path in metres, measured on the original (unfilled) DEM
     * between the most remote cell and the outlet.
     */
    public double longestPathDropMetres() {
        TerrainGrid g = network.grid();
        return g.elevation(farthestCell) - g.elevation(outlet);
    }

    /** Average slope of the longest flow path (m/m), drop / length, never below 1e-4. */
    public double longestPathSlope() {
        double l = longestFlowPathMetres();
        if (l <= 0) {
            return 0.0;
        }
        return Math.max(1.0e-4, longestPathDropMetres() / l);
    }

    /** Mask as 0/1 integers, row-major (convenient for drawing). */
    public int[] mask() {
        int[] m = new int[member.length];
        for (int i = 0; i < m.length; i++) {
            m[i] = member[i] ? 1 : 0;
        }
        return m;
    }

    /** Flow lengths for all cells (NaN outside the catchment). */
    public double[] flowLengths() { return flowLength.clone(); }

    /** Centroid of the member cells as fractional (row, col). */
    public double[] centroidRowCol() {
        TerrainGrid g = network.grid();
        double sr = 0, sc = 0;
        for (int i = 0; i < member.length; i++) {
            if (member[i]) {
                sr += g.rowOf(i);
                sc += g.colOf(i);
            }
        }
        return new double[] {sr / cellCount, sc / cellCount};
    }

    /** Mean elevation of the catchment on the original DEM. */
    public double meanElevation() {
        TerrainGrid g = network.grid();
        double s = 0;
        for (int i = 0; i < member.length; i++) {
            if (member[i]) {
                s += g.elevation(i);
            }
        }
        return s / cellCount;
    }

    public double maxElevation() {
        TerrainGrid g = network.grid();
        double m = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < member.length; i++) {
            if (member[i] && g.elevation(i) > m) {
                m = g.elevation(i);
            }
        }
        return m;
    }
}
