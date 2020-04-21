package com.example.surfaceex;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    boolean surfaceExists;
    SurfaceHolder surfaceHolder;
    private Random rand = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView;

        surfaceView = findViewById(R.id.surfaceView);
        CustomSurfaceCallback customSurfaceCallback = new CustomSurfaceCallback();
        surfaceHolder = surfaceView.getHolder();
        surfaceView.getHolder().addCallback(customSurfaceCallback);



    }

    public class CustomSurfaceCallback implements SurfaceHolder.Callback {
        private static final String TAG = "SurfaceCallback";


        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            //tryDrawing(holder);
            surfaceExists = true;
            MyThread thread = new MyThread();
            thread.execute();
        }

        private void tryDrawing(SurfaceHolder holder) {
            Log.i(TAG, "Trying to draw");
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) {
                Log.e(TAG, "Cannot Draw Canvas is null");
            } else {
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);
            }
        }

        private void draw(final Canvas canvas) {
            int randomR = rand.nextInt(255 + 1);
            int randomG = rand.nextInt(255 + 1);
            int randomB = rand.nextInt(255 + 1);

            canvas.drawRGB(randomR, randomG, randomB);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            //tryDrawing(holder);
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceExists = false;
        }
    }


        class MyThread extends AsyncTask {
            int deltaR = 5;
            int deltaG = 5;
            int deltaB = 5;
            int red = rand.nextInt(255 + 1);
            int green = rand.nextInt(255 + 1);
            int blue = rand.nextInt(255 + 1);

            @Override
            protected Object doInBackground(Object... params) {

                while (surfaceExists) {

                    Canvas canvas = surfaceHolder.lockCanvas();

                    if (red+deltaR > 255 || red+deltaR<0) {
                        deltaR *= -1;
                    } else {
                        red += deltaR;
                    }

                    if (blue+deltaB > 255 || blue+deltaB<0) {
                        deltaB *= -1;
                    } else {
                        blue += deltaB;
                    }

                    if (green+deltaG > 255 || green+deltaG<0) {
                        deltaG *= -1;
                    } else {
                        green += deltaG;
                    }


                    canvas.drawRGB(red, green, blue);

                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
                return null;
            }
        }
}
