package io.rippledown.cornerstone

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.navigation.NextPreviousControl
import io.rippledown.navigation.NextPreviousControlHandler

interface CornerstonePagerHandler {
    suspend fun selectCornerstone(index: Int): ViewableCase
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerstonePager(cornerstoneStatus: CornerstoneStatus, handler: CornerstonePagerHandler) {
    val currentIndex = remember { mutableStateOf(cornerstoneStatus.indexOfCornerstoneToReview) }
    var case: ViewableCase? by remember { mutableStateOf(null) }

    val pagerState = rememberPagerState(
        initialPage = cornerstoneStatus.indexOfCornerstoneToReview,
        pageCount = { cornerstoneStatus.numberOfCornerstones }
    )
    LaunchedEffect(Unit) {
        case = handler.selectCornerstone(currentIndex.value)
    }
    LaunchedEffect(currentIndex.value) {
        println("LE INDEX START: ${pagerState.currentPage}, ${currentIndex.value}")
        case = handler.selectCornerstone(currentIndex.value)
        pagerState.animateScrollToPage(currentIndex.value)
        println("LE INDEX END  : ${pagerState.currentPage}, ${currentIndex.value}")
    }

    LaunchedEffect(pagerState.currentPage) {
        println("LE PAGER START: ${pagerState.currentPage}, ${currentIndex.value}")
        case = handler.selectCornerstone(pagerState.currentPage)
        pagerState.animateScrollToPage(pagerState.currentPage)
        println("LE PAGER END  : ${pagerState.currentPage}, ${currentIndex.value}")
    }

    Column {
        NextPreviousControl(
            pagerState.currentPage,
            cornerstoneStatus.numberOfCornerstones,
            object : NextPreviousControlHandler {
                override fun next() {
                    println("NEXT: ${pagerState.currentPage}, ${currentIndex.value}")
                    currentIndex.value = pagerState.currentPage + 1
                }

                override fun previous() {
                    println("PREV: ${pagerState.currentPage}, ${currentIndex.value}")
                    currentIndex.value = pagerState.currentPage - 1
                }
            })

        VerticalPager(state = pagerState) {
            println("VerticalPager : ${pagerState.currentPage}, ${currentIndex.value}")
            if (case != null) CornerstoneInspection(case!!)
        }
    }
}