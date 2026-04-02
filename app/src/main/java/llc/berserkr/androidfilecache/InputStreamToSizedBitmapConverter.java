package llc.berserkr.androidfilecache;

import static llc.berserkr.cache.hash.SegmentedFile.delete;
import static llc.berserkr.cache.util.DataUtils.copyAndCount;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import llc.berserkr.cache.converter.Converter;
import llc.berserkr.cache.exception.ResourceException;

public class InputStreamToSizedBitmapConverter implements Converter<Bitmap,InputStream> {
    
    private final int w;
    private final int h;
    private final File temp;

    /**
     * 
     * @param w
     * @param h
     */
    public InputStreamToSizedBitmapConverter(final File tempFolder, final int w, final int h) {
        
        this.temp = tempFolder;
        this.w = w;
        this.h = h;
    }
    

    @Override
    public InputStream convert(Bitmap old) throws ResourceException {
        
        if(old == null) {
            return null;
        }
        
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        old.compress(CompressFormat.PNG, 0, bos); 
        
        return new ByteArrayInputStream(bos.toByteArray());
        
    }

    @Override
    public Bitmap restore(InputStream is) throws ResourceException {
        
        if(is == null) {
            return null;
        }
        
        try {
            
            final int inWidth;
            final int inHeight;

            temp.mkdirs();

            final File tempFile = new File(temp, System.currentTimeMillis() + is.toString() + ".temp");

            {
                
                final FileOutputStream tempOut = new FileOutputStream(tempFile);

                copyAndCount(is, tempOut);
                
                tempOut.close();
                
            }
            
           

            {
                
                final InputStream in = new FileInputStream(tempFile);
                final BitmapFactory.Options options = new BitmapFactory.Options();
                
                try {
                    
                    // decode image size (decode metadata only, not the whole image)
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(in, null, options);
                    
                }
                finally {
                    in.close();
                }
                
                // save width and height
                inWidth = options.outWidth;
                inHeight = options.outHeight;
                
            }

            final Bitmap roughBitmap;
            
            {
                
                // decode full image pre-resized
                final InputStream in = new FileInputStream(tempFile);
                
                try {
                    
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    // calc rought re-size (this is no exact resize)
                    
                    options.inSampleSize = Math.max(inWidth/w, inHeight/h);
                    
                    // decode full image
                    roughBitmap = BitmapFactory.decodeStream(in, null, options);
                    
                }
                finally {
                    in.close();
                }

                delete(tempFile.getAbsolutePath());

            }
            
            if(roughBitmap == null) {
                
                return null;
                
            }
            
            float[] values = new float[9];
            
            {
                
                // calc exact destination size
                final Matrix m = new Matrix();
                final RectF inRect = new RectF(0, 0, roughBitmap.getWidth(), roughBitmap.getHeight());
                final RectF outRect = new RectF(0, 0, w, h);
                m.setRectToRect(inRect, outRect, Matrix.ScaleToFit.CENTER);
                m.getValues(values);

            }
            
            final int newW = (int) (roughBitmap.getWidth() * values[0]);
            final int newH = (int) (roughBitmap.getHeight() * values[4]);
            
            // resize bitmap
            return Bitmap.createScaledBitmap(
                    roughBitmap,
                    newW,
                    newH,
                    true
            );
            
        }
        catch (IOException e) {

            throw new ResourceException("<InputStreamToSizedBitmapConverter><2>, could not create bitmap");
            
        }
    }


}
