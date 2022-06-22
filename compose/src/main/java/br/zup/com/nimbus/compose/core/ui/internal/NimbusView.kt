package br.zup.com.nimbus.compose.core.ui.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.zup.com.nimbus.compose.NimbusTheme
import br.zup.com.nimbus.compose.model.NimbusPageState
import br.zup.com.nimbus.compose.model.Page

@Composable
internal fun NimbusView(
    page: Page,
) {
    var loading by remember { mutableStateOf(true) }

    RenderPageState(page) { isLoading ->
        if (loading != isLoading)
            loading = isLoading
    }

    if (loading) {
        NimbusTheme.nimbusAppState.config.loadingView()
    }
}

@Composable
internal fun RenderPageState(
    page: Page,
    onLoading: (Boolean) -> Unit,
) {
    var nimbusPageState: NimbusPageState by remember {
        mutableStateOf(NimbusPageState.PageStateOnLoading)
    }

    with(nimbusPageState) {
        when (this) {
            is NimbusPageState.PageStateOnLoading -> {
                onLoading(true)
            }
            is NimbusPageState.PageStateOnError -> {
                onLoading(false)
                NimbusTheme.nimbusAppState.config.errorView(
                    this.throwable,
                    this.retry
                )
            }
            is NimbusPageState.PageStateOnShowPage -> {
                NimbusServerDrivenView(viewTree = this.serverDrivenNode)
                onLoading(false)
            }
        }
    }

    CollectSharedFlow(page.content) {
        nimbusPageState = it
    }
}

