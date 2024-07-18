/*

   Copyright 2021-2024 Michael Strasser.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package io.klogging

import io.klogging.context.ContextItem
import io.klogging.impl.KloggerImpl
import io.klogging.internal.KloggingEngine
import io.klogging.internal.trace
import kotlin.reflect.KClass

/**
 * Runtime list of current [Klogger] instances.
 */
private val LOGGERS: MutableMap<String, Klogger> = AtomicMutableMap()

/** Used by test classes only. */
internal fun clearKloggers() {
    LOGGERS.clear()
}

/**
 * Returns a [Klogger] for the specified name: returning an existing one
 * or creating a new one if needed.
 */
internal fun loggerFor(
    name: String?,
    vararg loggerContextItems: ContextItem,
    otherLogger: BaseLogger? = null,
): Klogger {
    // Ensure file configuration has been loaded
    KloggingEngine.configuration
    val loggerName = name ?: "Klogger"
    val contextItems = (otherLogger?.loggerContextItems ?: emptyMap()) + mapOf(*loggerContextItems)
    if (!LOGGERS.containsKey(loggerName)) {
        trace("Klogging", "Adding Klogger $loggerName")
    }
    return KloggerImpl(loggerName, contextItems).also { LOGGERS[loggerName] = it }
}

/** Returns a [Klogger] with the specified name. */
public fun logger(
    name: String,
    vararg loggerContextItems: ContextItem,
): Klogger = loggerFor(name, *loggerContextItems)

public fun logger(
    name: String,
    otherLogger: BaseLogger?,
    vararg loggerContextItems: ContextItem,
): Klogger = loggerFor(name, *loggerContextItems, otherLogger = otherLogger)

/** Returns a [Klogger] with the name of the specified class. */
public fun logger(
    ownerClass: KClass<*>,
    vararg loggerContextItems: ContextItem,
): Klogger = loggerFor(classNameOf(ownerClass), *loggerContextItems)

public fun logger(
    ownerClass: KClass<*>,
    otherLogger: BaseLogger?,
    vararg loggerContextItems: ContextItem,
): Klogger = loggerFor(classNameOf(ownerClass), *loggerContextItems, otherLogger = otherLogger)

/** Returns a [Klogger] with the name of the specified class. */
public inline fun <reified T> logger(
    vararg loggerContextItems: ContextItem,
): Klogger =
    logger(T::class, *loggerContextItems)

public inline fun <reified T> logger(
    otherLogger: BaseLogger?,
    vararg loggerContextItems: ContextItem,
): Klogger =
    logger(T::class, otherLogger, *loggerContextItems)

/**
 * Utility interface that supplies a [Klogger] property called `logger`.
 */
public interface Klogging {
    public val logger: Klogger
        get() = loggerFor(classNameOf(this::class))
}
