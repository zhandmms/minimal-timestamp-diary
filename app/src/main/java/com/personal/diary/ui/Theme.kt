package com.personal.diary.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全项目视觉常量。严格按【视觉规格】固定，禁止在页面里散写魔法数值。
 */
object Ui {
    // 颜色
    val Background = Color(0xFFFFFFFF)      // 纯白背景
    val TextPrimary = Color(0xFF111111)     // 主文字
    val Timestamp = Color(0xFF9A9A9A)       // 时间戳
    val Placeholder = Color(0xFFB0B0B0)     // 占位符
    val Hairline = Color(0xFFEEEEEE)        // 底部输入区上方 0.5dp 细线
    val SendDisabled = Color(0xFFC8C8C8)    // 发送按钮不可用态

    // 尺寸
    val PageHorizontalPadding = 20.dp       // 页面左右边距
    val TopBarHeight = 56.dp                // 顶部区域高
    val SectionLabelPadding = 16.dp         // am / pm 上下各留 16dp
    val RecordVerticalPadding = 12.dp       // 每条记录上下 padding
    val DividerThickness = 0.5.dp           // 底部输入区细线
    val DotSize = 6.dp                      // 日历圆点
    val DotSpacing = 3.dp                   // 圆点在日期数字正下方 3dp
    val CalendarCellHeight = 48.dp          // 日历单元格高度（规格未指定，固定常量）
    val DayCellWidth = 44.dp                // 日历单日点击区宽度
    val InputRowVerticalPadding = 8.dp      // 底部输入行上下留白
    val InputHorizontalPadding = 16.dp      // 输入框与发送按钮之间的间距
    val WeekHeaderVerticalPadding = 8.dp    // 星期表头上下留白
    val MonthArrowPaddingH = 12.dp          // 月份箭头左右点击热区
    val MonthArrowPaddingV = 4.dp           // 月份箭头上下点击热区
    val MonthTitleWidth = 120.dp            // 月份标题固定宽度，保证箭头不跳动
    val TimestampGap = 12.dp                // 正文与时间戳之间的间距
    val DialogHorizontalPadding = 16.dp     // Dialog 内容左右留白
    val DialogVerticalPadding = 8.dp        // Dialog 内容上下留白
    val DialogItemGap = 8.dp                // Dialog 内元素间距

    // Dialog 尺寸自适应相关
    val DialogMaxWidth = 420.dp             // 弹窗最大宽度，和平板/横屏下不无限拉宽
    val DialogScreenHMargin = 20.dp         // 弹窗距屏幕左右外边距
    val DialogScreenVMargin = 24.dp         // 弹窗距屏幕上下外边距
    val DialogInnerPadding = 20.dp          // 弹窗内容四周边距（保证日历 7 列均匀）
    val DialogDayCellMaxHeight = 44.dp      // 日历单日格子最大高度
    val DialogDayCellMinHeight = 28.dp      // 日历单日格子最小高度（极矮屏兜底，仍不滚动）
    val DialogSelectedCircle = 34.dp        // 选中日期圆点直径
    val DialogWeekHeaderHeight = 20.dp      // 弹窗内星期表头高度

    // 字号
    val DateTitleSize = 15.sp               // 日期标题
    val SectionLabelSize = 11.sp            // am / pm 小写
    val RecordContentSize = 16.sp           // 记录正文
    val TimestampSize = 13.sp               // 时间戳
    val InputTextSize = 16.sp               // 输入框文字
    val ActionTextSize = 15.sp              // 发送按钮 / 返回今天 / 倒数日文字
    val MonthArrowSize = 17.sp              // 月份切换箭头 ‹ ›
}

private val DiaryLightColors = lightColorScheme(
    primary = Ui.TextPrimary,
    onPrimary = Ui.Background,
    secondary = Ui.TextPrimary,
    onSecondary = Ui.Background,
    background = Ui.Background,
    onBackground = Ui.TextPrimary,
    surface = Ui.Background,
    onSurface = Ui.TextPrimary,
    surfaceVariant = Ui.Background,
    onSurfaceVariant = Ui.TextPrimary,
    outline = Ui.Hairline,
)

/**
 * 强制浅色主题：只用 lightColorScheme，不调用 dynamicColor，不跟随系统深色模式。
 */
@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiaryLightColors,
        content = content
    )
}
