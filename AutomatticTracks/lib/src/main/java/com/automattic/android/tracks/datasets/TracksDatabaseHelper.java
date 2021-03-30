package com.automattic.android.tracks.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * database for all tracks information
 */
public class TracksDatabaseHelper extends SQLiteOpenHelper {
    public static final String LOGTAG = "NosaraDB";
    protected static final String DB_NAME = "tracks.db";
    private static final int DB_VERSION = 1;

    /*
	 *  database singleton
	 */
    private static TracksDatabaseHelper mDatabaseHelper;
    private final static Object mDbLock = new Object();
    private final Context mContext;

    public static TracksDatabaseHelper getDatabase(Context ctx) {
        if (mDatabaseHelper == null) {
            synchronized(mDbLock) {
                if (mDatabaseHelper == null) {
                    mDatabaseHelper = new TracksDatabaseHelper(ctx);
                    // this ensures that onOpen() is called with a writable database (open will fail if app calls getReadableDb() first)
                    mDatabaseHelper.getWritableDatabase();
                }
            }
        }
        return mDatabaseHelper;
    }

    private TracksDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }


    public static SQLiteDatabase getReadableDb(Context ctx) {
        return getDatabase(ctx).getReadableDatabase();
    }
    public static SQLiteDatabase getWritableDb(Context ctx) {
        return getDatabase(ctx).getWritableDatabase();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Used during development to copy database to external storage and read its content.
        // copyDatabase(db);
    }

    /*
     * drop & recreate all tables (essentially clears the db of all data)
     */
    public void reset(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            dropAllTables(db);
            createAllTables(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just reset the db when upgrading, future versions may want to avoid this
        // and modify table structures, etc., on upgrade while preserving data
        Log.i(LOGTAG, "Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // IMPORTANT: do NOT call super() here - doing so throws a SQLiteException
        Log.w(LOGTAG, "Downgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    private void createAllTables(SQLiteDatabase db) {
       EventTable.createTables(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        EventTable.dropTables(db);
    }

    /*
     * used during development to copy database to external storage so we can access it via DDMS
    */
    @SuppressWarnings("unused")
    private void copyDatabase(SQLiteDatabase db) {
        String copyFrom = db.getPath();
        String copyTo = mContext.getExternalFilesDir(null).getAbsolutePath() + "/" + DB_NAME;

        try {
            InputStream input = new FileInputStream(copyFrom);
            OutputStream output = new FileOutputStream(copyTo);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to copy tracks database", e);
        }
    }
}
