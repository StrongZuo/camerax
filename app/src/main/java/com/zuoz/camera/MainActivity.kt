package com.zuoz.camera


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yanzhenjie.permission.Action
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AndPermission.with(this)
            .runtime()
            .permission(Permission.Group.STORAGE,Permission.Group.CAMERA)
            .onGranted(Action<List<String?>> { permissions: List<String?>? ->
                val intent = Intent()
                intent.setClass(this, CameraActivity::class.java)
                startActivity(intent)
                intent
            })
            .onDenied(Action<List<String?>> { permissions: List<String?>? ->
                Toast.makeText(this, "获取权限失败", Toast.LENGTH_SHORT)

            })
            .start()

    }

}