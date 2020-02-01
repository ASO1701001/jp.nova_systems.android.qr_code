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
import org.apache.commons.lang3.RandomStringUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.setting, SettingPreference())
            .commit()

        println(System.getProperty("file.encoding"))

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
                            val resultMsg = if (onJsonExport(file.absolutePath)) {
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
                    val filter: FileFilter = { it.isDirectory || it.extension.startsWith("json", true) }
                    fileChooser(filter = filter) { dialog, file ->
                        val snackBar = ProgressSnackBar(activity!!, "インポートしています…").create()
                        snackBar.show()
                        val resultMsg = if (onJsonImport(file.absolutePath)) {
                            "データをインポートしました"
                        } else {
                            "データのインポートに失敗しました"
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
                val random = RandomStringUtils.randomAlphabetic(20)
                val fileName = "qr_code_data_${random}.json"

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

        private fun onJsonExport(path: String): Boolean {
            try {
                val random = RandomStringUtils.randomAlphabetic(20)
                val fileName = "qr_code_data_${random}.json"
                val file = File("$path/$fileName")
                val stream = OutputStreamWriter(FileOutputStream(file), "UTF-8")
                val buffer = BufferedWriter(stream)
                val writer = PrintWriter(buffer)

                val jsonData = JSONObject()
                val jsonArray = JSONArray()
                val data = realm.where(CodeData::class.java).findAll()
                data.forEach {
                    JSONObject().apply {
                        put("uuid", it.uuid)
                        put("format", it.format)
                        put("data", it.data)
                        put("time", it.time)
                        jsonArray.put(this)
                    }
                }
                jsonData.put("data", jsonArray)

                writer.print(jsonData.toString(4))
                writer.println()
                writer.close()

                return true
            } catch (e: Exception) {
                e.printStackTrace()

                return false
            }
        }

        private fun onJsonImport(path: String): Boolean {
            try {
                val fileData = readAll(path)
                val jsonData = JSONObject(fileData)
                val jsonArray = jsonData.getJSONArray("data")

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    realm.executeTransaction { realm ->
                        realm.createObject(CodeData::class.java, UUID.randomUUID().toString()).apply {
                            format = obj.getString("format")
                            data = obj.getString("data")
                            time = obj.getString("time")
                        }
                    }
                }

                return true
            } catch (e: Exception) {
                e.printStackTrace()

                return false
            }
        }

        private fun readAll(path: String): String {
            return Files.lines(Paths.get(path), Charset.forName("UTF-8"))
                .collect(Collectors.joining(System.getProperty("line.separator")))
        }
    }
}
