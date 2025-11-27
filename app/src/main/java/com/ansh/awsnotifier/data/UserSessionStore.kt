package com.ansh.awsnotifier.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.userSessionDataStore by preferencesDataStore("user_session")
