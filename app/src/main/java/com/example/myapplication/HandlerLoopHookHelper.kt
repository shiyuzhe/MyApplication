package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.os.*
import android.util.Log
import android.util.Printer
import me.weishu.reflection.Reflection
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicLong

/**
 * @author: syz
 * @date: 2024/1/2
 */
/**
 *   @ClassName: HandlerLoopHookHelper
 *   @Date: 2024/1/2 21:54
 *   @Author: syz
 *   @Description:
 */
object HandlerLoopHookHelper {
    @Volatile
    private var isHooked = false
    private const val TAG = "HandlerLoopHookHelper"
    private fun checkHooked(context: Context?) {
        if (!isHooked) {
            Reflection.unseal(context)
            isHooked = true
        }
    }

    /**
     * Hook Looper 的 sObserver 观察消息耗时
     *
     * @param context 上下文
     */
    fun hookLooperObserver(context: Context?) {
        Log.d(TAG, "hookLoopObserver: " + Build.VERSION.SDK_INT)
        try {
            checkHooked(context)
            @SuppressLint("PrivateApi") val sObserverClass =
                Class.forName("android.os.Looper\$Observer")
            val invocation = ObserverInvocation()
            val o = sObserverClass.cast(
                Proxy.newProxyInstance(
                    sObserverClass.classLoader,
                    arrayOf(sObserverClass),
                    invocation
                )
            )
            val looperClass = Class.forName("android.os.Looper")
            @SuppressLint("BlockedPrivateApi") val sObserver =
                looperClass.getDeclaredField("sObserver")
            sObserver.isAccessible = true
            sObserver[Looper.getMainLooper()] = o
            Looper.getMainLooper().setMessageLogging(invocation.printer)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 钩针活套观察者
     *
     * @param context 上下文
     */
    fun unHookLooperObserver(context: Context?) {
        Log.d(TAG, "unHookLooperObserver: " + Build.VERSION.SDK_INT)
        try {
            checkHooked(context)
            @SuppressLint("PrivateApi") val sObserverClass =
                Class.forName("android.os.Looper\$Observer")
            val invocation = ObserverInvocation()
            val o = sObserverClass.cast(
                Proxy.newProxyInstance(
                    sObserverClass.classLoader,
                    arrayOf(sObserverClass),
                    invocation
                )
            )
            val looperClass = Class.forName("android.os.Looper")
            @SuppressLint("BlockedPrivateApi") val sObserver =
                looperClass.getDeclaredField("sObserver")
            sObserver.isAccessible = true
            sObserver[Looper.getMainLooper()] = null
            Looper.getMainLooper().setMessageLogging(null)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 挂钩主活套消息空闲处理程序
     *
     * @param context 上下文
     */
    fun hookMainLooperMessageIdleHandlers(context: Context?) {
        Log.d(TAG, "hookMainLooperMessageIdleHandlers: " + Build.VERSION.SDK_INT)
        try {
            checkHooked(context)
            val looperClass = Class.forName("android.os.Looper")
            val mQueueF = looperClass.getDeclaredField("mQueue")
            mQueueF.isAccessible = true
            val mainMessageQueue = mQueueF[Looper.getMainLooper()]
            val mQueueClass = Class.forName("android.os.MessageQueue")
            val mainIdleHandlerF = mQueueClass.getDeclaredField("mIdleHandlers")
            mainIdleHandlerF.isAccessible = true
            val o = mainIdleHandlerF[mainMessageQueue]
            val mIdleHandlers: ArrayList<MessageQueue.IdleHandler> = o as ArrayList<MessageQueue.IdleHandler>
            Log.d(TAG, "hookMainLooperMessageIdleHandlers size: " + mIdleHandlers.size)
            for (mIdleHandler in mIdleHandlers) {
                Log.d(
                    TAG,
                    "hookMainLooperMessageIdleHandlers content is: " + mIdleHandler.toString()
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}


//    object HandlerLoopHookHelper {
//        /**
//         * Hook Looper 的 sObserver 观察消息耗时
//         *
//         * @param context 上下文
//         */
//        fun hookLooperObserver(@Nullable context: Context?) {
//            Log.d(TAG, "hookLoopObserver: " + Build.VERSION.SDK_INT)
//            try {
//                @SuppressLint("PrivateApi") val sObserverClass =
//                    Class.forName("android.os.Looper\$Observer")
//                val invocation = ObserverInvocation()
//                val o = sObserverClass.cast(
//                    Proxy.newProxyInstance(
//                        sObserverClass.classLoader,
//                        arrayOf<Class<*>>(sObserverClass),
//                        invocation
//                    )
//                )
//                val looperClass = Class.forName("android.os.Looper")
//                @SuppressLint("BlockedPrivateApi") val sObserver: Field =
//                    looperClass.getDeclaredField("sObserver")
//                sObserver.isAccessible = true
//                sObserver.set(getMainLooper(), o)
//                getMainLooper().setMessageLogging(invocation.printer)
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }
//        }
//    }

class ObserverInvocation : InvocationHandler {
    private var dispatchStart: Long = 0
    private var dispatchEnd: Long = 0

    val printer = Printer {
//            Log.e("looper", it)
    }

    @Throws(Throwable::class)
    override operator fun invoke(proxy: Any?, method: Method, args: Array<Any>): Any? {
        if (Looper.getMainLooper().isCurrentThread) {
            if ("messageDispatchStarting" == method.getName()) {
                return messageDispatchStarting()
            } else if ("messageDispatched" == method.getName()) {
                messageDispatched(args[0] as Long, args[1] as Message)
            } else if ("dispatchingThrewException" == method.getName()) {
                dispatchingThrewException(
                    args[0] as Long,
                    args[1] as Message,
                    args[2] as Exception
                )
            }
        }
        return null
    }

    fun messageDispatchStarting(): Any {
        dispatchStart = SystemClock.uptimeMillis()
        val atomicLong = AtomicLong()
        return atomicLong.getAndIncrement()
    }

    fun messageDispatched(token: Any, msg: Message) {
        dispatchEnd = SystemClock.uptimeMillis()
        getTime(msg, token as Long)
    }

    fun dispatchingThrewException(token: Any?, msg: Message?, exception: Exception?) {}

    /**
     * 计时，可根据自己的需求进行详细的计
     */
    private fun getTime(message: Message, token: Long) {
        // message 指定开始时间，基于开机时间
        val `when`: Long = message.getWhen()
        // 等待的时长：开始执行 - 指定开始时间
        val wait = dispatchStart - `when`
        // 执行的时长：执行结束时间 - 执行开始时间
        val work = dispatchEnd - dispatchStart
        printRecord(message, token, wait, work)
    }

    private fun printRecord(message: Message, token: Long, wait: Long, work: Long) {
        val stringBuilder = StringBuilder()
        if (work <= MESSAGE_WORK_TIME_10) {
            stringBuilder.append("(可忽略)").append(dispatchStart).append("-").append(dispatchEnd)
                .append(">>> token: ").append(token).append(message.toString())
                .append(" wait: ").append(wait).append("ms")
                .append(" work: ").append(work).append("ms")
            Log.v(TAG, stringBuilder.toString())
        } else if (work <= MESSAGE_WORK_TIME_50) {
            stringBuilder.append("(看看就好)").append(dispatchStart).append("-").append(dispatchEnd)
                .append(">>> token: ").append(token).append(message.toString())
                .append(" wait: ").append(wait).append("ms")
                .append(" work: ").append(work).append("ms")
            Log.i(TAG, stringBuilder.toString())
        } else if (work <= MESSAGE_WORK_TIME_100) {
            stringBuilder.append("(需要关注一下)").append(dispatchStart).append("-")
                .append(dispatchEnd)
                .append(">>> token: ").append(token).append(message.toString())
                .append(" wait: ").append(wait).append("ms")
                .append(" work: ").append(work).append("ms")
            Log.w(TAG, stringBuilder.toString())
        } else if (work <= MESSAGE_WORK_TIME_300) {
            stringBuilder.append("(需要处理啦~)").append(dispatchStart).append("-")
                .append(dispatchEnd)
                .append(">>> token: ").append(token).append(message.toString())
                .append(" wait: ").append(wait).append("ms")
                .append(" work: ").append(work).append("ms")
            Log.d(TAG, stringBuilder.toString())
        } else {
            stringBuilder.append("(这个就超级严重啦~)").append(dispatchStart).append("-")
                .append(dispatchEnd)
                .append(">>> token: ").append(token).append(message.toString())
                .append(" wait: ").append(wait).append("ms")
                .append(" work: ").append(work).append("ms")
            Log.e(TAG, stringBuilder.toString())
        }
    }

    companion object {
        private const val MESSAGE_WORK_TIME_300: Long = 300
        private const val MESSAGE_WORK_TIME_100: Long = 100
        private const val MESSAGE_WORK_TIME_50: Long = 50
        private const val MESSAGE_WORK_TIME_10: Long = 10
    }
}