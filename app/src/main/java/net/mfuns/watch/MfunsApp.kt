package net.mfuns.watch

import android.app.Application
import android.content.Context
import coil.Coil
import coil.ImageLoader
import net.mfuns.watch.api.ApiClient
import net.mfuns.watch.api.MfunsRepository
import net.mfuns.watch.data.SessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient

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
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor(Interceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("User-Agent", ApiClient.USER_AGENT)
                                    .header("Referer", "https://www.mfuns.net/")
                                    .build(),
                            )
                        })
                        .build()
                }
                .build(),
        )
    }
}
