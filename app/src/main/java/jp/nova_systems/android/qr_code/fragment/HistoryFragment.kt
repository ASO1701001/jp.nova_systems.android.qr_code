package jp.nova_systems.android.qr_code.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import io.realm.Realm
import jp.nova_systems.android.qr_code.MainActivity
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.CodeData
import java.util.*
import kotlin.collections.ArrayList

class HistoryFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var listView: ListView
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        activity.let {
            if (it is MainActivity) {
                mActivity = it
            } else {
                return view
            }
        }

        realm = Realm.getDefaultInstance()

        listView = view.findViewById(R.id.list_view)

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        mSwipeRefreshLayout.apply {
            setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
            setOnRefreshListener {
                onSetList()
            }
        }

        onSetList()

        return view
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

            mActivity.onGenerator(text, format)
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
                        Snackbar.make(mActivity.mRootView, "データが見つからないため削除できませんでした", Snackbar.LENGTH_SHORT).show()
                    } else {
                        realm.executeTransaction {
                            item.deleteFromRealm()
                        }
                        onSetList()
                        Snackbar.make(mActivity.mRootView, "削除しました", Snackbar.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                setNegativeButton("キャンセル", R.drawable.icon_close) { dialog, _ ->
                    dialog.dismiss()
                }
            }.build().show()

            return@setOnItemLongClickListener true
        }
        mSwipeRefreshLayout.isRefreshing = false
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
}
