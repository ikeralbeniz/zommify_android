package net.ikeralbeniz.zoomifytest;


import net.ikeralbeniz.zoomifytest.ZoomifyTileProvider.Resolution;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.FragmentManager;
import android.app.Activity;
import android.graphics.Color;

import java.lang.Math;
import java.util.ArrayList;

import android.util.Log;

public class MainActivity extends Activity {
	
	private GoogleMap map;
    private static final String TAG = "ZoomifyTileLoader";
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        FragmentManager fragmentManager = getFragmentManager();  
        MapFragment mapFragment = (MapFragment)fragmentManager.findFragmentById(R.id.map);  
        map = mapFragment.getMap();  
        
        Zommify zomify = new Zommify(this.getApplicationContext(), map);
        
        zomify.loadMap("http://almor.mzk.cz/moll/AA22/0103/");
        
        
        zomify.getPolygonFromWKT("POLYGON((2383.75 1301.3125,4399.75 1685.3125,3611.75 365.3125,2167.75 409.3125,2383.75 1301.3125))");
        zomify.getPolygonFromWKT("POLYGON((1976.5 2719.375,2243.5 2570.375,2249.5 2728.375,1976.5 2719.375))");
        zomify.getPolygonFromWKT("POLYGON((1794.75 -1899.6875,2210.75 -1483.6875,2402.75 -1899.6875,1794.75 -1899.6875))");
        
        
    }
   
}