package com.android.internal.policy.impl;

import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;

import java.io.InputStream;

public class SmsCallWidgetHelper {

    private static final Uri CALL_CONTENT_URI = Uri.parse("content://call_log");
    private static final Uri CALL_LOG_CONTENT_URI = Uri.withAppendedPath(CALL_CONTENT_URI, "calls");
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    private static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
    private static final String CALLER_ID = "_id";
    private static final String CALL_LOG_MISSED = "type";
    private static final String MISSED_NEW = "new";
    private static final String MISSED_CONDITION = CALL_LOG_MISSED + "=3 AND " + MISSED_NEW + "=1";

    private static final String SMS_ID = "_id";
    private static final String SMS_READ_COLUMN = "read";
    private static final String UNREAD_CONDITION = SMS_READ_COLUMN + "=0";
    
    public static int getUnreadSmsCount(Context context) {
        int count = 0;
        Cursor cursor = context.getContentResolver().query(
            SMS_INBOX_CONTENT_URI,
            new String[] { SMS_ID },
            UNREAD_CONDITION, null, null);
        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    public static int getMissedCallCount(Context context) {
        int count = 0;
        Cursor cursor = context.getContentResolver().query(
            CALL_LOG_CONTENT_URI,
            new String[] { CALLER_ID },
            MISSED_CONDITION, null, null);
        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }
        return count;
    }

    public static String getName(Context context, String callNumber) {
        String caller = null;
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                callNumber); 
        Cursor cursor = context.getContentResolver().query(
                uri, new String[] { PhoneLookup.DISPLAY_NAME },
                null, null, null);
        String[] contacts = new String[] { PhoneLookup.DISPLAY_NAME };
        if (cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(contacts[0]);
            caller = cursor.getString(nameIndex);
        }
        if (caller == null) {
            caller = callNumber;
        }
        if (cursor != null) {
            cursor.close();
        }
        return caller;
    }

    public static String getSmsNumber(Context context) {
        String smsnumber = null;
        Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                new String[] { "address" },
                "read = 0", null, null);
        if (cursor.moveToFirst()) {
            smsnumber = cursor.getString(cursor.getColumnIndex("address"));
        }
        if (cursor != null) {
            cursor.close();
        }
        return smsnumber;
    }

    public static String getSmsBody(Context context) {
        String msg = null;
        Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                new String[] { "body" },
                null, null, null);
        if (cursor.moveToFirst()) {
            msg = cursor.getString(cursor.getColumnIndex("body"));
        }
        if (cursor != null) {
            cursor.close();
        }
        return msg;
    }

    public static long getSmsId(Context context) {
        long messageId = 0;
        Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                new String[] { "_id" },
                null, null, null);
        if (cursor.moveToFirst()) {
            messageId = cursor.getLong(cursor.getColumnIndex("_id"));
        }
        if (cursor != null) {
            cursor.close();
        }
        return messageId;
    }

    public static String getDate(Context context, int msgType) {
        long date = 0;
        String formattedDate;
        if (msgType == 0) {
            Cursor cursor = context.getContentResolver().query(
                SMS_INBOX_CONTENT_URI,
                new String[] { "date" },
                null, null, null);
            if (cursor.moveToFirst()) {
                date = cursor.getLong(cursor.getColumnIndex("date"));
            }
            if (cursor != null) {
                cursor.close();
            }
        } else if (msgType == 1) {
            Cursor cursor = context.getContentResolver().query(
                    Calls.CONTENT_URI,
                    new String[] { Calls.DATE, Calls.TYPE },
                    null, null,
                    Calls.DATE + " DESC");
            if (cursor.moveToFirst()) {
                boolean missed = cursor.getInt(cursor.getColumnIndex(
                    Calls.TYPE)) == Calls.MISSED_TYPE;
                if (missed) {
                    date = cursor.getLong(cursor.getColumnIndex(Calls.DATE));
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        formattedDate = new java.text.SimpleDateFormat("EEE MMM dd HH:mm aa")
                .format(new java.util.Date(date));
        return formattedDate;
    }

    public static String getCallNumber(Context context) {
        String callnumber = null;
        Cursor cursor = context.getContentResolver().query(
                Calls.CONTENT_URI,
                new String[] { Calls.NUMBER, Calls.DATE, Calls.TYPE },
                        null, null,
                        Calls.DATE + " DESC");
        if (cursor.moveToFirst()) {
            boolean missed = cursor.getInt(cursor.getColumnIndex(
                    Calls.TYPE)) == Calls.MISSED_TYPE;
            if (missed) {
                callnumber = cursor.getString(cursor.getColumnIndex(
                        Calls.NUMBER));
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return callnumber;
    }

    public static Bitmap getContactPicture(Context context, String callNumber) {
        Bitmap bitmap = null;
        Bitmap scaledBitmap = null;
        Cursor cursor = context.getContentResolver().query(
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.decode(callNumber)),
                new String[] { PhoneLookup._ID },
                null, null, null);
        if (cursor.moveToFirst()) {
            long contactId = cursor.getLong(0);
            InputStream inputStream = Contacts.openContactPhotoInputStream(
                    context.getContentResolver(),
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId));
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return scaledBitmap;
    }
}
