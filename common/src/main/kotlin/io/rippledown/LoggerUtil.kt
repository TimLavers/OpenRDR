package io.rippledown

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

inline fun <reified T : Any> KClass<T>.logger(): Logger = LoggerFactory.getLogger(this.java)