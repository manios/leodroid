package com.leodroid;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseHelper";
	private static final int DATABASE_VERSION = 1;

	// database file name.
	private static final String DB_NAME = "oasth.db";

	private final Context myContext;

	private static SQLiteDatabase myWritableDb;

	private static DatabaseHelper mInstance;

	/**
	 * Constructor takes and keeps a reference of the passed context in order to
	 * access to the application assets and resources.
	 * 
	 * @param context
	 *            the application context
	 */
	private DatabaseHelper(Context context) {

		super(context, DB_NAME, null, 1);
		this.myContext = context;
	}

	/**
	 * Get default instance of the class to keep it a singleton
	 * 
	 * @param context
	 *            the Application Context
	 */
	public static DatabaseHelper getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new DatabaseHelper(context.getApplicationContext());
		}
		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	/**
	 * Returns a writable database instance in order not to open and close many
	 * SQLiteDatabase objects simultaneously
	 * 
	 * @return a writable instance to SQLiteDatabase
	 */
	public SQLiteDatabase getMyWritableDatabase() {
		if ((myWritableDb == null) || (!myWritableDb.isOpen())) {
			myWritableDb = this.getWritableDatabase();
		}

		return myWritableDb;
	}

	@Override
	public void close() {
		super.close();
		if (myWritableDb != null) {
			myWritableDb.close();
			myWritableDb = null;
		}
	}

	/**
	 * Creates an empty database on the system and rewrites it with your own
	 * database.
	 * */
	public void createDataBase() throws IOException {

		boolean dbExist = databaseExists();

		if (dbExist) {
			// do nothing , database already exists
		} else {

			/*
			 * By calling this method and empty database will be created into
			 * the default system path of your application so we are gonna be
			 * able to overwrite that database with our database.
			 */
			this.getReadableDatabase();

			// release all opened database objects
			// if you omit this line you will get a "no such table exception"
			// and
			// the application will crash ONLY in the first run.
			this.close();

			try {

				copyDataBase();
			} catch (IOException e) {

				throw new Error("Error copying database");

			}
		}

	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean databaseExists() {

		SQLiteDatabase checkDB = null;

		try {
			String myPath = getDatabasePath();
			checkDB = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);

		} catch (SQLiteException e) {
			// database does not exist yet.
		}

		if (checkDB != null) {
			checkDB.close();
		}

		return checkDB != null ? true : false;
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transferring bytestream.
	 * */
	private void copyDataBase() throws IOException {
		Log.d(TAG, "copyDatabase()");

		// Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(DB_NAME);

		// Path to the just created empty db
		String outFileName = getDatabasePath();

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		SQLiteDatabase checkDB = null; // get a reference to the db.

		try {

			checkDB = SQLiteDatabase.openDatabase(getDatabasePath(), null,
					SQLiteDatabase.OPEN_READWRITE);

			// once the db has been copied, set the new version..
			checkDB.setVersion(DATABASE_VERSION);
			checkDB.close();
		} catch (SQLiteException e) {
			// database does?t exist yet.
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	/**
	 * Get absolute path to database file. The Android's default system path of
	 * your application database is /data/data/&ltpackage
	 * name&gt/databases/&ltdatabase name&gt
	 * 
	 * @return path to database file
	 */
	private String getDatabasePath() {
		// The Android's default system path of your application database.
		// /data/data/<package name>/databases/<databasename>
		return myContext.getFilesDir().getParentFile().getAbsolutePath()
				+ "/databases/" + DB_NAME;
	}

	/**
	 * This method searches the database for the busline that has the args
	 * string inside the number of in the busline name. It is called by the
	 * autocomplete textview
	 * 
	 * @param args
	 *            The search key
	 **/
	public Cursor getBusLineCursor(String args) {
		if (args == null) {
			return null;
		}
		String argion = args.toUpperCase();
		String locale_str = myContext.getResources().getString(
				R.string.locale_string);
		StringBuilder sqlquery1 = new StringBuilder();
		sqlquery1
				.append("SELECT distinct busline.id as _id , busline.num as num , busline.name_")
				.append(locale_str)
				.append(" as bname FROM busline WHERE busline.num LIKE '%")
				.append(argion).append("%' OR busline.name_")
				.append(locale_str).append(" LIKE '%").append(argion)
				.append("%' ORDER BY busline.num");

		Cursor result = null;
		SQLiteDatabase mDB = this.getReadableDatabase();

		if (mDB != null) {
			result = mDB.rawQuery(sqlquery1.toString(), null);
		}
		return result;
	}

	public Cursor getBusLines() {
		Cursor result = null;
		SQLiteDatabase mDB = this.getMyWritableDatabase();

		if (mDB != null) {
			result = mDB.rawQuery(
					myContext.getString(R.string.sql_select_buslines), null);
		}

		return result;
	}
}