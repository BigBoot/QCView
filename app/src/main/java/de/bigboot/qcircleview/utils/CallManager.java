package de.bigboot.qcircleview.utils;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import java.util.Date;
import java.util.HashSet;

public class CallManager extends BroadcastReceiver {

    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing
    private static HashSet<CallListener> listeners = new HashSet<>();


    public static void addListener(CallListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(CallListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        }
        else{
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                state = TelephonyManager.CALL_STATE_IDLE;
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                state = TelephonyManager.CALL_STATE_RINGING;
            }


            onCallStateChanged(context, state, number);
        }
    }

    protected void onIncomingCallStarted(Context ctx, String number, Date start){
        for(CallListener listener : listeners) {
            listener.onIncomingCallStarted(ctx, number, start);
        }
    }
    protected void onOutgoingCallStarted(Context ctx, String number, Date start){
        for(CallListener listener : listeners) {
            listener.onOutgoingCallStarted(ctx, number, start);
        }
    }
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end){
        for(CallListener listener : listeners) {
            listener.onIncomingCallEnded(ctx, number, start, end);
        }
    }
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end){
        for(CallListener listener : listeners) {
            listener.onOutgoingCallEnded(ctx, number, start, end);
        }
    }
    protected void onMissedCall(Context ctx, String number, Date start){
        for(CallListener listener : listeners) {
            listener.onMissedCall(ctx, number, start);
        }
    }

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        if(lastState == state){
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallStarted(context, number, callStartTime);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if(lastState != TelephonyManager.CALL_STATE_RINGING){
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);                     
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if(lastState == TelephonyManager.CALL_STATE_RINGING){
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                }
                else if(isIncoming){
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());                       
                }
                else{
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());                                               
                }
                break;
        }
        lastState = state;
    }

    public static long getContactId(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup._ID}, null, null, null);
        if (cursor == null) {
            return -1;
        }
        long contactId = -1;
        if(cursor.moveToFirst()) {
            contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
        }

        if(!cursor.isClosed()) {
            cursor.close();
        }

        return contactId;
    }

    public static String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(!cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    private static Uri getPhotoUriFromID(Context context, long id) {
        try {
            Cursor cur = context.getContentResolver()
                    .query(ContactsContract.Data.CONTENT_URI,
                            null,
                            ContactsContract.Data.CONTACT_ID
                                    + "="
                                    + id
                                    + " AND "
                                    + ContactsContract.Data.MIMETYPE
                                    + "='"
                                    + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                    + "'", null, null);
            if (cur != null) {
                if (!cur.moveToFirst()) {
                    return null; // no photo
                }
                if(!cur.isClosed())
                    cur.close();
            } else {
                return null; // error in cursor process
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Uri person = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, id);
        return Uri.withAppendedPath(person,
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    public static Uri getContactPhoto(Context context, String phoneNumber) {
        long contactId = getContactId(context, phoneNumber);
        Uri uri = getPhotoUriFromID(context, contactId);
        return uri;
    }

    public static interface CallListener {
        public void onIncomingCallStarted(Context ctx, String number, Date start);
        public void onOutgoingCallStarted(Context ctx, String number, Date start);
        public void onIncomingCallEnded(Context ctx, String number, Date start, Date end);
        public void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);
        public void onMissedCall(Context ctx, String number, Date start);
    }

    public static abstract class CallAdapter implements CallListener {
        public void onIncomingCallStarted(Context ctx, String number, Date start) {}
        public void onOutgoingCallStarted(Context ctx, String number, Date start) {}
        public void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {}
        public void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {}
        public void onMissedCall(Context ctx, String number, Date start) {}
    }
}