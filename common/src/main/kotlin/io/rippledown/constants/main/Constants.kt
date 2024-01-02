package io.rippledown.constants.main

import io.rippledown.model.KBInfo

const val TITLE = "OpenRDR"
const val MAIN_HEADING = "Open RippleDown"
const val APPLICATION_BAR_ID = "application_bar"
const val MAIN_HEADING_ID = "main_heading"

const val CREATE = "Create"
const val CANCEL = "Cancel"

const val KBS_DROPDOWN_ID = "kbs_dropdown"
const val CREATE_KB_ITEM_ID = "create_kb_item"
const val CREATE_KB_NAME_FIELD_ID = "create_kb_name"
const val CREATE_KB_OK_BUTTON_ID = "create_kb_ok_button"
const val CREATE_KB_CANCEL_BUTTON_ID = "create_kb_cancel_button"
const val CREATE_KB_TEXT = "Create KB"
const val CREATE_KB_NAME = "Name"

fun kbItemId(kbInfo: KBInfo) = "kb_item_${kbInfo.id}"
