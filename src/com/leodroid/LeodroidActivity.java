package com.leodroid;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.Spinner;
import android.widget.Toast;

public class LeodroidActivity extends ListActivity {
	private final static String TAG = "LeodroidActivity";
	private DatabaseHelper dbhelper;
	private Spinner spin_buslines;
	// TODO Remove this spinner in version 1.1 private Spinner spin_directions;
	private Button togbut_directions;
	private AutoCompleteTextView autocompl_search;
	private int busLinesId[];
	private int busStopsId[];

	private int selDirection;
	private int selLineid;
	private int selStopArrPosition;
	private String selLineNum;

	private final byte BUSLINE_ARRIVAL = 0;
	private final byte BUSLINES_ARRIVAL = 1;

	private ProgressDialog progressDialog;
	private volatile OasthHTTPThread progressThread;
	private String arrivalString;
	private Resources myResources;

	private SimpleCursorAdapter adapterLineSearch;
	private SimpleCursorAdapter adapterBusLineNames;
	private Cursor cursorBusLineNames;

	private int listviewSelPos;
	private int spinPos;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		/* Create the Database (no Errors if it already exists) */
		dbhelper = DatabaseHelper.getInstance(this);

		try {
			dbhelper.createDataBase();
		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		}

		if (savedInstanceState != null) {
			// restore position state of listview
			listviewSelPos = savedInstanceState.getInt("listviewSelection", 0);
			// restore position of spinner
			spinPos = savedInstanceState.getInt("spinnerSelection", 0);

		}

		// register context menu
		registerForContextMenu(getListView());

		// get Resources pointer
		myResources = getResources();

		// AssetManager asd = getAssets();
		spin_buslines = (Spinner) findViewById(R.id.lines);
		togbut_directions = (Button) findViewById(R.id.toggleDirection);
		autocompl_search = (AutoCompleteTextView) findViewById(R.id.autoCompLineSearch);
		autocompl_search.setThreshold(1);

		selDirection = 1;

		spin_buslines
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View view,
							int pos, long id) {
						cursorBusLineNames.moveToPosition(pos);
						selLineid = cursorBusLineNames
								.getInt(cursorBusLineNames
										.getColumnIndex("_id"));

						loadStopsList();

						if (listviewSelPos != 0) {
							getListView().setSelection(listviewSelPos);
							listviewSelPos = 0;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});

		togbut_directions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (selDirection == 1) {
					selDirection = 2;

					togbut_directions.setCompoundDrawablesWithIntrinsicBounds(
							0, 0, R.drawable.arrow_left, 0);
					togbut_directions
							.setText(R.string.msg_but_direction_return);
				} else {
					selDirection = 1;
					togbut_directions.setCompoundDrawablesWithIntrinsicBounds(
							0, 0, R.drawable.arrow_right, 0);
					togbut_directions.setText(R.string.msg_but_direction_going);

				}
				loadStopsList();
			}
		});

	}

	@Override
	protected void onStart() {
		super.onStart();

		cursorBusLineNames = dbhelper.getBusLines();

		adapterBusLineNames = new SimpleCursorAdapter(getApplicationContext(),
				android.R.layout.simple_spinner_item, cursorBusLineNames,
				new String[] { "linestr" }, new int[] { android.R.id.text1 });
		adapterBusLineNames
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin_buslines.setAdapter(adapterBusLineNames);
		spin_buslines.setSelection(spinPos);

		initAutocompleteViews();

	}

	private void initAutocompleteViews() {

		// Create SimpleCursorAdapters.
		adapterLineSearch = new SimpleCursorAdapter(this,
				R.layout.autocompleterow, null, new String[] { "bname" },
				new int[] { R.id.autocomplete_row_1 });

		// Set an OnItemClickListener, to update dependent fields when
		// a choice is made in the AutoCompleteTextView.
		// if a license plate is selected, fill the other AutocompleteTextViews
		// with the vehicle values (brand name,model,colour,type)
		autocompl_search
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View view,
							int position, long id) {
						Cursor curs1 = (Cursor) arg0
								.getItemAtPosition(position);

						selLineid = curs1.getInt(curs1.getColumnIndex("_id"));

						spin_buslines
								.setSelection(getPositionInSpinnerAdapter(selLineid));

						loadStopsList();
						autocompl_search.setText(null);
						curs1.close();
					}
				});

		// Set the CursorToStringConverter, to provide the labels for the
		// choices to be displayed in the AutoCompleteTextView.
		adapterLineSearch
				.setCursorToStringConverter(new CursorToStringConverter() {
					public String convertToString(Cursor cursor) {

						return cursor.getString(cursor.getColumnIndex("num"))
								+ " "
								+ cursor.getString(cursor
										.getColumnIndex("bname"));
					}
				});

		// Set the FilterQueryProvider, to run queries for choices
		// that match the specified input.
		adapterLineSearch.setFilterQueryProvider(new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				// Search for states whose names begin with the specified
				// letters.
				return dbhelper
						.getBusLineCursor(constraint != null ? constraint
								.toString() : null);
			}
		});

		// assign adapters to autocompletetextviews
		autocompl_search.setAdapter(adapterLineSearch);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	protected void onStop() {
		super.onStop();
		// TODO something goes wrong with the spinner

		// close cursors
		if (cursorBusLineNames != null) {
			cursorBusLineNames.close();
		}

		// close adapters
		if ((adapterBusLineNames != null)
				&& (adapterBusLineNames.getCursor() != null)) {
			adapterBusLineNames.getCursor().close();

			adapterBusLineNames = null;
		}
		if ((adapterLineSearch != null)
				&& (adapterLineSearch.getCursor() != null)) {
			adapterLineSearch.getCursor().close();

			adapterLineSearch = null;
		}

		// close database helper
		if (dbhelper != null) {
			dbhelper.close();
		}

		// store position state of spinner to SharedPreferences
		// SettingsHelper.saveSpinnerVehicleCategoryPosition(
		// getApplicationContext(), vehicleCatSpinnerSelIndex);

		Log.d(TAG, "onPause()");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// store position state of listview
		outState.putInt("listviewSelection", getListView()
				.getFirstVisiblePosition());
		outState.putInt("spinnerSelection",
				spin_buslines.getSelectedItemPosition());
		Log.d(TAG, getListView().getFirstVisiblePosition() + " listview pos");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemBusPositions:

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						final String bob = OasthArrivalParser.parseS(
								OasthHttp.getMobileTextualPosition(selLineid,
										selDirection),
								OasthArrivalParser.patternTextualPos,
								OasthArrivalParser.replacementDesktopLinesArrival);

						handler.post(new Runnable() {

							@Override
							public void run() {
								new AlertDialog.Builder(LeodroidActivity.this)
										.setTitle(R.string.text_bus_position)
										.setMessage(bob)
										.setPositiveButton("OK", null).create()
										.show();

							}
						});
					} catch (IOException e) {
						Log.d(TAG, Log.getStackTraceString(e));
					}

				}
			}).run();
			break;
		case R.id.itemBusPositions2:

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						final String bob = OasthArrivalParser
								.parseMobilePosition(OasthHttp
										.getMobilePosition(selLineid,
												selDirection));

						handler.post(new Runnable() {

							@Override
							public void run() {
								new AlertDialog.Builder(LeodroidActivity.this)
										.setTitle(R.string.text_bus_position)
										.setMessage(bob)
										.setPositiveButton("OK", null).create()
										.show();

							}
						});
					} catch (IOException e) {
						Log.d(TAG, Log.getStackTraceString(e));
					}

				}
			}).run();
			break;
		case R.id.itemGoToStart:
			if (busStopsId != null) {
				getListView().setSelection(0);
			}
			break;
		case R.id.itemGoToEnd:
			if (busStopsId != null) {
				getListView().setSelection(busStopsId.length - 1);
			}
			break;

		case R.id.itemInfo:
			AlertDialog ald1 = new AlertDialog.Builder(this).create();

			ald1.setTitle(myResources.getString(R.string.m_it_about));

			ald1.setMessage(myResources.getString(R.string.msg_about_legal));
			ald1.setButton("OK", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					// do nothing
				}
			});
			ald1.setIcon(R.drawable.icon);
			ald1.show();
			break;
		}

		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_CANCELED) {
			// Toast.makeText(this, "activity canceled", Toast.LENGTH_SHORT)
			// .show();
		} else if (resultCode == Activity.RESULT_OK) {
			String input = data.getStringExtra("input");
			// Toast.makeText(this, "input = " + input,
			// Toast.LENGTH_LONG).show();
			selLineid = Integer.parseInt(input);
			loadStopsList();

			for (int i = 0; i < busLinesId.length; i++) {
				if (selLineid == busLinesId[i]) {
					spin_buslines.setSelection(i);
					break;
				}
			}
		}
	}

	public void loadStopsList() {
		String busStopStr[];

		/* Create the Database (no Errors if it already exists) */
		// dbhelper = new DataBaseHelper(this);

		try {
			dbhelper.createDataBase();

		} catch (IOException ioe) {
			throw new Error("Unable to create database");
		}

		SQLiteDatabase myDB = dbhelper.getMyWritableDatabase();

		try {
			/* Query for some results with Selection and Projection. */

			// Cursor c = myDB
			// .rawQuery(
			// "select v_stop.stopid as stopid , v_stop.sorder || '. ' || v_stop.stopname as stopstr, v_stop.routeid as routeid "
			// + " from v_stop  where v_stop.direction = "
			// + selDirection
			// + " and v_stop.lineid = "
			// + selLineid, null);
			Cursor c = myDB.rawQuery(
					myResources.getString(R.string.sql_select_stops),
					new String[] { selDirection + "", selLineid + "" });

			/* Get the indices of the Columns we will need */
			int idColumn = c.getColumnIndex("stopid");
			int strColumn = c.getColumnIndex("stopstr");
			// int routeIDColumn = c.getColumnIndex("routeid");

			busStopStr = new String[1];
			busStopsId = new int[1];

			/* Check if our result was valid. */
			if (c != null) {
				/* Check if at least one Result was returned. */
				busStopStr = new String[c.getCount()];
				busStopsId = new int[c.getCount()];

				if (c.moveToFirst()) {
					int i = 0;
					// selRouteid = c.getInt(routeIDColumn);

					do {
						busStopStr[i] = c.getString(strColumn);
						busStopsId[i++] = c.getInt(idColumn);

						// android.util.Log.d("Line :", "Position: " + (i -
						// 1)
						// + " " + qID + " " + qStr);
					} while (c.moveToNext());
				}
			}

			// close cursor
			c.close();

			setListAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, busStopStr));

		} catch (SQLiteException e) {
			android.util.Log.e("Database error", e.getLocalizedMessage());
		} finally {
			if (myDB != null)
				myDB.close();
			dbhelper.close();
		}
	}

	/*
	 * @see
	 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
	 */
	public void showStopPassingLines(int stopid) {
		StringBuilder msgstr = new StringBuilder();

		/* Create the Database (no Errors if it already exists) */
		// dbhelper = new DataBaseHelper(this);

		SQLiteDatabase myDB = dbhelper.getMyWritableDatabase();

		try {
			/* Query for some results with Selection and Projection. */

			Cursor c = myDB.rawQuery(
					myResources.getString(R.string.sql_select_linespassing),
					new String[] { "" + stopid });

			/* Get the indices of the Columns we will need */
			int numColumn = c.getColumnIndex("busnum");
			int nameColumn = c.getColumnIndex("busname");

			/* Check if our result was valid. */
			if (c != null) {

				/* Check if our result was valid. */
				if (c != null) {
					if (c.moveToFirst()) {
						do {
							String busnum = c.getString(numColumn);
							String busname = c.getString(nameColumn);

							// append the message that shows the number and
							// the name of the passing bus line
							msgstr.append(busnum).append(". ").append(busname)
									.append("\n");

						} while (c.moveToNext());
					}
				}
			}

			// close cursor
			c.close();

			AlertDialog alertDialog1 = new AlertDialog.Builder(this).create();
			alertDialog1.setTitle(myResources
					.getString(R.string.msg_passinglines));
			alertDialog1.setMessage(msgstr);
			alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

				}
			});
			alertDialog1.setIcon(R.drawable.icon);
			alertDialog1.show();
		} catch (SQLiteException e) {
			android.util.Log.e("Database error", e.getLocalizedMessage());
		} finally {
			if (myDB != null)
				myDB.close();
			dbhelper.close();
		}
		// http://www.anddev.org/code-snippets-for-android-f33/autocompletetextview-cursoradapter-t12430.html
	}

	/**
	 * Returns the position of vehicle category String in the spinner cursor
	 * adapter.
	 * 
	 * @param vehicle_category
	 *            Vehicle category, for example Ι.Χ.Ε
	 * @return position in adapter
	 */
	private int getPositionInSpinnerAdapter(int lineId) {
		int adaptPosition = -1;

		cursorBusLineNames.moveToFirst();
		while (!cursorBusLineNames.isAfterLast()) {
			if (cursorBusLineNames.getInt(cursorBusLineNames
					.getColumnIndex("_id")) == lineId) {

				adaptPosition = cursorBusLineNames.getPosition();
				break;
			}
			cursorBusLineNames.moveToNext();
		}
		return adaptPosition;
	}

	@Override
	public void onListItemClick(ListView parent, View v, int position, long id) {

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v1,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v1, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.stopcontext, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.arrivaltime:
			// printBusArrival2(info.position);
			selStopArrPosition = info.position;
			fetchArrivals(BUSLINE_ARRIVAL);
			return true;
		case R.id.linesArrival:
			// printLinesArrivalFast(info.position);
			selStopArrPosition = info.position;

			fetchArrivals(BUSLINES_ARRIVAL);
			return true;
		case R.id.linespassing:
			showStopPassingLines(busStopsId[info.position]);
			selStopArrPosition = info.position;
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public boolean isOnline() {
		// http://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-timeouts

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}

		return false;
	}

	// int photoid = getResources().getIdentifier("exerbase", "drawable",
	// "com.opendrive");
	// ImageView bob = (ImageView) findViewById(R.id.imageView1);
	// bob.setImageResource(photoid);
	// bob.setAdjustViewBounds(true); // set the ImageView bounds to match the
	// // Drawable's dimensions
	// // AssetManager asd = getAssets();
	// testTest();
	// }
	//

	//
	// int[] apotelesmata = null;
	//
	// Set<Integer> questionids = new HashSet<Integer>();
	// Random rnd = new Random();
	// while (questionids.size() < 30) {
	// questionids.add(rnd.nextInt(1000));
	// }
	// Integer[] qestid = new Integer[questionids.size()];
	// questionids.toArray(qestid);
	//
	// questionids = null;
	// rnd = null;
	//
	// for (int i = 0; i < qestid.length; i++) {
	// android.util.Log.d("Question id:\t", "" + (qestid[i].intValue()));
	// }
	// android.util.Log.d("Question all:\t",
	// Arrays.toString(qestid).replace("[", "(").replace("]", ")"));
	//
	// AssetManager am = getAssets();
	// String assets[] = null;
	// try {
	// assets = am.list( "" );
	// for( int i = 0 ; i < assets.length ; ++i ) {
	// android.util.Log.d("Asset list "+ i, assets[i]);
	// }
	// } catch( IOException ex ) {
	// android.util.Log.e( "Assets log tag",
	// "I/O Exception",
	// ex );
	// }

	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case BUSLINE_ARRIVAL:
			progressDialog = new ProgressDialog(LeodroidActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Loading...");
			progressDialog.setCancelable(true);
			return progressDialog;
		case BUSLINES_ARRIVAL:
			progressDialog = new ProgressDialog(LeodroidActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage("Loading...");
			progressDialog.setCancelable(true);
			return progressDialog;

		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case BUSLINE_ARRIVAL:
			progressDialog.setProgress(0);
			progressThread = new OasthHTTPThread(handler, selStopArrPosition,
					BUSLINE_ARRIVAL);
			progressThread.start();
			break;
		case BUSLINES_ARRIVAL:
			progressDialog.setProgress(0);
			progressThread = new OasthHTTPThread(handler, selStopArrPosition,
					BUSLINES_ARRIVAL);
			progressThread.start();
			break;
		}
	}

	public void fetchArrivals(int what) {
		if (!isOnline()) {
			Toast toast = Toast.makeText(this,
					myResources.getString(R.string.msg_no_connection_found),
					Toast.LENGTH_LONG);
			toast.show();
			return;
		}
		showDialog(what);

	}

	public void showResultProgress(int what) {
		AlertDialog alertDialog1 = new AlertDialog.Builder(this).create();
		if (what == BUSLINE_ARRIVAL) {
			alertDialog1.setTitle(String.format(
					myResources.getString(R.string.msg_linearrival_header),
					selLineNum));
		} else {
			alertDialog1.setTitle(myResources
					.getString(R.string.msg_linesarrival_header));
		}

		alertDialog1.setMessage(arrivalString);
		alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				// do nothing
			}
		});
		alertDialog1.setIcon(R.drawable.icon);
		alertDialog1.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && progressThread != null) {
			Thread thrStop = progressThread;

			progressThread = null;
			thrStop.interrupt();
			dismissDialog(BUSLINE_ARRIVAL);

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// Define the Handler that receives messages from the thread and update the
	// progress
	final private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int total = msg.what;
			progressDialog.setProgress(total);
			// progressDialog.setMessage(getResources().getStringArray(
			// R.array.msg_connection_progress)[total]);
			if (total >= 100) {
				int fwhat = progressThread.getFetchWhat();
				dismissDialog(fwhat);

				progressThread = null;
				showResultProgress(fwhat);

			}
		}
	};

	private class OasthHTTPThread extends Thread {

		private int position;
		private int fetchWhat;
		private Handler prHandl;

		public OasthHTTPThread(Handler hand, int pos, int fetchWhat) {
			this.position = pos;
			this.fetchWhat = fetchWhat;
			this.prHandl = hand;
		}

		public void run() {
			if (this.fetchWhat == BUSLINE_ARRIVAL)
				getLineArrival();
			else
				getLinesArrival();

		}

		public void getLineArrival() {
			prHandl.sendEmptyMessage(10);
			try {

				arrivalString = OasthHttp.getLineArrival(selLineid,
						selDirection, busStopsId[position]);
				prHandl.sendEmptyMessage(50);

				arrivalString = OasthArrivalParser.parseS(arrivalString,
						OasthArrivalParser.patternDesktopLineArrival,
						OasthArrivalParser.replacementLineArrival);

				prHandl.sendEmptyMessage(100);

			} catch (IOException e) {
				android.util.Log.e("getLineArrival Exception", e.getMessage());

			}

		}

		public void getLinesArrival() {
			long startTime0 = System.currentTimeMillis();
			prHandl.sendEmptyMessage(10);
			try {

				arrivalString = OasthHttp.getLinesArrival(selLineid,
						selDirection, busStopsId[position]);
				prHandl.sendEmptyMessage(50);

				arrivalString = OasthArrivalParser.parseS(arrivalString,
						OasthArrivalParser.patternDesktopLinesArrival,
						OasthArrivalParser.replacementDesktopLinesArrival);

				long endTime0 = System.currentTimeMillis();
				Log.d(TAG, "Lines spent to donwload:" + (endTime0 - startTime0));

				prHandl.sendEmptyMessage(100);
			} catch (IOException e) {
				android.util.Log.e("getLinesArrival Exception", e.getMessage());
			}

		}

		public int getFetchWhat() {
			return this.fetchWhat;
		}

	}
}
