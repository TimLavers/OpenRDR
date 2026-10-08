package io.rippledown.constants.main

import io.rippledown.model.UserId

fun windowTitle(userId: UserId) = "$TITLE — ${userId.value}"
