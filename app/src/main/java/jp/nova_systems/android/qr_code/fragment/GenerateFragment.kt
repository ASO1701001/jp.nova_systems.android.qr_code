package jp.nova_systems.android.qr_code.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import jp.nova_systems.android.qr_code.MainActivity
import jp.nova_systems.android.qr_code.R

class GenerateFragment : Fragment() {
    private lateinit var mActivity: MainActivity
    private lateinit var inputData: TextInputLayout
    private lateinit var inputFormat: TextInputLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_generate, container, false)

        activity.let {
            if (it is MainActivity) {
                mActivity = it
            } else {
                return view
            }
        }

        inputData = view.findViewById(R.id.input_data)
        inputFormat = view.findViewById(R.id.input_format)
        inputFormat.editText?.apply {
            setText(BarcodeFormat.QR_CODE.name)
            setOnClickListener {
                val select = if (text.isNullOrEmpty()) {
                    BarcodeFormat.QR_CODE.name
                } else {
                    text.toString().trim()
                }
                val formatList = ArrayList<String>().apply {
                    BarcodeFormat.values().forEach {
                        add(it.name)
                    }
                }
                val index = formatList.indexOf(select)
                MaterialDialog(activity!!, BottomSheet()).show {
                    cornerRadius(10f)
                    listItemsSingleChoice(items = formatList, initialSelection = index) { _, _, text ->
                        setText(text)
                    }
                    positiveButton(text = "選択")
                    negativeButton(text = "キャンセル")
                }
            }
        }
        view.findViewById<Button>(R.id.button_generate).setOnClickListener {
            val text = inputData.editText?.text.toString()
            val format = inputFormat.editText?.text.toString()
            if (text.isEmpty() || format.isEmpty()) {
                return@setOnClickListener
            }

            mActivity.onGenerator(text, format)
        }

        return view
    }
}
