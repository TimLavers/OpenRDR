package steps

import io.rippledown.TestClientLauncher
import io.rippledown.integration.pageobjects.RippleDownUIOperator
import java.awt.Robot
import java.io.File
import javax.imageio.ImageIO

class LaunchedClient {
    private val testClientLauncher = TestClientLauncher()
    private val composeWindow = testClientLauncher.launchClient()
    private val rdUiOperator = RippleDownUIOperator(composeWindow)
    fun applicationBarPO() = rdUiOperator.applicationBarOperator()
    fun caseListPO() = rdUiOperator.caseListPO()
    fun cornerstoneCaseListPO() = rdUiOperator.cornerstoneCaseListPO()
    fun caseCountPO() = rdUiOperator.caseCountPO()
    fun kbControlsPO() = rdUiOperator.kbControlsPO()
    fun editCurrentKbControlPO() = rdUiOperator.editCurrentKbControlPO()
    fun caseViewPO() = rdUiOperator.caseViewPO()
    fun cornerstonePO() = rdUiOperator.cornerstonePO()
    fun interpretationViewPO() = rdUiOperator.interpretationViewPO()
    fun chatPO() = rdUiOperator.chatPO()
    fun ruleMakerPO() = rdUiOperator.ruleMakerPO()
    fun screenshot(file: File) {
        val bounds = composeWindow.bounds
        val image = Robot().createScreenCapture(bounds)
        file.parentFile?.mkdirs()
        ImageIO.write(image, "png", file)
    }

    fun stopClient() {
        testClientLauncher.stopClient()
    }
}