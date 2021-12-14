package com.worldtech.appupdateversion.utils

import android.app.Fragment
import android.util.SparseArray
import android.os.Bundle
import android.content.Intent

class OnActResultEventDispatcherFragment : Fragment() {
    private val mCallbacks = SparseArray<ActForResultCallback>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun startForResult(intent: Intent?, callback: ActForResultCallback) {
        mCallbacks.put(callback.hashCode(), callback)
        startActivityForResult(intent, callback.hashCode())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        val callback = mCallbacks[requestCode]
        mCallbacks.remove(requestCode)
        callback?.onActivityResult(resultCode, data)
    }

    companion object {
        const val TAG = "on_act_result_event_dispatcher"
    }
}