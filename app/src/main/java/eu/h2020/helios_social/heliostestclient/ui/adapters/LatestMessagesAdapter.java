package eu.h2020.helios_social.heliostestclient.ui.adapters;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.threeten.bp.Instant;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import eu.h2020.helios_social.core.info_control.MessageInfo;
import eu.h2020.helios_social.core.info_control.MessageImportance;
import eu.h2020.helios_social.heliostestclient.R;

public class LatestMessagesAdapter extends RecyclerView.Adapter<LatestMessagesAdapter.LatestMsgsViewHolder> {
    private List<MessageInfo> dataset;
    private HashMap<String, MessageImportance> mImportances;
    private HashMap<Integer, String> mImportanceColors;
    private OnItemClickListener mOnClickListener;

    // Reference to the views for each data item
    public class LatestMsgsViewHolder extends RecyclerView.ViewHolder {
        private TextView latestMsgsTextView;
        public LatestMsgsViewHolder(View itemView) {
            super(itemView);
            this.latestMsgsTextView = itemView.findViewById(R.id.latestMsgsTextView);
            itemView.setOnClickListener(v1 -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        MessageInfo mi = dataset.get(pos);
                        if (mOnClickListener != null) {
                            mOnClickListener.onClick(mi);
                        }
                    }
                });
        }
    }

    public LatestMessagesAdapter(List<MessageInfo> myDataset, HashMap<String, MessageImportance> importances,
                                 HashMap<Integer, String> importanceColors) {
        dataset = myDataset;
        mImportances = importances;
        mImportanceColors = importanceColors;
    }

    public void setOnItemClickListener(OnItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }
            
    // Create new views (invoked by the layout manager)
    public LatestMessagesAdapter.LatestMsgsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.item_latest_msgs, parent, false);
        LatestMessagesAdapter.LatestMsgsViewHolder viewHolder = new LatestMessagesAdapter.LatestMsgsViewHolder(listItem);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(LatestMessagesAdapter.LatestMsgsViewHolder holder, int position) {
        MessageInfo m = (MessageInfo) dataset.get(position);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(m.getTimestamp()), ZoneId.systemDefault());
        // Topic, date and context probability on first line
        String msgType = (m.getMessageTopic().equals(m.getFrom())) ? "DirectMessage: " : "Topic: ";
        String msgText = "<br>" + msgType + m.getMessageTopic() + " " + DateTimeFormatter.ofLocalizedDateTime
            (FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault())
            .withLocale(Locale.getDefault()).format(zdt)
            + String.format(" Pr:%.2f", mImportances.get(m.getId()).getContextProbability()) + "<br>";
        // Sender and contents highlighted by importance on second
        switch(mImportances.get(m.getId()).getImportance()) {
        case MessageImportance.IMPORTANCE_VERY_HIGH:
            msgText += "<font color='" + mImportanceColors.get(MessageImportance.IMPORTANCE_VERY_HIGH) + "'><b>"
                + m.getFrom() + ":" + m.getMessageText() + "</b></font>";
            break;
        case MessageImportance.IMPORTANCE_HIGH:
        case MessageImportance.IMPORTANCE_MEDIUM:
        case MessageImportance.IMPORTANCE_LOW:
        case MessageImportance.IMPORTANCE_VERY_LOW:
            msgText += "<font color='" + mImportanceColors.get(mImportances.get(m.getId()).getImportance()) + "'>"
                + m.getFrom() + ":" + m.getMessageText() + "</font>";
            break;
        default:
            msgText += m.getFrom() + ":" + m.getMessageText();
        }
        holder.latestMsgsTextView.setText(Html.fromHtml(msgText, 0),  TextView.BufferType.SPANNABLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public interface OnItemClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param mi The msg that was clicked
         */
        void onClick(MessageInfo mi);
    }
}
