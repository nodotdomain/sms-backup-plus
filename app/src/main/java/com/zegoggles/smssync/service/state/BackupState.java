package com.zegoggles.smssync.service.state;

import android.content.res.Resources;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.service.BackupType;
import com.zegoggles.smssync.service.exception.MissingPermissionException;

import static com.zegoggles.smssync.service.BackupType.UNKNOWN;
import static com.zegoggles.smssync.service.state.SmsSyncState.BACKUP;
import static com.zegoggles.smssync.service.state.SmsSyncState.INITIAL;

public class BackupState extends State {
    public final int currentSyncedItems, itemsToSync;
    public final BackupType backupType;

    public BackupState() {
        this(INITIAL, 0, 0, UNKNOWN, null, null);
    }

    public BackupState(SmsSyncState state,
                       int currentSyncedItems,
                       int itemsToSync,
                       BackupType backupType,
                       DataType dataType,
                       Exception exception) {
        super(state, dataType, exception);
        this.currentSyncedItems = currentSyncedItems;
        this.itemsToSync = itemsToSync;
        this.backupType = backupType;
    }

    @Override public String toString() {
        return "BackupState{" +
                "currentSyncedItems=" + currentSyncedItems +
                ", itemsToSync=" + itemsToSync +
                ", backupType=" + backupType +
                ", exception=" + exception +
                ", state=" + state +
                '}';
    }

    @Override
    public BackupState transition(SmsSyncState newState, Exception exception) {
        return new BackupState(newState, currentSyncedItems, itemsToSync, backupType, dataType, exception);
    }

    @Override
    public String getNotificationLabel(Resources resources) {
        String label = super.getNotificationLabel(resources);
        if (label != null) return label;
        if (state == BACKUP) {
            label = resources.getString(R.string.status_backup_details,
                    currentSyncedItems,
                    itemsToSync);
            if (dataType != null) {
                label += " ("+resources.getString(dataType.resId)+")";
            }
            return label;
        } else {
            return "";
        }
    }

    public String[] getMissingPermissions() {
        if (exception instanceof MissingPermissionException) {
            MissingPermissionException mpe = (MissingPermissionException)exception;
            return mpe.permissions.toArray(new String[mpe.permissions.size()]);
        } else {
            return new String[0];
        }
    }
}
