package com.example.kd.blackspot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kd.blackspot.adapters.PlaceAutocompleteAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener {

    private static String FINE_LOC = Manifest.permission.ACCESS_FINE_LOCATION;
    private static String COARSE_LOC = Manifest.permission.ACCESS_COARSE_LOCATION;
    final static int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private boolean mPermissionGranted = false;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-50, -200), new LatLng(  100, 150));

    //vars
    private GoogleMap mMap;
    private String TAG = "Tag";
    private FusedLocationProviderClient mFusedLocationProviderClient;
    LocationCallback locationCallback;
    LocationRequest mLocationRequest;
    PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;

    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGps;

    // google maps interface method
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mPermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.d(TAG, "onMapReady: setting my location true");
            mMap.setMyLocationEnabled(true);
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = findViewById(R.id.gps);
        getPermissions();
        init();
    }

    private void init(){

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_SEARCH ||
                        i == EditorInfo.IME_ACTION_DONE ||
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN ||
                        keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){

                    //locate the user entered location
                    geoLocate();
                }
                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });

//        Utils.hideSoftKeyboard(this);
    }

    private void geoLocate() {

        Geocoder gc = new Geocoder(NavigationActivity.this);
        List<Address> list = new ArrayList<>();

        try{
            list =  gc.getFromLocationName(mSearchText.getText().toString(), 1);
        }catch (Exception e){
            Log.d(TAG, "geoLocate: gen excp while locating location " + e);
        }

        if(list.size()>0){
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: location found : " + address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
        }
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mPermissionGranted &&
                    ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOC) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOC) == PackageManager.PERMISSION_GRANTED) {
                Task location = mFusedLocationProviderClient.getLastLocation();

                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: " + task.getException() + "result : " + task.getResult());
                            if (task.getResult() == null) {
                                //TODO fallback way if above task returns null
                                createLocationRequest();
                                createLocationCallback();
                                if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                Log.d(TAG, "onComplete: request");
                                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());

                            }
                            else {
                                Location loc = (Location) task.getResult();
                                moveCamera(new LatLng(loc.getLatitude(), loc.getLongitude()), DEFAULT_ZOOM, "My location");
                            }
                        }else
                            Log.d(TAG, "onComplete: unable to get location " + task.getException());
//                            Toast.makeText(MainActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }catch (Exception e){
            Log.d(TAG, "getDeviceLocation: " + e);
        }

    }

    private void createLocationCallback() {
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //code here
                Location loc = locationResult.getLastLocation();
                Log.d(TAG, "onLocationResult: from callback " + loc.getLatitude() + loc.getLongitude());
            }
        };
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2*1000);
        mLocationRequest.setFastestInterval(2*1000);
        mLocationRequest.setSmallestDisplacement(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if(!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(NavigationActivity.this);
    }

    private void getPermissions() {

        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOC) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOC) == PackageManager.PERMISSION_GRANTED) {
                mPermissionGranted = true;
                initMap();
            }
        }
        else
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionGranted = false;

        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0){
                    for(int i=0; i<grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mPermissionGranted = false;
                            //TODO call initMap here
                            return;
                        }
                    }
                    mPermissionGranted = true;
//                    initMap();
                    //init map now
                }
        }
    }

    /**
     ----------------------- google places api  autocomplete suggestions and adapter ------------------------
     */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
