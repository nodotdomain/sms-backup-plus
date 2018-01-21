/*
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import com.fsck.k9.mail.MessagingException;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.MainActivity;
import com.zegoggles.smssync.mail.BackupImapStore;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.state.State;
import com.zegoggles.smssync.utils.AppLog;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static java.util.Locale.ENGLISH;

public abstract class ServiceBase extends Service {
    @Nullable private PowerManager.WakeLock mWakeLock;
    @Nullable private WifiManager.WifiLock mWifiLock;

    private AppLog appLog;
    @Nullable Notification notification;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (new Preferences(this).isAppLogEnabled()) {
            char[] format;
            try {
                format = DateFormat.getDateFormatOrder(this);
            } catch (IllegalArgumentException e) {
                format = new char[] { 'd' };
            }
            this.appLog = new AppLog(format);
        }
        App.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (appLog != null) appLog.close();
        App.unregister(this);
        notification = null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    public abstract @NonNull State getState();

    public boolean isWorking() {
        return getState().isRunning();
    }

    protected BackupImapStore getBackupImapStore() throws MessagingException {
        final String uri = getAuthPreferences().getStoreUri();
        if (!BackupImapStore.isValidUri(uri)) {
            throw new MessagingException("No valid IMAP URI: "+uri);
        }
        return new BackupImapStore(getApplicationContext(), uri);
    }

    protected AuthPreferences getAuthPreferences() {
        return new AuthPreferences(this);
    }

    protected Preferences getPreferences() {
        return new Preferences(getApplicationContext());
    }

    protected synchronized void acquireLocks() {
        if (mWakeLock == null) {
            PowerManager pMgr = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pMgr.newWakeLock(wakeLockType(), TAG);
        }
        mWakeLock.acquire();

        if (isConnectedViaWifi()) {
            // we have Wifi, lock it
            WifiManager wMgr = getWifiManager();
            if (mWifiLock == null) {
                mWifiLock = wMgr.createWifiLock(getWifiLockType(), TAG);
            }
            mWifiLock.acquire();
        }
    }

    protected int wakeLockType() {
        return PowerManager.PARTIAL_WAKE_LOCK;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private int getWifiLockType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 ?
                WifiManager.WIFI_MODE_FULL_HIGH_PERF : WifiManager.WIFI_MODE_FULL;
    }

    protected synchronized void releaseLocks() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
            mWifiLock = null;
        }
    }

    protected boolean isBackgroundTask() {
        return false;
    }

    protected abstract void handleIntent(final Intent intent);

    protected void appLog(int id, Object... args) {
        final String msg = getString(id, args);
        if (appLog != null) {
            appLog.append(msg);
        } else if (LOCAL_LOGV) {
            Log.d(App.TAG, "AppLog: "+msg);
        }
    }

    protected void appLogDebug(String message, Object... args) {
        if (getPreferences().isAppLogDebug() && appLog != null) {
            appLog.append(String.format(ENGLISH, message, args));
        } else if (LOCAL_LOGV) {
            Log.v(App.TAG, "AppLog: "+String.format(ENGLISH, message, args));
        }
    }

    NotificationManager getNotifier() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }

    protected ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    protected WifiManager getWifiManager() {
        return (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @NonNull NotificationCompat.Builder createNotification(int resId) {
        return new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_notification)
            .setTicker(getString(resId))
            .setWhen(System.currentTimeMillis())
            .setOngoing(true);
    }

    PendingIntent getPendingIntent(@Nullable Bundle extras) {
        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        if (extras != null) {
            intent.putExtras(extras);
        }
         return PendingIntent.getActivity(getApplicationContext(),
                 0,
                 intent,
                 FLAG_UPDATE_CURRENT);
    }

    boolean isConnectedViaWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return isConnectedViaWifi_SDK21();
        } else {
            return isConnectedViaWifi_pre_SDK21();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isConnectedViaWifi_pre_SDK21() {
        WifiManager wifiManager = getWifiManager();
        return (wifiManager != null &&
                wifiManager.isWifiEnabled() &&
                getConnectivityManager().getNetworkInfo(TYPE_WIFI) != null &&
                getConnectivityManager().getNetworkInfo(TYPE_WIFI).isConnected());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean isConnectedViaWifi_SDK21() {
        for (Network network : getConnectivityManager().getAllNetworks()) {
            final NetworkInfo networkInfo = getConnectivityManager().getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == TYPE_WIFI && networkInfo.isConnectedOrConnecting()) {
                return true;
            }
        }
        return false;
    }
}
