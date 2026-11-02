package civiltech.terrain;

/**
 * Removes depressions (pits) from a DEM so that water can flow from every cell to the grid edge.
 *
 * <p>Algorithm: Priority-Flood + epsilon (Barnes, Lehman and Mulla, 2014, Computers and
 * Geosciences 62, 117-127, Algorithm 3). The edge cells are placed in a priority queue keyed by
 * elevation. The lowest cell is repeatedly removed and its unvisited neighbours are visited; a
 * neighbour that is not higher than the current cell is raised to the next representable
 * {@code double} above it ({@link Math#nextUp(double)}). Every interior cell therefore ends up
 * strictly higher than the cell that flooded it, which guarantees a strictly descending D8 path to
 * the edge (flat areas receive tiny, consistent gradients). Run time O(N log N).
 */
public final class PriorityFlood {

    private PriorityFlood() { }

    /** Returns the depression-filled elevations; the input grid is not changed. */
    public static double[] fill(TerrainGrid grid) {
        int n = grid.size();
        double[] filled = grid.elevations();
        boolean[] closed = new boolean[n];
        MinHeap heap = new MinHeap(n);

        for (int i = 0; i < n; i++) {
            if (grid.onEdge(i)) {
                closed[i] = true;
                heap.push(filled[i], i);
            }
        }
        while (!heap.isEmpty()) {
            int c = heap.pop();
            for (int k = 0; k < 8; k++) {
                int nb = grid.neighbour(c, k);
                if (nb < 0 || closed[nb]) {
                    continue;
                }
                closed[nb] = true;
                if (filled[nb] <= filled[c]) {
                    filled[nb] = Math.nextUp(filled[c]);
                }
                heap.push(filled[nb], nb);
            }
        }
        return filled;
    }

    /**
     * Binary min-heap of (key, cell index) pairs stored in two primitive arrays.
     * Ties are broken by insertion order, which makes results reproducible on every platform.
     */
    static final class MinHeap {
        private final double[] key;
        private final int[] value;
        private final long[] order;
        private int size;
        private long counter;

        MinHeap(int capacity) {
            key = new double[capacity];
            value = new int[capacity];
            order = new long[capacity];
        }

        boolean isEmpty() { return size == 0; }

        void push(double k, int v) {
            int i = size++;
            key[i] = k;
            value[i] = v;
            order[i] = counter++;
            while (i > 0) {
                int p = (i - 1) >>> 1;
                if (!less(i, p)) {
                    break;
                }
                swap(i, p);
                i = p;
            }
        }

        int pop() {
            int top = value[0];
            size--;
            if (size > 0) {
                key[0] = key[size];
                value[0] = value[size];
                order[0] = order[size];
                int i = 0;
                while (true) {
                    int l = 2 * i + 1, r = l + 1, m = i;
                    if (l < size && less(l, m)) m = l;
                    if (r < size && less(r, m)) m = r;
                    if (m == i) break;
                    swap(i, m);
                    i = m;
                }
            }
            return top;
        }

        private boolean less(int a, int b) {
            return key[a] < key[b] || (key[a] == key[b] && order[a] < order[b]);
        }

        private void swap(int a, int b) {
            double tk = key[a]; key[a] = key[b]; key[b] = tk;
            int tv = value[a]; value[a] = value[b]; value[b] = tv;
            long to = order[a]; order[a] = order[b]; order[b] = to;
        }
    }
}
