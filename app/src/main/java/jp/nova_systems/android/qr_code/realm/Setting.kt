package jp.nova_systems.android.qr_code.realm

import com.google.zxing.BarcodeFormat
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class Setting: RealmObject() {
    @PrimaryKey lateinit var uuid: String
//    @Required var format: List<BarcodeFormat> = listOf(BarcodeFormat.QR_CODE)
}