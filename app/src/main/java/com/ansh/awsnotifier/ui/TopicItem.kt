package com.ansh.awsnotifier.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TopicItem(
    val topicArn: String,
    val topicName: String,
    val isSubscribed: Boolean,
    val subscriptionArn: String?
) : Parcelable
