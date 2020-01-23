package jp.nova_systems.android.qr_code.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.realm.Realm
import io.realm.kotlin.createObject
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.Setting
import java.util.*

class SettingFragment : Fragment() {
    private lateinit var realm: Realm

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        realm = Realm.getDefaultInstance()

        val setting = realm.where(Setting::class.java)
        if (setting == null) {
            realm.createObject(Setting::class.java, UUID.randomUUID())
        }

        return inflater.inflate(R.layout.fragment_setting, container, false)
    }
}
