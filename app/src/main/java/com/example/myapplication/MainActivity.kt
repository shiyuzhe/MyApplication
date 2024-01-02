package com.example.myapplication

import android.os.*
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import me.weishu.reflection.Reflection


val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        mainLooper.setMessageLogging {
//            Log.e("looper", it)
//        }
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.hi).setOnClickListener {
            try {
                val activityClass = Class.forName("dalvik.system.VMRuntime")
                val field = activityClass.getDeclaredMethod(
                    "setHiddenApiExemptions",
                    Array<String>::class.java
                )
                field.isAccessible = true
                Log.i(TAG, "call success!!")
            } catch (e: Throwable) {
                Log.e(TAG, "error:", e)
            }
        }

        findViewById<TextView>(R.id.textView2).setOnClickListener {
            //抛异常 NullPointerException exemptAll.invoke(null)
            HandlerLoopHookHelper.hookLooperObserver(this)
            //成功，之后 可以直接反射调用hide的api了
            //val ret = Reflection.unseal(this@MainActivity)
        }

    }
    private fun toast(msg: String) {
        if (TextUtils.isEmpty(msg)) {
            return
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        Log.i(TAG, msg)
    }


}