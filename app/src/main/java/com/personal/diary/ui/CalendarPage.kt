package com.personal.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth

/**
 * 日历页（Pager index 0）。
 * 月份切换只用顶部 ‹ 2026年11月 › 两个箭头按钮 + 底部精准跳转文字按钮，
 * 页面内绝不嵌套 Pager，避免横向手势与外层翻页冲突。
 */
@Composable
fun CalendarPage(
    vm: DiaryViewModel,
    onDayClick: (LocalDate) -> Unit
) {
    val visibleMonth by vm.visibleMonth.collectAsStateWithLifecycle()
    val markedDates by vm.monthRecordDates.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()

    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ui.Background)
    ) {
        // 顶部 56dp：中间 ‹ 2026年11月 ›（年份随月份同步更新）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Ui.TopBarHeight)
                .padding(horizontal = Ui.PageHorizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            MonthSwitcher(
                month = visibleMonth,
                onPrev = { vm.previousMonth() },
                onNext = { vm.nextMonth() }
            )
        }

        // 主体可滚动，避免小屏/横屏时最后一周被裁掉
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // 星期表头：每周从周一开始
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Ui.PageHorizontalPadding,
                        vertical = Ui.WeekHeaderVerticalPadding
                    )
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

            MonthGrid(
                month = visibleMonth,
                today = today,
                markedDates = markedDates,
                onDayClick = onDayClick
            )
        }

        // 底部精准跳转功能区：无边框文字按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Ui.PageHorizontalPadding,
                    vertical = Ui.WeekHeaderVerticalPadding
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "跳转年份",
                color = Ui.TextPrimary,
                fontSize = Ui.ActionTextSize,
                modifier = Modifier.clickableNoRipple { showYearPicker = true }
            )
            Text(
                text = "跳转月份",
                color = Ui.TextPrimary,
                fontSize = Ui.ActionTextSize,
                modifier = Modifier.clickableNoRipple { showMonthPicker = true }
            )
            Text(
                text = "返回今天",
                color = Ui.TextPrimary,
                fontSize = Ui.ActionTextSize,
                modifier = Modifier.clickableNoRipple { vm.backToToday() }
            )
        }
    }

    if (showYearPicker) {
        NumberPickerDialog(
            title = "选择年份",
            values = YEAR_RANGE.toList(),
            current = visibleMonth.year,
            labelOf = { "${it}年" },
            onDismiss = { showYearPicker = false },
            onPick = { year ->
                vm.jumpToYear(year)
                showYearPicker = false
            }
        )
    }

    if (showMonthPicker) {
        NumberPickerDialog(
            title = "选择月份",
            values = (1..12).toList(),
            current = visibleMonth.monthValue,
            labelOf = { "${it}月" },
            onDismiss = { showMonthPicker = false },
            onPick = { month ->
                vm.jumpToMonth(month)
                showMonthPicker = false
            }
        )
    }
}

/** ‹ 2026年11月 › 月份切换：两个箭头按钮，不涉及横向手势。 */
@Composable
private fun MonthSwitcher(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "‹",
            color = Ui.TextPrimary,
            fontSize = Ui.MonthArrowSize,
            modifier = Modifier
                .clickableNoRipple(onPrev)
                .padding(
                    horizontal = Ui.MonthArrowPaddingH,
                    vertical = Ui.MonthArrowPaddingV
                )
        )
        Text(
            text = "${month.year}年${month.monthValue}月",
            color = Ui.TextPrimary,
            fontSize = Ui.DateTitleSize,
            modifier = Modifier.width(Ui.MonthTitleWidth),
            textAlign = TextAlign.Center
        )
        Text(
            text = "›",
            color = Ui.TextPrimary,
            fontSize = Ui.MonthArrowSize,
            modifier = Modifier
                .clickableNoRipple(onNext)
                .padding(
                    horizontal = Ui.MonthArrowPaddingH,
                    vertical = Ui.MonthArrowPaddingV
                )
        )
    }
}

/** 月历网格：当月天数用 YearMonth.lengthOfMonth()，首格偏移 = 当月1日.dayOfWeek.value - 1。 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    markedDates: Set<String>,
    onDayClick: (LocalDate) -> Unit
) {
    val cells = remember(month) {
        val offset = month.atDay(1).dayOfWeek.value - 1
        buildList<LocalDate?> {
            repeat(offset) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.PageHorizontalPadding)
    ) {
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(Ui.CalendarCellHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                hasRecord = markedDates.contains(date.toString()),
                                onClick = { onDayClick(date) }
                            )
                        }
                    }
                }
                repeat(DAYS_PER_WEEK - week.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 日期数字；有记录时正下方 3dp 处画 6dp 圆点。 */
@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickableNoRipple(onClick)
            .size(width = Ui.DayCellWidth, height = Ui.CalendarCellHeight)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = Ui.TextPrimary,
            fontSize = Ui.RecordContentSize,
            fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Ui.DotSpacing))
        Box(
            modifier = Modifier
                .size(Ui.DotSize)
                .background(
                    color = if (hasRecord) Ui.TextPrimary else Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}

private const val DAYS_PER_WEEK = 7
private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

/** 年份可选范围：过去 10 年到未来 10 年 */
private val YEAR_RANGE = (LocalDate.now().year - 10)..(LocalDate.now().year + 10)
