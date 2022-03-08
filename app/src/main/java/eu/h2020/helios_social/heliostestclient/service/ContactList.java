package eu.h2020.helios_social.heliostestclient.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.info_control.CenUtils;
import eu.h2020.helios_social.core.messaging.data.HeliosConversation;
import eu.h2020.helios_social.core.messaging.data.HeliosConversationList;
import eu.h2020.helios_social.core.messaging.data.HeliosMessagePart;
import eu.h2020.helios_social.core.messaging.data.HeliosTopicContext;
import eu.h2020.helios_social.core.messaging.db.HeliosMessageStore;

public class ContactList {
    private static final String TAG = "ContactList";
    private static final String CONTACT_LIST_SHARED_PREF = "contactlist";
    private static final String CONTACT_LIST_TOPICS_PREF_KEY = "topics";
    private static final String CONTACT_LIST_TOPIC_CONTEXTS_PREF_KEY = "topic-contexts";
    private static final String CONTACT_LIST_USER_CONTEXTS_PREF_KEY = "user-contexts";
    private static final ContactList ourInstance = new ContactList();
    // Interval after contact is considered offline (ms)
    private static final long mExpireInterval = 21600000;
    private static final ArrayList<HeliosTopicContext> mTopics = new ArrayList<>();
    private Context mContext;
    private ContextualEgoNetwork mEgoNetwork;
    private final Hashtable<String, Hashtable<String, Long>> mTopicContacts = new Hashtable<>();
    private final Hashtable<String, Pair<Long, String>> mContacts = new Hashtable<>();
    private final Hashtable<String, List<String>> mTopicContexts = new Hashtable<>();
    private final Hashtable<String, List<String>> mUserContexts = new Hashtable<>();
    private final List<String> mOnline = new ArrayList<>();
    private HeliosMessageStore mChatMessageStore;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEdit;

    private ContactList() {
        Log.d(TAG, "ContactList()");
    }

    public static ContactList getInstance() {
        return ourInstance;
    }

    public void loadTopics(Context ctx, ContextualEgoNetwork egoNetwork) {
        Log.d(TAG, "loadContacts()");
        mContext = ctx;
        mEgoNetwork = egoNetwork;
        mChatMessageStore = new HeliosMessageStore(ctx);
        Gson gson = new Gson();
        mPrefs = mContext.getSharedPreferences(CONTACT_LIST_SHARED_PREF, Context.MODE_PRIVATE);
        mEdit = mPrefs.edit();
        String topicsStr = mPrefs.getString(CONTACT_LIST_TOPICS_PREF_KEY, null);
        if (topicsStr != null) {
            Type type = new TypeToken<ArrayList<HeliosTopicContext>>() {}.getType();
            mTopics.clear();
            mTopics.addAll(gson.fromJson(topicsStr, type));
            // Create contact lists from stored messages
            updateStored();
        }
        String topicContextsStr = mPrefs.getString(CONTACT_LIST_TOPIC_CONTEXTS_PREF_KEY, null);
        if (topicContextsStr != null) {
            Type type = new TypeToken<Hashtable<String, List<String>>>() {}.getType();
            mTopicContexts.putAll(gson.fromJson(topicContextsStr, type));
        }
        String userContextsStr = mPrefs.getString(CONTACT_LIST_USER_CONTEXTS_PREF_KEY, null);
        if (userContextsStr != null) {
            Type type = new TypeToken<Hashtable<String, List<String>>>() {}.getType();
            mUserContexts.putAll(gson.fromJson(userContextsStr, type));
            // Add contacts as alters to CEN.
            if(mEgoNetwork != null) {
                for (String contactId : mUserContexts.keySet())
                    for (String contextId : mUserContexts.get(contactId)) {
                        // Add context too, if it does not exist
                        if (!CenUtils.hasContext(mEgoNetwork, contextId))
                            CenUtils.addContext(mEgoNetwork, contextId);
                        CenUtils.addAlter(mEgoNetwork, contactId, contextId);
                    }
            }
        }
    }

    public void storeTopics() {
        Log.d(TAG, "storeContacts()");
        Gson gson = new Gson();
        String topicsStr = gson.toJson(mTopics);
        mEdit.putString(CONTACT_LIST_TOPICS_PREF_KEY, topicsStr);
        String topicContextsStr = gson.toJson(mTopicContexts);
        mEdit.putString(CONTACT_LIST_TOPIC_CONTEXTS_PREF_KEY, topicContextsStr);
        String userContextsStr = gson.toJson(mUserContexts);
        mEdit.putString(CONTACT_LIST_USER_CONTEXTS_PREF_KEY, userContextsStr);
        mEdit.apply();
    }

    public void updateStored() {
        for (HeliosTopicContext htc: mTopics) {
            ArrayList<HeliosMessagePart> cmsgs = mChatMessageStore.loadMessages(htc.topic);
            for (HeliosMessagePart msg: cmsgs)
                update(htc.topic, msg, false);
            HeliosConversation hc = HeliosConversationList.getInstance().getConversation(htc.topic);
            if (hc != null) {
                List<HeliosMessagePart> hmsgs = hc.getMessagesAfter(0);
                for (HeliosMessagePart msg: hmsgs)
                    update(htc.topic, msg, false);
            }
        }
    }

    private int findTopic(String topicName) {
        int i;
        for (i = 0; i < mTopics.size(); i++)
            if (mTopics.get(i).topic.equals(topicName))
                return i;
        return -1;
    }

    public void addTopic(String topicName, String senderUUID, String lastMsg, String participants, String ts) {
        if (findTopic(topicName) == -1) {
            HeliosTopicContext topic = new HeliosTopicContext(topicName, lastMsg, participants, ts);
            if (senderUUID != null)
                topic.uuid = senderUUID;
            mTopics.add(topic);
            storeTopics();
        }
        // Check previous messages
        ArrayList<HeliosMessagePart> cmsgs = mChatMessageStore.loadMessages(topicName);
        for (HeliosMessagePart msg: cmsgs)
            update(topicName, msg, false);
        HeliosConversation hc = HeliosConversationList.getInstance().getConversation(topicName);
        if (hc != null) {
            List<HeliosMessagePart> hmsgs = hc.getMessagesAfter(0);
            for (HeliosMessagePart msg: hmsgs)
                update(topicName, msg, false);
        }
    }

    public void addTopic(String topicName) {
        addTopic(topicName, null, "-", "-", "-");
    }

    public void delTopic(String topicName) {
        int i = findTopic(topicName);
        if (i == -1)
            return;             // Not found
        mTopics.remove(i);
        storeTopics();
    }
    
    public ArrayList<HeliosTopicContext> getTopics() {
        return mTopics;
    }

    public void update(String topicName, HeliosMessagePart msg, Boolean newMsg) {
        if (msg.senderNetworkId == null)
            return;             // Faulty message
        if (!mTopicContacts.containsKey(topicName))
            mTopicContacts.put(topicName, new Hashtable<>());
        String contactId = msg.senderName + ":" + msg.senderNetworkId;
        Long ts = msg.getTimestampAsMilliseconds();
        // Update timestamps
        if (mTopicContacts.get(topicName).containsKey(contactId)) {
            long oldTs = mTopicContacts.get(topicName).get(contactId);
            if (ts > oldTs)
                mTopicContacts.get(topicName).put(contactId, ts);
        } else
            mTopicContacts.get(topicName).put(contactId, ts);
        if (mContacts.containsKey(contactId)) {
            Long oldTs = mContacts.get(contactId).first;
            if (ts > oldTs)
                mContacts.put(contactId, new Pair<>(ts, msg.senderUUID));
        } else
            mContacts.put(contactId, new Pair<>(ts, msg.senderUUID));
        if (newMsg && !mOnline.contains(contactId))
            mOnline.add(contactId);
        // TODO: update mTopics
    }

    public LinkedList<Pair<String, Long>> getContacts() {
        long tnow = System.currentTimeMillis();
        LinkedList<Pair<String, Long>> list = new LinkedList<>();
        Enumeration<String> e = mContacts.keys();
        while (e.hasMoreElements()) {
            String contactId = e.nextElement();
            list.add(new Pair<>(contactId, mContacts.get(contactId).first));
            // Put contact offline, if last message is older than mExpireInterval
            if (mOnline.contains(contactId)
                && tnow - mContacts.get(contactId).first > mExpireInterval)
                mOnline.remove(contactId);
        }
        // Sort list alphabetically by nickname
        Collections.sort(list, new Comparator<Pair<String, Long>>() {
                public int compare(Pair<String, Long> item1, Pair<String, Long> item2) {
                    return getName(item1.first).compareToIgnoreCase(getName(item2.first));
                }
            });
        return list;
    }

    public LinkedList<Pair<String, Long>> getTopicContacts(String topicName) {
        long tnow = System.currentTimeMillis();
        LinkedList<Pair<String, Long>> list = new LinkedList<>();
        if (mTopicContacts.containsKey(topicName)) {
            Enumeration<String> e = mTopicContacts.get(topicName).keys();
            while (e.hasMoreElements()) {
                String contactId = e.nextElement();
                list.add(new Pair<>(contactId, mTopicContacts.get(topicName).get(contactId)));
                // Put contact offline, if last message is older than mExpireInterval
                if (mOnline.contains(contactId)
                    && tnow - mContacts.get(contactId).first > mExpireInterval)
                    mOnline.remove(contactId);
            }
            // Sort list alphabetically by nickname
            Collections.sort(list, new Comparator<Pair<String, Long>>() {
                    public int compare(Pair<String, Long> item1, Pair<String, Long> item2) {
                        return getName(item1.first).compareToIgnoreCase(getName(item2.first));
                    }
                });
        }
        return list;
    }

    public String getName(String contactId) {
        int i = contactId.indexOf(':');
        if (i > -1)
            return contactId.substring(0, i);
        return "";
    }

    public String getNetworkId(String contactId) {
        int i = contactId.indexOf(':');
        if (i > -1)
            return contactId.substring(i+1);
        return "";
    }

    public String getUUID(String contactId) {
        if (mContacts.containsKey(contactId))
            return mContacts.get(contactId).second;
        return "";
    }

    public long getLastTimestamp(String contactId) {
        if (mContacts.containsKey(contactId))
            return mContacts.get(contactId).first;
        return 0;
    }

    public long getLastTopicTimestamp(String topicName, String contactId) {
        if (mTopicContacts.containsKey(topicName)
            && mTopicContacts.get(topicName).containsKey(contactId))
            return mTopicContacts.get(topicName).get(contactId);
        return 0;
    }

    public Boolean online(String contactId) {
        return (mOnline.contains(contactId)) ? true : false;
    }

    public void addContextToTopic(String topicName, String contextId) {
        if (!mTopicContexts.containsKey(topicName))
            mTopicContexts.put(topicName, new ArrayList<>());
        if (!mTopicContexts.get(topicName).contains(contextId)) {
            mTopicContexts.get(topicName).add(contextId);
            storeTopics();
        }
    }

    public void removeContextFromTopic(String topicName, String contextId) {
        if (!mTopicContexts.containsKey(topicName) ||
            !mTopicContexts.get(topicName).contains(contextId))
            return;             // Not found
        mTopicContexts.get(topicName).remove(contextId);
        storeTopics();
    }

    public void addContextToContact(String contactId, String contextId) {
        if (!mUserContexts.containsKey(contactId))
            mUserContexts.put(contactId, new ArrayList<>());
        if (!mUserContexts.get(contactId).contains(contextId)) {
            mUserContexts.get(contactId).add(contextId);
            // Add context too, if it does not exist
            if(mEgoNetwork != null) {
                if (!CenUtils.hasContext(mEgoNetwork, contextId))
                    CenUtils.addContext(mEgoNetwork, contextId);
                CenUtils.addAlter(mEgoNetwork, contactId, contextId);
            }
            storeTopics();
        }
    }

    public void removeContextFromContact(String contactId, String contextId) {
        if (!mUserContexts.containsKey(contactId) ||
            !mUserContexts.get(contactId).contains(contextId))
            return;
        mUserContexts.get(contactId).remove(contextId);
        CenUtils.removeAlter(mEgoNetwork, contactId, contextId);
        storeTopics();
    }

    public List<String> getTopicContexts(String topicName) {
        List<String> list = new ArrayList<>();
        if (mTopicContexts.containsKey(topicName))
            list.addAll(mTopicContexts.get(topicName));
        return list;
    }

    public List<String> getContactContexts(String contactId) {
        List<String> list = new ArrayList<>();
        if (mUserContexts.containsKey(contactId))
            list.addAll(mUserContexts.get(contactId));
        return list;
    }

    public List<String> getTopicAndContactContexts(String topicName, String contactId) {
        List<String> list = new ArrayList<>();
        if (mTopicContexts.containsKey(topicName))
            list.addAll(mTopicContexts.get(topicName));
        if (mUserContexts.containsKey(contactId)) {
            if (list.isEmpty())
                list.addAll(mUserContexts.get(contactId));
            else for (String contextId: mUserContexts.get(contactId))
                     // Leave out duplicates
                     if(!list.contains(contextId))
                         list.add(contextId);
        }
        return list;
    }
}
