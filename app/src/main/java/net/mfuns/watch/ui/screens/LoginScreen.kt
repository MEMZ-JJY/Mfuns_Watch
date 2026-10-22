package net.mfuns.watch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun LoginScreen(
    state: net.mfuns.watch.ui.UiState<Unit>,
    onLogin: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loading = state is net.mfuns.watch.ui.UiState.Loading
    val errorMessage = (state as? net.mfuns.watch.ui.UiState.Error)?.message
    val listState = rememberScalingLazyListState()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            listState.animateScrollToItem(4)
        }
    }

    Scaffold(timeText = { TimeText() }, modifier = modifier) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "Mfuns Watch",
                    style = MaterialTheme.typography.title1,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                WatchTextField(
                    value = account,
                    onValueChange = { account = it },
                    placeholder = "账号",
                    keyboardType = KeyboardType.Text,
                    enabled = !loading,
                )
            }
            item {
                WatchTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "密码",
                    keyboardType = KeyboardType.Password,
                    enabled = !loading,
                    imeAction = ImeAction.Done,
                    onImeAction = { onLogin(account, password) },
                )
            }
            item {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = { onLogin(account, password) }) {
                        Text("登录")
                    }
                }
            }
            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun WatchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.body1.copy(
                    color = MaterialTheme.colors.onSurface,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colors.primary),
                visualTransformation = if (keyboardType == KeyboardType.Password) {
                    PasswordVisualTransformation()
                } else {
                    androidx.compose.ui.text.input.VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { onImeAction() },
                    onSearch = { onImeAction() },
                    onGo = { onImeAction() },
                ),
                modifier = Modifier.fillMaxSize(),
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colors.onSurfaceVariant),
        )
    }
}
