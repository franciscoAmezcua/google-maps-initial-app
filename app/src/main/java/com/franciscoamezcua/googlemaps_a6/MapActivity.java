package com.franciscoamezcua.googlemaps_a6;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.franciscoamezcua.googlemaps_a6.models.PlaceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by famezcua on 2018-01-31.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener{

    private Spinner spinner;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: the map is ready");
        mMap = googleMap;

        if (mLocationPermissionGranted) {
//            getDeviceLocation();
            googleMap.clear();
            goToPlace1();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();
        }
    }

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private  static final int REQUEST_CODE_FOR_PERMISSION = 1234;
    private static final float DEFAULT_ZOOM = 17.0f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40,-168), new LatLng(71, 36));
    private Button btnPlace1;
    private Button btnPlace2;
    private Button btnPlace3;

    // Variables
    private Boolean mLocationPermissionGranted = false;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;

    // Widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGps;

    // Google Maps Variable
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final LatLng THEFORKS = new LatLng(49.887718, -97.131432);
    private static final LatLng MOUNTAIN_VIEW = new LatLng(37.4, -122.1);


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        btnPlace1 = (Button) findViewById(R.id.place1);
        btnPlace2 = (Button) findViewById(R.id.place2);
        btnPlace3 = (Button) findViewById(R.id.place3);

        getLocationPermission();

        //Initialize map type menu
        spinner = (Spinner) findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                switch (position){
                    case 0: // Change to Hybrid
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                    case 1: // Change to Satellite
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case 2: // Change to Normal
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        break;
                    case 3: // Change to Terrain
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;

                    default:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnPlace1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToPlace1();
            }
        });

        btnPlace2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToPlace2();
            }
        });

        btnPlace3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToPlace3();
            }
        });


    }

    private  void init(){
        Log.d(TAG, "init: initializing");

        mGoogleApiClient = new GoogleApiClient
                                        .Builder(this)
                                        .addApi(Places.GEO_DATA_API)
                                        .addApi(Places.PLACE_DETECTION_API)
                                        .enableAutoManage(this,this)
                                        .build();

        mSearchText.setOnItemClickListener(mAutoCompleteClickListener);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this,mGoogleApiClient,
                LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE ||
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN ||
                        keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){
                    //Executing Search
                    geoLocate();
                }

                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"onClick: getting device location when clicking locate button");
                getDeviceLocation();
            }
        });



        hideSoftKeyboard();
    }

    private void geoLocate(){
        Log.d(TAG,"geoLocate: starting geoLocation");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.d(TAG,"geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()),DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }

    private void getDeviceLocation(){
        mMap.clear();
        Log.d(TAG,"getDeviceLocation: Getting device's current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if(mLocationPermissionGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: Location is found");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM, "Current Location");
                        }else{
                            Log.d(TAG, "onComplete: Current location could not be found or null");
                            Toast.makeText(MapActivity.this, "unable to get current location",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.d(TAG,"getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: current position followed by camera to: lat: " + latLng.latitude
        + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));

        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
        hideSoftKeyboard();
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission (){

        Log.d(TAG, "getLocationPermission: getting permissions for location");

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                        COURSE_LOCATION)== PackageManager.PERMISSION_GRANTED){

                        mLocationPermissionGranted = true;
                        initMap();

                }else{
                    ActivityCompat.requestPermissions(this, permissions,REQUEST_CODE_FOR_PERMISSION);
                }
        }else{
            ActivityCompat.requestPermissions(this, permissions,REQUEST_CODE_FOR_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");

        mLocationPermissionGranted = false;

        switch (requestCode){
            case REQUEST_CODE_FOR_PERMISSION:{
                if(grantResults.length >0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){

                    for(int i =0; i < grantResults.length; i++){
                        if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission not granted");
                            return;
                        }
                    }

                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionGranted = true;
                    //Initializing the map
                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*
    _______________________Moving to Marker's position______________________________________
     */
    private void goToPlace1(){
        moveCamera(new LatLng(49.887718, -97.131432),DEFAULT_ZOOM,"");

        Marker marker = mMap.addMarker(
                new MarkerOptions()
                .position(new LatLng(49.887718, -97.131432))
                .title("The Forks")
                .snippet("My Favourite Place in Winnipeg")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_place1))
        );

    }

    private void goToPlace2(){
        moveCamera(new LatLng(20.631229, -87.064801),DEFAULT_ZOOM,"");

        Marker marker2 = mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(20.631229, -87.064801))
                        .title("Playa Mamitas")
                        .snippet("2nd Favourite Place")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_place2))
        );
    }

    private void goToPlace3(){
        moveCamera(new LatLng(53.988175, -100.948485),12.0f,"");

        Marker marker3 = mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(53.988175, -100.948485))
                        .title("Clearwater Lake")
                        .snippet("3rd Favourite Place")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher_place3))
        );
    }

    /*
    _______________________AUTO COMPLETE GOOGLE PLACES_APIs______________________________________
     */

    private AdapterView.OnItemClickListener mAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallBack);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallBack = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "Place Query was not completed successfully" + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try {
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                mPlace.setAddress(place.getAddress().toString());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                mPlace.setId(place.getId());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                mPlace.setLatlng(place.getLatLng());
                mPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: place: " + mPlace.toString());

            }catch (NullPointerException e){
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude,place.getViewport().getCenter().longitude),
                    DEFAULT_ZOOM, mPlace.getName() +": " + mPlace.getPhoneNumber());
            places.release();

        }
    };

}
