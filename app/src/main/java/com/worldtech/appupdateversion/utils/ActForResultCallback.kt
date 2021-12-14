package com.worldtech.appupdateversion.utils

import android.content.Intent

interface ActForResultCallback {
    fun onActivityResult(resultCode: Int, data: Intent?)
}