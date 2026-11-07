package com.personal.diary.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.diary.data.Record
import java.time.LocalDate

/**
 * 首页：当天记录页（Pager index 1）。
 * 顶部左：selectedDate；右上：倒数日按钮。
 * 中间：am / pm 两个区块。底部：固定输入区。
 */
@Composable
fun HomePage(
    vm: DiaryViewModel,
    countdownText: String?,
    onCountdownClick: () -> Unit
) {
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()

    // selectedDate 不等于今天时禁止输入
    val isToday = selectedDate == today

    var input by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Record?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ui.Background)
    ) {
        // 顶部区域，高 56dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Ui.TopBarHeight)
                .padding(horizontal = Ui.PageHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDateTitle(selectedDate),
                color = Ui.TextPrimary,
                fontSize = Ui.DateTitleSize
            )
            Spacer(Modifier.weight(1f))
            // 倒数日模块：可点击按钮
            Text(
                text = countdownText ?: "设置倒数日",
                color = if (countdownText != null) Ui.TextPrimary else Ui.Timestamp,
                fontSize = Ui.ActionTextSize,
                modifier = Modifier
                    .clickableNoRipple(onCountdownClick)
                    .padding(start = Ui.TimestampGap)
            )
        }

        // 中间内容区：严格 am(0:00-11:59) / pm(12:00-23:59) 两块
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            RecordSection(
                label = "am",
                items = records.filter { it.isAm },
                onLongPress = { deleteTarget = it }
            )
            RecordSection(
                label = "pm",
                items = records.filter { !it.isAm },
                onLongPress = { deleteTarget = it }
            )
        }

        // 底部固定输入区
        InputBar(
            value = input,
            enabled = isToday,
            onValueChange = { input = it },
            onSend = {
                vm.addRecord(input)
                input = ""
            }
        )
    }

    // 长按删除确认：原生 AlertDialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条记录？") },
            text = { Text(target.content, color = Ui.TextPrimary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteRecord(target.id)
                        deleteTarget = null
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

/** am / pm 区块：标签上下各 16dp；标签颜色 #9A9A9A，小写 11sp。 */
@Composable
private fun RecordSection(
    label: String,
    items: List<Record>,
    onLongPress: (Record) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Ui.Timestamp,
            fontSize = Ui.SectionLabelSize,
            modifier = Modifier.padding(
                horizontal = Ui.PageHorizontalPadding,
                vertical = Ui.SectionLabelPadding
            )
        )
        items.forEach { record ->
            RecordRow(record = record, onLongPress = { onLongPress(record) })
        }
    }
}

/** 一条记录：左侧正文 16sp，右侧时间戳 13sp，同一行基线对齐、右对齐。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordRow(
    record: Record,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(
                horizontal = Ui.PageHorizontalPadding,
                vertical = Ui.RecordVerticalPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = record.content,
            color = Ui.TextPrimary,
            fontSize = Ui.RecordContentSize,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline()
        )
        Text(
            text = formatTimestamp(record.timeString),
            color = Ui.Timestamp,
            fontSize = Ui.TimestampSize,
            modifier = Modifier
                .padding(start = Ui.TimestampGap)
                .alignByBaseline()
        )
    }
}

/** 底部固定输入区：上方 0.5dp #EEEEEE 细线；输入框无边框无圆角；发送按钮纯黑文字。 */
@Composable
private fun InputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = enabled && value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 底部安全区（safeDrawing.bottom = max(导航栏, IME)），避免与根布局重复叠加
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            )
    ) {
        HorizontalDivider(
            thickness = Ui.DividerThickness,
            color = Ui.Hairline,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Ui.PageHorizontalPadding,
                    vertical = Ui.InputRowVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                // 仅当 selectedDate 不等于今天时，占位文字为"仅可记录今天"
                if (value.isEmpty() && !enabled) {
                    Text(
                        text = "仅可记录今天",
                        color = Ui.Placeholder,
                        fontSize = Ui.InputTextSize
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Ui.TextPrimary,
                        fontSize = Ui.InputTextSize
                    ),
                    cursorBrush = SolidColor(Ui.TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "发送",
                color = if (canSend) Ui.TextPrimary else Ui.SendDisabled,
                fontSize = Ui.ActionTextSize,
                modifier = Modifier
                    .padding(start = Ui.InputHorizontalPadding)
                    .clickableNoRipple { if (canSend) onSend() }
            )
        }
    }
}

/** 无涟漪点击：保持原生极简，不加自定义动效。 */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    )
}

/** 日期标题，如 11月7日 */
private fun formatDateTitle(date: LocalDate): String =
    "${date.monthValue}月${date.dayOfMonth}日"

/** 时间戳显示，如 9:00（去掉小时前导零） */
private fun formatTimestamp(time: String): String {
    val parts = time.split(":")
    if (parts.size != 2) return time
    val hour = parts[0].trimStart('0').ifEmpty { "0" }
    return "$hour:${parts[1]}"
}
