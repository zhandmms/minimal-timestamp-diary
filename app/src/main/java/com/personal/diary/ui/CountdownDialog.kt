package com.personal.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth

/**
 * 倒数日设置 Dialog：高度、宽度均自适应。
 *
 * 尺寸策略（关键）：
 * - 高度：整个弹窗 Wrap Content，内部不放任何 verticalScroll；
 *   日历行高按"屏幕剩余可用高度 / 该月所需周数"实时计算，保证当月所有天数
 *   （含 23-31 号）一屏完整平铺，日历内部绝不折叠、绝不上下滑动。
 * - 宽度：弹窗宽度取 min(可用宽度, 420dp)，日历 7 列用 weight(1f) 均分，
 *   每列宽度完全相同，不会出现最左/最右列被挤压的情况。
 * - 边界：外层 BoxWithConstraints 先扣掉屏幕左右 20dp、上下 24dp 外边距，
 *   因此弹窗永远不会溢出屏幕。
 *
 * 说明：这里没有使用 Material3 的 DatePickerDialog + DatePicker 组合，原因是
 * 其内部对 Surface 硬编码 requiredWidth(360dp) 与 heightIn(max = 568dp)，
 * 且月历单元格硬编码 requiredSize(48dp)，7 列共需 336dp。在 360dp 宽的手机上
 * 扣掉任何边距后都不足 336dp，必然把最左/最右列裁掉——这正是原挤压问题的根因。
 * 这两处尺寸用 required* 施加，外部 Modifier 无法覆盖。故保留 Material3 的
 * Dialog 外观基元（Surface / OutlinedTextField / TextButton）而自绘等分月历。
 */
@Composable
fun CountdownDialog(
    store: CountdownStore,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }

    var name by rememberSaveable { mutableStateOf(store.name()) }

    // 已有配置且未过期则预选，否则默认选中今天
    val initialSelected = remember {
        store.target()?.takeIf { !it.isBefore(today) } ?: today
    }
    var selectedEpochDay by rememberSaveable { mutableStateOf(initialSelected.toEpochDay()) }
    var displayedMonthEpochDay by rememberSaveable {
        mutableStateOf(YearMonth.from(initialSelected).atDay(1).toEpochDay())
    }

    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val displayedMonth = YearMonth.from(LocalDate.ofEpochDay(displayedMonthEpochDay))
    val currentMonth = YearMonth.from(today)
    // 只能选今日及以后：不允许浏览到当前月之前的月份
    val canGoPrevMonth = displayedMonth > currentMonth

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = Ui.DialogScreenHMargin,
                    vertical = Ui.DialogScreenVMargin
                ),
            contentAlignment = Alignment.Center
        ) {
            val dialogWidth: Dp = minOf(maxWidth, Ui.DialogMaxWidth)

            val daysInMonth = displayedMonth.lengthOfMonth()
            val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
            val weekRows = (firstDayOffset + daysInMonth + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK

            // 除日历网格外的固定高度开销（取"预留值 > 实际值"，避免大字体时估算偏小误判）
            val fixedHeight = Ui.DialogInnerPadding * 2 +
                INPUT_FIELD_RESERVED +
                Ui.DialogItemGap +
                MONTH_HEADER_RESERVED +
                WEEK_HEADER_RESERVED +
                BUTTON_ROW_RESERVED

            val gridAvailable = maxHeight - fixedHeight
            val rawCell = gridAvailable / weekRows
            // 行高在合理区间内自适应，保证整月一屏铺满且不滚动
            val cellHeight: Dp = rawCell.coerceIn(
                Ui.DialogDayCellMinHeight,
                Ui.DialogDayCellMaxHeight
            )
            val circleSize = minOf(cellHeight - 6.dp, Ui.DialogSelectedCircle)
            // 内容总高是否超出可用高度（仅横屏/超大字体等极端场景会触发）
            val overflows = fixedHeight + cellHeight * weekRows > maxHeight

            Surface(
                modifier = Modifier
                    .width(dialogWidth)
                    .heightIn(max = maxHeight),
                shape = RectangleShape,   // 纯白极简，无大圆角装饰
                color = Ui.Background
            ) {
                Column(
                    modifier = Modifier
                        .padding(Ui.DialogInnerPadding)
                        // 仅在极端场景（横屏/超大字体）导致总高超出屏幕时，整窗可滚动兜底；
                        // 正常竖屏内容小于可用高度，verticalScroll 不产生任何滚动效果。
                        // 日历网格本身始终完整平铺，绝不折叠、绝不内部滚动。
                        .then(
                            if (overflows) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier
                            }
                        )
                ) {
                    // 事件名称输入框（heightIn 而非固定 height，大字体下可自然增高不被裁）
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("事件名称") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = INPUT_FIELD_MIN_HEIGHT)
                    )

                    // 月份切换：‹ 2026年11月 ›（只用按钮，无横向手势）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MONTH_HEADER_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "‹",
                                color = if (canGoPrevMonth) Ui.TextPrimary else Ui.SendDisabled,
                                fontSize = Ui.MonthArrowSize,
                                modifier = Modifier
                                    .clickable(enabled = canGoPrevMonth) {
                                        displayedMonthEpochDay = YearMonth.from(
                                            LocalDate.ofEpochDay(displayedMonthEpochDay)
                                        ).minusMonths(1).atDay(1).toEpochDay()
                                    }
                                    .padding(
                                        horizontal = Ui.MonthArrowPaddingH,
                                        vertical = Ui.MonthArrowPaddingV
                                    )
                            )
                            Text(
                                text = displayedMonth.let { "${it.year}年${it.monthValue}月" },
                                color = Ui.TextPrimary,
                                fontSize = Ui.DateTitleSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(Ui.MonthTitleWidth)
                            )
                            Text(
                                text = "›",
                                color = Ui.TextPrimary,
                                fontSize = Ui.MonthArrowSize,
                                modifier = Modifier
                                    .clickable {
                                        displayedMonthEpochDay = YearMonth.from(
                                            LocalDate.ofEpochDay(displayedMonthEpochDay)
                                        ).plusMonths(1).atDay(1).toEpochDay()
                                    }
                                    .padding(
                                        horizontal = Ui.MonthArrowPaddingH,
                                        vertical = Ui.MonthArrowPaddingV
                                    )
                            )
                        }
                    }

                    // 星期表头
                    WeekdayHeader()

                    // 完整月历：7 列等分，行数按当月实际所需计算
                    DayGrid(
                        month = displayedMonth,
                        firstDayOffset = firstDayOffset,
                        weekRows = weekRows,
                        today = today,
                        selectedDate = selectedDate,
                        cellHeight = cellHeight,
                        circleSize = circleSize,
                        onSelect = { selectedEpochDay = it.toEpochDay() }
                    )

                    // 取消 / 确定（heightIn 而非固定高度，避免挤压 TextButton 的 40dp 最小高度）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = BUTTON_ROW_HEIGHT),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消", color = Ui.Timestamp, fontSize = Ui.ActionTextSize)
                        }
                        TextButton(
                            onClick = {
                                val trimmed = name.trim()
                                if (trimmed.isNotEmpty() &&
                                    !selectedDate.isBefore(LocalDate.now())
                                ) {
                                    onSave(trimmed, selectedDate)
                                }
                            },
                            enabled = name.isNotBlank()
                        ) {
                            Text(
                                text = "确定",
                                color = if (name.isNotBlank()) Ui.TextPrimary else Ui.SendDisabled,
                                fontSize = Ui.ActionTextSize
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Ui.DialogWeekHeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WEEK_LABELS.forEach { label ->
            Text(
                text = label,
                color = Ui.Timestamp,
                fontSize = Ui.SectionLabelSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 7 列等分月历。行数由当月实际天数与首格偏移决定，一次铺满，无内部滚动。 */
@Composable
private fun DayGrid(
    month: YearMonth,
    firstDayOffset: Int,
    weekRows: Int,
    today: LocalDate,
    selectedDate: LocalDate,
    cellHeight: Dp,
    circleSize: Dp,
    onSelect: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until weekRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0 until DAYS_PER_WEEK) {
                    val dayNumber = row * DAYS_PER_WEEK + col - firstDayOffset + 1
                    val date: LocalDate? =
                        if (dayNumber in 1..month.lengthOfMonth()) month.atDay(dayNumber) else null

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val isSelected = date == selectedDate
                            val isSelectable = !date.isBefore(today)
                            Box(
                                modifier = Modifier
                                    .size(circleSize)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(Ui.TextPrimary, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(enabled = isSelectable) { onSelect(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    color = when {
                                        isSelected -> Ui.Background
                                        !isSelectable -> Ui.SendDisabled
                                        else -> Ui.TextPrimary
                                    },
                                    fontSize = Ui.RecordContentSize,
                                    fontWeight = if (date == today) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7
private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

// 实际尺寸
private val INPUT_FIELD_MIN_HEIGHT = 56.dp
private val MONTH_HEADER_HEIGHT = 40.dp
private val BUTTON_ROW_HEIGHT = 44.dp

// 预留高度（一律 >= 实际值，保证 fixedHeight 估算不会偏小而误判不溢出）
private val INPUT_FIELD_RESERVED = 64.dp
private val MONTH_HEADER_RESERVED = 44.dp
private val WEEK_HEADER_RESERVED = 24.dp
private val BUTTON_ROW_RESERVED = 48.dp
