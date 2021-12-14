package com.worldtech.appupdateversion.utils

import android.app.Activity
import android.app.FragmentManager
import android.content.Intent

class ActResultRequest(activity: Activity) {
    private val fragment: OnActResultEventDispatcherFragment
    private fun getEventDispatchFragment(activity: Activity): OnActResultEventDispatcherFragment {
        val fragmentManager = activity.fragmentManager
        return findEventDispatchFragment(fragmentManager)
    }

    private fun findEventDispatchFragment(manager: FragmentManager): OnActResultEventDispatcherFragment {
        return manager.findFragmentByTag(OnActResultEventDispatcherFragment.TAG) as OnActResultEventDispatcherFragment
    }

    fun startForResult(intent: Intent?, callback: ActForResultCallback?) {
        if (callback != null) {
            fragment.startForResult(intent, callback)
        }
    }

    init {
        fragment = getEventDispatchFragment(activity)
    }
}