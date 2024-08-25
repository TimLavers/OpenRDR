package io.rippledown.constants.api

const val API_URL = "http://localhost:9090"
const val WAITING_CASES = "/api/waitingCasesInfo"
const val CONDITION_HINTS = "/api/conditionHints"
const val CASE = "/api/case"
const val PROCESS_CASE = "/api/processCase"
const val DELETE_CASE_WITH_NAME = "/api/deleteProcessedCaseWithName"
const val MOVE_ATTRIBUTE = "/api/moveAttribute"
const val GET_OR_CREATE_ATTRIBUTE = "/api/attribute/getOrCreate"
const val SET_ATTRIBUTE_ORDER = "/api/attribute/setOrder"
const val GET_OR_CREATE_CONCLUSION = "/api/conclusion/getOrCreate"
const val GET_OR_CREATE_CONDITION = "/api/condition/getOrCreate"
const val START_SESSION_TO_ADD_CONCLUSION = "/api/startSessionToAddConclusion"
const val START_SESSION_TO_REMOVE_CONCLUSION = "/api/startSessionToRemoveConclusion"
const val START_SESSION_TO_REPLACE_CONCLUSION = "/api/startSessionToReplaceConclusion"
const val ADD_CONDITION = "/api/addCondition"
const val COMMIT_SESSION = "/api/commitSession"
const val BUILD_RULE = "/api/buildRule"
const val START_RULE_SESSION = "/api/startRuleSession"
const val SELECT_CORNERSTONE = "/api/selectCornerstone"
const val UPDATE_CORNERSTONES = "/api/updateCornerstones"
const val EXEMPT_CORNERSTONE = "/api/exemptCornerstone"
const val INDEX_PARAMETER = "index"
const val DEFAULT_KB = "/api/defaultKB"
const val KB_INFO = "/api/kbInfo"
const val DELETE_KB = "/api/deleteKB"
const val KB_LIST = "/api/kbList"
const val CREATE_KB = "/api/createKB"
const val CREATE_KB_FROM_SAMPLE = "/api/createKbFromSample"
const val SELECT_KB = "/api/selectKB"
const val IMPORT_KB = "/api/importKB"
const val EXPORT_KB = "/api/exportKB"