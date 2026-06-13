package com.orbit.mobile

import android.app.Application
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.BaseUrlProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

// App entry
@HiltAndroidApp
class OrbitApp : Application() {

    @Inject
    lateinit var session: SessionManager

    @Inject
    lateinit var baseUrlProvider: BaseUrlProvider

    override fun onCreate() {
        super.onCreate()
        // Hydrate prefs
        runBlocking {
            session.restore()
            baseUrlProvider.restore()
        }
    }
}
