package cn.neeto.guodelist

import android.app.Application
import android.content.Context
import cn.neeto.guodelist.core.data.GuoStore
import cn.neeto.guodelist.core.net.ApiClient
import cn.neeto.guodelist.core.net.SyncManager
import cn.neeto.guodelist.core.notifications.Notifications
import cn.neeto.guodelist.core.settings.AuthStore
import cn.neeto.guodelist.core.icon.LauncherIcon
import cn.neeto.guodelist.core.settings.SettingsStore

class AppContainer(application: Application) {
    val context: Context = application
    val store = GuoStore(application)
    val settings = SettingsStore(application)
    val auth = AuthStore(application)
    val api = ApiClient()
    val sync = SyncManager(api, store, auth, settings)
}

class GuoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        LauncherIcon.apply(this, container.settings.state.value.iconStyle)
        Notifications.ensureChannel(this)
    }
}
