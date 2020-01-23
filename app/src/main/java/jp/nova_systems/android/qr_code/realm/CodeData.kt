package jp.nova_systems.android.qr_code.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class CodeData: RealmObject() {
    @PrimaryKey lateinit var uuid: String
    @Required lateinit var format: String
    @Required lateinit var data: String
    @Required lateinit var time: String
}