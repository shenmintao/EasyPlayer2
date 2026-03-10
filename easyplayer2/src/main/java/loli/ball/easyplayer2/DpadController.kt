package loli.ball.easyplayer2

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 电视遥控器 D-pad 控制器
 * 为播放器提供遥控器按键支持：
 * - 左/右方向键：快退/快进（默认10秒）
 * - 中心键/Enter/空格：播放/暂停
 * - 上/下方向键：显示/隐藏控制栏
 * - 媒体键：播放/暂停/快进/快退/下一集
 */
@Composable
fun DpadVideoController(
    vm: ControlViewModel,
    modifier: Modifier = Modifier,
    seekStepMs: Long = 10_000L,
    enabled: Boolean = true,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var seekIndicatorJob by remember { mutableStateOf<Job?>(null) }

    // 自动请求焦点，确保可以接收按键事件
    LaunchedEffect(enabled) {
        if (enabled) {
            focusRequester.requestFocus()
        }
    }

    if (!enabled) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when (keyEvent.nativeKeyEvent.keyCode) {
                    // 播放/暂停
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_SPACE -> {
                        vm.onPlayPause(!vm.playWhenReady)
                        // 显示控制栏
                        vm.onSingleClick()
                        true
                    }

                    // 快进
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val newPosition = (vm.position + seekStepMs)
                            .coerceAtMost(vm.during)
                        vm.exoPlayer.seekTo(newPosition)
                        // 显示控制栏
                        vm.onSingleClick()
                        // 取消之前的隐藏任务，延迟隐藏
                        seekIndicatorJob?.cancel()
                        seekIndicatorJob = scope.launch {
                            delay(2000)
                        }
                        true
                    }

                    // 快退
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val newPosition = (vm.position - seekStepMs)
                            .coerceAtLeast(0)
                        vm.exoPlayer.seekTo(newPosition)
                        // 显示控制栏
                        vm.onSingleClick()
                        seekIndicatorJob?.cancel()
                        seekIndicatorJob = scope.launch {
                            delay(2000)
                        }
                        true
                    }

                    // 上/下方向键 - 显示/隐藏控制栏
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        vm.onSingleClick()
                        true
                    }

                    // 下一集
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        onNext()
                        true
                    }

                    // 上一集
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        onPrevious()
                        true
                    }

                    // 停止
                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        vm.onPlayPause(false)
                        true
                    }

                    else -> false
                }
            }
    )
}
