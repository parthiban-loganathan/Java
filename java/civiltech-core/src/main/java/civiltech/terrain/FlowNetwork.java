package civiltech.terrain;

/**
 * D8 drainage network of a depression-filled DEM.
 *
 * <p>Flow direction: every cell drains to the one neighbour with the steepest downward slope
 * (drop divided by centre-to-centre distance), the D8 method of O'Callaghan and Mark (1984,
 * Computer Vision, Graphics and Image Processing 28, 323-344). A cell with no lower neighbour
 * (only possible on the grid edge after Priority-Flood) drains out of the grid.
 *
 * <p>Flow accumulation: the number of cells (including itself) that drain through each cell.
 * The cells are processed in topological order, upstream first (Kahn's algorithm), which is
 * O(N) and also provides the ordering used by {@link Watershed}.
 */
public final class FlowNetwork {

    /** Receiver value of a cell that drains out of the grid. */
    public static final int OUTSIDE = -1;

    private final TerrainGrid grid;
    private final double[] filled;
    private final int[] receiver;
    private final int[] direction;
    private final int[] accumulation;
    private final int[] upstreamFirst;

    private FlowNetwork(TerrainGrid grid, double[] filled) {
        this.grid = grid;
        this.filled = filled;
        int n = grid.size();
        receiver = new int[n];
        direction = new int[n];
        for (int i = 0; i < n; i++) {
            double best = 0.0;
            int bestK = -1;
            for (int k = 0; k < 8; k++) {
                int nb = grid.neighbour(i, k);
                if (nb < 0) {
                    continue;
                }
                double slope = (filled[i] - filled[nb]) / grid.distance(k);
                if (slope > best) {
                    best = slope;
                    bestK = k;
                }
            }
            direction[i] = bestK;
            receiver[i] = bestK < 0 ? OUTSIDE : grid.neighbour(i, bestK);
        }

        int[] donors = new int[n];
        for (int i = 0; i < n; i++) {
            if (receiver[i] != OUTSIDE) {
                donors[receiver[i]]++;
            }
        }
        int[] queue = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) {
            if (donors[i] == 0) {
                queue[tail++] = i;
            }
        }
        accumulation = new int[n];
        java.util.Arrays.fill(accumulation, 1);
        while (head < tail) {
            int c = queue[head++];
            int r = receiver[c];
            if (r != OUTSIDE) {
                accumulation[r] += accumulation[c];
                if (--donors[r] == 0) {
                    queue[tail++] = r;
                }
            }
        }
        if (tail != n) {
            throw new IllegalStateException("flow directions contain a cycle; was the DEM filled?");
        }
        upstreamFirst = queue;
    }

    /** Fills depressions and builds the D8 network in one call. */
    public static FlowNetwork build(TerrainGrid grid) {
        return new FlowNetwork(grid, PriorityFlood.fill(grid));
    }

    public TerrainGrid grid() { return grid; }
    /** Depression-filled elevations (copy). */
    public double[] filledElevations() { return filled.clone(); }
    public double filledElevation(int index) { return filled[index]; }
    /** Index of the cell that receives the flow of {@code index}, or {@link #OUTSIDE}. */
    public int receiver(int index) { return receiver[index]; }
    /** D8 direction code 0..7 (see {@link TerrainGrid}) or -1 when draining out of the grid. */
    public int direction(int index) { return direction[index]; }
    /** Number of cells draining through this cell, including itself. */
    public int accumulation(int index) { return accumulation[index]; }
    /** Copy of the accumulation array. */
    public int[] accumulations() { return accumulation.clone(); }
    /** Cell indices ordered so that every cell appears before its receiver. */
    public int[] upstreamFirstOrder() { return upstreamFirst.clone(); }
    int[] upstreamFirstOrderShared() { return upstreamFirst; }

    /** Horizontal length in metres of the D8 step from a cell to its receiver (0 if none). */
    public double stepLength(int index) {
        return direction[index] < 0 ? 0.0 : grid.distance(direction[index]);
    }

    /**
     * Moves a proposed outlet to the cell with the largest accumulation within a square window of
     * the given radius (in cells). Clicked points rarely fall exactly on the D8 stream line.
     */
    public int snapToStream(int row, int col, int radius) {
        if (!grid.inside(row, col)) {
            throw new IllegalArgumentException("outlet (" + row + ", " + col + ") is outside the grid");
        }
        int best = grid.index(row, col);
        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                if (grid.inside(r, c) && accumulation[grid.index(r, c)] > accumulation[best]) {
                    best = grid.index(r, c);
                }
            }
        }
        return best;
    }
}
