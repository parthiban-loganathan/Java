package lab.civiltech.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.Locale;

/** Rainfall bars (from the top, reversed) and discharge lines, drawn with Canvas. */
public class HydrographView extends View {

    private double[] time, hourlyRain, qScs, qTimeArea;
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rainPaint = new Paint();
    private final Paint scsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint taPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HydrographView(Context context) {
        super(context);
        float d = getResources().getDisplayMetrics().density;
        axis.setColor(Color.GRAY);
        axis.setStrokeWidth(d);
        label.setColor(Color.DKGRAY);
        label.setTextSize(11 * d);
        rainPaint.setColor(Color.argb(120, 76, 140, 220));
        scsPaint.setColor(Color.rgb(217, 72, 15));
        scsPaint.setStyle(Paint.Style.STROKE);
        scsPaint.setStrokeWidth(2 * d);
        taPaint.setColor(Color.rgb(11, 110, 138));
        taPaint.setStyle(Paint.Style.STROKE);
        taPaint.setStrokeWidth(1.5f * d);
    }

    public void setData(double[] timeHours, double[] hourlyRainMm, double[] flowScs, double[] flowTimeArea) {
        time = timeHours;
        hourlyRain = hourlyRainMm;
        qScs = flowScs;
        qTimeArea = flowTimeArea;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (time == null) return;
        float d = getResources().getDisplayMetrics().density;
        float left = 44 * d, right = getWidth() - 10 * d, top = 18 * d, bottom = getHeight() - 22 * d;
        double tMax = time[time.length - 1];
        double qMax = 1e-9, pMax = 1e-9;
        for (int i = 0; i < qScs.length; i++) qMax = Math.max(qMax, Math.max(qScs[i], qTimeArea[i]));
        for (double p : hourlyRain) pMax = Math.max(pMax, p);
        qMax *= 1.1;
        double pScale = (bottom - top) * 0.35 / pMax;

        c.drawLine(left, bottom, right, bottom, axis);
        c.drawLine(left, top, left, bottom, axis);
        for (int h = 0; h < hourlyRain.length; h++) {
            float x0 = (float) (left + h / tMax * (right - left));
            float x1 = (float) (left + (h + 1) / tMax * (right - left));
            c.drawRect(x0, top, x1, (float) (top + hourlyRain[h] * pScale), rainPaint);
        }
        c.drawPath(line(qScs, tMax, qMax, left, right, top, bottom), scsPaint);
        c.drawPath(line(qTimeArea, tMax, qMax, left, right, top, bottom), taPaint);

        c.drawText(String.format(Locale.ROOT, "%.0f m3/s", qMax), 2 * d, top + 10 * d, label);
        c.drawText("0", left - 12 * d, bottom, label);
        c.drawText(String.format(Locale.ROOT, "%.0f h", tMax), right - 30 * d, bottom + 16 * d, label);
        c.drawText("rain (max " + String.format(Locale.ROOT, "%.1f", pMax) + " mm/h)   SCS UH (orange)   time-area (blue)",
                left, top - 6 * d, label);
    }

    private Path line(double[] q, double tMax, double qMax, float left, float right, float top, float bottom) {
        Path p = new Path();
        for (int i = 0; i < q.length; i++) {
            float x = (float) (left + time[i] / tMax * (right - left));
            float y = (float) (bottom - q[i] / qMax * (bottom - top));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        return p;
    }
}
