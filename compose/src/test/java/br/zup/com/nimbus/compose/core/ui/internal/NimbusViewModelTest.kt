package br.zup.com.nimbus.compose.core.ui.internal

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import br.zup.com.nimbus.compose.core.ui.internal.util.CoroutinesTestExtension
import br.zup.com.nimbus.compose.core.ui.internal.util.RandomData
import br.zup.com.nimbus.compose.core.ui.internal.util.RandomData.jsonExample
import br.zup.com.nimbus.compose.core.ui.internal.util.invokeHiddenMethod
import br.zup.com.nimbus.compose.model.NimbusPageState
import br.zup.com.nimbus.compose.model.Page
import com.zup.nimbus.core.ServerDrivenNavigator
import com.zup.nimbus.core.network.ViewRequest
import com.zup.nimbus.core.render.Listener
import com.zup.nimbus.core.render.Renderer
import com.zup.nimbus.core.render.ServerDrivenView
import com.zup.nimbus.core.tree.ServerDrivenNode
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(CoroutinesTestExtension::class)
class NimbusViewModelTest : BaseTest() {
    private val serverDrivenView: ServerDrivenView = mockk()
    private val serverDrivenNode: ServerDrivenNode = mockk()
    private val renderer: Renderer = mockk()
    private val page: Page = mockk()
    private lateinit var viewModel: NimbusViewModel

    //Slots
    private val pageOnChangeSlot = slot<Listener>()
    lateinit var serverDrivenNavigatorSlot: ServerDrivenNavigator

    private val pageManagerAddSlot = mutableListOf<Page>()

    @BeforeEach
    fun before() {
        viewModel = NimbusViewModel(nimbusConfig = nimbusConfig, pagesManager = pagesManager)
        every { pagesManager.add(capture(pageManagerAddSlot)) } answers { true }
        every { nimbusConfig.core.createView(captureLambda(), any()) } answers {
            serverDrivenNavigatorSlot = lambda<() -> ServerDrivenNavigator>().captured.invoke()
            serverDrivenView
        }
        every { serverDrivenView.renderer } returns renderer
        every { renderer.paint(any()) } just Runs
        every { serverDrivenView.onChange(capture(pageOnChangeSlot)) } answers { serverDrivenNode }
        every { pagesManager.popLastPage() } returns true

        clearMocks(pagesManager, answers = false)
        pageManagerAddSlot.clear()
        pageOnChangeSlot.clear()
    }

    @DisplayName("When initFirstViewWithRequest")
    @Nested
    inner class ViewWithRequest {
        @DisplayName("Then should post PageStateOnLoading and PageStateOnShowPage")
        @Test
        fun testGivenAViewRequestWhenInitFirstViewShouldLoadingOnShowPage() = runTest {
            val expectedFirstEmission = NimbusPageState.PageStateOnLoading
            val expectedSecondEmission = NimbusPageState.PageStateOnShowPage(serverDrivenNode)
            val page = initFirstViewWithSuccess()

            //Then
            page.content.test {
                assertEquals(expectedFirstEmission, awaitItem())
                assertEquals(expectedSecondEmission, awaitItem())
            }
            verify(exactly = 1) { pagesManager.add(any()) }
        }

        @DisplayName("Then should post PageStateOnLoading and PageStateOnError then retry with success")
        @Test
        fun testGivenAViewRequestWhenInitFirstViewShouldPageStateOnError() = runTest {

            // Given
            val viewRequest = ViewRequest(url = RandomData.httpUrl())
            val expectedException = RuntimeException("Any Exception")
            val expectedLoading = NimbusPageState.PageStateOnLoading
            val expectedPageError =
                NimbusPageState.PageStateOnError(throwable = expectedException, retry = {})
            val expectedOnShowPage = NimbusPageState.PageStateOnShowPage(serverDrivenNode)

            coEvery { nimbusConfig.core.viewClient.fetch(any()) } throws expectedException

            //When
            viewModel.initFirstViewWithRequest(viewRequest)

            val page = pageManagerAddSlot.last()

            //Then
            page.content.test {
                val secondItem = shouldEmitLoadingAndError(expectedLoading, expectedPageError)

                shouldEmitLoadingAndShowPageAfterRetry(secondItem,
                    expectedLoading,
                    expectedOnShowPage)
            }
            verify(exactly = 1) { pagesManager.add(any()) }
        }
    }

    private fun initFirstViewWithSuccess(): Page {
        // Given
        val viewRequest = ViewRequest(url = RandomData.httpUrl())

        shouldEmitRenderNodeFromCore(renderNode)

        //When
        viewModel.initFirstViewWithRequest(viewRequest)

        val page = pageManagerAddSlot.last()

        emitOnChangeServerDrivenNode(serverDrivenNode)
        return page
    }

    @DisplayName("When initFirstViewWithJson")
    @Nested
    inner class ViewWithJson {
        @DisplayName("Then should post PageStateOnLoading and PageStateOnShowPage")
        @Test
        fun testGivenAJsonWhenInitFirstViewShouldLoadingOnShowPage() = runTest {

            // Given
            val json = jsonExample()
            val expectedFirstEmission = NimbusPageState.PageStateOnLoading
            val expectedSecondEmission = NimbusPageState.PageStateOnShowPage(serverDrivenNode)
            shouldEmitRenderNodeFromCore(renderNode)

            //When
            viewModel.initFirstViewWithJson(json)

            val page = pageManagerAddSlot.last()

            emitOnChangeServerDrivenNode(serverDrivenNode)

            //Then
            page.content.test {
                assertEquals(expectedFirstEmission, awaitItem())
                assertEquals(expectedSecondEmission, awaitItem())
            }
            verify(exactly = 1) { pagesManager.add(any()) }
        }

        @DisplayName("Then should post PageStateOnLoading and PageStateOnError then retry with success")
        @Test
        fun testGivenAJsonWhenInitFirstViewShouldPageStateOnError() = runTest {

            // Given
            val json = jsonExample()
            val expectedException = RuntimeException("Any Exception")
            val expectedLoading = NimbusPageState.PageStateOnLoading
            val expectedPageError =
                NimbusPageState.PageStateOnError(throwable = expectedException, retry = {})
            val expectedOnShowPage = NimbusPageState.PageStateOnShowPage(serverDrivenNode)

            shouldEmitExceptionFromCore(expectedException)

            //When
            viewModel.initFirstViewWithJson(json)

            val page = pageManagerAddSlot.last()

            //Then
            page.content.test {
                val secondItem = shouldEmitLoadingAndError(expectedLoading, expectedPageError)

                shouldEmitLoadingAndShowPageAfterRetry(secondItem,
                    expectedLoading,
                    expectedOnShowPage)
            }

            verify(exactly = 1) { pagesManager.add(any()) }
        }
    }

    @DisplayName("When receive a ServerDrivenNavigator event")
    @Nested
    inner class Navigator {
        @DisplayName("Then should post NimbusViewModelNavigationState.Pop")
        @Test
        fun testGivenAPopNavigationEventShouldPostNimbusViewModelNavigationStatePop() = runTest {

            val url = RandomData.httpUrl()
            val secondViewRequest = ViewRequest(url)
            //Given
            val expectedFirstEmission = NimbusPageState.PageStateOnLoading
            val expectedSecondEmission = NimbusPageState.PageStateOnShowPage(serverDrivenNode)
            val expectedNavigationFirstEmission = NimbusViewModelNavigationState.Push(
                url)
            val expectedNavigationSecondEmission = NimbusViewModelNavigationState.Pop

            //When
            val page = initFirstViewWithSuccess()

            //Then
            page.content.test {
                assertEquals(expectedFirstEmission, awaitItem())
                assertEquals(expectedSecondEmission, awaitItem())
            }

            verify(exactly = 1) { pagesManager.add(any()) }

            //After that push another view and pops it
            serverDrivenNavigatorSlot.push(secondViewRequest)
            serverDrivenNavigatorSlot.pop()

            viewModel.nimbusViewNavigationState.test {

                assertEquals(expectedNavigationFirstEmission, awaitItem())
                assertEquals(expectedNavigationSecondEmission, awaitItem())

            }

            verify(exactly = 2) { pagesManager.add(any()) }
            verify(exactly = 1) { pagesManager.popLastPage() }
        }

        @DisplayName("Then should post NimbusViewModelNavigationState.OnShowModalModalState")
        @Test
        fun testGivenAPresentNavigationEventShouldPostOnShowModalModalState() = runTest {
            //Given
            val url = RandomData.httpUrl()
            val viewRequest = ViewRequest(url)
            val expectedModalState = NimbusViewModelModalState.OnShowModalModalState(viewRequest)
            val expectedSecondModalState = NimbusViewModelModalState.OnHideModalState

            //When
            initFirstViewWithSuccess()
            serverDrivenNavigatorSlot.present(viewRequest)
            serverDrivenNavigatorSlot.dismiss()

            //Then
            viewModel.nimbusViewModelModalState.test {
                assertEquals(expectedModalState, awaitItem())
                assertEquals(expectedSecondModalState, awaitItem())
            }

        }

        @DisplayName("Then should post NimbusViewModelNavigationState.PopTo(url)")
        @Test
        fun testGivenAPopToUrlEventShouldPostPopTo() = runTest {
            //Given
            val url = RandomData.httpUrl()
            val expectedState = NimbusViewModelNavigationState.PopTo(url)

            every {  pagesManager.getPageBy(url) } returns page
            every {  pagesManager.removePagesAfter(page) } just Runs

            //When
            initFirstViewWithSuccess()
            serverDrivenNavigatorSlot.popTo(url)

            //Then
            viewModel.nimbusViewNavigationState.test {
                assertEquals(expectedState, awaitItem())
            }

        }
    }

    private suspend fun FlowTurbine<NimbusPageState>.shouldEmitLoadingAndShowPageAfterRetry(
        secondItem: NimbusPageState.PageStateOnError,
        expectedLoading: NimbusPageState.PageStateOnLoading,
        expectedOnShowPage: NimbusPageState.PageStateOnShowPage,
    ) {
        shouldEmitRenderNodeFromCore(renderNode)
        //Simulates user clicking retry button on error screen
        secondItem.retry.invoke()
        emitOnChangeServerDrivenNode(serverDrivenNode)

        val thirdItem = awaitItem()
        assertEquals(expectedLoading, thirdItem)

        val fourthItem = awaitItem()
        assertEquals(expectedOnShowPage, fourthItem)
    }

    @DisplayName("When receive a view model method call")
    @Nested
    inner class ViewModel {
        @DisplayName("Then should return false when in first page")
        @Test
        fun testGivenAPopWithOnlyOnePageShouldReturnFalse() = runTest {
            val expectedPop = false

            //Given
            every { pagesManager.popLastPage() } returns false

            //When
            val pop = viewModel.pop()

            //Then
            verify(exactly = 1) { pagesManager.popLastPage() }
            assertEquals(expectedPop , pop)
        }

        @DisplayName("Then should return the page when getPageBy")
        @Test
        fun testGivenAGetPageByShouldReturnThePage() = runTest {
            val expectedPage = page
            val url = RandomData.httpUrl()

            //Given
            every { pagesManager.getPageBy(url) } returns page

            //When
            val page = viewModel.getPageBy(url)

            //Then
            verify(exactly = 1) { pagesManager.getPageBy(url) }
            assertEquals(expectedPage , page)
        }

        @DisplayName("Then should return the page count when getPageCount")
        @Test
        fun testGivenAGetPageCountShouldReturnThePageCount() = runTest {
            val expectedPageCount = RandomData.int()

            //Given
            every { pagesManager.getPageCount() } returns expectedPageCount

            //When
            val result = viewModel.getPageCount()

            //Then
            verify(exactly = 1) { pagesManager.getPageCount() }
            assertEquals(expectedPageCount , result)
        }

        @DisplayName("Then should dispose the pages")
        @Test
        fun testGivenDisposeCallShouldDisposePages() = runTest {

            //Given
            every { pagesManager.removeAllPages() } just Runs

            //When
            viewModel.dispose()

            //Then
            verify(exactly = 1) { pagesManager.removeAllPages() }
        }

        @DisplayName("Then should dispose the pages when cleared")
        @Test
        fun testGivenClearedShouldDisposePages() = runTest {
            //Given
            every { pagesManager.removeAllPages() } just Runs

            //When
            viewModel.invokeHiddenMethod("onCleared")

            //Then
            verify(exactly = 1) { pagesManager.removeAllPages() }
        }
    }

    @DisplayName("When receive an event to change model to hidden state")
    @Nested
    inner class ViewModelModalState {
        @DisplayName("Then should return receive a hidden state emission")
        @Test
        fun testGivenAPopWithOnlyOnePageShouldReturnFalse() = runTest {
            val expectedModalState = NimbusViewModelModalState.HiddenModalState

            //When
            viewModel.setModalHiddenState()

            //Then
            viewModel.nimbusViewModelModalState.test {
                assertEquals(expectedModalState, awaitItem())
            }
        }
    }

    private suspend fun FlowTurbine<NimbusPageState>.shouldEmitLoadingAndError(
        expectedLoading: NimbusPageState.PageStateOnLoading,
        expectedPageError: NimbusPageState.PageStateOnError,
    ): NimbusPageState.PageStateOnError {
        val firstItem = awaitItem()
        assertEquals(expectedLoading, firstItem)

        val secondItem = (awaitItem() as NimbusPageState.PageStateOnError)
        assertEquals(expectedPageError.throwable,
            secondItem.throwable)
        return secondItem
    }

    private fun emitOnChangeServerDrivenNode(serverDrivenNode: ServerDrivenNode) {
        val list = pageOnChangeSlot.captured
        list.invoke(serverDrivenNode)
    }
}
