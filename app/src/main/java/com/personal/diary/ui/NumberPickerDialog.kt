package com.personal.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 极简列表选择 Dialog（用于跳转年份/月份）。
 * Material3 原生 Surface + TextButton，纯白无边框，无第三方库。
 * 列表使用 verticalScroll，列表本身可滚动，弹窗高度不溢出屏幕。
 */
@Composable
fun NumberPickerDialog(
    title: String,
    values: List<Int>,
    current: Int,
    labelOf: (Int) -> String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = Ui.DialogScreenHMargin, vertical = Ui.DialogScreenVMargin)
                .width(Ui.DialogMaxWidth)
                .heightIn(max = MAX_LIST_HEIGHT),
            shape = RectangleShape,
            color = Ui.Background
        ) {
            Column(modifier = Modifier.padding(Ui.DialogInnerPadding)) {
                Text(
                    text = title,
                    color = Ui.TextPrimary,
                    fontSize = Ui.DateTitleSize,
                    modifier = Modifier.padding(bottom = Ui.DialogItemGap)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = MAX_LIST_SCROLL_HEIGHT)
                        .verticalScroll(rememberScrollState())
                ) {
                    values.forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Ui.CalendarCellHeight)
                                .clickable { onPick(value) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = labelOf(value),
                                color = if (value == current) Ui.TextPrimary else Ui.Timestamp,
                                fontSize = Ui.RecordContentSize,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Ui.Timestamp, fontSize = Ui.ActionTextSize)
                    }
                }
            }
        }
    }
}

private val MAX_LIST_HEIGHT = 420.dp
private val MAX_LIST_SCROLL_HEIGHT = 340.dp
