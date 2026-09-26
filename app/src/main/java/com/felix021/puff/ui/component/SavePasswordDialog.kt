package com.felix021.puff.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.felix021.puff.ui.component.AppDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.puff.ui.BrowserController
import androidx.compose.ui.res.stringResource
import com.felix021.puff.R

@Composable
fun SavePasswordDialog(
    prompt: BrowserController.SavePrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_password_title)) },
        text = {
            Column {
                Text(stringResource(R.string.save_password_site, prompt.host))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.save_password_account, prompt.username))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.save_password_password))
                prompt.replacing?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.save_password_replacing, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.save_password_no)) } },
    )
}
