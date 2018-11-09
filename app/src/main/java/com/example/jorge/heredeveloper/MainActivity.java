package com.example.jorge.heredeveloper;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.cluster.ClusterLayer;
import com.here.android.mpa.cluster.ClusterTheme;
import com.here.android.mpa.cluster.ImageClusterStyle;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.LocationDataSource;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.customlocation2.CLE2Geometry;
import com.here.android.mpa.customlocation2.CLE2ProximityRequest;
import com.here.android.mpa.customlocation2.CLE2Request;
import com.here.android.mpa.customlocation2.CLE2Result;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements PositioningManager.OnPositionChangedListener {

    private Map map;
    // positioning manager instance
    private PositioningManager mPositioningManager;

    // HERE location data source instance
    private LocationDataSourceHERE mHereLocation;

    // flag that indicates whether maps is being transformed
    private boolean mTransforming;

    // callback that is called when transforming ends
    private Runnable mPendingUpdate;

    // text view instance for showing location information
    private TextView mLocationInfo;
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        setContentView(R.layout.activity_main);
        final Map map;

        final MapFragment mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.mapfragment2);

        // initialize the Map Fragment and
        // retrieve the map that is associated to the fragment
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // now the map is ready to be used
                    final Map map = mapFragment.getMap();

                    map.setCenter(new GeoCoordinate(49.196261,
                            -123.004773), Map.Animation.NONE);

                    // Move the map to London.


// Get the current center of the Map

                    // Get the maximum,minimum zoom level.
                    double maxZoom = map.getMaxZoomLevel();
                    double minZoom = map.getMinZoomLevel();

// Set the zoom level to the median (10).
                    map.setZoomLevel((maxZoom + minZoom)/2);


// Get the zoom level back
                    double zoom = map.getZoomLevel();



                    mPositioningManager = PositioningManager.getInstance();
                    LocationDataSource m_hereDataSource = LocationDataSourceHERE.getInstance();
                    if (m_hereDataSource != null) {


                        mPositioningManager.setDataSource(m_hereDataSource);
                        mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(MainActivity.this));
                        if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR)) {
                            // Position updates started successfully.
                            mapFragment.getPositionIndicator().setVisible(true);
                        }
                    }
                    // ...
                } else {
                    System.out.println("ERROR: Cannot initialize MapFragment");
                }
            }
        });

    }



    @Override
    public void onPositionUpdated(final PositioningManager.LocationMethod locationMethod, final GeoPosition geoPosition, final boolean b) {


        final GeoCoordinate coordinate = geoPosition.getCoordinate();
        if (mTransforming) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    onPositionUpdated(locationMethod, geoPosition, b);
                }
            };
        } else {
            map.setCenter(coordinate, Map.Animation.BOW);
            updateLocationInfo(locationMethod, geoPosition);
        }





    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {

    }


    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                //initialize();
                break;
        }
    }

        public void updateLocationInfo (PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition) {
            if (mLocationInfo == null) {
                return;
            }
            final StringBuffer sb = new StringBuffer();
            final GeoCoordinate coord = geoPosition.getCoordinate();
            sb.append("Type: ").append(String.format(Locale.US, "%s\n", locationMethod.name()));
            sb.append("Coordinate:").append(String.format(Locale.US, "%.6f, %.6f\n", coord.getLatitude(), coord.getLongitude()));
            if (coord.getAltitude() != GeoCoordinate.UNKNOWN_ALTITUDE) {
                sb.append("Altitude:").append(String.format(Locale.US, "%.2fm\n", coord.getAltitude()));
            }
            if (geoPosition.getHeading() != GeoPosition.UNKNOWN) {
                sb.append("Heading:").append(String.format(Locale.US, "%.2f\n", geoPosition.getHeading()));
            }
            if (geoPosition.getSpeed() != GeoPosition.UNKNOWN) {
                sb.append("Speed:").append(String.format(Locale.US, "%.2fm/s\n", geoPosition.getSpeed()));
            }
            if (geoPosition.getBuildingName() != null) {
                sb.append("Building: ").append(geoPosition.getBuildingName());
                if (geoPosition.getBuildingId() != null) {
                    sb.append(" (").append(geoPosition.getBuildingId()).append(")\n");
                } else {
                    sb.append("\n");
                }
            }
            if (geoPosition.getFloorId() != null) {
                sb.append("Floor: ").append(geoPosition.getFloorId()).append("\n");
            }
            sb.deleteCharAt(sb.length() - 1);
            mLocationInfo.setText(sb.toString());
        }



    private void setLocationMethod() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String[] names = getResources().getStringArray(R.array.locationMethodNames);
        builder.setTitle("Location")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            final String[] values = getResources().getStringArray(R.array.locationMethodValues);
                            final PositioningManager.LocationMethod method =
                                    PositioningManager.LocationMethod.valueOf(values[which]);
                            setLocationMethod(method);
                        } catch (IllegalArgumentException ex) {
                            Toast.makeText(MainActivity.this, "setLocationMethod failed: "
                                    + ex.getMessage(), Toast.LENGTH_LONG).show();
                        } finally {
                            dialog.dismiss();
                        }
                    }
                });
        builder.create().show();
    }







}
