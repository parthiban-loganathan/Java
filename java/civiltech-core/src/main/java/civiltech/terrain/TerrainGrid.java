package civiltech.terrain;

/**
 * A rectangular grid of ground elevations (a Digital Elevation Model, DEM).
 *
 * <p>Cells are stored in a one-dimensional array in row-major order: the first row is the
 * northern-most row, and inside a row the cells run from west to east. Cell (row, col) is
 * stored at index {@code row * cols + col}. One array is faster and simpler than a
 * {@code double[][]}, and every algorithm in this package works with these indices.
 *
 * <p>The eight neighbours of a cell are numbered clockwise starting from north:
 * <pre>
 *   7 0 1        NW  N  NE
 *   6 . 2   =    W   .  E
 *   5 4 3        SW  S  SE
 * </pre>
 */
public final class TerrainGrid {

    /** Row offset of neighbour k (k = 0..7). */
    public static final int[] DR = {-1, -1, 0, 1, 1, 1, 0, -1};
    /** Column offset of neighbour k (k = 0..7). */
    public static final int[] DC = {0, 1, 1, 1, 0, -1, -1, -1};

    private final int rows;
    private final int cols;
    private final double cellSizeX;
    private final double cellSizeY;
    private final double[] elevation;
    private final double[] neighbourDistance = new double[8];

    /**
     * @param rows      number of rows (north to south)
     * @param cols      number of columns (west to east)
     * @param cellSizeX cell width in metres (east-west)
     * @param cellSizeY cell height in metres (north-south)
     * @param elevation rows*cols elevations in metres, row-major, no missing values
     */
    public TerrainGrid(int rows, int cols, double cellSizeX, double cellSizeY, double[] elevation) {
        if (rows < 3 || cols < 3) {
            throw new IllegalArgumentException("grid must be at least 3 x 3");
        }
        if (elevation.length != rows * cols) {
            throw new IllegalArgumentException("expected " + rows * cols + " elevations, got " + elevation.length);
        }
        if (!(cellSizeX > 0) || !(cellSizeY > 0)) {
            throw new IllegalArgumentException("cell sizes must be positive");
        }
        for (int i = 0; i < elevation.length; i++) {
            if (Double.isNaN(elevation[i]) || Double.isInfinite(elevation[i])) {
                throw new IllegalArgumentException("elevation at index " + i + " is not a finite number");
            }
        }
        this.rows = rows;
        this.cols = cols;
        this.cellSizeX = cellSizeX;
        this.cellSizeY = cellSizeY;
        this.elevation = elevation.clone();
        double diagonal = Math.sqrt(cellSizeX * cellSizeX + cellSizeY * cellSizeY);
        for (int k = 0; k < 8; k++) {
            if (DR[k] != 0 && DC[k] != 0) {
                neighbourDistance[k] = diagonal;
            } else if (DR[k] != 0) {
                neighbourDistance[k] = cellSizeY;
            } else {
                neighbourDistance[k] = cellSizeX;
            }
        }
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public int size() { return rows * cols; }
    public double cellSizeX() { return cellSizeX; }
    public double cellSizeY() { return cellSizeY; }
    /** Plan area of one cell in square metres. */
    public double cellArea() { return cellSizeX * cellSizeY; }
    public double elevation(int index) { return elevation[index]; }
    /** A copy of all elevations. */
    public double[] elevations() { return elevation.clone(); }

    public int index(int row, int col) { return row * cols + col; }
    public int rowOf(int index) { return index / cols; }
    public int colOf(int index) { return index % cols; }

    public boolean inside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean onEdge(int index) {
        int r = rowOf(index), c = colOf(index);
        return r == 0 || c == 0 || r == rows - 1 || c == cols - 1;
    }

    /** Index of neighbour k of a cell, or -1 when that neighbour is outside the grid. */
    public int neighbour(int index, int k) {
        int r = rowOf(index) + DR[k], c = colOf(index) + DC[k];
        return inside(r, c) ? r * cols + c : -1;
    }

    /** Horizontal distance in metres from a cell centre to the centre of neighbour k. */
    public double distance(int k) { return neighbourDistance[k]; }
}
