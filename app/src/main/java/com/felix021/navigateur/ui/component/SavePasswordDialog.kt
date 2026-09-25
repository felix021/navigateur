package com.felix021.navigateur.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.ui.BrowserController

@Composable
fun SavePasswordDialog(
    prompt: BrowserController.SavePrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存密码？") },
        text = {
            Column {
                Text("网站：${prompt.host}")
                Spacer(Modifier.height(4.dp))
                Text("账号：${prompt.username}")
                Spacer(Modifier.height(4.dp))
                Text("密码：••••••••")
                prompt.replacing?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "将替换该站点已保存的账号 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("不保存") } },
    )
}
