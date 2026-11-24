package app.sbandcode.android.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.sbandcode.android.R
import app.sbandcode.android.tr
import app.sbandcode.android.ui.MainActivity

object ChatNotifications {
    const val OPEN_CHAT = "app.sbandcode.android.OPEN_CHAT"
    private const val CHANNEL = "chat-updates"

    fun show(context: Context, session: String, title: String?, permission: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, tr("Обновления чатов", "Chat updates"), NotificationManager.IMPORTANCE_DEFAULT,
        ))
        val intent = Intent(context, MainActivity::class.java).setAction(OPEN_CHAT)
            .setData(Uri.Builder().scheme("sbandcode").authority("chat").appendPath(session).build())
        val open = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(if (permission) tr("Агент ждёт разрешения", "Agent needs permission")
                else tr("Ответ завершён", "Response finished"))
            .setContentText(title?.takeIf { it.isNotBlank() } ?: "SbandCode")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(session, 2, notification) }
    }
}
