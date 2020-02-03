package jp.nova_systems.android.qr_code.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.folderChooser
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import io.realm.Realm
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.CodeData
import org.apache.commons.lang3.RandomStringUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class HistoryFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var rootView: ConstraintLayout
    private lateinit var listView: ListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        realm = Realm.getDefaultInstance()

        rootView = activity!!.findViewById(R.id.container)
        listView = view.findViewById(R.id.list_view)

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener(mOnRefreshListener)
        swipeRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)

        onSetList()

        return view
    }

    private val mOnRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        onSetList()
    }

    private fun onSetList() {
        val data = onGetList()
        val adapter = SimpleAdapter(
            activity,
            data,
            R.layout.layout_history_item,
            arrayOf("data", "format"),
            intArrayOf(R.id.text1, R.id.text2)
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, i, _ ->
            val text = data[i]["data"].toString()
            val format = data[i]["format"].toString()

            onGenerator(text, format)

//            BottomSheetMaterialDialog.Builder(activity!!).apply {
//                val text = data[i]["data"].toString()
//                setTitle("データ")
//                setMessage(text)
//                setCancelable(true)
//                setPositiveButton("Share", R.drawable.icon_share) { dialog, _ ->
//                    ShareCompat.IntentBuilder.from(activity).apply {
//                        setText(text)
//                        setType("text/plain")
//                        startChooser()
//                    }
//
//                    dialog.dismiss()
//                }
//                setNegativeButton("キャンセル", R.drawable.icon_close) { dialog, _ ->
//                    dialog.dismiss()
//                }
//            }.build().show()
        }
        listView.setOnItemLongClickListener { _, _, i, _ ->
            val uuid = data[i]["uuid"]
            BottomSheetMaterialDialog.Builder(activity!!).apply {
                setTitle("削除")
                setMessage("一件のデータを削除しますか？")
                setCancelable(true)
                setPositiveButton("削除", R.drawable.icon_delete) { dialog, _ ->
                    val item = realm.where(CodeData::class.java).equalTo("uuid", uuid).findFirst()
                    if (item == null) {
                        Snackbar.make(rootView, "データが見つからないため削除できませんでした", Snackbar.LENGTH_SHORT).show()
                    } else {
                        realm.executeTransaction {
                            item.deleteFromRealm()
                        }
                        onSetList()
                        Snackbar.make(rootView, "削除しました", Snackbar.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                setNegativeButton("キャンセル", R.drawable.icon_close) { dialog, _ ->
                    dialog.dismiss()
                }
            }.build().show()

            return@setOnItemLongClickListener true
        }
        swipeRefreshLayout.isRefreshing = false
    }

    private fun onGetList(): ArrayList<HashMap<String, String>> {
        val data = realm.where(CodeData::class.java).sort("time").findAll()
        val list = ArrayList<HashMap<String, String>>()
        return if (data == null) {
            list
        } else {
            for (i in 0 until data.size) {
                val d = data[i]
                if (d !== null) {
                    val map = hashMapOf(
                        "uuid" to d.uuid,
                        "format" to d.format,
                        "data" to d.data
                    )
                    list.add(map)
                }
            }
            list
        }
    }

    private fun onGenerator(text: String, format: String) {
        val encoder = BarcodeEncoder()
        val bitmap = encoder.encodeBitmap(text, BarcodeFormat.valueOf(format), 400, 400)

        val view = View.inflate(activity, R.layout.layout_generate_dialog, null)
        view.findViewById<ImageView>(R.id.image_view).setImageBitmap(bitmap)
        MaterialDialog(activity!!, BottomSheet()).show {
            cornerRadius(10f)
            customView(view = view)
            positiveButton(text = "保存") {
                MaterialDialog(activity!!).show {
                    folderChooser { _, file ->
                        onSaveQrCode(bitmap, file.absolutePath)
                    }
                }
            }
            negativeButton(text = "キャンセル")
        }

        realm.executeTransaction { realm ->
            realm.createObject(CodeData::class.java, UUID.randomUUID().toString()).apply {
                this.format = format
                this.data = text
                time = LocalDateTime.now().toString()
            }
        }
    }

    private fun onSaveQrCode(bitmap: Bitmap, path: String) {
        try {
            val random = RandomStringUtils.randomAlphabetic(20)
            val file = File("$path/$random.png")
            FileOutputStream(file).apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                close()
            }

            Snackbar.make(rootView, "保存しました", Snackbar.LENGTH_LONG).apply {
                setAction("Open") {
                    Intent().apply {
                        action = Intent.ACTION_VIEW
                        val uri = Uri.parse(file.path)
                        setDataAndType(uri, "image/*")
                        startActivity(this)
                    }
                }
                show()
            }
        } catch (e: Exception) {
            Snackbar.make(rootView, "保存に失敗しました", Snackbar.LENGTH_SHORT).show()
        }
    }
}
