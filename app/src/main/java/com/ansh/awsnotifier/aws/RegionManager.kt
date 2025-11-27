package com.ansh.awsnotifier.aws

data class AwsRegionInfo(
    val id: String,
    val displayName: String,
    val platformApplicationArn: String? = null
)

object RegionManager {
    val SUPPORTED_REGIONS = listOf(
        AwsRegionInfo("us-east-1", "US East (N. Virginia)"),
        AwsRegionInfo("us-east-2", "US East (Ohio)"),
        AwsRegionInfo("us-west-1", "US West (N. California)"),
        AwsRegionInfo("us-west-2", "US West (Oregon)"),
        AwsRegionInfo("ap-south-1", "Asia Pacific (Mumbai)"),
        AwsRegionInfo("ap-southeast-1", "Asia Pacific (Singapore)"),
        AwsRegionInfo("ap-southeast-2", "Asia Pacific (Sydney)"),
        AwsRegionInfo("ap-northeast-1", "Asia Pacific (Tokyo)"),
        AwsRegionInfo("eu-west-1", "Europe (Ireland)"),
        AwsRegionInfo("eu-central-1", "Europe (Frankfurt)"),
        AwsRegionInfo("sa-east-1", "South America (São Paulo)")
    )

    fun getRegionByName(regionName: String): AwsRegionInfo? =
        SUPPORTED_REGIONS.find { it.id == regionName }

    fun getDisplayNames(): List<String> =
        SUPPORTED_REGIONS.map { it.displayName }
}
