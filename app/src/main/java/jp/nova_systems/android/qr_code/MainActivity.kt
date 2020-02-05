package jp.nova_systems.android.qr_code

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.files.folderChooser
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.realm.Realm
import jp.nova_systems.android.qr_code.realm.CodeData
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.lang3.RandomStringUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    lateinit var mRootView: ConstraintLayout

    companion object {
        const val REQUEST_CODE_PERMISSION = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Permission
        onRequiredPermission()

        // Navigation
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        val navigationSet = setOf(
            R.id.navigation_generate,
            R.id.navigation_read,
            R.id.navigation_history,
            R.id.navigation_setting
        )
        val appBarConfiguration = AppBarConfiguration(navigationSet)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        realm = Realm.getDefaultInstance()
        mRootView = container
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "必要な権限が付与されていません", Toast.LENGTH_SHORT).show()
                    Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", applicationContext.packageName, null)
                        startActivity(this)
                    }
                    return
                }
            }
            return
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onRequiredPermission() {
        val requiredPermission = arrayListOf<String>()
        var offPermissionCount = 0

        val checkCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (checkCamera != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                offPermissionCount += 1
            } else {
                requiredPermission.add(Manifest.permission.CAMERA)
            }
        }
        val checkWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (checkWrite != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                offPermissionCount += 1
            } else {
                requiredPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        val checkRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (checkRead != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                offPermissionCount += 1
            } else {
                requiredPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (offPermissionCount >= 1) {
            Toast.makeText(this, "必要な権限が付与されていません", Toast.LENGTH_SHORT).show()
            Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", applicationContext.packageName, null)
                startActivity(this)
            }
        } else if (requiredPermission.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermission.toTypedArray(), REQUEST_CODE_PERMISSION)
        }
    }

    fun onGenerator(text: String, format: String) {
        val encoder = BarcodeEncoder()
        val bitmap = encoder.encodeBitmap(text, BarcodeFormat.valueOf(format), 400, 400)

        val view = View.inflate(this, R.layout.layout_generate_dialog, null)
        view.findViewById<ImageView>(R.id.image_view).setImageBitmap(bitmap)
        MaterialDialog(this, BottomSheet()).show {
            cornerRadius(10f)
            customView(view = view)
            positiveButton(text = "保存") {
                MaterialDialog(context).show {
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

    fun onSaveQrCode(bitmap: Bitmap, path: String) {
        try {
            val random = RandomStringUtils.randomAlphabetic(20)
            val file = File("$path/$random.png")
            FileOutputStream(file).apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                close()
            }

            Snackbar.make(mRootView, "保存しました", Snackbar.LENGTH_LONG).apply {
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
            Snackbar.make(mRootView, "保存に失敗しました", Snackbar.LENGTH_SHORT).show()
        }
    }
}
