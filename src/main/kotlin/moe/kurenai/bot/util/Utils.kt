package moe.kurenai.bot.util

import org.apache.logging.log4j.util.StackLocatorUtil
import org.slf4j.LoggerFactory

/**
 * @author Kurenai
 * @since 2022/10/27 16:20
 */

fun getLogger() = LoggerFactory.getLogger(StackLocatorUtil.getCallerClass(2))