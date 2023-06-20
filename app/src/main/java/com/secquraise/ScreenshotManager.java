package com.secquraise;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenshotManager {
    public static final int REQUEST_CODE_CAPTURE_SCREENSHOT = 1;
    private static final String SCREENSHOT_FILE_PREFIX = "screenshot_";
    private static final String SCREENSHOT_FILE_EXTENSION = ".png";
    private Activity context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private ImageView screenshotImageView;


    public ScreenshotManager(Activity context, ImageView screenShot) {
        this.context = context;
        this.screenshotImageView = screenShot;

        projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);


    }

    public void startScreenshotCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(captureIntent, REQUEST_CODE_CAPTURE_SCREENSHOT);
            }
        }
    }

    @SuppressLint("WrongConstant")
    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection != null) {

                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                display.getMetrics(metrics);

                int screenWidth = metrics.widthPixels;
                int screenHeight = metrics.heightPixels;

                imageReader = ImageReader.newInstance(screenWidth, screenHeight, 0x1, 2);
                virtualDisplay = mediaProjection.createVirtualDisplay("Screenshot", screenWidth, screenHeight,
                        metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(), null, null);

                imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireLatestImage();
                        if (image != null) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            int width = image.getWidth();
                            int height = image.getHeight();
                            int pixelStride = image.getPlanes()[0].getPixelStride();
                            int rowStride = image.getPlanes()[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * width;
                            Bitmap screenshotBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                            screenshotBitmap.copyPixelsFromBuffer(buffer);
                            image.close();


                            // Save the screenshot to a file
                            saveScreenshot(screenshotBitmap);

                            // Display the screenshot in an ImageView
                            displayScreenshot(screenshotBitmap);
                            try {
                                screenshotImageView.setImageBitmap(screenshotBitmap);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }



                            // Release resources
                            releaseResources();
                        }
                    }
                }, null);
            }
        }
    }

    private void saveScreenshot(Bitmap screenshotBitmap) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String screenshotFileName = SCREENSHOT_FILE_PREFIX + timestamp + SCREENSHOT_FILE_EXTENSION;

        File screenshotDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!screenshotDirectory.exists()) {
            screenshotDirectory.mkdirs();
        }

        File screenshotFile = new File(screenshotDirectory, screenshotFileName);
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(screenshotFile);
            screenshotBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayScreenshot(Bitmap screenshotBitmap) {
        // TODO: Display the screenshot in an ImageView
        // screenshotImageView.setImageBitmap(screenshotBitmap);
    }

    public void releaseResources() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
