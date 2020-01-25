package jp.nova_systems.android.qr_code.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import io.realm.Realm
import jp.nova_systems.android.qr_code.R
import jp.nova_systems.android.qr_code.realm.CodeData
import java.time.LocalDateTime
import java.util.*

class ReadFragment : Fragment() {
    private lateinit var realm: Realm
    private var lastData: String = ""
    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_read, container, false)

        realm = Realm.getDefaultInstance()

        barcodeView = view.findViewById(R.id.barcode_view)
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if (result.text != lastData) {
                    val uuid = UUID.randomUUID().toString()
                    realm.executeTransaction { realm ->
                        realm.createObject(CodeData::class.java, uuid).apply {
                            format = result.barcodeFormat.toString()
                            data = result.text
                            time = LocalDateTime.now().toString()
                        }
                    }
                    lastData = result.text
                    val dialog = BottomSheetMaterialDialog.Builder(activity!!).apply {
                        setTitle("Success!")
                        setMessage("Barcode loaded.")
                        setCancelable(true)
                        setPositiveButton("Share", R.drawable.icon_share) { dialog, _ ->
                            ShareCompat.IntentBuilder.from(activity).apply {
                                setText(result.text)
                                setType("text/plain")
                                startChooser()
                            }

                            dialog.dismiss()
                        }
                        setNegativeButton("Close", R.drawable.icon_close) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }.build()
                    dialog.setOnDismissListener {
                        lastData = ""
                    }
                    dialog.show()
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>) {}
        })
        barcodeView.resume()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
    }

    override fun onResume() {
        super.onResume()

        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()

        barcodeView.pause()
    }
}
