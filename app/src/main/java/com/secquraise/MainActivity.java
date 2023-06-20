package com.secquraise;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class MainActivity extends AppCompatActivity implements
        BatteryInfoReceiver.BatteryInfoListener {
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
            // Add more permissions as needed
    };
    private int screenshotCount = 0;
    private final static int ALL_PERMISSIONS_RESULT = 101;
    boolean isMobileDataEnabled;
    GPSManager locationTrack;
    private BatteryInfoReceiver batteryInfoReceiver;
    private TextView captureCount, freq, conn, bCharging, bCharg, Loc, time;
    private ScreenshotManager screenshotManager;
    private ImageView screenShot;
    private Button btn;
    private ArrayList permissionsToRequest;
    private final ArrayList permissionsRejected = new ArrayList();
    private final ArrayList permissions = new ArrayList();
    private Handler handler;
    private Runnable screenshotRunnable;
    private StorageReference storageReference;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    LocationData locationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        batteryInfoReceiver = new BatteryInfoReceiver(this);
        registerBatteryInfoReceiver();

        //mobile data ON/OFF
        isMobileDataEnabled = MobileDataHelper.isMobileDataEnabled(this);

        // Check the permissions
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (!PermissionUtils.checkPermission(this, permission)) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            // All permissions are granted
           // performAction();
        } else {
            // Request the permissions
            PermissionUtils.requestPermissions(this, permissionsToRequest.toArray(new String[0]));
        }

        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);

        permissionsToRequest = findUnAskedPermissions(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                try {
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }
        getViews();
        setView();
        screenshotManager = new ScreenshotManager(this, screenShot);

        // Get the Firebase Storage reference
        storageReference = FirebaseStorage.getInstance().getReference();

        // instance of our FIrebase database.
        firebaseDatabase = FirebaseDatabase.getInstance();

        // below line is used to get reference for our database.
        databaseReference = firebaseDatabase.getReference("LocationData");

        // initializing our object
        // class variable.
        locationData = new LocationData();


    }

    private void performAction() {
        // Permission is granted, perform the desired action
        // Your code goes here


    }


    @SuppressLint("SetTextI18n")
    private void setView() {
        //  captureCount.setText("");
        freq.setText("15");
        try {
            if (isMobileDataEnabled) {
                conn.setText("ON");
            } else {
                conn.setText("OFF");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        time.setText(DateUtils.getCurrentDate());


        btn.setOnClickListener(v -> {
            time.setText("");
            freq.setText("");
            conn.setText("");
            Loc.setText("");
            bCharg.setText("");
            bCharging.setText("");

            isMobileDataEnabled = MobileDataHelper.isMobileDataEnabled(this);
            time.setText(DateUtils.getCurrentDate());
            freq.setText("15");
            if (isMobileDataEnabled) {
                conn.setText("ON");
            } else {
                conn.setText("OFF");
            }


            locationTrack = new GPSManager(MainActivity.this);

            batteryInfoReceiver = new BatteryInfoReceiver(this);
            registerBatteryInfoReceiver();
            captureScreenshot();

            if (locationTrack.canGetLocation()) {


                double longitude = locationTrack.getLongitude();
                double latitude = locationTrack.getLatitude();

                String Locas = latitude + ", " + longitude;
                Loc.setText(Locas);
                //  Toast.makeText(getApplicationContext(), "Longitude:" + longitude + "\nLatitude:" + latitude, Toast.LENGTH_SHORT).show();
            } else {

                locationTrack.showSettingsAlert();
            }


        });

        locationTrack = new GPSManager(MainActivity.this);


        if (locationTrack.canGetLocation()) {


            double longitude = locationTrack.getLongitude();
            double latitude = locationTrack.getLatitude();

            String Loca = latitude + ", " + longitude;
            Loc.setText(Loca);
            //  Toast.makeText(getApplicationContext(), "Longitude:" + longitude + "\nLatitude:" + latitude, Toast.LENGTH_SHORT).show();
        } else {

            locationTrack.showSettingsAlert();
        }
        handler = new Handler();
        screenshotRunnable = new Runnable() {
            @Override
            public void run() {
                captureScreenshot();
                handler.postDelayed(this, 1 * 60 * 1000); // Capture every 5 minutes (5 * 60 * 1000 milliseconds)
            }
        };

        startAutoCapture();


    }

    private void getViews() {
        captureCount = findViewById(R.id.captureCountValue);
        freq = findViewById(R.id.frequencyValue);
        conn = findViewById(R.id.connectivityValue);
        bCharging = findViewById(R.id.batteryChargingValue);
        bCharg = findViewById(R.id.batteryChargValue);
        Loc = findViewById(R.id.locationValue);
        screenShot = findViewById(R.id.imageView2);
        time = findViewById(R.id.timeValue);
        btn = findViewById(R.id.button);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryInfoReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryInfoReceiver);
        locationTrack.stopListener();
    }

    private void captureScreenshot() {
        screenshotManager.startScreenshotCapture();
        screenshotCount++;
        captureCount.setText(String.valueOf(screenshotCount));

        // Get the bitmap from the screenshotManager (assuming it is a Bitmap)
        Bitmap screenshotBitmap =  ((BitmapDrawable) screenShot.getDrawable()).getBitmap();

        // Generate a unique filename for the image
        String filename = DateUtils.getCurrentDate() + ".jpg";

        // Create a reference to the filename in Firebase Storage
        StorageReference imageRef = storageReference.child("/LocationDataScreenshot").child(filename);

        // Convert the Bitmap to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        screenshotBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        // Upload the byte array to Firebase Storage
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Image upload successful
              //  Toast.makeText(MainActivity.this, "Image uploaded to Firebase Storage", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Image upload failed
               // Toast.makeText(MainActivity.this, "Failed to upload image to Firebase Storage", Toast.LENGTH_SHORT).show();
            }
        });



        String f_captureCount = captureCount.getText().toString();
        String f_connectivity = conn.getText().toString();
        String f_bCharging = bCharging.getText().toString();
        String f_frequency = freq.getText().toString();
        String f_bCharge = bCharg.getText().toString();
        String f_location = Loc.getText().toString();


        locationData.setCaptureCount(f_captureCount);
        locationData.setConnectivity(f_connectivity);
        locationData.setbCharging(f_bCharging);
        locationData.setFrequency(f_frequency);
        locationData.setbCharge(f_bCharge);
        locationData.setLocation(f_location);




        // we are use add value event listener method
        // which is called with database reference.
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // inside the method of on Data change we are setting
                // our object class to our database reference.
                // data base reference will sends data to firebase.
                databaseReference.child(DateUtils.getCurrentDate()).setValue(locationData);

                // after adding this data we are showing toast message.
              //  Toast.makeText(MainActivity.this, "data added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // if the data is not added or it is cancelled then
                // we are displaying a failure toast message.
               // Toast.makeText(MainActivity.this, "Fail to add data " + error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void startAutoCapture() {
        handler.postDelayed(screenshotRunnable, 1 * 60 * 1000); // Start capturing after 5 minutes (5 * 60 * 1000 milliseconds)
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBatteryInfoReceiver();
        locationTrack.stopListener();
        screenshotManager.releaseResources();
        handler.removeCallbacks(screenshotRunnable); // Stop the auto capture when the activity is destroyed
    }

    private void registerBatteryInfoReceiver() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryInfoReceiver, intentFilter);
    }

    private void unregisterBatteryInfoReceiver() {
        unregisterReceiver(batteryInfoReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ScreenshotManager.REQUEST_CODE_CAPTURE_SCREENSHOT) {
            screenshotManager.onActivityResult(resultCode, data);
        }

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // All permissions granted
               // performAction();
            } else {
                // Some permissions denied
                // Toast.makeText(this, "Some permissions were denied.", Toast.LENGTH_SHORT).show();

            }
        }

        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (Object perms : permissionsToRequest) {
                if (!hasPermission(perms)) {
                    permissionsRejected.add(perms);
                }
            }

            if (permissionsRejected.size() > 0) {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale((String) permissionsRejected.get(0))) {
                        showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions((String[]) permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                        }
                                    }
                                });
                        return;
                    }
                }

            }
        }

    }


    @SuppressLint("SetTextI18n")
    @Override
    public void onBatteryInfoReceived(String batteryPercentage, String chargingStatus) {
        bCharg.setText(batteryPercentage);
        bCharging.setText(chargingStatus);
    }


    private ArrayList findUnAskedPermissions(ArrayList wanted) {
        ArrayList result = new ArrayList();

        for (Object perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(Object permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission((String) permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}