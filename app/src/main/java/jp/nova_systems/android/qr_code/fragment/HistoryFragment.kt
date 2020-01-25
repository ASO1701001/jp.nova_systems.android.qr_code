package jp.nova_systems.android.qr_code.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import io.realm.Realm
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.CodeData

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
            android.R.layout.simple_list_item_1,
            arrayOf("data"),
            intArrayOf(android.R.id.text1)
        )
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, i, _ ->
            BottomSheetMaterialDialog.Builder(activity!!).apply {
                val text = data[i]["data"].toString()
                setTitle("データ")
                setMessage(text)
                setCancelable(true)
                setPositiveButton("Share", R.drawable.icon_share) { dialog, _ ->
                    ShareCompat.IntentBuilder.from(activity).apply {
                        setText(text)
                        setType("text/plain")
                        startChooser()
                    }

                    dialog.dismiss()
                }
                setNegativeButton("キャンセル", R.drawable.icon_close) { dialog, _ ->
                    dialog.dismiss()
                }
            }.build().show()
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
                    val map = hashMapOf("uuid" to d.uuid, "data" to d.data)
                    list.add(map)
                }
            }
            list
        }
    }
}
