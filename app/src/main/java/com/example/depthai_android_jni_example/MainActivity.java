package com.example.depthai_android_jni_example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.depthai_android_jni_example.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'depthai_android_jni_example' library on application startup.
    static {
        System.loadLibrary("depthai_android_jni_example");
    }
    private AssetManager mgr;

    private static final String yolov3_model_path = "yolo-v3-tiny-tf.blob";
    private static final String yolov4_model_path = "yolov4_tiny_coco_416x416_6shave.blob";
    private static final String yolov5_model_path = "yolov5s_416_6shave.blob";
    private static final String mobilenet_model_path = "mobilenet-ssd.blob";

    private ImageView rgbImageView, depthImageView;
    private Bitmap rgb_image, depth_image;

    private static final int rgbWidth = 416;
    private static final int rgbHeight = 416;
    private static final int disparityWidth = 640;
    private static final int disparityHeight = 400;
    private static final int framePeriod = 30;

    private boolean running, firstTime;
    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("libusb.android.USB_PERMISSION".equals(action)) {
                isPermissionGranted = true;
                Log.d("MYTAG", "Detected! Permission granted: " + isPermissionGranted);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("libusb.android.USB_PERMISSION");
        registerReceiver(usbPermissionReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(usbPermissionReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mgr = getResources().getAssets();
        load(mgr);
        com.example.depthai_android_jni_example.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());

        // Initialize the image views to show the images
        rgbImageView = binding.rgbImageView;
        depthImageView = binding.depthImageView;

        // Initialize the image bitmaps to store the image data
        rgb_image = Bitmap.createBitmap(rgbWidth, rgbHeight, Bitmap.Config.ARGB_8888);
        depth_image = Bitmap.createBitmap(disparityWidth, disparityHeight, Bitmap.Config.ARGB_8888);

        // Specify running and firstTime (to connect to device)
        if(savedInstanceState != null){
            running = savedInstanceState.getBoolean("running", true);
            firstTime = savedInstanceState.getBoolean("firstTime", true);
        } else {
            running = true;
            firstTime = true;
        }

        // Main loop that will read the feed from the device at framePeriod (ms) intervals
        runnable.run();
        isPermissionGranted = connectDevice();
        Log.d("MYTAG", "isPermissionGranted: " + isPermissionGranted);
    }

    // Main loop where the data is obtained from the device and shown into the screen
    private final Handler handler = new Handler();
    private boolean isPermissionGranted;
    private final Runnable runnable = new Runnable() {
        public void run() {

            if(running){
                if (isPermissionGranted) {
                    if(firstTime){
                        // Start the device
                        isPermissionGranted = startDevice(yolov5_model_path, rgbWidth, rgbHeight);
                        firstTime = false;
                    }

                    int[] rgb = imageFromJNI();
                    if (rgb != null && rgb.length > 0) {
                        rgb_image.setPixels(rgb, 0, rgbWidth, 0, 0, rgbWidth, rgbHeight);
                        rgbImageView.setImageBitmap(rgb_image);
                    }

                    int[] detections_img = detectionImageFromJNI();
                    if (detections_img != null && detections_img.length > 0) {
                        rgb_image.setPixels(detections_img, 0, rgbWidth, 0, 0, rgbWidth, rgbHeight);
                        rgbImageView.setImageBitmap(rgb_image);
                    }

                    int[] depth = depthFromJNI();
                    if (depth != null && depth.length > 0) {
                        depth_image.setPixels(depth, 0, disparityWidth, 0, 0, disparityWidth, disparityHeight);
                        depthImageView.setImageBitmap(depth_image);
                    }
                }

                handler.postDelayed(this, framePeriod);

            }

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        running = false;
        firstTime = false;
    }

    // Save the variable before completing the activity
    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        // Write the variable with the key in the Bundle
        outState.putBoolean("running", running);
        outState.putBoolean("firstTime", firstTime);
    }

    public AssetManager getAssetManager() { return getAssets(); }

    /**
     * A native method that is implemented by the 'depthai_android_jni_example' native library,
     * which is packaged with this application.
     *
     * @return
     */

    private native void load(AssetManager mgr);
    public native boolean connectDevice();
    public native boolean startDevice(String model_path, int rgbWidth, int rgbHeight);
    public native int[] imageFromJNI();
    public native int[] detectionImageFromJNI();
    public native int[] depthFromJNI();
}