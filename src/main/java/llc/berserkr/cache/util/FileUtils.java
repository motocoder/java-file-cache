package llc.berserkr.cache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileUtils {

    public static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static boolean delete(File dir) {

        if (dir != null) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    boolean success = delete(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            dir.setWritable(true);

            final boolean returnVal = dir.delete();

            if(!returnVal) {
                dir.deleteOnExit();
            }
            return returnVal;
        }else {
            return false;
        }
    }

    public static void delete(String filename) {

        // Create a File object to represent the filename
        final File f = new File(filename);
        f.setWritable(true);
        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) fail("Delete: no such file or directory: " + filename);
        if (!f.canWrite()) fail("Delete: write protected: " + filename);
        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
            String[] children = f.list();

            for (int i = 0; i < children.length; i++) {
                boolean success = delete(new File(f, children[i]));
                if (!success) {
                    fail("Delete: child not deleted: " + filename);
                }
            }
        }
        // If we passed all the tests, then attempt to delete it
        boolean success = f.delete();
        // And throw an exception if it didn't work for some (unknown) reason.
        // For example, because of a bug with Java 1.1.1 on Linux,
        // directory deletion always fails
        if (!success) {

            f.deleteOnExit();
            fail("Delete: deletion failed");

        }

    }


    /** A convenience method to throw an exception */
    protected static void fail(String msg) throws IllegalArgumentException {
        logger.error("ERROR DELETING " + msg);
//        throw new IllegalArgumentException(msg);
    }



    /**
     * By using reflection we can access the system properties which otherwise could only be be had by running this command in command shell:
     * "adb shell getprop"
     *
     * @param key the name of the property
     * @return value of the property or null
     */
    public static String getSystemProperty(String key) {
        String value = null;

        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
            if (StringUtilities.isEmpty(value)) {
                value = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }
}
