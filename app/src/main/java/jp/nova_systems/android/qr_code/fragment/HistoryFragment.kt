package jp.nova_systems.android.qr_code.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import io.realm.Realm
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.CodeData

class HistoryFragment : Fragment() {
    private lateinit var realm: Realm

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        realm = Realm.getDefaultInstance()

        val listView = view.findViewById<ListView>(R.id.list_view)
        val data = onGetList()
        val adapter = ArrayAdapter<HashMap<String, String>>(
            activity!!,
            android.R.layout.simple_list_item_1,
            data
        )
        listView.adapter = adapter
//        listView.setOnClickListener {
//
//        }
        listView.setOnItemLongClickListener { _, _, i, _ ->
            val uuid = data[i]["uuid"]
            BottomSheetMaterialDialog.Builder(activity!!).apply {
                setTitle("Delete?")
                setMessage("Are you sure want to delete this file?")
                setCancelable(false)
                setPositiveButton("Delete", R.drawable.icon_delete) { dialog, _ ->
                    val item = realm.where(CodeData::class.java).equalTo("uuid", uuid).findFirst()
                    if (item == null) {
                        Snackbar.make(view.rootView, "No data found.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        realm.executeTransaction {
                            item.deleteFromRealm()
                        }
                        Snackbar.make(view.rootView, "Data deleted!", Snackbar.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                }
                setNegativeButton("Cancel", R.drawable.icon_close) { dialog, _ ->
                    dialog.dismiss()
                }
            }.build().show()

            return@setOnItemLongClickListener true
        }

        return view
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
