package net.ikeralbeniz.zoomifytest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ikeralbeniz.zoomifytest.Zommify.Size;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Environment;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;


public class ZoomifyTileProvider implements TileProvider {
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;

    private Context mContext;
    private String mUrl;
    private Size mSize;
    
    public enum Resolution { Normal, Retina };
    
    private Resolution mResoultion;
    
    public ZoomifyTileProvider(Context context, String url, Size size) {
    	mContext = context;
        mResoultion = Resolution.Normal;
        mUrl = url;
        mSize = size;
    }
    
    public ZoomifyTileProvider(Context context, String url, Size size, Resolution resolution) {
    	mContext = context;
        mResoultion = resolution;
        mUrl = url;
        mSize = size;
    }
    
    
    @Override
    public Tile getTile(int x, int y, int zoom) {
    	if(mResoultion == Resolution.Normal){
    		byte[] image = readTileImage(x, y, zoom);
        	return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);
    	}else{
    		byte[] image;
			try {
				image = readBigTileImage(x, y, zoom);
				return image == null ? null : new Tile(TILE_WIDTH*2, TILE_HEIGHT*2, image);
			} catch (InterruptedException e) {
				image = readTileImage(x, y, zoom);
	        	return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);
			}
        	
    	}
    }
    
    private byte[] readBigTileImage(int x, int y, int zoom) throws InterruptedException {
    	
    	ExecutorService executor = Executors.newFixedThreadPool(5);
    	
    	TileLoader worker0 = new TileLoader(mContext, mUrl, mSize, x*2, y*2, zoom+1);
        executor.execute(worker0);
        
        TileLoader worker1 = new TileLoader(mContext, mUrl, mSize,(x*2)+1, y*2, zoom+1);
        executor.execute(worker1);
        
        TileLoader worker2 = new TileLoader(mContext, mUrl, mSize, x*2, (y*2)+1, zoom+1);
        executor.execute(worker2);
        
        TileLoader worker3 = new TileLoader(mContext, mUrl, mSize, (x*2)+1, (y*2)+1, zoom+1);
        executor.execute(worker3);
        
        executor.shutdown();
      
        while (!executor.isTerminated()) {Thread.sleep(100);}
        
        byte[] image0 = worker0.raw_tile;
    	Bitmap bmp0 = BitmapFactory.decodeByteArray(image0,0,image0.length);
    	
    	byte[] image1 = worker1.raw_tile;
    	Bitmap bmp1 = BitmapFactory.decodeByteArray(image1,0,image1.length);
    	
    	byte[] image2 = worker2.raw_tile;
    	Bitmap bmp2 = BitmapFactory.decodeByteArray(image2,0,image2.length);
    	
    	byte[] image3 = worker3.raw_tile;
    	Bitmap bmp3 = BitmapFactory.decodeByteArray(image3,0,image3.length);
    	
    	Bitmap mBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        mBitmap.eraseColor(0x00000000);
        Canvas comboImage = new Canvas(mBitmap);
    	
    	comboImage.drawBitmap(bmp0, 0f, 0f, null);
    	comboImage.drawBitmap(bmp1, 256f, 0f, null);
    	comboImage.drawBitmap(bmp2, 0f, 256f, null);
    	comboImage.drawBitmap(bmp3, 256f, 256f, null);
    	
    	ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
    	mBitmap.compress(CompressFormat.PNG, 0, bos); 
        byte[] bitmapdata = bos.toByteArray();
        
        return bitmapdata;
    }
    
    

    private byte[] readTileImage(int x, int y, int zoom) {
    	
    	ExecutorService executor = Executors.newFixedThreadPool(5);
    	TileLoader worker = new TileLoader(mContext, mUrl, mSize, x, y , zoom);
        executor.execute(worker);
        executor.shutdown();
      
        while (!executor.isTerminated()) {}
        
        byte[] image0 = worker.raw_tile;
        Bitmap bmp0 = BitmapFactory.decodeByteArray(image0,0,image0.length);
        
        Bitmap mBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        mBitmap.eraseColor(0x00000000);
        Canvas comboImage = new Canvas(mBitmap);
        
        comboImage.drawBitmap(bmp0, 0f, 0f, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
    	mBitmap.compress(CompressFormat.PNG, 0, bos); 
        byte[] bitmapdata = bos.toByteArray();
        
        return bitmapdata;
    }


    
   
    
    
    
    
    
}