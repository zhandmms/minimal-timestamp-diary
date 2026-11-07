package com.personal.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.diary.ui.CalendarPage
import com.personal.diary.ui.CountdownDialog
import com.personal.diary.ui.CountdownStore
import com.personal.diary.ui.DiaryTheme
import com.personal.diary.ui.DiaryViewModel
import com.personal.diary.ui.HomePage
import com.personal.diary.ui.Ui
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val vm: DiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // edge-to-edge：状态栏/导航栏透明，内容为纯白背景即白底；
        // 强制深色图标，不跟随系统深色模式。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars = true

        setContent {
            DiaryTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ui.Background)
                        // 只取顶部与左右安全区；底部（含 IME）由各页面内输入区自己处理，
                        // 避免与 imePadding 叠加导致键盘弹出时双重留白。
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                            )
                        )
                ) {
                    // HorizontalPager 共两页：index 0 = 日历页，index 1 = 首页，初始页 = 1
                    val pagerState = rememberPagerState(initialPage = 1) { 2 }
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    val countdown = remember { CountdownStore(context) }
                    var showCountdownDialog by rememberSaveable { mutableStateOf(false) }

                    val today by vm.today.collectAsStateWithLifecycle()
                    var countdownText by remember { mutableStateOf(countdown.displayText(today)) }
                    // 跨天后同步刷新倒数日文字
                    LaunchedEffect(today) {
                        countdownText = countdown.displayText(today)
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        // offscreenPageLimit = 0，关闭页面预加载。
                        // Foundation 1.7.0 中对应参数名为 beyondViewportPageCount，0 即不预加载。
                        beyondViewportPageCount = 0
                    ) { page ->
                        when (page) {
                            // index 0 = 日历页
                            0 -> CalendarPage(
                                vm = vm,
                                onDayClick = { date ->
                                    vm.selectDate(date)
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                }
                            )

                            // index 1 = 首页
                            else -> HomePage(
                                vm = vm,
                                countdownText = countdownText,
                                onCountdownClick = { showCountdownDialog = true }
                            )
                        }
                    }

                    if (showCountdownDialog) {
                        CountdownDialog(
                            store = countdown,
                            onDismiss = { showCountdownDialog = false },
                            onSave = { name, date ->
                                countdown.save(name, date)
                                countdownText = countdown.displayText(LocalDate.now())
                                showCountdownDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ON_RESUME 触发一次日期校验，并启动 60 秒周期协程（ON_PAUSE 取消）
        vm.startDayWatcher()
    }

    override fun onPause() {
        super.onPause()
        vm.stopDayWatcher()
    }
}
