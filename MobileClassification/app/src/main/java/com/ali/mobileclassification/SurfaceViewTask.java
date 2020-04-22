package com.ali.mobileclassification;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceViewTask extends AsyncTask implements SurfaceHolder.Callback {
    private SurfaceHolder mSurfaceHolder;
    private boolean surfaceExists;


    public SurfaceViewTask(SurfaceHolder surfaceHolder) {
        super();
        mSurfaceHolder = surfaceHolder;

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
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }

        return null;
    }
}
