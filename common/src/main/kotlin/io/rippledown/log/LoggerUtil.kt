package io.rippledown.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T : Any> T.lazyLogger: Logger
    get() = lazy { LoggerFactory.getLogger(T::class.java) }.value

