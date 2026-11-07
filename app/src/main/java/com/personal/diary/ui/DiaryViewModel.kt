package com.personal.diary.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.diary.data.AppDatabase
import com.personal.diary.data.Record
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 极简 ViewModel：selectedDate、today、当天记录列表、日历月记录集合。
 * 所有日期计算统一走 java.time（LocalDate / YearMonth / ChronoUnit）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).recordDao()

    /** 首页维护的 selectedDate，默认 LocalDate.now()。 */
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** 当前"今天"，由 ON_RESUME 校验与 60 秒周期协程维护，供倒数日等 UI 响应式使用。 */
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** 日历页当前浏览的月份。 */
    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    /** 当前展示日期的记录列表；日期切换时自动重查。 */
    val records: StateFlow<List<Record>> = _selectedDate
        .flatMapLatest { date -> dao.observeByDate(date.format(ISO_DATE)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 日历页：当前浏览月份中有记录的日期集合，一次查询（monthPrefix 如 2026-11-%）。 */
    val monthRecordDates: StateFlow<Set<String>> = _visibleMonth
        .flatMapLatest { ym -> dao.observeMonthDates(monthPrefix(ym)) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** 60 秒周期协程，绑定 ViewModel 生命周期。 */
    private var dayWatcher: Job? = null

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun previousMonth() {
        _visibleMonth.value = _visibleMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _visibleMonth.value = _visibleMonth.value.plusMonths(1)
    }

    /** 精准跳转：切到指定年份，停留该年的"当前月份"（数值与今天同月）。 */
    fun jumpToYear(year: Int) {
        val todayMonth = LocalDate.now().monthValue
        _visibleMonth.value = YearMonth.of(year, todayMonth)
    }

    /** 精准跳转：切到"当前年份"的指定月份。 */
    fun jumpToMonth(month: Int) {
        _visibleMonth.value = YearMonth.of(_visibleMonth.value.year, month)
    }

    /** 返回今天：selectedDate 复位到今天。 */
    fun backToToday() {
        val now = LocalDate.now()
        _selectedDate.value = now
        _visibleMonth.value = YearMonth.from(now)
    }

    /**
     * 跨 0 点校验：比较 LocalDate.now() 与 selectedDate。
     * 若已跨天且当前展示的正是"旧今天"，自动切到新今天（records 由 Flow 自动刷新）。
     * 若用户主动停留在历史日期，则不动。
     */
    fun checkDayRollover() {
        val now = LocalDate.now()
        val previousToday = _today.value
        if (now != previousToday) {
            _today.value = now
            if (_selectedDate.value == previousToday) {
                _selectedDate.value = now
                _visibleMonth.value = YearMonth.from(now)
            }
        }
    }

    /** ON_RESUME 调用：启动 60 秒周期校验协程。 */
    fun startDayWatcher() {
        checkDayRollover()
        if (dayWatcher?.isActive == true) return
        dayWatcher = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                checkDayRollover()
            }
        }
    }

    /** ON_PAUSE 调用：取消周期协程。 */
    fun stopDayWatcher() {
        dayWatcher?.cancel()
        dayWatcher = null
    }

    /**
     * 插入记录：禁止使用 selectedDate 拼接日期，
     * 必须用 System.currentTimeMillis() 现算 dateString / timeString / isAm，
     * 且解析统一使用设备本地时区（ZoneId.systemDefault()），绝不用 UTC。
     */
    fun addRecord(content: String) {
        val text = content.trim()
        if (text.isEmpty()) return
        val now = Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val record = Record(
            dateString = now.format(ISO_DATE),
            timeString = now.format(HOUR_MINUTE),
            isAm = now.hour < 12,
            content = text
        )
        viewModelScope.launch { dao.insert(record) }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    companion object {
        private val ISO_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
        private val HOUR_MINUTE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        private val MONTH_PREFIX: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-", Locale.ROOT)

        /** 生成 LIKE 前缀，如 2026-11-% */
        fun monthPrefix(ym: YearMonth): String = ym.format(MONTH_PREFIX) + "%"
    }
}
