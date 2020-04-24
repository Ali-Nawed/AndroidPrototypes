package com.ali.mobileclassification;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.SurfaceHolder;
import android.widget.TextView;

import java.util.concurrent.SynchronousQueue;

public class SurfaceViewTask extends AsyncTask implements SurfaceHolder.Callback {

    private Activity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private SynchronousQueue<PredictionTuple> mPredictionQueue;
    private TextView mTextView;
    private static final int BAR_WIDTH = 40;
    private Paint paint = new Paint();
    private boolean surfaceExists;
    private static final int PROB_BAR_MAX_LENGTH = 600;

    SurfaceViewTask(Activity activity, SurfaceHolder surfaceHolder, SynchronousQueue<PredictionTuple> predictionTupleSynchronousQueue, TextView textView) {
        super();
        mActivity = activity;
        mSurfaceHolder = surfaceHolder;
        mPredictionQueue = predictionTupleSynchronousQueue;
        mTextView = textView;
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


            String label = "";
            float prob = 0;
            try {
                PredictionTuple prediction = mPredictionQueue.take();
                label = prediction.getLabel();
                prob = prediction.getProbability();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final String fLabel = label;
            int probBar1Length = getPobBarLength(prob);
            Canvas canvas = mSurfaceHolder.lockCanvas();
            canvas.drawColor(Color.BLACK);
            Rect rect1 = new Rect(400+probBar1Length,160+BAR_WIDTH,400,160);

            canvas.drawRect(rect1, paint);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(fLabel);
                }
            });

        }
        return null;
    }

    private int getPobBarLength(float probability) {
        float length = PROB_BAR_MAX_LENGTH*probability/100;
        int pixelLength = Math.round(length);
        return pixelLength;
    }
}
