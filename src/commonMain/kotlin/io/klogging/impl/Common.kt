/*

   Copyright 2021 Michael Strasser.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package io.klogging.impl

import io.klogging.BaseLogger
import io.klogging.events.Level
import io.klogging.events.LogEvent
import io.klogging.events.now

public fun LogEvent.copyWith(newLevel: Level, newStacktrace: String?): LogEvent = LogEvent(
    timestamp, host, logger, newLevel, template, message, newStacktrace, items
)

public fun BaseLogger.eventFrom(
    level: Level,
    exception: Exception?,
    event: Any?,
    withItems: Map<String, Any?> = mapOf(),
): LogEvent {
    return when (event) {
        is LogEvent ->
            event.copyWith(level, exception?.stackTraceToString())
        else -> {
            val (message, stackTrace) = messageAndStackTrace(event, exception)
            LogEvent(
                timestamp = now(),
                logger = this.name,
                level = level,
                message = message,
                stackTrace = stackTrace,
                items = withItems,
            )
        }
    }
}

internal fun messageAndStackTrace(event: Any?, exception: Exception?) = when (event) {
    is Exception -> (event.message ?: "Exception") to event.stackTraceToString()
    else -> event.toString() to exception?.stackTraceToString()
}
