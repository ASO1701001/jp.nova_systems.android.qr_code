package jp.nova_systems.android.qr_code.realm

import android.app.Application
import io.realm.Realm

class InitRealm: Application() {
    override fun onCreate() {
        super.onCreate()

        Realm.init(this)
    }
}