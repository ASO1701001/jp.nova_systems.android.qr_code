package jp.nova_systems.android.qr_code.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.realm.Realm
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.CodeData
import org.apache.commons.lang3.RandomStringUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class GenerateFragment : Fragment() {
    private lateinit var realm: Realm
    private lateinit var rootView: ConstraintLayout
    private lateinit var inputData: TextInputLayout
    private lateinit var inputFormat: TextInputLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_generate, container, false)

        realm = Realm.getDefaultInstance()

        rootView = activity!!.findViewById(R.id.container)

        inputData = view.findViewById(R.id.input_data)
        inputFormat = view.findViewById(R.id.input_format)
        inputFormat.editText?.setText(BarcodeFormat.QR_CODE.name)
        inputFormat.editText?.setOnClickListener {
            val select = if (inputFormat.editText?.text.isNullOrEmpty()) {
                BarcodeFormat.QR_CODE.name
            } else {
                inputFormat.editText?.text.toString().trim()
            }
            val formatList = ArrayList<String>().apply {
                BarcodeFormat.values().forEach {
                    add(it.name)
                }
            }
            val index = formatList.indexOf(select)
            MaterialDialog(activity!!, BottomSheet()).show {
                cornerRadius(10f)
                listItemsSingleChoice(items = formatList, initialSelection = index) { dialog, index, text ->
                    inputFormat.editText?.setText(text)
                }
                positiveButton(text = "選択")
                negativeButton(text = "キャンセル")
            }
        }
        view.findViewById<Button>(R.id.button_generate).setOnClickListener {
            onGenerator()
        }

        return view
    }

    private fun onGenerator() {
        val text = inputData.editText?.text.toString()
        val format = inputFormat.editText?.text.toString()
        if (text.isEmpty() || format.isEmpty()) return

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

        inputData.editText?.setText("")
    }

    private fun onSaveQrCode(bitmap: Bitmap, path: String) {
        try {
            val random = RandomStringUtils.randomAlphabetic(20)
            val file = File("$path/$random.png")
            FileOutputStream(file).apply {
                bitmap.compress(CompressFormat.PNG, 100, this)
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
