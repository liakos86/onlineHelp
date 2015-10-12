
package com.kostas.model;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class DataProvider extends ContentProvider {
    private static final String TAG = Thread.currentThread().getStackTrace()[2].getClassName()
            .substring(0, 23);
    private Database database;

    private static final String sOrderAsc = "%s ASC";
    private static final String sWhere = "%s = ?";
    private static final String sWhereLike = "%s LIKE '%%%s%%'";
    private static final String sJoin = "%s JOIN %s ON %s=%s";

    @Override
    public boolean onCreate() {
        Context ctx = getContext();
        database = new Database(ctx);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        Log.v(TAG, String.format("GetType for uri [%s]", uri));
        final int match = ContentDescriptor.URI_MATCHER.match(uri);
        switch (match) {
        
        
            case ContentDescriptor.Running.PATH_TOKEN:
            case ContentDescriptor.Running.PATH_START_LETTERS_TOKEN:
                return ContentDescriptor.Running.CONTENT_TYPE_DIR;
           


            case ContentDescriptor.Running.PATH_FOR_ID_TOKEN:
                return ContentDescriptor.Running.CONTENT_ITEM_TYPE;


            case ContentDescriptor.Interval.PATH_TOKEN:
            case ContentDescriptor.Interval.PATH_START_LETTERS_TOKEN:
                return ContentDescriptor.Interval.CONTENT_TYPE_DIR;



            case ContentDescriptor.Interval.PATH_FOR_ID_TOKEN:
                return ContentDescriptor.Interval.CONTENT_ITEM_TYPE;


            case ContentDescriptor.Plan.PATH_TOKEN:
            case ContentDescriptor.Plan.PATH_START_LETTERS_TOKEN:
                return ContentDescriptor.Plan.CONTENT_TYPE_DIR;



            case ContentDescriptor.Plan.PATH_FOR_ID_TOKEN:
                return ContentDescriptor.Plan.CONTENT_ITEM_TYPE;
                

          
            default:
                throw new UnsupportedOperationException("URI " + uri + " is not supported.");
        }
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder) {
        Log.v(TAG, String.format("Query for uri [%s]", uri));
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            if (projection != null) {
                String proj = "projection: ";
                for (int i = 0; i < projection.length; i++)
                    proj += String.format(" [%s] ", projection[i]);
                Log.v(TAG, proj);
            } else{
                Log.v(TAG, "to projection einai null");
            }
        }
        SQLiteDatabase db = database.getReadableDatabase();
        Cursor toRet = null;
        final int match = ContentDescriptor.URI_MATCHER.match(uri);
        switch (match) {
            //START Running
            case ContentDescriptor.Running.PATH_TOKEN: {
                String searchFor = uri.getQueryParameter(ContentDescriptor.PARAM_SEARCH);
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(ContentDescriptor.Running.TABLE_NAME);

                if (!TextUtils.isEmpty(searchFor)) {
                    String where = String.format(sWhereLike, ContentDescriptor.Running.Cols.DESCRIPTION, searchFor);
                    where += " OR " + (String.format(sWhereLike, ContentDescriptor.Running.Cols.DESCRIPTION, searchFor));
                    Log.v(TAG, String.format("where [%s]", where));
                    builder.appendWhere(where);
                }

                toRet = builder.query(db, projection, selection, selectionArgs, null, null,
                        sortOrder);
            }
            break;
            case ContentDescriptor.Running.PATH_START_LETTERS_TOKEN: {
                SQLiteDatabase rdb = database.getReadableDatabase();
                toRet = rdb
                        .rawQuery(
                                "select distinct substr(description, 1, 1) from running order by 1 asc",
                                null);
            }
            break;
            case ContentDescriptor.Running.PATH_FOR_ID_TOKEN: {
                String id = uri.getLastPathSegment();
                Log.v(TAG, String.format("querying for [%s]", id));
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(ContentDescriptor.Running.TABLE_NAME);
                toRet = builder.query(db, projection,
                        String.format(sWhere, ContentDescriptor.Running.Cols.ID), new String[]{
                        id
                },
                        null, null, null, sortOrder);
            }
            break;
            // END Running


            //START Interval
            case ContentDescriptor.Interval.PATH_TOKEN: {
                String searchFor = uri.getQueryParameter(ContentDescriptor.PARAM_SEARCH);
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(ContentDescriptor.Interval.TABLE_NAME);

                if (!TextUtils.isEmpty(searchFor)) {
                    String where = String.format(sWhereLike, ContentDescriptor.Interval.Cols.MILLISECONDS, searchFor);
                    where += " OR " + (String.format(sWhereLike, ContentDescriptor.Interval.Cols.MILLISECONDS, searchFor));
                    Log.v(TAG, String.format("where [%s]", where));
                    builder.appendWhere(where);
                }

                toRet = builder.query(db, projection, selection, selectionArgs, null, null,
                        sortOrder);
            }
            break;
            case ContentDescriptor.Interval.PATH_START_LETTERS_TOKEN: {
                SQLiteDatabase rdb = database.getReadableDatabase();
                toRet = rdb
                        .rawQuery(
                                "select distinct substr(milliseconds, 1, 1) from interval order by 1 asc",
                                null);
            }
            break;
            case ContentDescriptor.Interval.PATH_FOR_ID_TOKEN: {
                String id = uri.getLastPathSegment();
                Log.v(TAG, String.format("querying for [%s]", id));
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(ContentDescriptor.Interval.TABLE_NAME);
                toRet = builder.query(db, projection,
                        String.format(sWhere, ContentDescriptor.Interval.Cols.ID), new String[]{
                                id
                        },
                        null, null, null, sortOrder);
            }
            break;
            // END Interval


            //START Plan
            case ContentDescriptor.Plan.PATH_TOKEN: {
                String searchFor = uri.getQueryParameter(ContentDescriptor.PARAM_SEARCH);
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(ContentDescriptor.Plan.TABLE_NAME);

                if (!TextUtils.isEmpty(searchFor)) {
                    String where = String.format(sWhereLike, ContentDescriptor.Plan.Cols.DESCRIPTION, searchFor);
                    where += " OR " + (String.format(sWhereLike, ContentDescriptor.Plan.Cols.SECONDS, searchFor));
                    Log.v(TAG, String.format("where [%s]", where));
                    builder.appendWhere(where);
                }

                toRet = builder.query(db, projection, selection, selectionArgs, null, null,
                        sortOrder);
            }
            break;
            case ContentDescriptor.Plan.PATH_START_LETTERS_TOKEN: {
                SQLiteDatabase rdb = database.getReadableDatabase();
                toRet = rdb
                        .rawQuery(
                                "select distinct substr(description, 1, 1) from plan order by 1 asc",
                                null);
            }
            break;
            case ContentDescriptor.Plan.PATH_FOR_ID_TOKEN: {
                String id = uri.getLastPathSegment();
                Log.v(TAG, String.format("querying for [%s]", id));
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(ContentDescriptor.Plan.TABLE_NAME);
                toRet = builder.query(db, projection,
                        String.format(sWhere, ContentDescriptor.Plan.Cols.ID), new String[]{
                                id
                        },
                        null, null, null, sortOrder);
            }
            break;
            // END Plan

            default:
                Log.d(TAG, String.format("Could not handle matcher [%d]", match));
        }
        if (toRet != null) {
            toRet.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return toRet;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v(TAG, String.format("Insert for uri [%s]", uri));
        SQLiteDatabase db = database.getWritableDatabase();
        int token = ContentDescriptor.URI_MATCHER.match(uri);
        long id = Database.INVALID_ID;
        switch (token) {
            //Running
            case ContentDescriptor.Running.PATH_TOKEN: {
                id = db.insert(ContentDescriptor.Running.TABLE_NAME, null,
                        adjustIdField(values, ContentDescriptor.Running.Cols.ID));
            }
            break;
            //End Running

            //Plan
            case ContentDescriptor.Plan.PATH_TOKEN: {
                id = db.insert(ContentDescriptor.Plan.TABLE_NAME, null,
                        adjustIdField(values, ContentDescriptor.Plan.Cols.ID));
            }
            break;
            //End Plan

            //Interval
            case ContentDescriptor.Interval.PATH_TOKEN: {
                id = db.insert(ContentDescriptor.Interval.TABLE_NAME, null,
                        adjustIdField(values, ContentDescriptor.Interval.Cols.ID));
            }
            break;
            //End Interval


            default: {
                throw new UnsupportedOperationException("URI: " + uri + " not supported.");
            }
        }
        Uri toRet = ContentUris.withAppendedId(uri, id);
        Log.v(TAG, String.format("new id [%d] notify via [%s]", id, toRet));
        getContext().getContentResolver().notifyChange(toRet, null);
        return toRet;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.v(TAG, String.format("update for uri [%s]", uri));
        int toRet = 0;
        SQLiteDatabase db = database.getWritableDatabase();
        int token = ContentDescriptor.URI_MATCHER.match(uri);
        switch (token) {
            //Running
            case ContentDescriptor.Running.PATH_TOKEN: {
                toRet = db.update(ContentDescriptor.Running.TABLE_NAME, values, selection,
                        selectionArgs);
            }
            break;
            //End Running

            //Interval
            case ContentDescriptor.Interval.PATH_TOKEN: {
                toRet = db.update(ContentDescriptor.Interval.TABLE_NAME, values, selection,
                        selectionArgs);
            }
            break;
            //End Interval

            //Plan
            case ContentDescriptor.Plan.PATH_TOKEN: {
                toRet = db.update(ContentDescriptor.Plan.TABLE_NAME, values, selection,
                        selectionArgs);
            }
            break;
            //End Plan
           

            default: {
                throw new UnsupportedOperationException("URI: " + uri + " not supported.");
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return toRet;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v(TAG, String.format("Delete for uri [%s]", uri));
        int toRet = 0;
        SQLiteDatabase db = database.getWritableDatabase();
        int token = ContentDescriptor.URI_MATCHER.match(uri);
        switch (token) {
            //running
            case ContentDescriptor.Running.PATH_TOKEN: {
                toRet = db.delete(ContentDescriptor.Running.TABLE_NAME, selection, selectionArgs);
            }
            break;
            //running

            //Plan
            case ContentDescriptor.Plan.PATH_TOKEN: {
                toRet = db.delete(ContentDescriptor.Plan.TABLE_NAME, selection, selectionArgs);
            }
            break;
            //Plan

            //Interval
            case ContentDescriptor.Interval.PATH_TOKEN: {
                toRet = db.delete(ContentDescriptor.Interval.TABLE_NAME, selection, selectionArgs);
            }
            break;
            //Interval


            default: {
                throw new UnsupportedOperationException("URI: " + uri + " not supported.");
            }
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return toRet;
    }

    /**
     * checks if we have an invalid mId in values. If so, it removes it and let
     * autoincrement do the job
     *
     * @param values
     * @param idcol
     * @return
     */
    static ContentValues adjustIdField(ContentValues values, String idcol) {
        synchronized (values) {
            if (values.getAsLong(idcol) == Database.INVALID_ID) {
                values.remove(idcol);
            }
            return values;
        }
    }
}
