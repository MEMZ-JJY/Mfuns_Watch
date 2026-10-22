package net.mfuns.watch

import android.app.Application
import android.content.Context
import net.mfuns.watch.api.ApiClient
import net.mfuns.watch.api.MfunsRepository
import net.mfuns.watch.data.SessionStore

object ServiceLocator {
    lateinit var api: ApiClient
        private set
    lateinit var sessionStore: SessionStore
        private set
    lateinit var repository: MfunsRepository
        private set

    fun init(context: Context) {
        api = ApiClient()
        sessionStore = SessionStore(context)
        repository = MfunsRepository(api)
    }
}

class MfunsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
