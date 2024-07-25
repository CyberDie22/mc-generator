package com.benbuzard.mcgenerator

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import java.util.*

fun <T : Any?> T.optional() = Optional.ofNullable(this)

fun ProgressBarBuilder.build(task: String, initialMax: Long): ProgressBar {
    return apply {
        setInitialMax(initialMax)
        setTaskName(task)
    }.build()
}