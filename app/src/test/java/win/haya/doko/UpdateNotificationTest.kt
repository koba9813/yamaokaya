package win.haya.doko

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UpdateNotificationTest {

    @Test
    fun updateNotificationIsPostedOnUpdateChannel() {
        val context: Context = ApplicationProvider.getApplicationContext()
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        createUpdateNotificationChannel(context)
        sendUpdateNotification(context, "V.beta9.9")

        val manager = context.getSystemService(NotificationManager::class.java)
        val shadowManager = shadowOf(manager)

        // アップデート用チャンネルが作成されている
        assertNotNull(
            "アップデート用チャンネルが作成されること",
            manager.getNotificationChannel(UPDATE_NOTIFICATION_CHANNEL_ID)
        )
        // 通知が投稿されている
        assertTrue("アップデート通知が投稿されること", shadowManager.allNotifications.isNotEmpty())
    }
}
