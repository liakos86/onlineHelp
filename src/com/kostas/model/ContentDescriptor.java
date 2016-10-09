
package com.kostas.model;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;
import com.kostas.dbObjects.Interval;

/**
 * based on http://www.nofluffjuststuff.com/blog/vladimir_vivien/2011/11/
 * a_pattern_for_creating_custom_android_content_providers <br/>
 * workaround (wa1): We don't use {@link #applyBatch(java.util.ArrayList)}for
 * number matching (see
 * http://code.google.com/p/android/issues/detail?mId=27031). We use * and take
 * care of it in code.<br/>
 *
 * @author kliakopoulos
 */
public class ContentDescriptor {

    public static final String AUTHORITY = "com.kostas.contentprovider";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    public static final UriMatcher URI_MATCHER = buildUriMatcher();

    public static final String PARAM_FULL = "full";
    public static final String PARAM_SEARCH = "search";
    public static final String PARAM_COUNT = "count";
    

    // argument passed via query params, start with this string
    // in other words: query params starting with this string are arguments
    public static final String ARG_PREFIX = "arg_";
    // helper format strings
    private static final String sFormatArg = ARG_PREFIX + "%s";
    // helper format strings for table creation
    private static final String sFrmIdAutoinc = " %s INTEGER PRIMARY KEY AUTOINCREMENT ";
    private static final String sFrmId = " %s INTEGER PRIMARY KEY ";
    private static final String sFrmInt = " %s INTEGER ";
    private static final String sFrmText = " %s TEXT ";
    private static final String sFrmTextNotNull = " %s TEXT NOT NULL ";
    private static final String sFrmFloat = " %s FLOAT ";
    private static final String sFrmPrimaryKey = " UNIQUE (%s) ON CONFLICT REPLACE ";

    private ContentDescriptor() {
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AUTHORITY;

       
        Running.addToUriMatcher(authority, matcher);
        Interval.addToUriMatcher(authority, matcher);
        Plan.addToUriMatcher(authority, matcher);
        
     

        return matcher;
    }

    

    public static class Running {
        public static final String TABLE_NAME = "running";
        // content://xxxxx/running
        public static final String PATH = "running";
        public static final int PATH_TOKEN = 10;
        // content://xxxxx/running/20
        public static final String PATH_FOR_ID = "running/#";
        // see wa1 content://xxxxx/running/21
        public static final String PATH_FOR_ID_WA = "running/*";
        public static final int PATH_FOR_ID_TOKEN = 11;
        // content://xxxxx/simcounterdetailresponses/startletters
        public static final String PATH_START_LETTERS = "running/startletters";
        public static final int PATH_START_LETTERS_TOKEN = 12;

        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH).build();

        public static final String CONTENT_TYPE_DIR = "vnd.android.cursor.dir/vnd.com.kostas.onlineHelp.app";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.kostas.onlineHelp.app";

        public static class Cols {
            public static final String ID = BaseColumns._ID; // by convention
            public static final String DATE = "date";
            public static final String DISTANCE = "distance";
            public static final String TIME = "time";
            public static final String AVGPACETEXT = "avgpacetext";
            public static final String DESCRIPTION = "description";

        }

        protected static UriMatcher addToUriMatcher(String authority, UriMatcher matcher) {
            matcher.addURI(authority, Running.PATH, Running.PATH_TOKEN);
            matcher.addURI(authority, Running.PATH_FOR_ID, Running.PATH_FOR_ID_TOKEN);
            matcher.addURI(authority, Running.PATH_FOR_ID_WA, Running.PATH_FOR_ID_TOKEN);
            matcher.addURI(authority, Running.PATH_START_LETTERS, Running.PATH_START_LETTERS_TOKEN);
            return matcher;
        }

        public static String createTable() { 
            return "CREATE TABLE " + Running.TABLE_NAME + " ( "
                    + String.format(sFrmIdAutoinc, Cols.ID) + " , "
                    + String.format(sFrmTextNotNull, Cols.DATE) + " , "
                     + String.format(sFrmText, Cols.DESCRIPTION) + " , "
                      + String.format(sFrmTextNotNull, Cols.TIME) + " , "
                    + String.format(sFrmTextNotNull, Cols.AVGPACETEXT) + " , "
                    + String.format(sFrmTextNotNull, Cols.DISTANCE) + " , "

                    + String.format(sFrmPrimaryKey, Cols.ID) + ")";
        }
    }


    public static class Interval {
        public static final String TABLE_NAME = "interval";
        // content://xxxxx/running
        public static final String PATH = "interval";
        public static final int PATH_TOKEN = 20;
        // content://xxxxx/running/20
        public static final String PATH_FOR_ID = "interval/#";
        // see wa1 content://xxxxx/running/21
        public static final String PATH_FOR_ID_WA = "interval/*";
        public static final int PATH_FOR_ID_TOKEN = 21;
        // content://xxxxx/simcounterdetailresponses/startletters
        public static final String PATH_START_LETTERS = "interval/startletters";
        public static final int PATH_START_LETTERS_TOKEN = 22;

        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH).build();

        public static final String CONTENT_TYPE_DIR = "vnd.android.cursor.dir/vnd.com.kostas.onlineHelp.app";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.kostas.onlineHelp.app";

        public static class Cols {
            public static final String ID = BaseColumns._ID; // by convention
            public static final String RUNNING_ID = "running_id";
            public static final String MILLISECONDS = "milliseconds";
            public static final String LATLONLIST = "latlonlist";
            public static final String DISTANCE = "distance";
            public static final String PACETEXT = "pacetext";
            public static final String FASTEST = "fastest";
        }

        protected static UriMatcher addToUriMatcher(String authority, UriMatcher matcher) {
            matcher.addURI(authority, Interval.PATH, Interval.PATH_TOKEN);
            matcher.addURI(authority, Interval.PATH_FOR_ID, Interval.PATH_FOR_ID_TOKEN);
            matcher.addURI(authority, Interval.PATH_FOR_ID_WA, Interval.PATH_FOR_ID_TOKEN);
            matcher.addURI(authority, Interval.PATH_START_LETTERS, Interval.PATH_START_LETTERS_TOKEN);
            return matcher;
        }

        public static String createTable() {
            return "CREATE TABLE " + Interval.TABLE_NAME + " ( "
                    + String.format(sFrmIdAutoinc, Cols.ID) + " , "
                    + String.format(sFrmTextNotNull, Cols.RUNNING_ID) + " , "
                    + String.format(sFrmTextNotNull, Cols.MILLISECONDS) + " , "
                    + String.format(sFrmTextNotNull, Cols.LATLONLIST) + " , "
                    + String.format(sFrmTextNotNull, Cols.DISTANCE) + " , "
                    + String.format(sFrmTextNotNull, Cols.PACETEXT) + " , "
                    + String.format(sFrmInt, Cols.FASTEST) + " , "
                    + String.format(sFrmPrimaryKey, Cols.ID) + ")";
        }
    }


    public static class Plan {
        public static final String TABLE_NAME = "plan";
        // content://xxxxx/running
        public static final String PATH = "plan";
        public static final int PATH_TOKEN = 30;
        // content://xxxxx/running/20
        public static final String PATH_FOR_ID = "plan/#";
        // see wa1 content://xxxxx/running/21
        public static final String PATH_FOR_ID_WA = "plan/*";
        public static final int PATH_FOR_ID_TOKEN = 31;
        // content://xxxxx/simcounterdetailresponses/startletters
        public static final String PATH_START_LETTERS = "plan/startletters";
        public static final int PATH_START_LETTERS_TOKEN = 32;

        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH).build();

        public static final String CONTENT_TYPE_DIR = "vnd.android.cursor.dir/vnd.com.kostas.onlineHelp.app";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.kostas.onlineHelp.app";

        public static class Cols {
            public static final String ID = BaseColumns._ID; // by convention
            public static final String DESCRIPTION = "description";
            public static final String METERS = "meters";
            public static final String SECONDS = "seconds";
            public static final String ROUNDS = "rounds";
            public static final String START_REST = "startRest";
            public static final String IS_METRIC_MILES = "isMetricMiles";





        }

        protected static UriMatcher addToUriMatcher(String authority, UriMatcher matcher) {
            matcher.addURI(authority, Plan.PATH, Plan.PATH_TOKEN);
            matcher.addURI(authority, Plan.PATH_FOR_ID, Plan.PATH_FOR_ID_TOKEN);
            matcher.addURI(authority, Plan.PATH_FOR_ID_WA, Plan.PATH_FOR_ID_TOKEN);
            matcher.addURI(authority, Plan.PATH_START_LETTERS, Plan.PATH_START_LETTERS_TOKEN);
            return matcher;
        }

        public static String createTable() {
            return "CREATE TABLE " + Plan.TABLE_NAME + " ( "
                    + String.format(sFrmIdAutoinc, Cols.ID) + " , "
                    + String.format(sFrmTextNotNull, Cols.DESCRIPTION) + " , "
                    + String.format(sFrmTextNotNull, Cols.METERS) + " , "
                    + String.format(sFrmTextNotNull, Cols.SECONDS) + " , "
                    + String.format(sFrmTextNotNull, Cols.ROUNDS) + " , "
                    + String.format(sFrmTextNotNull, Cols.START_REST) + " , "
                    + String.format(sFrmInt, Cols.IS_METRIC_MILES) + " , "

                    + String.format(sFrmPrimaryKey, Cols.ID) + ")";
        }
    }

}
