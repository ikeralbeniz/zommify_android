package net.ikeralbeniz.zoomifytest;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import net.ikeralbeniz.zoomifytest.Zommify.Size;

import android.content.Context;
import android.os.Environment;
import android.util.Log;


public class TileLoader implements Runnable {
    
	private static final int BUFFER_SIZE = 16 * 1024;
	private static final String TAG = "ZoomifyTileLoader";
	
	private Context context;
    private String url;
    private Size size;
    private int x;
    private int y;
    private int zoom;
    private String file_path;
    private String cache_subfolder;
    
    public byte[] raw_tile;
    
    private boolean useExternalextorage;
     
    public TileLoader(Context context, String url, Size size, int x, int y, int zoom){
    	this.context = context;
    	this.url = url;
    	this.size = size;
    	
    	this.x=x;
        this.y=y;
        this.zoom=zoom;
        this.file_path = getTileFileUrl(x, y, zoom);
        cache_subfolder = getCacheSubfolder(url);
        useExternalextorage = isExternalStorageWritable() && isExternalStorageReadable();
    }
    
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
 
    @Override
    public void run() {
    	
    	raw_tile = readTileImage(x, y, zoom);
        
    }
    
    private byte[] readTileImage(int x, int y, int zoom) {
        InputStream in = null;
        ByteArrayOutputStream buffer = null;

        try {
        	String local_file_name =  getTileFilename(x, y, zoom);
        	
        	if(useExternalextorage){
	        	if(testCachedTile(local_file_name)){
	        		try{
	        			in = (InputStream) new FileInputStream(getCachedTile(local_file_name));
	        		}catch(Exception e){
	        			in = (InputStream) new URL(getTileFileUrl(x, y, zoom)).getContent();
	        		}
	        	}else{
	        		in = (InputStream) new URL(getTileFileUrl(x, y, zoom)).getContent();
	        	}
        	}else{
        		in = (InputStream) new URL(getTileFileUrl(x, y, zoom)).getContent();
        	}
            
        	buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[BUFFER_SIZE];

            while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            
            byte[] tileBuffer = buffer.toByteArray();
            
            try{
            	if(useExternalextorage){
            		if(!testCachedTile(local_file_name)){
            			FileOutputStream fos= new FileOutputStream(getCachedTile(local_file_name));
            			fos.write(tileBuffer);
            			fos.close();
            		}
            	}else{
            		Log.v(TAG, "Imposible to write on cache.. (no use of cache)"+ Integer.toString(zoom) +"-" + Integer.toString(x) +"-"+ Integer.toString(y));
            	}
            }catch(Exception e){
            	Log.v(TAG, "Imposible to write on cache.."+ Integer.toString(zoom) +"-" + Integer.toString(x) +"-"+ Integer.toString(y));
            }
            	
            return tileBuffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (buffer != null) try { buffer.close(); } catch (Exception ignored) {}
        }
    }
 
 
    @Override
    public String toString(){
        return this.file_path;
    }
    
    private String getTileFilename(int x, int y, int zoom) {
        return "mapcache/"+ cache_subfolder + "/" + zoom + '-' + x + '-' + y + ".png";
    }
    
    private String getTileFileUrl(int x, int y, int zoom) {
    	
    	int group = getTileIndex(x, y,zoom);
        return this.url+"TileGroup"+group+"/"+zoom+"-"+x+"-"+y+".jpg";
    }
    
    private int getTileIndex(int x, int y, int zoom){
    	
    	int prev_tiles = getNumofTilesPerzoom(zoom-1);
    	int max_y_tiles = (int)(Math.ceil((size.height /  Math.pow(2, (size.deep-zoom)))/size.tilesize ));
    	int max_x_tiles = (int)(Math.ceil((size.width /  Math.pow(2, (size.deep-zoom)))/size.tilesize ));
    	for(int i=0; i < max_y_tiles;i++){
    		
    		for(int j=0; j < max_x_tiles;j++){
    			prev_tiles = prev_tiles +1;
    			if(i == y && x == j){
    				return (int)Math.floor((prev_tiles -1)/size.tilesize);
    			}
        	}
    		
    	}
    	return (int)Math.floor((prev_tiles -1)/size.tilesize);
    }
    
    private int getNumofTilesPerzoom(int zoom){
    	int numoftiles = 0;
    	for(int i=0; i <= zoom; i++){
    		int x_tiles = (int)Math.ceil((size.width / Math.pow(2, (size.deep-i)))/size.tilesize );
    		int y_tiles = (int)Math.ceil((size.height / Math.pow(2, (size.deep-i)))/size.tilesize );
    		numoftiles = numoftiles + (x_tiles*y_tiles);
    	}
    	return numoftiles;
    }
    
    
    public File getCachedTile(String albumName) throws IOException {
        // Get the directory for the user's public pictures directory.
        File file = new File(context.getExternalCacheDir(), albumName);
        
        if(!file.getParentFile().exists()){
        	if (!file.getParentFile().mkdirs()) {
        		return null;
        	}
        }
        
        if(!file.exists()){
        	if (!file.createNewFile()) {
        		return null;
        	}
        }
        return file;
    }
    
    public boolean testCachedTile(String albumName) {
    	
        File file = new File(context.getExternalCacheDir(), albumName);
        return file.exists();
    }
    
    public String getCacheSubfolder(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "default";
    }
}
