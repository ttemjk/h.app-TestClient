package eu.h2020.helios_social.heliostestclient.ui.adapters;

import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.info_control.InfoControl;
import eu.h2020.helios_social.core.info_control.MessageImportance;
import eu.h2020.helios_social.core.info_control.MessageInfo;
import eu.h2020.helios_social.core.messaging.data.HeliosTopicContext;
import eu.h2020.helios_social.core.profile.HeliosProfileManager;
import eu.h2020.helios_social.heliostestclient.service.ContactList;
import eu.h2020.helios_social.heliostestclient.ui.ChatActivity;
import eu.h2020.helios_social.heliostestclient.ui.DirectChatActivity;
import eu.h2020.helios_social.heliostestclient.R;


public class ContextAdapter extends RecyclerView.Adapter<ContextAdapter.MyViewHolder> {
    private List<Context> dataset;
    private InfoControl mInfoControl;
    private List<MessageInfo> mMessageInfo;
    private HashMap<String, List<MessageImportance>> mImportances;
    private HashMap<Integer, String> mImportanceColors;
    private int mMaxMsgs;
    private int mLowestToShow;
    private android.content.Context mCaller;
    private OnItemClickListener mOnClickListener;

    // Reference to the views for each data item
    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView contextNameView;
        private RecyclerView latestMsgsView;
        private TextView msgCountTextView;
        public MyViewHolder(View itemView) {
            super(itemView);
            this.contextNameView = itemView.findViewById(R.id.contextNameView);
            this.latestMsgsView = itemView.findViewById(R.id.latest_msgs_view);
            this.msgCountTextView = itemView.findViewById(R.id.msgCountTextView);
            itemView.setOnLongClickListener(v1 -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    Context c = dataset.get(pos);
                    if (mOnClickListener != null) {
                        mOnClickListener.onLongClick(c);
                    }
                }
                return true;
            });
            itemView.setOnClickListener(v1 -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    Context c = dataset.get(pos);
                    if (mOnClickListener != null) {
                        mOnClickListener.onClick(c);
                    }
                }
            });

        }
    }

    public ContextAdapter(android.content.Context caller, List<Context> myDataset, InfoControl infoControl,
                          List<MessageInfo> messageInfo, HashMap<String, List<MessageImportance>> importances) {
        mCaller = caller;
        dataset = myDataset;
        mInfoControl = infoControl;
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

    public void setOnItemClickListener(OnItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    // Create new views (invoked by the layout manager)
    public ContextAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.item_mycontexts, parent, false);
        ContextAdapter.MyViewHolder viewHolder = new ContextAdapter.MyViewHolder(listItem);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ContextAdapter.MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Context c = dataset.get(position);
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

    public void updateDataset(List<Context> myDataset) {
        dataset = myDataset;
    }

    private final LatestMessagesAdapter.OnItemClickListener msgClickHandler = new LatestMessagesAdapter.OnItemClickListener() {
        @Override
        public void onClick(MessageInfo mi) {
            Iterator<MessageInfo> itr = mMessageInfo.iterator();
            while (itr.hasNext()) {
                MessageInfo m = itr.next();
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

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public interface OnItemClickListener {

        /**
         * Called when a view has been clicked.
         *
         * @param c The context that was clicked
         */
        void onClick(Context c);
        /**
         * Called when a view has been long clicked.
         *
         * @param c The context that was clicked
         */
        void onLongClick(Context c);
    }
}
