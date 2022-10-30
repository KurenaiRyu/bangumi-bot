package moe.kurenai.bot.util

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.apache.logging.log4j.util.StackLocatorUtil
import org.redisson.api.RBucketReactive
import org.slf4j.LoggerFactory

/**
 * @author Kurenai
 * @since 2022/10/27 16:20
 */

fun getLogger() = LoggerFactory.getLogger(StackLocatorUtil.getCallerClass(2))

suspend fun <T> RBucketReactive<out T>.getAwait(): T? = get().awaitSingleOrNull()