package jp.nova_systems.android.qr_code.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.files.folderChooser
import com.google.android.material.snackbar.Snackbar
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import io.realm.Realm
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.dialog.ProgressSnackBar
import jp.nova_systems.android.qr_code.realm.CodeData
import java.io.*
import java.time.LocalDateTime
import java.util.*

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.setting, SettingPreference())
            .commit()

        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    class SettingPreference : PreferenceFragmentCompat() {
        private lateinit var realm: Realm

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_setting, rootKey)
            val rootView = activity!!.findViewById<ConstraintLayout>(R.id.container)

            realm = Realm.getDefaultInstance()

            val dataExport = findPreference<PreferenceScreen>("data_export")
            dataExport?.setOnPreferenceClickListener {
                val data = realm.where(CodeData::class.java).findAll()
                if (data.size == 0) {
                    Snackbar.make(rootView, "エクスポートが可能なデータが見つかりませんでした", Snackbar.LENGTH_SHORT).show()
                } else {
                    MaterialDialog(activity!!).show {
                        folderChooser { _, file ->
                            val snackBar = ProgressSnackBar(activity!!, "エクスポートしています…").create()
                            snackBar.show()
                            val resultMsg = if (onCsvExport(file.absolutePath)) {
                                "データをエクスポートしました"
                            } else {
                                "データのエクスポートに失敗しました"
                            }
                            snackBar.dismiss()
                            Snackbar.make(rootView, resultMsg, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                return@setOnPreferenceClickListener true
            }

            val dataImport = findPreference<PreferenceScreen>("data_import")
            dataImport?.setOnPreferenceClickListener {
                MaterialDialog(activity!!).show {
                    val filter: FileFilter = { it.isDirectory || it.extension.startsWith("csv", true) }
                    fileChooser(filter = filter) { dialog, file ->
                        val snackBar = ProgressSnackBar(activity!!, "インポートしています…").create()
                        snackBar.show()
                        val resultMsg = if (onCsvImport(file.absolutePath)) {
                            "データをエクスポートしました"
                        } else {
                            "データのエクスポートに失敗しました"
                        }
                        snackBar.dismiss()
                        Snackbar.make(rootView, resultMsg, Snackbar.LENGTH_SHORT).show()
                    }
                }
                return@setOnPreferenceClickListener true
            }

            val dataDelete = findPreference<PreferenceScreen>("data_delete")
            dataDelete?.setOnPreferenceClickListener {
                val data = realm.where(CodeData::class.java).findAll()
                if (data.size == 0) {
                    Snackbar.make(rootView, "データが見つかりません", Snackbar.LENGTH_SHORT).show()
                } else {
                    BottomSheetMaterialDialog.Builder(activity!!).apply {
                        setTitle("データ削除")
                        setMessage("すべての読み取り履歴を削除しますか？")
                        setCancelable(true)
                        setPositiveButton("削除", R.drawable.icon_delete) { dialog, _ ->
                            realm.executeTransaction {
                                data.deleteAllFromRealm()
                            }
                            Snackbar.make(rootView, "削除しました", Snackbar.LENGTH_SHORT).show()

                            dialog.dismiss()
                        }
                        setNegativeButton("キャンセル", R.drawable.icon_close) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }.build().show()
                }
                return@setOnPreferenceClickListener true
            }
        }

        private fun onCsvExport(path: String): Boolean {
            return try {
                val time = LocalDateTime.now()
                val fileName = "qr_code_data_${time.year}${time.monthValue}${time.dayOfMonth}-${time.hour}${time.minute}${time.second}.csv"

                val fWriter = FileWriter("$path/$fileName", false)
                val pWriter = PrintWriter(BufferedWriter(fWriter))

                pWriter.print("uuid,")
                pWriter.print("format,")
                pWriter.print("data,")
                pWriter.print("time")
                pWriter.println()

                val data = realm.where(CodeData::class.java).findAll()
                for (d in data) {
                    pWriter.print(d.uuid + ",")
                    pWriter.print(d.format + ",")
                    pWriter.print(d.data.replace("\n", " ") + ",")
                    pWriter.print(d.time)
                    pWriter.println()
                }

                pWriter.close()

                true
            } catch (e: Exception) {
                false
            }
        }

        private fun onCsvImport(path: String): Boolean {
            return try {
                val sReader = InputStreamReader(FileInputStream(path), "UTF-8")
                val bReader = BufferedReader(sReader)

                bReader.readLine()
                bReader.forEachLine { line ->
                    println(line)
                    val list = line.split(",").dropLastWhile { it.isEmpty() }
                    realm.executeTransaction { realm ->
                        realm.createObject(CodeData::class.java, UUID.randomUUID().toString()).apply {
                            format = list[1]
                            data = list[2]
                            time = list[3]
                        }
                    }
                }
                bReader.close()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
