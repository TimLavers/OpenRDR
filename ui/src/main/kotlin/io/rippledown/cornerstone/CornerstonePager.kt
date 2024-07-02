package io.rippledown.cornerstone

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus

interface CornerstonePagerHandler {
    suspend fun selectCornerstone(index: Int): ViewableCase
    fun exemptCornerstone(index: Int)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerstonePager(cornerstoneStatus: CornerstoneStatus, handler: CornerstonePagerHandler) {
    val currentIndex = remember { mutableStateOf(cornerstoneStatus.indexOfCornerstoneToReview) }
    var case: ViewableCase? by remember { mutableStateOf(cornerstoneStatus.cornerstoneToReview) }

    val pagerState = rememberPagerState(
        initialPage = cornerstoneStatus.indexOfCornerstoneToReview,
        pageCount = { cornerstoneStatus.numberOfCornerstones }
    )

    LaunchedEffect(cornerstoneStatus) {
        pagerState.animateScrollToPage(cornerstoneStatus.indexOfCornerstoneToReview)
    }

    LaunchedEffect(currentIndex.value) {
        val index = cornerstoneStatus.indexOfCornerstoneToReview
        if (index > -1) {
            case = handler.selectCornerstone(currentIndex.value)
            pagerState.animateScrollToPage(currentIndex.value)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val index = pagerState.currentPage
        if (index > -1) {
            case = handler.selectCornerstone(index)
            pagerState.animateScrollToPage(index)
        }
    }

    Column {
        CornerstoneControl(
            pagerState.currentPage,
            cornerstoneStatus.numberOfCornerstones,
            object : CornerstoneControlHandler {
                override fun next() {
                    currentIndex.value = pagerState.currentPage + 1
                }

                override fun previous() {
                    currentIndex.value = pagerState.currentPage - 1
                }

                override fun exempt() {
                    handler.exemptCornerstone(currentIndex.value)
                }
            })

        VerticalPager(state = pagerState) {
            if (case != null) CornerstoneInspection(case!!)
        }
    }
}