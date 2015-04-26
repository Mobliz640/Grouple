package cs460.grouple.grouple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.gcm.GoogleCloudMessaging;
/*
 * MessagesActivity has not been implemented yet.
 */
public class MessagesActivity extends BaseActivity
{
	private Button sendMessageButton;
	private EditText messageEditText;
	private User user;
	private String recipient; //user that this user is talking to
	private String SENDER_ID = "957639483805";
	private LinearLayout listViewLayout;
	private ListView listView;
	private ArrayList<Message> messages = new ArrayList<Message>();
	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private String recipientRegID = "";

	// This is the handler that will manager to process the broadcast intent
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String receiver = intent.getStringExtra("receiver");
			if (receiver.equals(user.getEmail()))
			{
				populateMessages();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_messages);
		Bundle extras = getIntent().getExtras();
		user = GLOBAL.getCurrentUser();
		// initializing the variables for the send button / message edit text
		sendMessageButton = (Button) findViewById(R.id.sendButton);
		messageEditText = (EditText) findViewById(R.id.messageEditText);
		listViewLayout = (LinearLayout) findViewById(R.id.listViewLayout);
		listView = (ListView) findViewById(R.id.listView);
		initActionBar(extras.getString("name"), true);
		gcm = GoogleCloudMessaging.getInstance(this);
		recipient = extras.getString("email");
		// Get the recipient
		new getRegIDTask().execute("http://68.59.162.183/android_connect/get_chat_id.php", recipient);
	}

	@Override
	protected void onResume()
	{
		registerReceiver(mMessageReceiver, new IntentFilter("USER_MESSAGE"));
		super.onResume();
		// new
		// getRegIDTask().execute("http://68.59.162.183/android_connect/get_chat_id.php",
		// recipient);
		fetchMessages();
	}

	@Override
	protected void onPause()
	{
		unregisterReceiver(mMessageReceiver);
		super.onPause();
	}
	

	/*
	 * Exclusive to this activity
	 */
	private void fetchMessages()
	{
		new getMessagesTask().execute("http://68.59.162.183/android_connect/get_messages.php");
	}

	class getMessagesTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("sender", recipient));
			nameValuePairs.add(new BasicNameValuePair("receiver", user.getEmail()));
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
					JSONArray jsonArray = jsonObject.getJSONArray("messages");
					messages.clear();
					// looping thru array
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONObject o = (JSONObject) jsonArray.get(i);
						Message m = new Message(o.getString("message"), o.getString("send_date"),
								o.getString("sender"), "name", o.getString("receiver"), null);
						messages.add(m); // adding message to message array
					}
					populateMessages();
				}
				// user has no friends
				if (jsonObject.getString("success").toString().equals("2"))
				{
					Log.d("fetchMessages", "failed = 2 return");
				}
				if (jsonObject.getString("success").toString().equals("0"))
				{
					// layout inflater
					TextView sadGuyTextView;
					View row = null;
					// messages consist of some things (messagebody, date,
					// sender, receiver)
					int index = 0;
					// loop through messages (newest first), maybe a map String
					// String with messagebody, date

					row = inflater.inflate(R.layout.list_item_sadguy, null);
					sadGuyTextView = (TextView) row.findViewById(R.id.sadGuyTextView);
					sadGuyTextView.setText("No messages to display!");
					listView.setVisibility(View.GONE);
					listViewLayout.addView(row);
				}
			}
			catch (Exception e)
			{
				Log.d("fetchMessages", "exception caught");
				Log.d("ReadJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}

	/*
	 * 
	 * will be fetching the friends key->val stuff here
	 */
	// Get numFriends, TODO: work on returning the integer
	public int readMessages()
	{
		new readMessagesTask().execute("http://68.59.162.183/android_connect/update_messageread_date.php");
		return 1;
	}

	class readMessagesTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("sender", recipient));
			nameValuePairs.add(new BasicNameValuePair("receiver", user.getEmail()));
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
					System.out.println("WE HAD SUCCESS IN READ MESSAGES!");
				}
				// user has no friends
				if (jsonObject.getString("success").toString().equals("2"))
				{
					Log.d("readMessage", "failed = 2 return");
				}
			}
			catch (Exception e)
			{
				Log.d("readMessage", "exception caught");
				Log.d("ReadJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}
	
	private class MessageListAdapter extends ArrayAdapter<Message>
	{
		public MessageListAdapter()
		{
			super(MessagesActivity.this, 0, messages);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View itemView = convertView;
			if (itemView == null)
				itemView = inflater.inflate(getItemViewType(position), parent, false);
			Message m = messages.get(position);
			TextView messageBody = (TextView) itemView.findViewById(R.id.messageBody);
			TextView messageDate = (TextView) itemView.findViewById(R.id.messageDate);
			messageBody.setText(m.getMessage());
			messageDate.setText(m.getDateString());
			itemView.setId(m.getID());
			return itemView;
		}

		@Override
		public int getItemViewType(int position)
		{
			int listItemID = R.layout.list_row;
			Message m = messages.get(position);
			if (m.getSender().equals(user.getEmail()))
				listItemID = R.layout.message_row; 
			else
				listItemID = R.layout.message_row_out;

			return listItemID;
		}
	}

	private void populateMessages()
	{

		// scrolling to last message
		//final ScrollView scrollview = ((ScrollView) findViewById(R.id.messagesScrollView));
		//scrollview.post(new Runnable()
		//{

			//@Override
		//	public void run()
			//{
			//	scrollview.fullScroll(ScrollView.FOCUS_DOWN);
			//}
		//});
		ArrayAdapter<Message> adapter = new MessageListAdapter();
		listView.setAdapter(adapter);
		messageEditText.requestFocus();
		scrollListView(adapter.getCount()-1, listView);
		readMessages();
	}

	// Send an upstream message.
	public void onClick(final View view)
	{
		super.onClick(view);
		if (view == findViewById(R.id.sendButton))
		{
			sendMessageButton.setClickable(false);
			// Get message from edit text
			String message = messageEditText.getText().toString();

			// make sure message field is not blank
			if (!(message.compareTo("") == 0))
			{
				// PHP expects message,sender,receiver.
				new storeMessageTask().execute("http://68.59.162.183/android_connect/send_message.php", message,
						user.getEmail(), recipient);
				new AsyncTask<Void, Void, String>()
				{
					@Override
					protected String doInBackground(Void... params)
					{
						String message = "";
						try
						{
							Bundle data = new Bundle();
							// Get message from edit text
							message = messageEditText.getText().toString();
							Message m = new Message(message, new Date(), user.getEmail(), user.getName(), recipient,
									null);
							messages.add(m);
							data.putString("msg", m.getMessage());
							data.putString("my_action", "cs460.grouple.grouple.ECHO_NOW");
							data.putString("content", "USER_MESSAGE");
							data.putString("sender", m.getSender());
							data.putString("receiver", m.getReceiver());
							// This is where we put the recipients regID.
							data.putString("recipient", recipientRegID);
							// This is where we put our first and last name.
							// That way the recipient knows who sent it.
							data.putString("first", user.getFirstName());
							data.putString("last", user.getLastName());
							String id = Integer.toString(msgId.incrementAndGet());
							gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
							message = "Sent message";
						}
						catch (IOException ex)
						{
							Toast toast = GLOBAL.getToast(MessagesActivity.this,
									"Error sending message. Please try again.");
							toast.show();
							sendMessageButton.setClickable(true);
							messageEditText.requestFocus();
							message = "Error :" + ex.getMessage();
						}
						return message;
					}

					@Override
					protected void onPostExecute(String msg)
					{
						messageEditText.setText("");
						sendMessageButton.setClickable(true);
						messageEditText.requestFocus();
						populateMessages();
					}
				}.execute(null, null, null);
			}
			else
			{
				Toast toast = GLOBAL.getToast(this, "Please enter a message!");
				toast.show();
			}
		}
	}

	// This task gets your friend's regid
	private class getRegIDTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			// The recipient's email is urls[1]
			nameValuePairs.add(new BasicNameValuePair("email", urls[1]));
			return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
		}

		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);
				System.out.println(jsonObject.getString("success"));
				if (jsonObject.getString("success").toString().equals("1"))
				{
					recipientRegID = jsonObject.getString("regid").toString();
				}
				else
				{
					Toast toast = GLOBAL.getToast(MessagesActivity.this, "Error getting GCM REG_ID.");
					toast.show();
				}
			}
			catch (Exception e)
			{
				Log.d("ReadJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}

	// Stores the message in the database.
	private class storeMessageTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... urls)
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			// The msg is urls[1], sender urls[2], receiver urls[3]
			nameValuePairs.add(new BasicNameValuePair("msg", urls[1]));
			nameValuePairs.add(new BasicNameValuePair("sender", urls[2]));
			nameValuePairs.add(new BasicNameValuePair("receiver", urls[3]));
			return GLOBAL.readJSONFeed(urls[0], nameValuePairs);
		}

		@Override
		protected void onPostExecute(String result)
		{
			try
			{
				JSONObject jsonObject = new JSONObject(result);
				System.out.println(jsonObject.getString("success"));
				if (jsonObject.getString("success").toString().equals("1"))
				{
					/*
					 * //Message was successfully stored, now notify the user.
					 * String msg = ""; try { Bundle data = new Bundle(); //Get
					 * message from edit text EditText mymessage =
					 * (EditText)findViewById(R.id.messageEditText); msg =
					 * mymessage.getText().toString(); messages.add(msg);
					 * data.putString("my_message", msg);
					 * data.putString("my_action",
					 * "cs460.grouple.grouple.ECHO_NOW");
					 * data.putString("recipient",getRecipientRegID()); String
					 * id = Integer.toString(msgId.incrementAndGet());
					 * gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
					 * msg = "Sent message"; } catch (IOException ex) { msg =
					 * "Error :" + ex.getMessage(); }
					 */
				}
				else
				{
					Toast toast = GLOBAL.getToast(MessagesActivity.this, "Error sending message. Please try again.");
					toast.show();
				}
			}
			catch (Exception e)
			{
				Log.d("ReadJSONFeedTask", e.getLocalizedMessage());
			}
		}
	}
}
