package com.zegoggles.smssync.mail;

import com.zegoggles.smssync.R;

import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_SMS;

public enum DataType {
    SMS     (R.string.sms,     R.string.sms_with_field,  PreferenceKeys.IMAP_FOLDER,         Defaults.SMS_FOLDER,     PreferenceKeys.BACKUP_SMS,      Defaults.SMS_BACKUP_ENABLED,     PreferenceKeys.RESTORE_SMS,     Defaults.SMS_RESTORE_ENABLED,     PreferenceKeys.MAX_SYNCED_DATE_SMS, new String[]{READ_SMS, READ_CONTACTS}),
    MMS     (R.string.mms,     R.string.mms_with_field,  PreferenceKeys.IMAP_FOLDER,         Defaults.SMS_FOLDER,     PreferenceKeys.BACKUP_MMS,      Defaults.MMS_BACKUP_ENABLED,     null,                           Defaults.MMS_RESTORE_ENABLED,     PreferenceKeys.MAX_SYNCED_DATE_MMS, new String[]{READ_SMS, READ_CONTACTS}),
    CALLLOG (R.string.calllog, R.string.call_with_field, PreferenceKeys.IMAP_FOLDER_CALLLOG, Defaults.CALLLOG_FOLDER, PreferenceKeys.BACKUP_CALLLOG,  Defaults.CALLLOG_BACKUP_ENABLED, PreferenceKeys.RESTORE_CALLLOG, Defaults.CALLLOG_RESTORE_ENABLED, PreferenceKeys.MAX_SYNCED_DATE_CALLLOG, new String[]{READ_CONTACTS, READ_CALL_LOG});

    public final int resId;
    public final int withField;
    public final String backupEnabledPreference;
    public final String restoreEnabledPreference;
    public final String folderPreference;
    public final String defaultFolder;
    public final boolean backupEnabledByDefault;
    public final boolean restoreEnabledByDefault;
    public final String maxSyncedPreference;
    public final String[] requiredPermissions;

    DataType(int resId,
             int withField,
             String folderPreference,
             String defaultFolder,
             String backupEnabledPreference,
             boolean backupEnabledByDefault,
             String restoreEnabledPreference,
             boolean restoreEnabledByDefault,
             String maxSyncedPreference,
             String[] requiredPermissions) {
        this.resId = resId;
        this.withField = withField;
        this.folderPreference = folderPreference;
        this.defaultFolder = defaultFolder;
        this.backupEnabledPreference = backupEnabledPreference;
        this.backupEnabledByDefault = backupEnabledByDefault;
        this.restoreEnabledPreference = restoreEnabledPreference;
        this.restoreEnabledByDefault = restoreEnabledByDefault;
        this.maxSyncedPreference = maxSyncedPreference;
        this.requiredPermissions = requiredPermissions;
    }

    public static class PreferenceKeys {
        static final String IMAP_FOLDER = "imap_folder";
        static final String IMAP_FOLDER_CALLLOG = "imap_folder_calllog";

        static final String BACKUP_SMS = "backup_sms";
        static final String BACKUP_MMS = "backup_mms";
        static final String BACKUP_CALLLOG = "backup_calllog";

        static final String RESTORE_SMS = "restore_sms";
        static final String RESTORE_CALLLOG = "restore_calllog";

        static final String MAX_SYNCED_DATE_SMS = "max_synced_date";
        static final String MAX_SYNCED_DATE_MMS = "max_synced_date_mms";
        static final String MAX_SYNCED_DATE_CALLLOG = "max_synced_date_calllog";

        private PreferenceKeys() {}
    }

    /**
     * Defaults for various settings
     */
    public static class Defaults {
        public static final long   MAX_SYNCED_DATE = -1;
        static final String SMS_FOLDER     = "SMS";
        static final String CALLLOG_FOLDER = "Call log";

        static final boolean SMS_BACKUP_ENABLED       = true;
        static final boolean MMS_BACKUP_ENABLED       = true;
        static final boolean CALLLOG_BACKUP_ENABLED   = false;

        static final boolean SMS_RESTORE_ENABLED      = true;
        static final boolean MMS_RESTORE_ENABLED      = false;
        static final boolean CALLLOG_RESTORE_ENABLED  = true;

        private Defaults() {}
    }
}
