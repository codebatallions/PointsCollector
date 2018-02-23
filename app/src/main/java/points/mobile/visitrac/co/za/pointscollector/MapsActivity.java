package points.mobile.visitrac.co.za.pointscollector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback
        ,  ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {

    private GoogleMap visitracMap;
    private boolean locationPermissionGranted = false;
    private static final float DEFAULT_ZOOM = 21;
    private long UPDATE_INTERVAL = 1;  /* 5 secs */
    private long  FASTEST_INTERVAL = 1;
    private float DISPLACEMENT = 1;
    private FusedLocationProviderClient locationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location lastKnownLocation;
    private List<LatLng> pointOfInterest = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getSupportActionBar().setLogo(R.drawable.ic_launcher_background);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(checkPermissions()){
            startLocationUpdates();
        }


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
            };
        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        // inflater.inflate(R.menu.access_log_menu, menu);
        menu.add("Save Location Points Support  ");

        inflater.inflate(R.menu.save_points_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.save_points_id:
                showSavePreview();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        visitracMap = googleMap;


        if(checkPermissions()) {
            visitracMap.setMyLocationEnabled(true);
            getLastLocation();
        }
        visitracMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d("Marker", "onMarkerClick: " + marker.getPosition().latitude +"," + marker.getPosition().longitude);
                pointOfInterest.add(marker.getPosition());
                return true;
            }
        });
        visitracMap.setOnMyLocationButtonClickListener(
                new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        LatLng position = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        Utils.showSaveDialog(MapsActivity.this, pointOfInterest, position);
                        int size = pointOfInterest.size();
                        if(size >1){
                            visitracMap.addPolyline(new PolylineOptions()
                                    .addAll(pointOfInterest)
                                    .width(5)
                                    .color(Color.GREEN));
                        }
                        if(size >0) {
                            for (LatLng point : pointOfInterest) {
                                visitracMap.addMarker(new MarkerOptions().position(point)
                                        .title("Point " + (size - 1))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            }
                        }
                        return true;
                    }
                }
        );
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation() {


        locationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            onLocationChanged(location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("VisitracMap", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }



    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                303);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case 303: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }



    private void updateLocationUI() {
        if (visitracMap == null) {
            return;
        }
        try {
            if (checkPermissions()) {
                visitracMap.setMyLocationEnabled(true);
                visitracMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                requestPermissions();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void changeCameraPosition(CameraUpdate update){
        visitracMap.animateCamera(update);
    }

    /*
    private void getDeviceLocation() {

        try {
            if (locationPermissionGranted) {
                Task locationResult = locationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            visitracMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d("POI", "Current location is null. Using defaults.");
                            Log.e("POI", "Exception: %s", task.getException());
                            //visitracMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            visitracMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }*/

    private void showSavePreview() {
        //final ListView points = new ListView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int i = 0;
        final TextView detail = new TextView(this);
        StringBuilder makeDetails = new StringBuilder();
        for(LatLng poi: pointOfInterest) {
            ++i;
            makeDetails.append("("+ i + ") (" + poi.latitude + "," + poi.longitude +")");
            makeDetails.append("\n");
        }
        detail.setText(makeDetails.toString());
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("Saved clocking Points")
                .setView(detail)
                .setPositiveButton("Save",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent mailIntent = new Intent(Intent.ACTION_SEND);
                                mailIntent.setType("message/rfc822");
                                mailIntent.putExtra(Intent.EXTRA_EMAIL  , new String[]{"obakeng.balatseng@gmail.com, sefako@gmail.com"});
                                mailIntent.putExtra(Intent.EXTRA_SUBJECT, "Point of Interest");
                                mailIntent.putExtra(Intent.EXTRA_TEXT   , emailDetails());
                                try {
                                    startActivity(Intent.createChooser(mailIntent, "Send mail..."));
                                } catch (android.content.ActivityNotFoundException ex) {
                                    Toast.makeText(MapsActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
        alertDialog.create();
        alertDialog.show();

    }

    private String emailDetails(){
        StringBuilder emailMsg = new StringBuilder();
        int i= 0;
        for(LatLng poi: pointOfInterest) {

            ++i;
            emailMsg.append("("+ i + ") (" + poi.latitude + " , " + poi.longitude +")" );
            emailMsg.append("\n");
        }

        return emailMsg.toString();
    }


    @Override
    public void onLocationChanged(Location location) {
                // update map plus last location here
        // update map
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        lastKnownLocation = location;
        visitracMap.clear();
        visitracMap.animateCamera(CameraUpdateFactory.zoomTo(12f), 2000, null);
//        visitracMap.addMarker(new MarkerOptions().position(currentLocation)
//                .title(location.getLatitude() + ", " + location.getLongitude()));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLocation, 21);


        visitracMap.animateCamera(cameraUpdate);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @SuppressLint("MissingPermission")
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        locationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                          onLocationChanged(locationResult.getLastLocation());
                        //markAsVisited(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    private String pathAsJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        int index = 0;
        for (LatLng point : pointOfInterest) {
            index++;
            builder.add("code", index);
            builder.add("location", Json.createObjectBuilder().add("latitude", point.latitude)
                    .add("longitude", point.longitude));

        }
        return builder.build().toString();
    }

}
