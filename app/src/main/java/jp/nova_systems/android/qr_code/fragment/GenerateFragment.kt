package jp.nova_systems.android.qr_code.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import jp.nova_systems.android.qr_code.R

class GenerateFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*
        val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder
                    .encodeBitmap(editText.text.toString(), BarcodeFormat.QR_CODE, 400, 400)
                imageView.setImageBitmap(bitmap)
         */

        return inflater.inflate(R.layout.fragment_generate, container, false)
    }
}
