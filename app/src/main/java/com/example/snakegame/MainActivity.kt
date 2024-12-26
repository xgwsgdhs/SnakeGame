package com.example.snakegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.absoluteValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button


enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 生成随机食物位置
        fun generateRandomFoodPosition(screenSize: Size, blockPosition: Offset): Offset {
            if (screenSize.width <= 0f || screenSize.height <= 0f) {
                return Offset(100f, 100f) // 返回默认位置
            }

            var x: Float
            var y: Float
            do {
                x = (0..(screenSize.width.toInt() / 50 - 1)).random() * 50f
                y = (0..(screenSize.height.toInt() / 50 - 1)).random() * 50f
            } while (x == blockPosition.x && y == blockPosition.y) // 避免与蛇重合

            return Offset(x, y)
        }



        // 检测是否发生碰撞
        fun isCollision(blockPosition: Offset, foodPosition: Offset): Boolean {
            val threshold = 50f // 允许的误差范围（与方块大小一致）
            return (blockPosition.x - foodPosition.x).absoluteValue < threshold &&
                    (blockPosition.y - foodPosition.y).absoluteValue < threshold
        }

        setContent {
            @Composable
            fun SnakeGameScreen() {
                // 记录当前方向，初始为向右
                val direction = remember { mutableStateOf(Direction.RIGHT) }
                val screenSize = remember { mutableStateOf(Size.Zero) }
                val foodPosition = remember { mutableStateOf(Offset(0f, 0f)) } // 默认值为 (0, 0)
                val score = remember { mutableStateOf(0) }
                val snakeBody = remember { mutableStateListOf(Offset(100f, 100f)) } // 只有一个方块
                val gameOver = remember { mutableStateOf(false) }
                // 动态更新食物位置，当屏幕尺寸初始化完成后
                LaunchedEffect(screenSize.value) {
                    if (screenSize.value != Size.Zero) {
                        foodPosition.value = generateRandomFoodPosition(screenSize.value, snakeBody[0])
                    }
                }
                // 动态更新方块位置
                LaunchedEffect(gameOver.value) {
                    if (!gameOver.value) {
                        while (true) {
                            // 如果游戏结束，跳出循环
                            if (gameOver.value) break

                            // 移动蛇的身体
                            for (i in snakeBody.size - 1 downTo 1) {
                                snakeBody[i] = snakeBody[i - 1]
                            }

                            // 移动蛇头
                            snakeBody[0] = when (direction.value) {
                                Direction.UP -> snakeBody[0].copy(y = (snakeBody[0].y - 50f).coerceAtLeast(0f))
                                Direction.DOWN -> snakeBody[0].copy(y = (snakeBody[0].y + 50f).coerceAtMost(screenSize.value.height - 50f))
                                Direction.LEFT -> snakeBody[0].copy(x = (snakeBody[0].x - 50f).coerceAtLeast(0f))
                                Direction.RIGHT -> snakeBody[0].copy(x = (snakeBody[0].x + 50f).coerceAtMost(screenSize.value.width - 50f))
                            }

                            // 碰撞检测
                            if (isCollision(snakeBody[0], foodPosition.value)) {
                                score.value += 1
                                foodPosition.value = generateRandomFoodPosition(screenSize.value, snakeBody[0])

                                // 将新增加的一节初始化为蛇尾的位置
                                val tail = snakeBody.last()
                                snakeBody.add(tail)
                            }

                            // 碰撞到墙壁
                            if (snakeBody[0].x < 0 || snakeBody[0].x >= screenSize.value.width - 50f ||
                                snakeBody[0].y < 0 || snakeBody[0].y >= screenSize.value.height - 50f) {
                                gameOver.value = true // 撞墙，游戏结束
                            }

                            delay(200L) // 控制移动速度
                        }
                    }
                }

                // UI 部分
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val centerX = screenSize.value.width / 2
                                val centerY = screenSize.value.height / 2

                                direction.value = when {
                                    offset.x < centerX && offset.y < centerY -> Direction.UP
                                    offset.x > centerX && offset.y < centerY -> Direction.RIGHT
                                    offset.x < centerX && offset.y > centerY -> Direction.LEFT
                                    else -> Direction.DOWN
                                }
                            }
                        }
                ) {
                    if (gameOver.value) {
                        // 显示游戏结束文本
                        Text(
                            text = "Game Over!",
                            color = Color.White,
                            fontSize = 32.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                        )

                        // Play Again 按钮
                        Button(
                            onClick = {
                                // 重置游戏状态
                                snakeBody.clear()
                                snakeBody.add(Offset(100f, 100f))
                                score.value = 0
                                direction.value = Direction.RIGHT
                                gameOver.value = false

                                // 重新生成食物位置
                                foodPosition.value = generateRandomFoodPosition(screenSize.value, snakeBody[0])
                            },
                            modifier = Modifier.align(Alignment.Center).padding(top = 60.dp)
                        ) {
                            Text("Play Again")
                        }

                        // Back to Home 按钮
                        Button(
                            onClick = {
                                // 处理返回首页的逻辑
                                // 例如，使用导航控制器跳转到首页
                            },
                            modifier = Modifier.align(Alignment.Center).padding(top = 120.dp)
                        ) {
                            Text("Back to Home")
                        }
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 更新屏幕尺寸，避免为空
                            screenSize.value = size
                        // 绘制蛇的每一节
                        snakeBody.forEach { segment ->
                            drawRect(
                                color = Color.Green,
                                topLeft = segment,
                                size = Size(50f, 50f)
                            )
                        }
                        // 绘制红色食物
                        drawRect(
                            color = Color.Red,
                            topLeft = foodPosition.value,
                            size = Size(50f, 50f)
                        )
                    }
                    Text(
                        text = "Score: ${score.value}",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                }
            }
            SnakeGameScreen()
        }
    }
}
