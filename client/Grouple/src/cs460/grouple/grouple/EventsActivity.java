package cs460.grouple.grouple;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
 * EventsActivity has not been implemented yet.
 */
public class EventsActivity extends BaseActivity
{
	private User user;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_events);
	}

	private void load()
	{
		user = GLOBAL.getCurrentUser();
		setNotifications();
		initActionBar("Events");
	}
	
	private void setNotifications()
	{
		((Button) findViewById(R.id.eventsUpcomingButtonEA)).setText("Upcoming Events (" + user.getNumEventsUpcoming() + ")");
		((Button) findViewById(R.id.eventsPendingButtonEA)).setText("Pending Events (" + user.getNumEventsPending() + ")");
		((Button) findViewById(R.id.eventInvitesButtonEA)).setText("Event Invites (" + user.getNumEventsInvites() + ")");
		((Button) findViewById(R.id.eventsPastButtonEA)).setText("Past Events (" + user.getNumEventsPast() + ")");		
	}
	
	public void onClick(View view)
	{
		loadDialog.show();
		Intent intent =  new Intent(this, ListActivity.class);
		switch (view.getId())
		{
		case R.id.eventsPendingButtonEA:
			user.fetchEventsPending();
			intent.putExtra("CONTENT", "EVENTS_PENDING");
			break;
		case R.id.eventsUpcomingButtonEA:
			user.fetchEventsUpcoming();
			intent.putExtra("CONTENT", "EVENTS_UPCOMING");
			break;
		case R.id.eventCreateButtonEA:
			intent = new Intent(this, EventCreateActivity.class);
			user.fetchGroups();
			break;
		case R.id.eventInvitesButtonEA:
			user.fetchEventsInvites();
			intent.putExtra("CONTENT", "EVENTS_INVITES");
			break;
		case R.id.eventsPastButtonEA:
			user.fetchEventsPast();
			intent.putExtra("CONTENT", "EVENTS_PAST");
			break;
		}
		GLOBAL.setCurrentUser(user);
		intent.putExtra("EMAIL", user.getEmail());
		startActivity(intent);
	}

	@Override
	public void onResume()
	{
		super.onResume(); // Always call the superclass method first
		load();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_events)
		{
			//do nothing, already here
		}
		return super.onOptionsItemSelected(item);
	}

	/* Start activity functions for going back and logging out */
	public void startHomeActivity(View view)
	{
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
		intent.putExtra("EMAIL", user.getEmail());
	}
		
	@Override
	public void onBackPressed() 
	{
		super.onBackPressed();
		user.fetchEventsInvites();
    	user.fetchFriendRequests();
    	user.fetchGroupInvites();
    	GLOBAL.setCurrentUser(user);
	}
}
