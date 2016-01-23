package de.bigboot.qcircleview.cover;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;


public class LocationProvider {
    private Context context;
    private LocationManager locManager;
    private android.location.LocationListener listener = null;
    LocationListener locListener = new LocationListener();

    public LocationProvider(Context context) {
        this.context = context;
        this.locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void getCurrentLocation(android.location.LocationListener listener) {
        this.listener = listener;
        Location location = null;

        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {

        }


        if (gps_enabled) {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        }
        location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location == null) {
            if (network_enabled) {
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
            }
            location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        listener.onLocationChanged(location);
    }


    public class LocationListener implements android.location.LocationListener {

        public LocationListener() {
        }

        public void onLocationChanged(Location location) {
            if (listener != null) {
                listener.onLocationChanged(location);
            }
            locManager.removeUpdates(this);
        }

        public void onProviderDisabled(String provider) {
            if (listener != null) {
                listener.onProviderDisabled(provider);
            }
        }
        public void onProviderEnabled(String provider) {
            if (listener != null) {
                listener.onProviderEnabled(provider);
            }
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (listener != null) {
                listener.onStatusChanged(provider, status, extras);
            }
        }
    }

}
