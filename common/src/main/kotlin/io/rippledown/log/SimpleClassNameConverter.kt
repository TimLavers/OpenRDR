package io.rippledown.log

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

class SimpleClassNameConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val loggerName = event.loggerName
        return loggerName.substringAfterLast(".")
    }
}