package io.rippledown.cornerstone

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.rippledown.model.rule.CornerstoneStatus

interface CornerstonePagerHandler {
    fun selectCornerstone(index: Int)
    fun exemptCornerstone(index: Int)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerstonePager(cornerstoneStatus: CornerstoneStatus, handler: CornerstonePagerHandler) {
    val case = cornerstoneStatus.cornerstoneToReview
    println("---CornerstonePager: summary = ${cornerstoneStatus.summary()}")

    val pagerState = rememberPagerState(
        initialPage = cornerstoneStatus.indexOfCornerstoneToReview,
        pageCount = { cornerstoneStatus.numberOfCornerstones }
    )

    LaunchedEffect(cornerstoneStatus) {
        pagerState.animateScrollToPage(cornerstoneStatus.indexOfCornerstoneToReview)
        println("---CornerstonePager: LaunchedEffect 1: animateScrollToPage = ${cornerstoneStatus.indexOfCornerstoneToReview}")
        println("---CornerstonePager: LaunchedEffect 1: currentPage = ${pagerState.currentPage}")
    }

    LaunchedEffect(pagerState.currentPage) {
        val index = pagerState.currentPage
        println("---CornerstonePager: LaunchedEffect 2: currentPage = $index")
        if (index > -1) {
            handler.selectCornerstone(index)
            pagerState.animateScrollToPage(index)
            println("---CornerstonePager: LaunchedEffect 2: animateScrollToPage = ${index}")

        }
    }

    Column {
        CornerstoneControl(
            pagerState.currentPage,
            cornerstoneStatus.numberOfCornerstones,
            object : CornerstoneControlHandler {
                override fun next() {
                    handler.selectCornerstone(pagerState.currentPage + 1)
                }

                override fun previous() {
                    handler.selectCornerstone(pagerState.currentPage - 1)
                }

                override fun exempt() {
                    handler.exemptCornerstone(pagerState.currentPage)
                }
            })

        VerticalPager(state = pagerState) {
            if (case != null) {
                CornerstoneInspection(case)
            }
        }
    }
}