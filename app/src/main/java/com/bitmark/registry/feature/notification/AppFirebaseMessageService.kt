package com.bitmark.registry.feature.notification

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.bitmark.registry.R
import com.bitmark.registry.feature.main.MainActivity
import com.bitmark.registry.util.extension.getResIdentifier
import com.bitmark.registry.util.extension.toStringArray
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject


/**
 * @author Hieu Pham
 * @since 7/4/19
 * Email: hieupham@bitmark.com
 * Copyright © 2019 Bitmark. All rights reserved.
 */
class AppFirebaseMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        if (isApplicationInForeground()) return

        val remoteNotification = remoteMessage?.notification
        val title = remoteNotification?.title
        val body = remoteNotification?.body
        val icon = remoteNotification?.icon

        val bundle = Bundle()
        if (title != null) bundle.putString("title", title)
        if (body != null) bundle.putString("message", body)
        if (icon != null) bundle.putString("icon", icon)

        val entries = remoteMessage?.data?.entries ?: return

        for (entry in entries) {
            bundle.putString(entry.key, entry.value)
        }

        val notification = try {
            JSONObject(bundle.getString("notification_payload", ""))
        } catch (e: Throwable) {
            null
        }

        if (notification != null) {
            if (notification.has("body_loc_key")) {
                try {
                    val locKey = notification.optString("body_loc_key")
                    val locArgs =
                        JSONArray(notification.optString("body_loc_args")).toStringArray()
                    bundle.putString(
                        "message",
                        if (locArgs == null) getLocMessage(locKey) else getLocMessage(
                            locKey
                        ).format(*locArgs)
                    )

                } catch (ignore: Throwable) {
                }
            }

            val keys = notification.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                when {
                    key.equals(
                        "body",
                        ignoreCase = true
                    ) -> bundle.putString(
                        "message",
                        notification.optString(key)
                    )

                    key.equals("icon", ignoreCase = true) -> {
                        bundle.putString("icon", notification.optString(key))
                    }

                    else -> bundle.putString(key, notification.optString(key))
                }
                keys.remove()
            }

        }

        val data = try {
            JSONObject(bundle.getString("data", ""))
        } catch (e: Throwable) {
            null
        }

        if (data != null) {
            if (!bundle.containsKey("message")) {
                bundle.putString("message", data.optString("alert", null))
            }
            if (!bundle.containsKey("title")) {
                bundle.putString("title", data.optString("title", null))
            }
            if (!bundle.containsKey("sound")) {
                bundle.putString("soundName", data.optString("sound", null))
            }
            if (!bundle.containsKey("color")) {
                bundle.putString("color", data.optString("color", null))
            }

            val badge = data.optInt("badge", -1)
            if (badge > -1) {
                bundle.putInt("badge", badge)
            }
        }

        sendNotification(bundle)

    }

    private fun isApplicationInForeground(): Boolean {
        val activityManager =
            this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = activityManager.runningAppProcesses
        if (processes != null) {
            for (process in processes) {
                if (process.processName == application.packageName) {
                    if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        for (d in process.pkgList) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun getLocMessage(locKey: String): String {
        val stringRes = when (locKey) {
            "notification_intercom_new_messages" -> R.string.notification_intercom_new_messages
            "notification_tracking_transfer_confirmed" -> R.string.notification_tracking_transfer_confirmed
            "notification_transfer_confirmed_sender" -> R.string.notification_transfer_confirmed_sender
            "notification_transfer_confirmed_receiver" -> R.string.notification_transfer_confirmed_receiver
            "notification_transfer_request" -> R.string.notification_transfer_request
            "notification_transfer_failed" -> R.string.notification_transfer_failed
            "notification_transfer_rejected" -> R.string.notification_transfer_rejected
            "notification_transfer_accepted" -> R.string.notification_transfer_accepted
            "notification_claim_request" -> R.string.notification_claim_request
            "notification_claim_request_rejected" -> R.string.notification_claim_request_rejected
            "notification_ifttt_new_issue" -> R.string.notification_ifttt_new_issue
            else -> -1
        }
        return if (stringRes != -1) applicationContext.getString(stringRes) else ""
    }

    private fun sendNotification(bundle: Bundle) {

        val context = applicationContext
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("notification", bundle)
        intent.putExtra("direct_from_notification", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelName = getString(R.string.notification_channel_name)
        val notificationBuilder = NotificationCompat.Builder(this, channelName)
            .setContentTitle(bundle.getString("title", ""))
            .setContentText(bundle.getString("message"))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bundle.getString("message"))
            )

        val icon =
            context.getResIdentifier(bundle.getString("icon", ""), "drawable")
        notificationBuilder.setSmallIcon(if (icon != null && icon > 0) icon else R.mipmap.ic_notification)
        notificationBuilder.setLargeIcon(
            BitmapFactory.decodeResource(
                context.resources,
                if (icon != null && icon > 0) icon else R.mipmap.ic_notification
            )
        )

        val color = try {
            Color.parseColor(bundle.getString("color", ""))
        } catch (e: Throwable) {
            null
        }
        if (color != null) notificationBuilder.color = color

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelName,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

}