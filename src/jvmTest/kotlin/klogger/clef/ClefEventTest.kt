package klogger.clef

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import klogger.Event
import klogger.Level
import klogger.iso
import timestampNow
import java.util.UUID

class ClefEventTest : DescribeSpec({
    describe("Creating CLEF event JSON") {
        val ts = timestampNow()
        val event = Event(UUID.randomUUID().toString(), ts, "Test", Level.INFO, "Message", mapOf())

        clef(event) shouldBe """{"@t":"${iso(event.timestamp)}","@m":"${event.message}","@l":"${event.level}","logger":"${event.name}"}"""
    }
})