package eu.h2020.helios_social.heliostestclient.ui.adapters;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Intent;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.info_control.InfoControl;
import eu.h2020.helios_social.core.info_control.MessageInfo;
import eu.h2020.helios_social.core.info_control.MessageImportance;
import eu.h2020.helios_social.core.messaging.data.HeliosConversation;
import eu.h2020.helios_social.core.messaging.data.HeliosConversationList;
import eu.h2020.helios_social.core.messaging.data.HeliosMessagePart;
import eu.h2020.helios_social.core.profile.HeliosProfileManager;
import eu.h2020.helios_social.core.messaging.data.HeliosTopicContext;
import eu.h2020.helios_social.heliostestclient.service.ContactList;
import eu.h2020.helios_social.heliostestclient.ui.ChatActivity;
import eu.h2020.helios_social.heliostestclient.ui.DirectChatActivity;
import eu.h2020.helios_social.heliostestclient.R;

/**
 * Provides a view to topics, direct messages and active contexts of the user
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.TopicContextViewHolder> {
    private static final String TAG = "MainAdapter";
    private List<HeliosTopicContext> mTopics;
    private List<Context> mActiveContexts;
    private InfoControl mInfoControl;
    private List<MessageInfo> mMessageInfo;
    private HashMap<String, List<MessageImportance>> mImportances;
    private HashMap<Integer, String> mImportanceColors;
    private int mMaxMsgs;
    private int mLowestToShow;
    private final android.content.Context mCaller;
    private OnItemClickListener mOnClickListener;

    private static final int TOPIC_VIEW = 1;
    private static final int DIRECT_MESSAGE_VIEW = 2;
    private static final int CONTEXT_VIEW = 3;

    // Reference to the views for each data item
    public class TopicContextViewHolder extends RecyclerView.ViewHolder {
        private TextView contextNameView;
        private RecyclerView latestMsgsView;
        private TextView msgCountTextView;
        public TopicContextViewHolder(View itemView) {
            super(itemView);
            this.contextNameView = itemView.findViewById(R.id.contextNameView);
            this.latestMsgsView = itemView.findViewById(R.id.latest_msgs_view);
            this.msgCountTextView = itemView.findViewById(R.id.msgCountTextView);

            itemView.setOnClickListener(v1 -> {
                Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    int type = MainAdapter.this.getItemViewType(pos);
                    if(type == CONTEXT_VIEW) {
                        Context c = mActiveContexts.get(pos);
                        if (mOnClickListener != null) {
                            mOnClickListener.onClick(c);
                        }
                    } else {
                        HeliosTopicContext topic = mTopics.get(pos - mActiveContexts.size());
                        if (mOnClickListener != null) {
                            mOnClickListener.onClick(topic);
                        }
                    }
                }
            });
            itemView.setOnLongClickListener(v12 -> {
                Log.d(TAG, "Element " + getAdapterPosition() + " long clicked.");
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    int type = MainAdapter.this.getItemViewType(pos);
                    if(type == CONTEXT_VIEW) {
                        Context c = mActiveContexts.get(pos);
                        if (mOnClickListener != null) {
                            mOnClickListener.onLongClick(c);
                        }
                    } else {
                        HeliosTopicContext topic = mTopics.get(pos - mActiveContexts.size());
                        if (mOnClickListener != null) {
                            mOnClickListener.onLongClick(topic);
                        }
                    }
                }
                return true;
            });
        }
    }

    public MainAdapter(android.content.Context caller, List<HeliosTopicContext> heliosTopics, InfoControl infoControl,
                          List<MessageInfo> messageInfo, HashMap<String, List<MessageImportance>> importances) {
        mCaller = caller;
        mTopics = heliosTopics;
        mInfoControl = infoControl;
        mActiveContexts = infoControl.getActiveContexts();
        mMessageInfo = messageInfo;
        mImportances = importances;
        // Font colors in MessageImportance categories
        mImportanceColors = new HashMap<Integer, String>();
        mImportanceColors.put
                (MessageImportance.IMPORTANCE_VERY_HIGH,
                        '#' + Integer.toHexString(mCaller.getColor(R.color.tc_context_msg_importance_very_high) & 0xffffff));
        mImportanceColors.put
                (MessageImportance.IMPORTANCE_HIGH,
                        '#' + Integer.toHexString(mCaller.getColor(R.color.tc_context_msg_importance_high) & 0xffffff));
        mImportanceColors.put
                (MessageImportance.IMPORTANCE_MEDIUM,
                        '#' + Integer.toHexString(mCaller.getColor(R.color.tc_context_msg_importance_medium) & 0xffffff));
        mImportanceColors.put
                (MessageImportance.IMPORTANCE_LOW,
                        '#' + Integer.toHexString(mCaller.getColor(R.color.tc_context_msg_importance_low) & 0xffffff));
        mImportanceColors.put
                (MessageImportance.IMPORTANCE_VERY_LOW,
                        '#' + Integer.toHexString(mCaller.getColor(R.color.tc_context_msg_importance_very_low) & 0xffffff));
        mImportanceColors.put(MessageImportance.IMPORTANCE_UNKNOWN, "");
        HeliosProfileManager profileMgr = HeliosProfileManager.getInstance();
        String maxMsgs = profileMgr.load(mCaller, "max_context_messages");
        String lowestToShow = profileMgr.load(mCaller, "lowest_message_importance");
        // Maximum number of messages shown on context item.
        try {
            mMaxMsgs = Integer.parseInt(maxMsgs);
        } catch (NumberFormatException e) {
            mMaxMsgs = 5;
        }
        // Lowest message importance to show
        try  {
            mLowestToShow = Integer.parseInt(lowestToShow);
        } catch (NumberFormatException e) {
            mLowestToShow = MessageImportance.IMPORTANCE_VERY_LOW;
        }
    }

    // Returns the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mTopics.size() + mActiveContexts.size();
    }


    @Override
    public int getItemViewType(int position) {
        return (position < mActiveContexts.size()) ? CONTEXT_VIEW : TOPIC_VIEW;
    }

    public void setOnItemClickListener(OnItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    // Create new views (invoked by the layout manager)
    public MainAdapter.TopicContextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = R.layout.item_main_chat;
        switch(viewType) {
            case TOPIC_VIEW:
            case DIRECT_MESSAGE_VIEW:
                layout = R.layout.item_main_chat;
                break;
            case CONTEXT_VIEW:
                layout = R.layout.item_mycontexts;
                break;
        }
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(layout, parent, false);
        return new MainAdapter.TopicContextViewHolder(listItem);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MainAdapter.TopicContextViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TOPIC_VIEW:
            case DIRECT_MESSAGE_VIEW:
                onBindTopicViewHolder(holder, position);
                break;
            case CONTEXT_VIEW:
                onBindContextViewHolder(holder, position);
                break;
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    private void onBindTopicViewHolder(MainAdapter.TopicContextViewHolder holder, int position) {
        HeliosTopicContext topic = mTopics.get(position-mActiveContexts.size());
        holder.itemView.setTag(position);

        // Lookup view for data population
        TextView tvParticipants = (TextView) holder.itemView.findViewById(R.id.participantTextView);
        TextView tvTopic = (TextView) holder.itemView.findViewById(R.id.topicTextView);
        TextView tvTime = (TextView) holder.itemView.findViewById(R.id.timeTextView);

        getLastMessageInfo(topic);
        // Populate the data into the template view using the data object
        tvParticipants.setText(topic.participants);

        // Add type of topic
        if (!TextUtils.isEmpty(topic.uuid)) {
            tvTopic.setText("DirectMessage: " + topic.topic);
        } else {
            tvTopic.setText("Topic: " + topic.topic);
        }

        tvTime.setText(topic.ts);
        Log.d(TAG, "last holder at " + position);
    }

    // Update topic latest message info
    private void getLastMessageInfo(HeliosTopicContext topic) {
        if(topic != null && topic.topic != null) {
            HeliosConversation c = HeliosConversationList.getInstance().getConversation(topic.topic);
            if(c != null) {
                HeliosMessagePart msg = c.getLatestMessage();
                if(msg != null && msg.msg != null && msg.senderName != null) {
                    topic.lastMsg = msg.msg;
                    topic.ts = msg.getLocaleTs();
                    topic.participants = msg.senderName + ":" + msg.msg;
                }
            }
        }
    }

    private void onBindContextViewHolder(MainAdapter.TopicContextViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Context c = (Context)mActiveContexts.get(position);
        holder.contextNameView.setText(c.getName());
        if(c.isActive())
            holder.itemView.setBackground(mCaller.getDrawable(R.drawable.tc_active_context_item));
        else
            holder.itemView.setBackground(mCaller.getDrawable(R.drawable.tc_context_item));

        List<MessageInfo> msgsInContext = new ArrayList<>();
        HashMap<String, MessageImportance> importancesInContext = new HashMap<>();
        int[] msgCount = new int[MessageImportance.IMPORTANCE_VERY_HIGH+1];
        for (MessageInfo m: mMessageInfo) {
            List<MessageImportance> mil = mImportances.get(m.getId());
            if (mil != null && mil.size() > 0) {
                double probThreshold = 1.0 / mil.size() - 0.01;
                for (MessageImportance mi : mil) {
                    if (mi.getContext().getId().equals(c.getId()) && mi.getImportance() >= mLowestToShow &&
                            mi.getContextProbability() > probThreshold) {
                        msgsInContext.add(m);
                        importancesInContext.put(m.getId(), mi);
                        msgCount[mi.getImportance()]++;
                    }
                }
            }
        }
        // sort received messages in context by importance and, secondly, by time
        msgsInContext.sort((messageInfo1, messageInfo2) -> {
            int importance1 = messageInfo1.getImportance();
            int importance2 = messageInfo2.getImportance();
            Long time1 = messageInfo1.getTimestamp();
            Long time2 = messageInfo2.getTimestamp();
            return importance1 > importance2 ? -1 : importance1 < importance2 ? 1 : time2.compareTo(time1);
        });
        // show max mMaxMsgs messages in context
        msgsInContext = msgsInContext.stream().limit(mMaxMsgs).collect(Collectors.toList());

        String countText = "<font color='" + mImportanceColors.get(MessageImportance.IMPORTANCE_VERY_HIGH) + "'><b> "
                + msgCount[MessageImportance.IMPORTANCE_VERY_HIGH] + " </b></font>";
        if (mLowestToShow <= MessageImportance.IMPORTANCE_HIGH)
            countText += "<font color='" + mImportanceColors.get(MessageImportance.IMPORTANCE_HIGH) + "'> "
                    + msgCount[MessageImportance.IMPORTANCE_HIGH] + " </font>";
        if (mLowestToShow <= MessageImportance.IMPORTANCE_MEDIUM)
            countText += "<font color='" + mImportanceColors.get(MessageImportance.IMPORTANCE_MEDIUM) + "'> "
                    + msgCount[MessageImportance.IMPORTANCE_MEDIUM] + " </font>";
        if (mLowestToShow <= MessageImportance.IMPORTANCE_LOW)
            countText += "<font color='" + mImportanceColors.get(MessageImportance.IMPORTANCE_LOW) + "'> "
                    + msgCount[MessageImportance.IMPORTANCE_LOW] + " </font>";
        if (mLowestToShow <= MessageImportance.IMPORTANCE_VERY_LOW)
            countText += "<font color='" + mImportanceColors.get(MessageImportance.IMPORTANCE_VERY_LOW) + "'> "
                    + msgCount[MessageImportance.IMPORTANCE_VERY_LOW] + " </font>";

        holder.msgCountTextView.setText(Html.fromHtml(countText, 0),  TextView.BufferType.SPANNABLE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(holder.latestMsgsView.getContext());
        holder.latestMsgsView.setLayoutManager(layoutManager);
        LatestMessagesAdapter msgsAdapter = new LatestMessagesAdapter(msgsInContext, importancesInContext,
                mImportanceColors);
        msgsAdapter.setOnItemClickListener(msgClickHandler);
        holder.latestMsgsView.setAdapter(msgsAdapter);
    }

    private LatestMessagesAdapter.OnItemClickListener msgClickHandler = new LatestMessagesAdapter.OnItemClickListener() {
        @Override
        public void onClick(MessageInfo mi) {
            Iterator itr = mMessageInfo.iterator();
            while (itr.hasNext()) {
                MessageInfo m = (MessageInfo)itr.next();
                // Get all messages in the same topic as the one clicked.
                if (m.getMessageTopic().equals(mi.getMessageTopic())) {
                    // Set reaction time and MessageContext
                    mInfoControl.readMessage(m);
                    // Remove from MessageInfo list
                    itr.remove();
                }
            }
            if (mi.getMessageTopic().equals(mi.getFrom())) {
                // Find Direct Chat UUID
                String uuid = null;
                ArrayList<HeliosTopicContext> htcs = ContactList.getInstance().getTopics();
                for (HeliosTopicContext htc: htcs)
                    if (htc.topic.equals(mi.getFrom())) {
                        uuid = htc.uuid;
                        break;
                    }
                if (uuid != null) {
                    Intent i = new Intent(mCaller, DirectChatActivity.class);
                    i.putExtra(DirectChatActivity.CHAT_UUID, uuid);
                    mCaller.startActivity(i);
                }
            } else {
                Intent i = new Intent(mCaller, ChatActivity.class);
                i.putExtra(ChatActivity.CHAT_ID, mi.getMessageTopic());
                mCaller.startActivity(i);
            }
        }
    };

    public void updateDataset() {
        mActiveContexts = mInfoControl.getActiveContexts();
        notifyDataSetChanged();
    }

    public void updateContexts() {
        List<Context> contexts = mInfoControl.getActiveContexts();
        for (Context c : contexts) {
            if (!mActiveContexts.contains(c)) {
                mActiveContexts.add(c);
            }
        }
        notifyDataSetChanged();
    }

    public int getActiveContextsCount() { return mActiveContexts.size(); }

    public interface OnItemClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param htc The msg that was clicked
         */
        void onClick(HeliosTopicContext htc);

        void onClick(Context c);

        /**
         * Called when a view has been clicked.
         *
         * @param htc The msg that was clicked
         */
        void onLongClick(HeliosTopicContext htc);

        void onLongClick(Context c);
    }
}
