package com.example.cameraex2;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int MAX_LUM = 170;
    private int luma;
    private int currRed = 0;
    private boolean surfaceExists;
    private SynchronousQueue<Integer> synchronousQueue = new SynchronousQueue();
    private String[] REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA,
                                                          Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                            Manifest.permission.READ_EXTERNAL_STORAGE};
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    PreviewView previewView;
    TextView textView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor executor = Executors.newSingleThreadExecutor();

    public MainActivity() {
        luma = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

    }


    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[byteBuffer.remaining()];
                byteBuffer.get(data);
                byteBuffer.clear();
                int luma = 0;

                for (int i=0; i<data.length; i++) {
                    luma += ((int) data[i]) & 0xFF;
                }
                luma = luma/data.length;

                try {
                    synchronousQueue.put((Integer) luma);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                textView = findViewById(R.id.textView);
                textView.setText(String.format("%s", luma));
                image.close();
            }
        });
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Camera  camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));


    }
    private int interpolate(int lum) {
        //return red component in RGB based on luminosity value
        int red;
        float prop;
        if (lum >= MAX_LUM) {
            return 255;
        }
        prop = ((float)lum)/MAX_LUM;
        red = (int) Math.round(255*prop);
        return red;
    }

    class CustomSurfaceCallback extends AsyncTask implements SurfaceHolder.Callback{

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceExists = true;
            CustomSurfaceCallback thread = this;
            thread.execute();
        }

        private void tryDrawing(SurfaceHolder holder) {
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) {
                Log.e("SurfaceHolderCallback", "Canvas is null");
            } else {
                draw(canvas, luma);
                holder.unlockCanvasAndPost(canvas);
            }
        }

        private void draw(Canvas canvas, int luma) {
            int red = interpolate(luma);
            canvas.drawRGB(red, 0, 255-red);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            //tryDrawing(holder);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceExists = false;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            while (surfaceExists) {

                Canvas canvas = surfaceHolder.lockCanvas();
                int newLuma = 255;
                try {
                    newLuma = (int) synchronousQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int red = interpolate(newLuma);
                Log.i("AlphaCheck", String.format("%s", red));

                canvas.drawRGB(red, 0, 255-red);
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
            return null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int grantResults[]) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permission not Granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera(){

        previewView = findViewById(R.id.preview_view);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.setZOrderMediaOverlay(true);
        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.addCallback(new CustomSurfaceCallback());
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(
                () -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        bindPreview(cameraProvider);
                    } catch (ExecutionException e) {

                    } catch (InterruptedException e) {

                    }

                }, ContextCompat.getMainExecutor(this));
    };

    public class MyCameraXApplication extends Application implements CameraXConfig.Provider {
        @NonNull
        @Override
        public CameraXConfig getCameraXConfig() {
            return Camera2Config.defaultConfig();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission: REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }
}