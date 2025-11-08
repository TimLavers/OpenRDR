package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString

data class ReviewCornerstonesReplaceComment(val comment: String, val replacementComment: String) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        val sessionCase = currentCase ?: throw IllegalStateException("No current case")
        val cornerstoneStatus =
            ruleService.startCornerstoneReviewSessionToReplaceComment(sessionCase, comment, replacementComment)
        return modelResponder.response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
    }
}