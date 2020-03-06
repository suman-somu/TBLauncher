package rocks.tbog.tblauncher.dataprovider;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.loader.LoadEntryItem;

public abstract class Provider<T extends EntryItem> extends Service implements IProvider {
    private final static String TAG = "Provider";

    /**
     * Binder given to clients
     */
    private final IBinder binder = new LocalBinder();
    /**
     * Storage for search items used by this provider
     */
    List<T> pojos = new ArrayList<>();
    private boolean loaded = false;
    /**
     * Scheme used to build ids for the pojos created by this provider
     */
    private String pojoScheme = "(none)://";

    private long start;

    /**
     * (Re-)load the providers resources when the provider has been completely initialized
     * by the Android system
     */
    @Override
    public void onCreate() {
        super.onCreate();

        this.reload();
    }


    void initialize(LoadEntryItem<T> loader) {
        start = System.currentTimeMillis();

        Log.i(TAG, "Starting provider: " + this.getClass().getSimpleName());

        loader.setProvider(this);
        this.pojoScheme = loader.getPojoScheme();
        loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void reload() {
        // Handled at subclass level
        if (pojos.size() > 0) {
            Log.v(TAG, "Reloading provider: " + this.getClass().getSimpleName());
        }
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public void loadOver(ArrayList<T> results) {
        long time = System.currentTimeMillis() - start;

        Log.i(TAG, "Time to load " + this.getClass().getSimpleName() + ": " + time + "ms");

        // Store results
        this.pojos = results;
        this.loaded = true;

        // Broadcast this event
        Intent i = new Intent(TBLauncherActivity.LOAD_OVER);
        this.sendBroadcast(i);
    }

    /**
     * Tells whether or not this provider may be able to find the pojo with
     * specified id
     *
     * @param id id we're looking for
     * @return true if the provider can handle the query ; does not guarantee it
     * will!
     */
    public boolean mayFindById(String id) {
        return id.startsWith(pojoScheme);
    }

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    public EntryItem findById(String id) {
        for (EntryItem pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    @Override
    public List<? extends EntryItem> getPojos() {
        return Collections.unmodifiableList(pojos);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public IProvider getService() {
            // Return this instance of the provider so that clients can call public methods
            return Provider.this;
        }
    }
}
