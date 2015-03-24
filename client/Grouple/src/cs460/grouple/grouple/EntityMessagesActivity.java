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

import com.google.android.gms.gcm.GoogleCloudMessaging;

import cs460.grouple.grouple.ListActivity.CONTENT_TYPE;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class EntityMessagesActivity extends ActionBarActivity
{
	
	private BroadcastReceiver broadcastReceiver;
	private Global GLOBAL;
	private User user; //will be null for now
	private Group group;
	private Event event;
	private Dialog loadDialog = null;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private String ID;
    //Tag to search for when logging info.
    static final String TAG = "GCM";
    private String CONTENT_TYPE;
    //Sender ID is the project number from API console. Needs to be secret.
    String SENDER_ID = "957639483805"; 
    private ArrayList<Message> messages = new ArrayList<Message>();
    
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;
    String regid;
    String recipientRegID = "";
    
	@Override
	protected void onStop()
	{
		super.onStop();
		loadDialog.hide();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		GLOBAL = ((Global) getApplicationContext());
		
		Bundle extras = getIntent().getExtras();
		user = GLOBAL.getCurrentUser();
		group = GLOBAL.getGroupBuffer();
		setContentView(R.layout.activity_messages);
		/* Action bar */
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		gcm = GoogleCloudMessaging.getInstance(this);
		CONTENT_TYPE = extras.getString("CONTENT_TYPE");
		if (CONTENT_TYPE.equals("GROUP"))
			ID = extras.getString("GID"); 
		else
			ID = extras.getString("EID");
		ab.setCustomView(R.layout.actionbar);
		loadDialog = GLOBAL.getLoadDialog(new Dialog(this));
        loadDialog.setOwnerActivity(this);
		ab.setDisplayHomeAsUpEnabled(false);
		TextView actionbarTitle = (TextView) findViewById(R.id.actionbarTitleTextView);
		String name = extras.getString("NAME");
		//String first = name.split(" ")[0];
		actionbarTitle.setText(name);
	//	initKillswitchListener();
		context = getApplicationContext();
		//onNewIntent(getIntent());
		//Get the recipient 
		//new getRegIDTask().execute("http://68.59.162.183/android_connect/get_chat_id.php", recipient);
		//fetchMessages(); 
		setContentView(R.layout.activity_messages);
	}


	private void populateMessages()
	{
		//layout to inflate into
		LinearLayout messageLayout = (LinearLayout) findViewById(R.id.messageLayout);
		//clear out any previous views already inflated
		messageLayout.removeAllViews();
		
		//layout inflater
		LayoutInflater li = getLayoutInflater();
		
		TextView messageBody, messageDate;
		Button contactName;
		View row = null;
		String message = "";
		
		//messages consist of some things (messagebody, date, sender, receiver)
		
		int index = 0;
		//loop through messages (newest first), maybe a map String String with messagebody, date
		for (Message m : messages)
		{
			
			//if (senders.get(index).equals(user.getEmail()/*our email*/))
			//	row =  li.inflate(R.layout.message_row_out, null); //inflate this message row
			//else
				
			if (m.getSender().equals(user.getEmail()))
				row =  li.inflate(R.layout.message_row_entity, null); //inflate the sender message row
			else
				row =  li.inflate(R.layout.message_row_entity_out, null); //inflate the sender message row
			
			contactName = (Button) row.findViewById(R.id.contactNameButton);
			contactName.setText(m.getSenderName());
			contactName.setId(index);
			messageBody = (TextView) row.findViewById(R.id.messageBody);
			messageBody.setText(m.getMessage());
			messageDate = (TextView) row.findViewById(R.id.messageDate);
			messageDate.setText(m.getDateString());
			//set these values to what you want
			row.setId(index);
			//add row into scrollable layout
			messageLayout.addView(row);
			index++;
		}
		
		final ScrollView scrollview = ((ScrollView) findViewById(R.id.messagesScrollView));
		scrollview.post(new Runnable() {

		        @Override
		        public void run() {
		            scrollview.fullScroll(ScrollView.FOCUS_DOWN);
		        }
		    });
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e)  
	{
		
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	loadDialog.show();
	    	finish();
	    }
	    return true;
	}
	
	 
    //Stores the message in the database.
    private class storeMessageTask extends AsyncTask<String, Void, String>
	{

		@Override
		protected String doInBackground(String... urls)
		{
			String type = CONTENT_TYPE.equals("GROUP") ? "g_id" : "e_id";
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			//The msg is urls[1], sender urls[2], receiver urls[3]
			nameValuePairs.add(new BasicNameValuePair("msg", urls[1]));
			nameValuePairs.add(new BasicNameValuePair("sender", urls[2]));
			nameValuePairs.add(new BasicNameValuePair(type, urls[3]));	
			
			return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
		}

		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);
				System.out.println("CURRENTLY ABOUT TO GET SUCCESS STRING IT IS ");
				System.out.print(jsonObject.getString("success") + "\n\n\n");
				if (jsonObject.getString("success").toString().equals("1"))
				{
					/*//Message was successfully stored, now notify the user.
					String msg = "";
					try {
                        Bundle data = new Bundle();
                        //Get message from edit text
                        
                        messages.add(msg);
                        data.putString("my_message", msg);
                        data.putString("my_action", "cs460.grouple.grouple.ECHO_NOW");
                        data.putString("recipient",getRecipientRegID());
                        String id = Integer.toString(msgId.incrementAndGet());
                        gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                        msg = "Sent message";
                    } catch (IOException ex) {
                        msg = "Error :" + ex.getMessage();
                    }*/
					EditText mymessage   = (EditText)findViewById(R.id.messageEditText);
                  mymessage.setText("");
					fetchMessages();
					
				} else
				{
					Context context = getApplicationContext();
					Toast toast = Toast.makeText(context, "Error Storing Message. Message not sent. Contact Devs", Toast.LENGTH_LONG);
					toast.show();
				}
			} catch (Exception e)
			{
				Log.d("ReadatherJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}
	
	@Override
    protected void onResume() {
        super.onResume();
	 //   context .registerReceiver(mMessageReceiver, new IntentFilter("NEW_MESSAGE"));
		//new getRegIDTask().execute("http://68.59.162.183/android_connect/get_chat_id.php", recipient);
		fetchMessages(); 
    }
	
	//Starts a USER/GROUP/EVENT profile
	public void startProfile(View view)
	{
		loadDialog.show();
		int id = view.getId();		
		Intent intent = new Intent(this, ProfileActivity.class);
	
		String friendEmail = messages.get(id).getSender();
		User u = new User(friendEmail);
		u.fetchEventsUpcoming();
		u.fetchFriends();
		u.fetchGroups();
		u.fetchUserInfo();
		if (!GLOBAL.isCurrentUser(friendEmail))
			GLOBAL.setUserBuffer(u);
		else
			GLOBAL.setCurrentUser(u); //reloading user
		intent.putExtra("EMAIL", friendEmail);
		intent.putExtra("CONTENT", "USER");	
		startActivity(intent);	
	}
	
	// Send an upstream message.
    public void onClick(final View view) {

        if (view == findViewById(R.id.sendButton)) 
        {
            //Get message from edit text
            EditText mymessage   = (EditText)findViewById(R.id.messageEditText);
            String msg = mymessage.getText().toString();
            System.out.println("MESSAGE: " + msg);
            //make sure message field is not blank
            if(!(msg.compareTo("") ==0))
            {
               if (CONTENT_TYPE.equals("GROUP"))
            	   new storeMessageTask().execute("http://68.59.162.183/android_connect/send_group_message.php",msg, user.getEmail(), ID);
               else
            	   new storeMessageTask().execute("http://68.59.162.183/android_connect/send_event_message.php",msg, user.getEmail(), ID);
               /* new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        String msg = "";

                        
                        try {
                            Bundle data = new Bundle();
                            //Get message from edit text
                            EditText mymessage   = (EditText)findViewById(R.id.messageEditText);
                            msg = mymessage.getText().toString();
        
                            messages.add(msg);
                           // receivers.add(recipient);
                            senders.add(user.getEmail());
                          
                    		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE h:mma");
                    		
                    		String date = dateFormat.format(new Date());
                    		dates.add(date);
                            data.putString("my_message", msg);
                            data.putString("my_action", "cs460.grouple.grouple.ECHO_NOW");
                            data.putString("sender", user.getEmail());
                            //Clear edit text
                           // mymessage.setText("");
                            //Get friend's regID based off their email address from db
                            //Todd's Reg ID
                            //String recipientRegId = "APA91bFdWkh9GiaNoLJvGyFpSK3HRQy8vtlmh3OPK8FekU4aWEhZn_hwvr7LmYu_s11dQnoPmj6hKuklISIh_A2Dhyjm_cNT-K4kh5-bYhPYpp-QGbqScbwE9YCnWqyXORN2gwY3fNQx-_ex7D6i-ONaT7peHcu3Hlzbc-60amu0pTu8SD9l7xI";
                            //Brett's Reg ID
                            
                            //This is where we put the recipients regID.
                          //  data.putString("recipient",getRecipientRegID());
                            //This is where we put our first and last name. That way the recipient knows who sent it.
                            data.putString("first", user.getFirstName());
                            data.putString("last", user.getLastName());
                            String id = Integer.toString(msgId.incrementAndGet());
                            gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                            msg = "Sent message";
                        } catch (IOException ex) {
                            msg = "Error :" + ex.getMessage();
                        }
                      
                        return msg;
                    }

                    @Override
                    protected void onPostExecute(String msg) {
                    	
                    	EditText mymessage   = (EditText)findViewById(R.id.messageEditText);
                    	mymessage.setText("");
                        //add to an array of some sort
                    	//ideal to store if it is being received / sent, date, message body
                    	//assuming to start just 1 person to 1 person manually set up
                    	//repopulate messages for now
                    	//GOAL, oncreate pull all messages from server for you and the user you want to see messages of
                    	//if new messages get beamed in add them to that array with their timestamp and all and repopulate the messages
                    	//also when you send a message, add it to the array and repopulate messages
                    	populateMessages();
                    }
                }.execute(null, null, null); */
            }      
        }
    }

	
    /*
	 * 
	 * will be fetching the friends key->val stuff here
	 */
	// Get numFriends, TODO: work on returning the integer
	public int fetchMessages()
	{
		if (CONTENT_TYPE.equals("GROUP"))
			new getMessagesTask().execute("http://68.59.162.183/android_connect/get_group_messages.php");
		else
			new getMessagesTask().execute("http://68.59.162.183/android_connect/get_event_messages.php");
		
		return 1;
	}

	class getMessagesTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			String type = CONTENT_TYPE.equals("GROUP") ? "g_id" : "e_id";
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			System.out.println("GRABBING MESSAGES ID = " + ID);
			nameValuePairs.add(new BasicNameValuePair(type, ID));
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
					messages.clear();
					// gotta make a json array
					JSONArray jsonArray = jsonObject.getJSONArray("messages");
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONObject o = (JSONObject) jsonArray.get(i);
						Message m = new Message(o.getString("message"), o.getString("send_date"), o.getString("sender"),
								o.getString("first") + " " + o.getString("last"), ID, null);
						messages.add(m);
					}
					populateMessages();
				}
				if (jsonObject.getString("success").toString().equals("2"))
				{
					Log.d("fetchFriends", "failed = 2 return");
				}
			} 
			catch (Exception e)
			{
				Log.d("getMessages", "exception caught");
				Log.d("ReadJSONFeedTask", e.getLocalizedMessage());
			}
		}
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
}