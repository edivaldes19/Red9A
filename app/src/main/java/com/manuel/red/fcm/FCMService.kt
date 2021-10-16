package com.manuel.red.fcm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.manuel.red.R
import com.manuel.red.requested_contract.RequestedContractActivity
import com.manuel.red.utils.Constants

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        registerNewTokenLocal(newToken)
    }

    private fun registerNewTokenLocal(newToken: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.edit {
            putString(Constants.PROP_TOKEN, newToken).apply()
        }
        Log.i("New token", newToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage.data.isNotEmpty()) {
            sendNotificationByData(remoteMessage.data)
        }
        remoteMessage.notification?.let { notification ->
            val imagePath = notification.imageUrl
            if (imagePath == null) {
                sendNotification(notification)
            } else {
                Glide.with(applicationContext).asBitmap().load(imagePath)
                    .into(object : CustomTarget<Bitmap?>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?
                        ) {
                            sendNotification(notification, resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun sendNotification(notification: RemoteMessage.Notification, bitmap: Bitmap? = null) {
        val intent = Intent(this, RequestedContractActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = getString(R.string.notification_channel_id_default)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setColor(ContextCompat.getColor(this, R.color.orange_a400))
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
        bitmap?.let {
            notificationBuilder.setLargeIcon(bitmap).setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null)
            )
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name_default),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun sendNotificationByData(data: Map<String, String>) {
        val intent = Intent(this, RequestedContractActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = getString(R.string.notification_channel_id_default)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(data[Constants.PROP_TITLE])
                .setContentText(data[Constants.PROP_BODY]).setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setColor(ContextCompat.getColor(this, R.color.orange_a400))
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(data[Constants.PROP_BODY]))
        val actionIntent = data[Constants.ACTION_INTENT]?.toInt()
        val requestedContractId = data[Constants.PROP_ID]
        val status = data[Constants.PROP_STATUS]?.toInt()
        val contractStatusIntent = Intent(this, RequestedContractActivity::class.java).apply {
            putExtra(Constants.ACTION_INTENT, actionIntent)
            putExtra(Constants.PROP_ID, requestedContractId)
            putExtra(Constants.PROP_STATUS, status)
        }
        val contractStatusPendingIntent =
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                contractStatusIntent,
                0
            )
        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_check_box,
            getString(R.string.view_status),
            contractStatusPendingIntent
        ).build()
        notificationBuilder.addAction(action)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name_default),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
    }
}