package llc.berserkr.androidfilecache;

import android.app.Application;
import android.util.Log;

public class FileCacheApplication extends Application {

    private static final String TAG = "filecachetag";

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception on thread " + thread.getName(), throwable);
        });
    }
}
