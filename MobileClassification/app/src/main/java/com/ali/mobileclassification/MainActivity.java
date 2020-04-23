package com.ali.mobileclassification;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

public class MainActivity extends AppCompatActivity implements CameraXConfig.Provider {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA};
    private Activity activity = this;
    private Context context = this;
    private static final String modelName = "quantmobilenet_imagenet.pt";
    private static final int IMAGE_SIZE = 224;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private PreviewView previewView;
    private TextView textView1;
    private TextView textView2;
    private TextView textView3;

    private Tensor inputTensor;
    private Module module;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Executor executor = Executors.newSingleThreadExecutor();
    private SynchronousQueue predictionQueue;
    private PriorityQueue<PredictionTuple> predictionHolder;


    private String label1;
    private String label2;
    private String label3;
    private float label1Prob;
    private float label2Prob;
    private float label3Prob;
    private FloatBuffer mInputTensorBuffer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.setZOrderMediaOverlay(true);
        surfaceHolder = surfaceView.getHolder();

        textView1 = findViewById(R.id.textView);

        textView2 = findViewById(R.id.textView2);

        textView3 = findViewById(R.id.textView3);

        predictionQueue = new SynchronousQueue();

        try {
            module = Module.load(getModule(this, modelName));
        } catch (IOException e) {
            Log.e("MainActivity", "Error reading module", e);
            finish();
            e.printStackTrace();
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {



        surfaceHolder.addCallback(new SurfaceViewTask(activity, surfaceHolder, predictionQueue, textView1));
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
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                                          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                          .setTargetResolution(new Size(IMAGE_SIZE,IMAGE_SIZE))
                                          .build();




        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @WorkerThread
            @Override
            public void analyze(@NonNull ImageProxy image) {
                try {
                    if (module == null) {
                        module = Module.load(getModule(context, modelName));
                    }

                    FloatBuffer mInputTensorBuffer = Tensor.allocateFloatBuffer(3 * IMAGE_SIZE * IMAGE_SIZE);
                    Tensor inputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1L, 3L, IMAGE_SIZE, IMAGE_SIZE});



                    TensorImageUtils.imageYUV420CenterCropToFloatBuffer(image.getImage(), 0, IMAGE_SIZE, IMAGE_SIZE,
                                                                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                                                                        TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                                                                        mInputTensorBuffer,0);
                    Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                    final float[] scores = outputTensor.getDataAsFloatArray();

                    String topLabel = "";
                    float topScore = -Float.MAX_VALUE;
                    for (int i=0; i<scores.length; i++) {
                        if (scores[i] > topScore) {

                            topScore =scores[i];
                            topLabel = ImageNetLabels.labels[i];
                        }
                    }
                    PredictionTuple predictionTuple = new PredictionTuple(topScore, topLabel);
                    predictionQueue.add(predictionTuple);
                    image.close();
                    Log.v("ImageAnalysis", String.format("%s: %s", topLabel, topScore));
                } catch (IOException e) {
                    Log.e("Image Analysis", "Error during image analysis");
                    e.printStackTrace();
                    image.close();
                }
            }
        });
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);

        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int grantResults[]) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not Granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getModule(Context context, String moduleFileName) throws IOException {
        File file = new File(context.getFilesDir(), moduleFileName);
        if (file.exists() && file.length()>0) {
            return file.getAbsolutePath();
        }

        try (InputStream inputStream = context.getAssets().open(moduleFileName)) {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] moduleBuffer = new byte[4 * 1024];
                int read;
                while ((read=inputStream.read(moduleBuffer)) != -1) {
                    outputStream.write(moduleBuffer, 0, read);
                }
                outputStream.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }
}
