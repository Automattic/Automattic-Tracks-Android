package com.automattic.android.tracks

fun <K> Map<K, Any>.toStringValues(): Map<K, String> = this.mapValues { it.value.toString() }
