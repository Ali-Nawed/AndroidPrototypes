package com.ali.mobileclassification;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.constraintlayout.solver.widgets.Rectangle;

public class SurfaceViewTask extends AsyncTask implements SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
   /* private TextView mtextView1;
    private TextView mtextView2;
    private TextView mtextView3;

    */
    private static final int BAR_WIDTH = 10;
    private Paint paint = new Paint();

    private boolean surfaceExists;
    private float label1Prob = 1;
    private float label2Prob = 1;
    private float label3Prob = 1;

    private float label1BarX;
    private float label1BarY;
    private int label2BarX;
    private int label2BarY;
    private int label3BarX;
    private int label3BarY;


    SurfaceViewTask(SurfaceHolder surfaceHolder) {
        super();
        mSurfaceHolder = surfaceHolder;


        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceExists = true;
        SurfaceViewTask surfaceThread = this;
        surfaceThread.execute();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceExists = false;
    }

    @Override
    protected Object doInBackground(Object... objects) {

        while (surfaceExists) {

            Canvas canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(Color.BLACK);
            Rect rect1 = new Rect(1000,200,400,160);
            canvas.drawRect(rect1, paint);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }

        return null;
    }
}
