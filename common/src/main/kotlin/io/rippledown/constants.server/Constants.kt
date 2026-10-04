package io.rippledown.constants.server

const val SHUTDOWN = "/api/shutdown"
const val PING = "/api/ping"
const val IN_MEMORY = "InMemory"

const val STARTING_SERVER = "Starting server"
const val STOPPING_SERVER = "Stopping server"

const val DEFAULT_PROJECT_NAME = "Thyroids"
const val KB_ID = "kb"
const val KB_NAME = "kb_name"
const val CASE_ID = "caseId"
const val USER_ID_HEADER = "X-User-Id"

// Why a 409 was sent: the KB is held by another user, or the rule session went stale.
const val REFUSAL_HEADER = "X-Refusal"
const val REFUSAL_HELD = "held"
const val REFUSAL_STALE = "stale"

const val EXPRESSION = "expression"
const val ATTRIBUTE_NAMES = "attributeNames"



