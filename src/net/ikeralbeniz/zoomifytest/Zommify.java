package net.ikeralbeniz.zoomifytest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.ikeralbeniz.zoomifytest.ZoomifyTileProvider.Resolution;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.Color;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;


public class Zommify {
	
	private static final String TAG = "Zommify";
	
	private GoogleMap map;
	private String zoomify_url;
	private Size image_size;
	private int max_zoom;
	private int max_image_size;
	private Context parent_context;
	private LatLngBounds BOUNDS;
	
	private PointF _pixelOrigin;
    private double _pixelsPerLonDegree;
    private double _pixelsPerLonRadian;
	
	public Zommify(Context context, GoogleMap map){
		parent_context = context;
		this.map = map;
	}
	
	public void loadMap( String zoomify_url){
		
		
		try{
			image_size = getSizeFromXML(zoomify_url);
		}catch(Exception e){
			image_size = new Size(2560,2560,256);
			Log.v(TAG, e.toString());
		}
		InitMap(zoomify_url);
	}
	
	public void loadMap(Context context, GoogleMap map, String zoomify_url, Size size){

		this.image_size = size;
		InitMap(zoomify_url);
	}
	
	private void InitMap(String zoomify_url){
		
		
		this.zoomify_url = zoomify_url;
		
		getMaxZoom();
		
		_pixelOrigin = new PointF(image_size.tilesize / 2.0,image_size.tilesize / 2.0);
	    _pixelsPerLonDegree = image_size.tilesize / 360.0;
	    _pixelsPerLonRadian = image_size.tilesize / (2 * Math.PI);
	    
		BOUNDS = getLimitForZoom(image_size.width, image_size.height, max_zoom);
		
		map.clear();
		map.setMapType(GoogleMap.MAP_TYPE_NONE);
		map.setPadding(0, 0, 0, 0);
		map.addTileOverlay(new TileOverlayOptions().tileProvider(new ZoomifyTileProvider( this.getApplicationContext(), zoomify_url, image_size, Resolution.Normal)));
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(BOUNDS.getCenter(), 0); 
		map.moveCamera(cameraUpdate);
		
	}

	private Context getApplicationContext() {
		// TODO Auto-generated method stub
		return this.parent_context;
	}

	private void getMaxZoom(){
		
		this.max_zoom = 0;
		this.max_image_size = (int)( image_size.tilesize * Math.pow(2, max_zoom));
		
		while(max_image_size < image_size.width || max_image_size < image_size.height){
			this.max_zoom++;
			this.max_image_size = (int)( image_size.tilesize * Math.pow(2, max_zoom));
		}
		image_size.deep = max_zoom;
	}
	
	private Size getSizeFromXML(String url) throws ParserConfigurationException, MalformedURLException, SAXException, IOException{
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new URL(url+"ImageProperties.xml").openStream());
		
		int width = 2560;
		int height = 2560;
		int tilesize = 256;
		
		try{width = Integer.parseInt(doc.getDocumentElement().getAttribute("WIDTH"));}catch(Exception e){}
		try{height = Integer.parseInt(doc.getDocumentElement().getAttribute("HEIGHT"));}catch(Exception e){}
		try{tilesize = Integer.parseInt(doc.getDocumentElement().getAttribute("TILESIZE"));}catch(Exception e){}
		
		return new Size(width,height,tilesize);
	}
	
    private LatLngBounds getLimitForZoom(int width, int height, int zoom){
    	
    	return new LatLngBounds(convertPixelToLatLong(0, -0),convertPixelToLatLong(image_size.width , image_size.height));
    }
	
	public class Size{
		public int width;
	    public int height;
	    public int tilesize;
	    public int deep = 0;
	    
	    public Size(int width, int height, int tilesize){
	    	this.width = width;
	    	this.height = height;
	    	this.tilesize = tilesize;
	    }
	}
	
    public void getPolygonFromWKT(String wkt_string){
    	
    	ArrayList<String> coordinates_s = new ArrayList<String>();
    	ArrayList<LatLng> coordinates = new ArrayList<LatLng>();
    	String[] wkt_command = wkt_string.split("\\(");
    	String command = wkt_command[0];
    	Log.v(TAG,command);
    	Log.v(TAG,wkt_command[2]);
    	String points = wkt_command[2].replaceAll("[()]", "");


    	String[] commatokens = points.split(",");
    	for (String commatoken : commatokens) {
    		coordinates_s.add(commatoken);
    	}
    	     
    	for (int i = 0; i < coordinates_s.size(); i++) {
    		
    		
    	    String[] tokens = coordinates_s.get(i).split("\\s");
    	    try{
    	    	double x = Double.parseDouble(tokens[0]);
    	    	double y = Double.parseDouble(tokens[1]);
    	    	Log.v(TAG,Double.toString(x)+"/"+Double.toString(y));
    	    	coordinates.add(convertPixelToLatLong(x,y));
    	    }catch(Exception e){}
    	}
    	
    	int a = 0;
    	a = a +1;
    	Polygon polygon = map.addPolygon(new PolygonOptions().addAll(coordinates).strokeColor(Color.RED).zIndex(99).fillColor(Color.BLUE));
    	
    }
    
    private LatLng convertPixelToLatLong(double x, double y){
    	PointF newpoint = fromPointToLatLng(new PointF(x, (image_size.height - y)), max_zoom);
    	return new LatLng(newpoint.x,newpoint.y);
    }
    
    double radiansToDegrees(double rad) 
    {
        return rad / (Math.PI / 180);
    }
    
    PointF fromPointToLatLng(PointF point, int zoom)
    {
        int numTiles = 1 << zoom;
        point.x = point.x / numTiles;
        point.y = point.y / numTiles;       

        double lng = (point.x - _pixelOrigin.x) / _pixelsPerLonDegree;
        double latRadians = (point.y - _pixelOrigin.y) / - _pixelsPerLonRadian;
        double lat = radiansToDegrees(2 * Math.atan(Math.exp(latRadians)) - Math.PI / 2);
        return new PointF(lat, lng);
    }
    
    public final class PointF 
    {
        public double x;
        public double y;

        public PointF(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
    }

}
