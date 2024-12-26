package com.example.snakegame
import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import kotlin.math.absoluteValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext

data class RankingItem(val rank: Int, val score: Int, val duration: Int)
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
fun saveRankingData(context: Context, rankingList: List<RankingItem>) {
    val sharedPreferences = context.getSharedPreferences("game_ranking", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()

    // 清除以前的数据
    editor.clear()

    // 根据得分降序和时长升序排序
    val sortedList = rankingList.sortedWith(
        compareByDescending<RankingItem> { it.score }
            .thenBy { it.duration }
    )

    // 为排序后的结果动态分配排名（rank）
    sortedList.forEachIndexed { index, item ->
        // 为每条数据添加排名
        editor.putInt("rank_${index}_score", item.score)
        editor.putInt("rank_${index}_duration", item.duration)
    }

    // 保存到 SharedPreferences
    editor.apply()
}
fun loadRankingData(context: Context): List<RankingItem> {
    val sharedPreferences = context.getSharedPreferences("game_ranking", Context.MODE_PRIVATE)
    val rankingList = mutableListOf<RankingItem>()

    var index = 0
    while (sharedPreferences.contains("rank_${index}_score")) {
        val score = sharedPreferences.getInt("rank_${index}_score", 0)
        val duration = sharedPreferences.getInt("rank_${index}_duration", 0)
        rankingList.add(RankingItem(rank = index, score = score, duration = duration))
        index++
    }

    // 对排名数据进行排序并计算排名
    val sortedList = rankingList.sortedWith(
        compareByDescending<RankingItem> { it.score }
            .thenBy { it.duration }
    )

    // 重新计算排名
    return sortedList.mapIndexed { index, item ->
        item.copy(rank = index + 1)  // 为每条数据分配新的排名
    }
}
fun recordGameData(context: Context, score: Int, duration: Int) {
    // 获取现有的排名列表
    val rankingList = loadRankingData(context)

    // 添加新的游戏数据为新的排名项
    val newRankingItem = RankingItem(rank = rankingList.size + 1,score = score, duration = duration)
    val updatedRankingList = rankingList + newRankingItem

    // 保存更新后的排名列表
    saveRankingData(context, updatedRankingList)
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentScreen = remember { mutableStateOf("welcome") }

            // 欢迎页面
            if (currentScreen.value == "welcome") {
                WelcomeScreen(
                    onStartGameClick = { currentScreen.value = "game" },
                    onRankingClick = { currentScreen.value = "ranking" }
                )
            }

            // 游戏页面
            if (currentScreen.value == "game") {
                SnakeGameScreen(
                    context = LocalContext.current,
                    onBackToHomeClick = { currentScreen.value = "welcome" }
                )
            }

            // 排名页面（暂时只显示占位文本）
            if (currentScreen.value == "ranking") {
                RankingScreen(
                    context = LocalContext.current,
                    onBackToHomeClick = { currentScreen.value = "welcome" }
                )
            }
        }

    }
}
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

@Composable
fun WelcomeScreen(onStartGameClick: () -> Unit, onRankingClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "贪吃蛇",
                color = Color.White,
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 50.dp)
            )
            Button(
                onClick = onStartGameClick,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text("Start Game")
            }
            Button(
                onClick = onRankingClick,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text("Ranking")
            }
        }
    }
}

@Composable
fun RankingScreen(context: Context, onBackToHomeClick: () -> Unit) {
    val rankingList = remember { loadRankingData(context) }

    // 根据 Score 降序排序，如果 Score 相同，按照 Duration 升序排序
    val sortedList = rankingList.sortedWith(compareByDescending<RankingItem> { it.score }
        .thenBy { it.duration }).toMutableList()

    // 动态更新排名，避免新建列表，直接修改 sortedList
    var lastRank = 1  // 初始化排名
    var lastScore = -1
    var lastDuration = -1

    sortedList.forEachIndexed { index, item ->
        if (item.score == lastScore && item.duration == lastDuration) {
            // 如果当前项的得分和时长与前一项相同，保持相同的排名
            sortedList[index] = item.copy(rank = lastRank)
        } else {
            // 否则，分配新的排名
            sortedList[index] = item.copy(rank = index + 1)
            lastRank = index + 1
        }

        // 更新前一项的得分和时长
        lastScore = item.score
        lastDuration = item.duration
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // 页面标题
            Text(
                text = "Ranking Page",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 显示排序后的排名列表
            sortedList.forEach { item ->
                Text(
                    text = "Rank: ${item.rank} | Score: ${item.score} | Duration: ${item.duration} seconds",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }

            // 返回首页按钮
            Button(onClick = onBackToHomeClick, modifier = Modifier.padding(top = 20.dp)) {
                Text("Back to Home")
            }
        }
    }
}






@Composable
fun SnakeGameScreen(context: Context, onBackToHomeClick: () -> Unit) {
    // 记录当前方向，初始为向右
    val direction = remember { mutableStateOf(Direction.RIGHT) }
    val screenSize = remember { mutableStateOf(Size.Zero) }
    val foodPosition = remember { mutableStateOf(Offset(0f, 0f)) } // 默认值为 (0, 0)
    val score = remember { mutableStateOf(0) }
    val gameOver = remember { mutableStateOf(false) }
    val gameTime = remember { mutableStateOf(0) }
    val snakeBody = remember { mutableStateListOf(
        Offset(100f, 100f),  // 蛇头
        Offset(150f, 100f),  // 第2节
        Offset(200f, 100f),  // 第3节
        Offset(250f, 100f)   // 第4节
    ) }

    // 动态更新食物位置，当屏幕尺寸初始化完成后
    LaunchedEffect(screenSize.value) {
        if (screenSize.value != Size.Zero) {
            foodPosition.value = generateRandomFoodPosition(screenSize.value, snakeBody[0])
        }
    }
    LaunchedEffect(gameOver.value) {
        if (!gameOver.value) {
            while (true) {
                delay(1000L)  // 每秒增加一次
                if (!gameOver.value) {
                    gameTime.value += 1
                } else {
                    break
                }
            }
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

                // 移动蛇头，根据方向控制蛇头的位置
                snakeBody[0] = when (direction.value) {
                    Direction.UP -> {
                        // 如果蛇头碰到上边界，从下边界出现
                        if (snakeBody[0].y > 0) snakeBody[0].copy(y = snakeBody[0].y - 50f)
                        else snakeBody[0].copy(y = screenSize.value.height - 50f)  // 碰到上边界，出现在下边界
                    }
                    Direction.DOWN -> {
                        // 如果蛇头碰到下边界，从上边界出现
                        if (snakeBody[0].y < screenSize.value.height - 50f) snakeBody[0].copy(y = snakeBody[0].y + 50f)
                        else snakeBody[0].copy(y = 0f)  // 碰到下边界，出现在上边界
                    }
                    Direction.LEFT -> {
                        // 如果蛇头碰到左边界，从右边界出现
                        if (snakeBody[0].x > 0) snakeBody[0].copy(x = snakeBody[0].x - 50f)
                        else snakeBody[0].copy(x = screenSize.value.width - 50f)  // 碰到左边界，出现在右边界
                    }
                    Direction.RIGHT -> {
                        // 如果蛇头碰到右边界，从左边界出现
                        if (snakeBody[0].x < screenSize.value.width - 50f) snakeBody[0].copy(x = snakeBody[0].x + 50f)
                        else snakeBody[0].copy(x = 0f)  // 碰到右边界，出现在左边界
                    }
                }

                // 碰撞检测
                if (isCollision(snakeBody[0], foodPosition.value)) {
                    score.value += 1
                    foodPosition.value = generateRandomFoodPosition(screenSize.value, snakeBody[0])

                    // 将新增加的一节初始化为蛇尾的位置
                    val tail = snakeBody.last()
                    snakeBody.add(tail)
                }

//                if (snakeBody.subList(1, snakeBody.size).any { it == snakeBody[0] }) {
//                    gameOver.value = true  // 撞到自己，游戏结束
//                }
//                // 碰撞到墙壁
//                if (snakeBody[0].x-50f < 0 || snakeBody[0].x >= screenSize.value.width - 50f ||
//                    snakeBody[0].y-50f < 0 || snakeBody[0].y >= screenSize.value.height - 50f) {
//                    gameOver.value = true // 撞墙，游戏结束
//                }
//                if (gameTime.value == 10){
//                    gameOver.value = true//10秒结束
//                }
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

                    direction.value = when (direction.value) {
                        Direction.UP, Direction.DOWN -> {
                            if (offset.x < centerX) Direction.LEFT else Direction.RIGHT
                        }
                        Direction.LEFT -> {
                            if (offset.y < centerY) Direction.UP else Direction.DOWN
                        }
                        Direction.RIGHT -> {
                            if (offset.y < centerY) Direction.UP else Direction.DOWN
                        }
                    }
                }
            }
    ) {
        if (gameOver.value) {
            // Record the game data
            recordGameData(context, score.value, gameTime.value)
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
                    snakeBody.addAll(
                        listOf(
                            Offset(100f, 100f),  // 蛇头
                            Offset(150f, 100f),  // 第2节
                            Offset(200f, 100f),  // 第3节
                            Offset(250f, 100f)   // 第4节
                        )
                    )
                    score.value = 0
                    direction.value = Direction.RIGHT
                    gameOver.value = false
                    gameTime.value = 0  // 重置游戏时间
                    // 重新生成食物位置
                    foodPosition.value = generateRandomFoodPosition(screenSize.value, snakeBody[0])
                },
                modifier = Modifier.align(Alignment.Center).padding(top = 60.dp)
            ) {
                Text("Play Again")
            }

            // Back to Home 按钮
            Button(
                onClick = onBackToHomeClick,
                modifier = Modifier.align(Alignment.Center).padding(top = 120.dp)
            ) {
                Text("Back to Home")
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 更新屏幕尺寸，避免为空
            screenSize.value = size
            // 绘制蛇的每一节
            snakeBody.forEachIndexed { index, segment ->
                val color = if (index == 0) Color.Blue else Color.Green // 蛇头是红色，身体是绿色
                drawRect(
                    color = color,
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
        // 显示分数
        Text(
            text = "Score: ${score.value}",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        // 显示游戏时间
        Text(
            text = "Time: ${gameTime.value} seconds",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

