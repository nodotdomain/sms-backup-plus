package com.zegoggles.smssync.mail;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.mail.MessageConverter.CONTENT_URI;

public class PersonLookup {
    private static final String[] PHONE_PROJECTION = getPhoneProjection();
    private static final Uri CONTENT_FILTER_URI =
            Uri.parse("content://com.android.contacts/phone_lookup");

    private static final int MAX_PEOPLE_CACHE_SIZE = 500;

    // simple LRU cache
    private final Map<String, PersonRecord> mPeopleCache =
            new LinkedHashMap<String, PersonRecord>(MAX_PEOPLE_CACHE_SIZE + 1, .75F, true) {
                @Override
                public boolean removeEldestEntry(Map.Entry<String, PersonRecord> eldest) {
                    return size() > MAX_PEOPLE_CACHE_SIZE;
                }
            };

    private final ContentResolver mResolver;

    public PersonLookup(ContentResolver resolver) {
        mResolver = resolver;
    }

    /**
     * Look up a person
     * @throws SecurityException if the caller does not hold READ_CONTACTS
     */
    public @NonNull PersonRecord lookupPerson(final String address) {
        if (TextUtils.isEmpty(address)) {
            return new PersonRecord(0, null, null, "-1");
        } else if (!mPeopleCache.containsKey(address)) {
            Uri personUri = Uri.withAppendedPath(CONTENT_FILTER_URI, Uri.encode(address));

            Cursor c = mResolver.query(personUri, PHONE_PROJECTION, null, null, null);
            final PersonRecord record;
            if (c != null && c.moveToFirst()) {
                long id = c.getLong(c.getColumnIndex(PHONE_PROJECTION[0]));

                record = new PersonRecord(
                    id,
                    c.getString(c.getColumnIndex(PHONE_PROJECTION[1])),
                    getPrimaryEmail(id),
                        address
                );

            } else {
                if (LOCAL_LOGV) Log.v(TAG, "Looked up unknown address: " + address);
                record = new PersonRecord(0, null, null, address);
            }
            mPeopleCache.put(address, record);

            if (c != null) c.close();
        }
        return mPeopleCache.get(address);
    }

    private String getPrimaryEmail(final long personId) {
        if (personId <= 0) {
            return null;
        }
        String primaryEmail = null;

        // Get all e-mail addresses for that person.
        Cursor c = mResolver.query(
                CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.DATA},
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[]{String.valueOf(personId)},
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY + " DESC");
        int columnIndex = c != null ? c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA) : -1;

        // Loop over cursor and find a Gmail address for that person.
        // If there is none, pick first e-mail address.
        while (c != null && c.moveToNext()) {
            String e = c.getString(columnIndex);
            if (primaryEmail == null) {
                primaryEmail = e;
            }
            if (isGmailAddress(e)) {
                primaryEmail = e;
                break;
            }
        }

        if (c != null) c.close();
        return primaryEmail;
    }

    // Returns whether the given e-mail address is a Gmail address or not.
    private static boolean isGmailAddress(String email) {
        return email != null &&
                (email.toLowerCase(Locale.ENGLISH).endsWith("gmail.com") ||
                 email.toLowerCase(Locale.ENGLISH).endsWith("googlemail.com"));
    }

    private static String[] getPhoneProjection() {
        return new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
    }
}
