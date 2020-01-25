package jp.nova_systems.android.qr_code

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PERMISSION = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        onRequiredPermission()

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
}
