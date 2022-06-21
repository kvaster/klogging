/*

   Copyright 2021-2022 Michael Strasser.

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

package io.klogging.config

import io.klogging.Level.DEBUG
import io.klogging.Level.FATAL
import io.klogging.Level.INFO
import io.klogging.internal.KloggingEngine
import io.klogging.randomString
import io.klogging.rendering.RENDER_CLEF
import io.klogging.rendering.RENDER_SIMPLE
import io.klogging.sending.STDOUT
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
internal class JsonConfigurationTest : DescribeSpec({
    describe("Configuration from JSON") {
        describe("invalid JSON") {
            it("does not configure anything") {
                val config = configureFromJson("*** THIS IS NOT JSON ***")

                config?.apply {
                    sinks shouldHaveSize 0
                    configs shouldHaveSize 0
                }
            }
        }
        describe("simple, using built-in, named configuration") {
            it("sets up the configuration") {
                val simpleJsonConfig = """{ "configName": "DEFAULT_CONSOLE" }"""
                val config = configureFromJson(simpleJsonConfig)

                config?.apply {
                    sinks shouldHaveSize 1
                    sinks.keys.first() shouldBe "console"
                    with(sinks.values.first()) {
                        renderer shouldBe RENDER_SIMPLE
                        stringSender shouldBe STDOUT
                    }
                }
            }
            it("ignores any other configuration in the file") {
                val jsonConfig = """
                    {
                      "configName":"DEFAULT_CONSOLE",
                      "sinks": {
                        "stdout": {
                          "renderWith": "RENDER_SIMPLE",
                          "sendTo": "STDOUT"
                        }
                      }
                    }
                """.trimIndent()
                val config = configureFromJson(jsonConfig)

                config?.apply { sinks shouldHaveSize 1 }
            }
        }
        describe("simple, using built-in, named renderers and senders") {
            val simpleJsonConfig = """
                {
                  "sinks": {
                    "stdout": {
                      "renderWith": "RENDER_SIMPLE",
                      "sendTo": "STDOUT"
                    }
                  },
                  "logging": [
                    {
                      "fromLoggerBase": "com.example",
                      "levelRanges": [
                        {
                          "fromMinLevel": "INFO",
                          "toSinks": [
                            "stdout"
                          ]
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
            it("reads the configuration from string") {
                val jsonConfig = readConfig(simpleJsonConfig)

                jsonConfig shouldNotBe null
                jsonConfig?.apply {
                    sinks shouldHaveSize 1
                    sinks.keys.first() shouldBe "stdout"
                    with(sinks.values.first()) {
                        renderWith shouldBe "RENDER_SIMPLE"
                        sendTo shouldBe "STDOUT"
                    }
                    logging shouldHaveSize 1
                    with(logging.first()) {
                        fromLoggerBase shouldBe "com.example"
                        levelRanges shouldHaveSize 1
                        with(levelRanges.first()) {
                            fromMinLevel shouldBe INFO
                            toSinks shouldHaveSize 1
                            toSinks.first() shouldBe "stdout"
                        }
                    }
                }
            }
            it("sets up built-in sinks") {
                val config = configureFromJson(simpleJsonConfig)

                config?.apply {
                    sinks shouldHaveSize 1
                    sinks.keys.first() shouldBe "stdout"
                    with(sinks.values.first()) {
                        renderer shouldBe RENDER_SIMPLE
                        stringSender shouldBe STDOUT
                    }
                }
            }
            it("sets up the logging configurations") {
                val config = configureFromJson(simpleJsonConfig)

                config shouldNotBe null
                config?.apply {
                    configs shouldHaveSize 1
                    with(configs.first()) {
                        ranges shouldHaveSize 1
                        with(ranges.first()) {
                            minLevel shouldBe INFO
                            maxLevel shouldBe FATAL
                            sinkNames shouldBe listOf("stdout")
                        }
                    }
                }
            }
        }
        describe("Klogging minimum log level") {
            beforeTest {
                KloggingEngine.setConfig(KloggingConfiguration())
            }
            it("is not changed if not set in JSON") {
                configureFromJson("""{}""")?.let { KloggingEngine.setConfig(it) }

                KloggingEngine.kloggingMinLogLevel() shouldBe defaultKloggingMinLogLevel
            }
            it("is changed if set in JSON") {
                configureFromJson("""{"kloggingMinLogLevel":"DEBUG"}""")?.let {
                    KloggingEngine.setConfig(it)
                }

                KloggingEngine.kloggingMinLogLevel() shouldBe DEBUG
            }
        }
        describe("sink configuration") {
            describe("using `renderWith` and `sendTo` keys") {
                it("returns a configuration using names of built-in components") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "renderWith": "RENDER_SIMPLE",
                        "sendTo": "STDOUT"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig?.renderer shouldBe RENDER_SIMPLE
                    sinkConfig?.stringSender shouldBe STDOUT
                }
                it("returns null if `renderWith` key is missing") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "sendTo": "STDOUT"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig shouldBe null
                }
                it("returns null if `sendTo` key is missing") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "renderWith": "RENDER_SIMPLE"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig shouldBe null
                }
                it("returns null if names are not of built-in components") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "renderWith": "${randomString()}",
                        "sendTo": "${randomString()}"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig shouldBe null
                }
            }
            describe("using `seqServer` key") {
                it("returns a Seq configuration with RENDER_CLEF if only that key is present") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "seqServer": "http://localhost:5341"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig?.renderer shouldBe RENDER_CLEF
                }
                it("returns a Seq configuration with another renderer if specified") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "seqServer": "http://localhost:5341",
                        "renderWith": "RENDER_SIMPLE"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig?.renderer shouldBe RENDER_SIMPLE
                }
                it("returns a Seq configuration, overriding any other dispatcher") {
                    val sinkConfig = Json.decodeFromString<FileSinkConfiguration>(
                        """{
                        "seqServer": "http://localhost:5341",
                        "sendTo": "STDOUT"
                    }
                        """.trimIndent()
                    ).toSinkConfiguration()

                    sinkConfig?.stringSender shouldNotBe STDOUT
                }
            }
        }
    }
})
