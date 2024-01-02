package com.example.myapplication

import android.app.Application
import android.content.Context
import me.weishu.reflection.Reflection

//import me.weishu.reflection.Reflection

/**
 * @author: syz
 * @date: 2024/1/2
 */
/**
 *   @ClassName: Application
 *   @Date: 2024/1/2 20:13
 *   @Author: syz
 *   @Description:
 */
class App:Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
//        Reflection.unseal(base);
//        MainActivity.HandlerLoopHookHelper.hookLooperObserver(this)

    }
}