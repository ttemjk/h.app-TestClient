package eu.h2020.helios_social.heliostestclient.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import eu.h2020.helios_social.core.profile.HeliosUserData;
import eu.h2020.helios_social.heliostestclient.service.ContactList;
import eu.h2020.helios_social.heliostestclient.R;

public class ContactListActivity extends AppCompatActivity {
    public static final String TOPIC_NAME = ContactListActivity.class.getCanonicalName() + "TOPICNAME";
    private static final String TAG = "Helios-ContactListActivity";
    private Handler mHeartbeat;
    private final int mHeartbeatInterval = 10 * 1000; // 10 seconds
    private static String mOnlineColor;
    private TagListAdapter mTagListAdapter;
    private static String mUserId;
    private static final ContactList mContactList = ContactList.getInstance();
    private static String mTopicName;  // Non-null, if activity called from a topic.
    private static List<String> mOnline = new ArrayList<>();

    private static class TagListAdapter extends ListAdapter<Pair<String, Long>, TagListAdapter.TagListViewHolder> {
        protected TagListAdapter() {
            super(DIFF_CALLBACK);
        }

        private static final DiffUtil.ItemCallback<Pair<String, Long>> DIFF_CALLBACK = new DiffUtil.ItemCallback<Pair<String, Long>>() {
            @Override
            public boolean areItemsTheSame(@NonNull Pair<String, Long> oldItem, @NonNull Pair<String, Long> newItem) {
                return oldItem.first.equals(newItem.first);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Pair<String, Long> oldItem, @NonNull Pair<String, Long> newItem) {
                return oldItem.first.equals(newItem.first) && oldItem.second.equals(newItem.second)
                    && ((mOnline.contains(newItem.first) && mContactList.online(newItem.first))
                        || (!mOnline.contains(newItem.first) && !mContactList.online(newItem.first)));
            }
        };

        private static final class TagListViewHolder extends RecyclerView.ViewHolder {
            protected TextView tagLabelView;
            protected TextView tagTextView;
            protected TextView tagTextView2;

            public TagListViewHolder(@NonNull View itemView) {
                super(itemView);

                tagLabelView = (TextView) itemView.findViewById(R.id.peer_tag_label);
                tagTextView = (TextView) itemView.findViewById(R.id.peer_tag_text);
                tagTextView2 = (TextView) itemView.findViewById(R.id.peer_tag_text2);
            }

            private void bind(Pair<String, Long> item) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openDirectChat(v.getContext(), item.first);
                    }
                });
            }
        }

        @NonNull
        @Override
        public TagListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_peer_tag, parent, false);

            return new TagListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TagListViewHolder holder, int position) {
            Log.d(TAG, "onBindViewHolder:" + position);

            Pair<String, Long> item = getItem(position);
            String contactId = item.first;
            holder.tagLabelView.setText(mContactList.getName(contactId));

            String userText = contactId + " UUID:" + mContactList.getUUID(contactId) + " ";
            holder.tagTextView.setText(userText);

            long ts = item.second;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String datetimeStr = sdf.format(ts);

            String statusText = "";

            if (mContactList.online(contactId)) {
                statusText += "<font color='" + mOnlineColor + "'><b>Online</b></font>, ";
                if (!mOnline.contains(contactId))
                    mOnline.add(contactId);
            } else if (mOnline.contains(contactId))
                mOnline.remove(contactId);

            if (mTopicName == null)
                statusText += "Last msg: " + datetimeStr;
            else
                statusText += "Last topic msg: " + datetimeStr;

            holder.tagTextView2.setText(Html.fromHtml(statusText, 0),  TextView.BufferType.SPANNABLE);

            holder.bind(item);
        }
    }

    private static void openDirectChat(Context ctx, String contactId) {
        Log.d(TAG, "openDirectChat networkId:" + mContactList.getNetworkId(contactId));
        Log.d(TAG, "openDirectChat UUID:" + mContactList.getUUID(contactId));

        if (mUserId.equals(mContactList.getUUID(contactId))) {
            Log.d(TAG, "This is the user, not opening a chat");
            return;
        }
        Intent i = new Intent(ctx, DirectChatActivity.class);
        i.putExtra(DirectChatActivity.CHAT_NETWORK_ID, mContactList.getNetworkId(contactId));
        i.putExtra(DirectChatActivity.CHAT_UUID, mContactList.getUUID(contactId));
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_tag);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        mOnlineColor = '#' + Integer.toHexString(getColor(R.color.tc_contact_list_online) & 0xffffff);

        mUserId = HeliosUserData.getInstance().getValue(getString(R.string.setting_user_id));
        Log.d(TAG, "mUserId:" + mUserId);

        mTagListAdapter = new TagListAdapter();

        RecyclerView listView = (RecyclerView) findViewById(R.id.tag_peer_list);
        listView.setAdapter(mTagListAdapter);
        listView.setLayoutManager(new LinearLayoutManager(this));

        mTopicName = this.getIntent().getStringExtra(TOPIC_NAME);
        if (mTopicName != null)
            getSupportActionBar().setTitle(mTopicName + ": Contact list");

        mHeartbeat = new Handler();
        mHeartbeat.postDelayed(mRunnableUpdate, 1);
    }

    private Runnable mRunnableUpdate =
            new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "mRunnableUpdate()");

                    LinkedList<Pair<String, Long>> list;
                    if (mTopicName != null)
                        list = mContactList.getTopicContacts(mTopicName);
                    else
                        list = mContactList.getContacts();

                    mTagListAdapter.submitList(list);

                    mHeartbeat.postDelayed(this, mHeartbeatInterval);
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        mTagListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");

        mHeartbeat.removeCallbacks(mRunnableUpdate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
