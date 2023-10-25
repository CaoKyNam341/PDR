package edu.gcu.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private SensorManager sensorManager;
    private SensorManager sensorManagerr;
    private SensorManager sensorManagerrr;

    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeSensorListener;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private GoogleMap googleMap; // Declare GoogleMap reference
    private GoogleMap searchMap;

    private EditText searchEditText;
    private Button searchButton;

    double x, y;
    float accuracy;

    Location lastLocation = null;
    Location location = null;

    private Sensor accelerometerSensor, magnetometerSensor;

    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    boolean isLastAccelerometerArrayCopied = false;
    boolean isLastMagnetometerArrayCopied = false;

    float lastUpdateTime = 0;
    float currentDegree = 0f;
    float angle = 0;
    float last = 0;

    float gyro = 0;

    private long lastSensorUpdateTime = 0;
    private static final long SENSOR_UPDATE_INTERVAL = 600;

    private TextView tech;
    private TextView accu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize search views
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        tech = findViewById(R.id.technology);
        accu = findViewById(R.id.accuracyy);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLocation();
            }
        });

        //sensor
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            }
        } else {
            Toast.makeText(this, "Sensor service not detected", Toast.LENGTH_SHORT).show();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManagerr = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManagerr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManagerr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManagerrr = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroscopeSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gyro = event.values[0];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.sensor == accelerometerSensor) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            isLastAccelerometerArrayCopied = true;
        } else if (event.sensor == magnetometerSensor) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            isLastMagnetometerArrayCopied = true;
        }
        if (isLastAccelerometerArrayCopied && isLastMagnetometerArrayCopied) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegree = (float) Math.toDegrees(azimuthInRadians);
            RotateAnimation rotateAnimation = new RotateAnimation(currentDegree, -azimuthInDegree,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(250);
            rotateAnimation.setFillAfter(true);
            currentDegree = -azimuthInDegree;
            angle = azimuthInDegree;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        accuracy = location.getAccuracy();
                    }
                });
            }
            if (accuracy < 10 && currentTime - lastSensorUpdateTime >= SENSOR_UPDATE_INTERVAL) {
                startLocationUpdates();
                tech.setText("Công nghệ đang sử dụng: GPS");
            }
            else if (Math.abs(event.values[2] - last) > 1.5 && currentTime - lastSensorUpdateTime >= SENSOR_UPDATE_INTERVAL
                    && Math.abs(gyro) > 0.25) {
                x += 0.00000585 * Math.cos(Math.toRadians(angle));
                y += 0.00000585 * Math.sin(Math.toRadians(angle));
                LatLng latLng = new LatLng(x, y);
                last = event.values[2];
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Vị trí của bạn"));
                lastSensorUpdateTime = currentTime;
                tech.setText("Công nghệ đang sử dụng: PDR");
            }
        }
        accu.setText("Độ chính xác: ~" + String.valueOf(accuracy) + "m");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onMapReady(GoogleMap map) {
        // This method will be called when the map is ready to be used.
        googleMap = map; // Assign the GoogleMap reference here
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            googleMap.setMyLocationEnabled(true);
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //locationRequest.setInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    location = locationResult.getLastLocation();
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        x = latitude;
                        y = longitude;
                        lastLocation = location;
                        accuracy = location.getAccuracy();
                        LatLng latLng = new LatLng(latitude, longitude);
                        googleMap.clear();
                        Geocoder geocoder = new Geocoder(MainActivity.this);
                        try {
                            List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            googleMap.addMarker(new MarkerOptions().position(latLng).title(address.get(0).getAddressLine(0)));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManagerr.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManagerr.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManagerrr.registerListener(gyroscopeSensorListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mapView.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManagerr.unregisterListener(this, accelerometerSensor);
        sensorManagerr.unregisterListener(this, magnetometerSensor);
        sensorManager.unregisterListener(gyroscopeSensorListener);
        stopLocationUpdates();
        mapView.onPause();

    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapView.getMapAsync(this);
            } else {
                // Handle permission denied
            }
        }
    }

    private void searchLocation() {
        String addressQuery = searchEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(addressQuery)) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocationName(addressQuery, 1);
                if (!addressList.isEmpty()) {
                    Address address = addressList.get(0);
                    double latitude = address.getLatitude();
                    double longitude = address.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                    //googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(latLng).title(address.getAddressLine(0)));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 30));
                } else {
                    // Handle address not found
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle empty search query
        }
    }
}

