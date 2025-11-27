package com.ansh.awsnotifier.model

data class Topic(
    val arn: String,
    val name: String,
    val isSubscribed: Boolean,
    val subscriptionArn: String?
)
