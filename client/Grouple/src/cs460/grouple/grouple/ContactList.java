package cs460.grouple.grouple;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import cs460.grouple.grouple.MessagesActivity.getMessagesTask;
import cs460.grouple.grouple.ProfileActivity.CONTENT_TYPE;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

/*
 * MessagesActivity has not been implemented yet.
 */
public class ContactList extends ActionBarActivity
{
	private BroadcastReceiver broadcastReceiver;
	private Global GLOBAL;
	private int IMAGE_INDEX = 0;//holy shit
	private User user; //will be null for now
	
	
	private ArrayList<String> emails = new ArrayList<String>();
	private ArrayList<String> dates = new ArrayList<String>();
	private ArrayList<String> names = new ArrayList<String>();
	private ArrayList<String> messages = new ArrayList<String>();
	private ArrayList<ImageView> images = new ArrayList<ImageView>();
	
	
	
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    //Tag to search for when logging info.
    static final String TAG = "GCM";
    //Sender ID is the project number from API console. Needs to be secret.
    String SENDER_ID = "957639483805";

    
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;



	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		GLOBAL = ((Global) getApplicationContext());
		super.onCreate(savedInstanceState);
		user = GLOBAL.getCurrentUser();
		setContentView(R.layout.activity_contact_list);
		/* Action bar */
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		ab.setCustomView(R.layout.actionbar);
		ab.setDisplayHomeAsUpEnabled(false);
		populateRecentContacts();
		TextView actionbarTitle = (TextView) findViewById(R.id.actionbarTitleTextView);
		actionbarTitle.setText("Messages");
		initKillswitchListener();
		context = getApplicationContext();
		fetchRecentContacts();
		//new getContactsTask().execute("http://68.59.162.183/android_connect/get_chat_id.php");
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.

	}
	
	@Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	finish(); //preventing back-loop
	    }
	    return true;
	   }

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation_actions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_logout)
		{
			
			Intent login = new Intent(this, LoginActivity.class);
			GLOBAL.destroySession();
			startActivity(login);
			Intent intent = new Intent("CLOSE_ALL");
			this.sendBroadcast(intent);
			return true;
		}
		if (id == R.id.action_home)
		{
			Intent intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	public void startMessages(View view)
	{
		Intent intent = new Intent(this, MessagesActivity.class);
		intent.putExtra("email", emails.get(view.getId()));
		startActivity(intent);
	}
	
	private void populateRecentContacts()
	{
		//populate all of the chats between peers, groups and events that are most active
		//layout to inflate into
		LinearLayout messageLayout = (LinearLayout) findViewById(R.id.contactLayout);
		//layout inflater
		LayoutInflater li = getLayoutInflater();
		TextView messageBody, messageDate, contactName;
		View row = null;

		//messages consist of some things (messagebody, date, sender, receiver)
		for (String message : messages)
		{
			//loop through messages (newest first), maybe a map String String with messagebody, date
			row =  li.inflate(R.layout.contact_row, null); //inflate this message row
			row.setId(messages.indexOf(message));
			messageBody = (TextView) row.findViewById(R.id.messageBody);
			messageDate = (TextView) row.findViewById(R.id.messageDate);
			contactName = (TextView) row.findViewById(R.id.contactName);
			messageDate.setText(parseDate(dates.get(messages.indexOf(message))));
			messageBody.setText(message);
			images.add((ImageView)row.findViewById(R.id.contactImage));
			new getImageTask().execute("http://68.59.162.183/android_connect/get_profile_image.php", emails.get(messages.indexOf(message)));
			contactName.setText(names.get(messages.indexOf(message)));
			ImageView 	contactImage = (ImageView) row.findViewById(R.id.contactImage);
			contactImage.setImageBitmap(user.getImage());

			//add row into scrollable layout
			messageLayout.addView(row);
		}
		
	}
	

	/* TASK FOR GRABBING IMAGE OF EVENT/USER/GROUP */
	private class getImageTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("email", urls[1]));
			return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
		}

		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);
				 
				//json fetch was successful
				if (jsonObject.getString("success").toString().equals("1"))
				{
					
					ImageView iv = images.get(IMAGE_INDEX);
					IMAGE_INDEX++;
					String image = jsonObject.getString("image").toString();
	
					
					Bitmap bmp = null;
					//jsonArray.getString("image");
				
					// decode image back to android bitmap format
					byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
					if (decodedString != null)
					{
						bmp = BitmapFactory.decodeByteArray(decodedString, 0,
								decodedString.length);
						//setting bmp;
					}
	
					if (bmp != null)
						iv.setImageBitmap(bmp);
					else
						iv.setImageResource(R.drawable.user_image_default);
					
					iv.setScaleType(ScaleType.CENTER_CROP);
				} 
				else
				{
					// failed
					Log.d("FETCH ROLE FAILED", "FAILED");
				}
			} 
			catch (Exception e)
			{
				Log.d("atherjsoninuserpost", "here");
				Log.d("ReadatherJSONFeedTask", e.getLocalizedMessage());
			}
			//do next thing here
		}
	}

    
	public void initKillswitchListener()
	{
		// START KILL SWITCH LISTENER
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("CLOSE_ALL");
		broadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				// close activity
				if (intent.getAction().equals("CLOSE_ALL"))
				{
					Log.d("app666", "we killin the login it");
					// System.exit(1);
					finish();
				}

			}
		};
		registerReceiver(broadcastReceiver, intentFilter);
		// End Kill switch listener
	}

    

    /*
	 * 
	 * will be fetching the friends key->val stuff here
	 */
	// Get numFriends, TODO: work on returning the integer
	public int fetchRecentContacts()
	{
		new getRecentContactsTask().execute("http://68.59.162.183/android_connect/testcontacts4.php");
		
		return 1;
	}

	class getRecentContactsTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("email", user.getEmail()));
			return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
		}

		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);
				if (jsonObject.getString("success").toString().equals("1"))
				{
					System.out.println("WE HAD SUCCESS IN GET MESSAGES!");
					// gotta make a json array
					JSONArray jsonArray = jsonObject.getJSONArray("messages");
					// success so clear previous
					// getUsers().clear();
					// looping thru array
					for (int i = 0; i < jsonArray.length(); i++)
					{
						// at each iteration set to hashmap friendEmail ->
						// 'first last'
						JSONObject o = (JSONObject) jsonArray.get(i);
						// function adds friend to the friends map					
				
						String otheremail = o.getString("sender").equals(user.getEmail()) ? o.getString("receiver") : o.getString("sender");
						
						if (!emails.contains(otheremail))
						{
							System.out.println("IN THE IF BABY!!");
							emails.add(otheremail);
							dates.add(o.getString("senddate"));
							messages.add(o.getString("message"));
							names.add(o.getString("first") + " " + o.getString("last"));
						}
						else if (dates.get(emails.indexOf(otheremail)).compareTo(o.getString("senddate")) < 0)
						{
							System.out.println("IN ELSE IF");
							int index = emails.indexOf(otheremail);
							dates.add(index, o.getString("senddate"));
							messages.add(index, o.getString("message"));
						}		
					}
					populateRecentContacts();
				}
				// user has no friends
				if (jsonObject.getString("success").toString().equals("2"))
				{
					Log.d("fetchFriends", "failed = 2 return");
					// setNumFriends(0); //PANDA need to set the user class not
					// global
				}
			} catch (Exception e)
			{
				Log.d("fetchFriends", "exception caught");
				Log.d("ReadatherJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}
	
	
	private String parseDate(String dateString)
	{
		System.out.println("\n\nDATE IS FIRST: " + dateString);
		String date = "";
		SimpleDateFormat raw = new SimpleDateFormat("yyyy-M-d h:mm:ss");
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEEE h:mma");
		try
		{
			Date parsedDate = (Date) raw.parse(dateString);
			date = dateFormat.format(parsedDate);
			// date = raw.format(parsedDate);
			System.out.println("\nDATE IN RAW TRANSLATION: "
					+ raw.format(parsedDate));
			System.out.println("\nDATE IN FINAL: "
					+ dateFormat.format(parsedDate) + "\n\n");
		} catch (ParseException ex)
		{
			System.out.println("Exception " + ex);
		}
		return date;
	}
}
