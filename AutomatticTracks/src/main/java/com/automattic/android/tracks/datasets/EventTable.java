package com.automattic.android.tracks.datasets;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.automattic.android.tracks.Event;
import com.automattic.android.tracks.Exceptions.EventNameException;
import com.automattic.android.tracks.TracksClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;

public class EventTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_events ("
                + " event_id        INTEGER PRIMARY KEY ASC," //  also alias for the built-in rowid:  "rowid", "oid", or "_rowid_"
                + " event_name      TEXT NOT NULL,"
                + " user            TEXT NOT NULL,"
                + " user_agent      TEXT,"
                + " timestamp       INTEGER,"
                + " retry_count     INTEGER DEFAULT 0,"
                + " user_type       INTEGER DEFAULT 0,"
                + " user_props      TEXT,"
                + " device_info     TEXT,"
                + " custom_props    TEXT"+
                ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_events");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    public static boolean hasEvents(Context ctx) {
        return getEventsCount(ctx) > 0;
    }

    public static int getEventsCount(Context ctx) {
        if (ctx == null) {
            Log.e(TracksDatabaseHelper.LOGTAG, "Cannot access the event table since the passed context is null. Context is required " +
                    "to access the DB.");
            return 0;
        }

        SQLiteDatabase db = TracksDatabaseHelper.getReadableDb(ctx);
        return (int) SqlUtils.getRowCount(db, "tbl_events");
    }

    public static void insertEvent(Context ctx, Event event) {
        if (event == null) {
            Log.e(TracksDatabaseHelper.LOGTAG, "Cannot insert a null event");
            return;
        }

        if (ctx == null) {
            Log.e(TracksDatabaseHelper.LOGTAG, "Cannot insert a null event since the passed context is null. Context is required " +
                    "to access the DB.");
            return;
        }

        SQLiteDatabase db = TracksDatabaseHelper.getWritableDb(ctx);
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO tbl_events (event_name, user, user_agent, user_type, " +
                "user_props, device_info, custom_props, timestamp, retry_count) VALUES (?1,?2,?3,?4,?5,?6,?7,?8, ?9)");
        try {
            stmt.bindString(1, event.getEventName());
            stmt.bindString(2, event.getUser());
            stmt.bindString(3, event.getUserAgent());
            stmt.bindLong(4, event.getUserType().ordinal());

            if (event.getUserProperties() != null) {
                stmt.bindString(5, event.getUserProperties().toString());
            } else {
                stmt.bindNull(5);
            }

            if (event.getDeviceInfo() != null) {
                stmt.bindString(6, event.getDeviceInfo().toString());
            } else {
                stmt.bindNull(6);
            }
            if (event.getCustomEventProperties() != null) {
                stmt.bindString(7, event.getCustomEventProperties().toString());
            } else {
                stmt.bindNull(7);
            }

            stmt.bindLong(8, event.getTimeStamp());
            stmt.bindLong(9, event.getRetryCount());

            stmt.execute();

            db.setTransactionSuccessful();
        } catch (IllegalArgumentException e) {
            Log.e(TracksDatabaseHelper.LOGTAG, "Cannot insert the current event. Please check the details of the event!", e);
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }


    public static LinkedList<Event> getAndDeleteEvents(Context ctx, int maxEvents) {
        if (ctx == null) {
            Log.e(TracksDatabaseHelper.LOGTAG, "Cannot load events!  The passed context is null. Context is required " +
                    "to access the DB.");
            return null;
        }

        String sql = "SELECT * FROM tbl_events ORDER BY tbl_events.event_id ASC";

        if (maxEvents > 0) {
            sql += " LIMIT " + Integer.toString(maxEvents);
        }

        SQLiteDatabase db = TracksDatabaseHelper.getWritableDb(ctx);
        db.beginTransaction();
        Cursor cursor = db.rawQuery(sql, new String[]{});

        LinkedList<Event> events = new LinkedList<>();
        LinkedList<Long> idToDelete = new LinkedList<>();

        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // mark the row for deletion asap
                    idToDelete.add(cursor.getLong(cursor.getColumnIndexOrThrow("event_id")));

                    final String eventName = cursor.getString(cursor.getColumnIndexOrThrow("event_name"));
                    final String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                    final String userAgent;
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow("user_agent"))){
                        userAgent = cursor.getString(cursor.getColumnIndexOrThrow("user_agent"));
                    } else {
                        userAgent = null;
                    }

                    int uTypeCardinal = (int) cursor.getLong(cursor.getColumnIndexOrThrow("user_type"));
                    TracksClient.NosaraUserType userType = TracksClient.NosaraUserType.values()[uTypeCardinal];

                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));

                    final String userProps;
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow("user_props"))){
                        userProps = cursor.getString(cursor.getColumnIndexOrThrow("user_props"));
                    } else {
                        userProps = null;
                    }

                    final String deviceInfo;
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow("device_info"))){
                        deviceInfo = cursor.getString(cursor.getColumnIndexOrThrow("device_info"));
                    } else {
                        deviceInfo = null;
                    }

                    final String customProps;
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow("custom_props"))){
                        customProps = cursor.getString(cursor.getColumnIndexOrThrow("custom_props"));
                    } else {
                        customProps = null;
                    }

                    try {
                        Event currentEvent = new Event(eventName, user, userType, userAgent, timestamp);
                        if (deviceInfo != null) {
                            currentEvent.setDeviceInfo(new JSONObject(deviceInfo));
                        }
                        if (userProps!= null) {
                            currentEvent.setUserProperties(new JSONObject(userProps));
                        }
                        if (customProps != null) {
                            currentEvent.setCustomProperties(new JSONObject(customProps));
                        }
                        events.add(currentEvent);
                    } catch (EventNameException e) {
                        Log.e(TracksDatabaseHelper.LOGTAG, "Cannot load event from the DB!  Name of the event is not correct?!?", e);
                    } catch (JSONException e) {
                        Log.e(TracksDatabaseHelper.LOGTAG, "Cannot recreate the event obj from the DB data!", e);
                    } catch (Exception e) {
                        Log.e(TracksDatabaseHelper.LOGTAG, "Cannot recreate the event obj from the DB data!", e);
                    }
                } while (cursor.moveToNext());
            }

            // delete selected rows now
            if (maxEvents <= 0) {
                // All events in the table are returned. We can safely remove all of them.
                db.execSQL("DELETE FROM tbl_events");
            } else {
                // We need to delete returned events only.
                if (idToDelete.size() > 0) {
                    StringBuilder deleteInClause = new StringBuilder();
                    boolean first = true;
                    for (Iterator<Long> iterator = idToDelete.iterator(); iterator.hasNext(); ) {
                        Long currentID = iterator.next();
                        if (first) {
                            deleteInClause.append(currentID.toString());
                            first = false;
                        } else {
                            deleteInClause.append(" ,");
                            deleteInClause.append(currentID.toString());
                        }
                    }
                    int numDeleted = db.delete("tbl_events", "event_id IN (" + deleteInClause.toString() + ")", null);
                    Log.d(TracksDatabaseHelper.LOGTAG, "Deleted " + numDeleted + " rows from the Events table.");
                }
            }
            db.setTransactionSuccessful();
            return events;
        } finally {
            db.endTransaction();
            SqlUtils.closeCursor(cursor);
        }
    }


    public static void purgeAll(Context ctx) {
        if (ctx == null) {
            Log.e(TracksDatabaseHelper.LOGTAG, "Cannot purge the events table!  The passed context is null. Context is required " +
                    "to access the DB.");
            return;
        }

        SQLiteDatabase db = TracksDatabaseHelper.getWritableDb(ctx);
        db.beginTransaction();
        try {
            // first delete all recommended tags
            db.execSQL("DELETE FROM tbl_events");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

}
