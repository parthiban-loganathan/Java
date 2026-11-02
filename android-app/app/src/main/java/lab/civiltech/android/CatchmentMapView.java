package lab.civiltech.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import civiltech.lab.GridData;
import civiltech.terrain.FlowNetwork;

/** Plan view of the grid: land-cover colours with hillshade, streams, catchment and outlet. */
public class CatchmentMapView extends View {

    /** Called with the grid cell the user tapped. */
    public interface OnCellTapListener {
        void onCellTap(int row, int col);
    }

    private GridData grid;
    private FlowNetwork network;
    private Bitmap base;
    private Bitmap overlay;
    private int outletRow = -1, outletCol = -1;
    private OnCellTapListener listener;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect dst = new Rect();

    public CatchmentMapView(Context context) {
        super(context);
        marker.setColor(Color.RED);
        setBackgroundColor(Color.rgb(11, 23, 32));
    }

    public void setOnCellTapListener(OnCellTapListener l) { listener = l; }

    public void setGrid(GridData grid, FlowNetwork network) {
        this.grid = grid;
        this.network = network;
        int rows = grid.rows, cols = grid.cols;
        int[] px = new int[rows * cols];
        double maxAcc = 1;
        for (int i = 0; i < px.length; i++) maxAcc = Math.max(maxAcc, network.accumulation(i));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                // hillshade from the elevation gradient, light from the north-west
                double zw = grid.elevation[r * cols + Math.max(0, c - 1)];
                double ze = grid.elevation[r * cols + Math.min(cols - 1, c + 1)];
                double zn = grid.elevation[Math.max(0, r - 1) * cols + c];
                double zs = grid.elevation[Math.min(rows - 1, r + 1) * cols + c];
                double gx = (ze - zw) / (2 * grid.cellSizeX), gy = (zs - zn) / (2 * grid.cellSizeY);
                double shade = Math.max(0.35, Math.min(1.15, 0.85 - 2.0 * (gx * 0.7071 - gy * 0.7071)));
                int color = landCoverColor(grid.landCover[i]);
                if (Math.log(network.accumulation(i)) / Math.log(maxAcc) > 0.55) {
                    color = Color.rgb(40, 110, 230);           // stream lines
                    shade = 1.0;
                }
                px[i] = Color.rgb(clamp(Color.red(color) * shade), clamp(Color.green(color) * shade), clamp(Color.blue(color) * shade));
            }
        }
        base = Bitmap.createBitmap(px, cols, rows, Bitmap.Config.ARGB_8888);
        invalidate();
    }

    public void setCatchment(int[] mask, int outletRow, int outletCol) {
        int rows = grid.rows, cols = grid.cols;
        int[] px = new int[rows * cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                if (mask[i] == 0) {
                    px[i] = Color.argb(120, 0, 0, 0);           // dim outside the catchment
                } else {
                    boolean edge = r == 0 || c == 0 || r == rows - 1 || c == cols - 1
                            || mask[i - 1] == 0 || mask[i + 1] == 0 || mask[i - cols] == 0 || mask[i + cols] == 0;
                    px[i] = edge ? Color.rgb(255, 224, 102) : Color.TRANSPARENT;
                }
            }
        }
        overlay = Bitmap.createBitmap(px, cols, rows, Bitmap.Config.ARGB_8888);
        this.outletRow = outletRow;
        this.outletCol = outletCol;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (base == null) return;
        fitRect();
        canvas.drawBitmap(base, null, dst, paint);
        if (overlay != null) canvas.drawBitmap(overlay, null, dst, paint);
        if (outletRow >= 0) {
            float x = dst.left + (outletCol + 0.5f) * dst.width() / grid.cols;
            float y = dst.top + (outletRow + 0.5f) * dst.height() / grid.rows;
            canvas.drawCircle(x, y, 9f, marker);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && grid != null && listener != null) {
            fitRect();
            int col = (int) ((e.getX() - dst.left) / dst.width() * grid.cols);
            int row = (int) ((e.getY() - dst.top) / dst.height() * grid.rows);
            if (row >= 0 && row < grid.rows && col >= 0 && col < grid.cols) listener.onCellTap(row, col);
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Keeps the grid's aspect ratio (cell sizes in metres) inside the view. */
    private void fitRect() {
        double aspect = (grid.cols * grid.cellSizeX) / (grid.rows * grid.cellSizeY);
        int w = getWidth(), h = getHeight();
        if (w / (double) h > aspect) {
            int dw = (int) (h * aspect);
            dst.set((w - dw) / 2, 0, (w + dw) / 2, h);
        } else {
            int dh = (int) (w / aspect);
            dst.set(0, (h - dh) / 2, w, (h + dh) / 2);
        }
    }

    private static int clamp(double v) { return (int) Math.max(0, Math.min(255, v)); }

    /** ESA WorldCover legend colours. */
    static int landCoverColor(int code) {
        switch (code) {
            case 10: return Color.rgb(0, 100, 0);
            case 20: return Color.rgb(255, 187, 34);
            case 30: return Color.rgb(255, 255, 76);
            case 40: return Color.rgb(240, 150, 255);
            case 50: return Color.rgb(250, 0, 0);
            case 60: return Color.rgb(180, 180, 180);
            case 70: return Color.rgb(240, 240, 240);
            case 80: return Color.rgb(0, 100, 200);
            case 90: return Color.rgb(0, 150, 160);
            case 95: return Color.rgb(0, 207, 117);
            case 100: return Color.rgb(250, 230, 160);
            default: return Color.GRAY;
        }
    }
}
