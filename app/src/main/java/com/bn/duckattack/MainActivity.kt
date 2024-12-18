package com.bn.duckattack

import android.app.Activity
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpaceDuckAttackGame()
        }
    }
}

@Composable
fun SpaceDuckAttackGame() {
    val mContext = LocalContext.current
    val screenWidth = 400f
    val screenHeight = 800f

    val shootingSound = remember {
        MediaPlayer.create(mContext, R.raw.shoting)
    }
    shootingSound.setOnPreparedListener {
        // Set playback speed to 2x
        shootingSound.playbackParams = shootingSound.playbackParams.setSpeed(3.0f)
        shootingSound.start()
    }

    val duckKilledSound = remember {
        MediaPlayer.create(mContext, R.raw.duckkilled)
    }
    duckKilledSound.setOnPreparedListener {
        duckKilledSound.start()
    }

    // Warplane properties
    var warplaneX by remember { mutableStateOf(200f) }
    val warplaneWidth = 100f
    val warplaneHeight = 100f

    // Ducks and bullets
    val ducks by remember { mutableStateOf(mutableStateListOf<Duck>()) }
    val bullets by remember { mutableStateOf(mutableStateListOf<Bullet>()) }
    var score by remember { mutableStateOf(0) }
    var healthPoints by remember { mutableStateOf(5) }
    var isGameOver by remember { mutableStateOf(false) }

    // Animated stars
    val stars = remember { mutableStateListOf<Star>() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            stars.add(Star(Random.nextFloat() * screenWidth, 0f))
        }
    }

    // Star movement
    LaunchedEffect(stars) {
        while (true) {
            delay(16) // ~60 FPS
            stars.forEach { it.moveDown(screenHeight) }
            stars.removeAll { it.y > screenHeight }
        }
    }

    // Spawn ducks periodically
    LaunchedEffect(isGameOver) {
        if (!isGameOver) {
            while (true) {
                delay(2000)  // Add a delay between spawns
                if (!isGameOver) {
                    ducks.add(
                        Duck(
                            Random.nextFloat() * (screenWidth - 80),
                            0f
                        )
                    ) // Spawn ducks at random positions
                }
            }
        }
    }
    // Automatic shooting
    LaunchedEffect(Unit, isGameOver) {
        while (!isGameOver) {
            delay(500)
            bullets.add(Bullet(warplaneX + warplaneWidth / 2 - 5, 720f))

            if (!shootingSound.isPlaying) {
                shootingSound.start()
            }
        }
    }
    // Clean up the sound effect when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            shootingSound.release()
            duckKilledSound.release()
        }
    }

    // Update game elements
    LaunchedEffect(ducks, bullets, isGameOver) {
        while (!isGameOver) {
            delay(16) // ~60 FPS
            ducks.forEach { it.moveDown() }
            bullets.forEach { it.moveUp() }

            // Collision detection
            val collidedDucks = ducks.filter { duck ->
                bullets.any { bullet ->
                    bullet.x in duck.x..(duck.x + duck.width) &&
                            bullet.y in duck.y..(duck.y + duck.height)
                }
            }

            // Play sound for each duck killed
            if (collidedDucks.isNotEmpty()) {
                duckKilledSound.start()
            }

            // Remove collided ducks and bullets
            ducks.removeAll(collidedDucks)
            bullets.removeAll { bullet ->
                collidedDucks.any { duck ->
                    bullet.x in duck.x..(duck.x + duck.width) &&
                            bullet.y in duck.y..(duck.y + duck.height)
                }
            }
            // Update score
            score += collidedDucks.size

            // Remove off-screen ducks and lose health if they pass
            val escapedDucks = ducks.filter { it.y > screenHeight }
            ducks.removeAll(escapedDucks)
            healthPoints -= escapedDucks.size

            // Check for game over
            if (healthPoints <= 0) {
                isGameOver = true
            }
        }
    }

    // Render game elements
    if (!isGameOver) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        warplaneX =
                            (warplaneX + dragAmount.x).coerceIn(0f, screenWidth - warplaneWidth)
                    }
                }
        ) {
            // Show score and health
            Text(
                text = "Score: $score",
                style = TextStyle(color = Color.White),
                modifier = Modifier.offset(16.dp, 16.dp)
            )
            Text(
                text = "Health: $healthPoints",
                style = TextStyle(color = Color.Red),
                modifier = Modifier.offset(16.dp, 40.dp)
            )

            // Draw animated stars (background)
            stars.forEach { star ->
                Canvas(
                    modifier = Modifier
                        .offset(star.x.dp, star.y.dp)
                        .size(5.dp)
                ) {
                    drawCircle(color = Color.White)
                }
            }

            // Draw warplane
            Image(
                painter = painterResource(id = R.drawable.warplane),  // Replace with your image file name
                contentDescription = "Warplane",
                modifier = Modifier
                    .offset(warplaneX.dp, 720.dp)
                    .size(warplaneWidth.dp, warplaneHeight.dp)
            )


            // Draw ducks
            ducks.forEach { duck ->
                Image(
                    painter = painterResource(id = R.drawable.duck),  // Replace with your image file name
                    contentDescription = "Duck",
                    modifier = Modifier
                        .offset(duck.x.dp, duck.y.dp)
                        .size(warplaneWidth.dp, warplaneHeight.dp)
                )
            }

            // Draw bullets
            bullets.forEach { bullet ->
                Canvas(
                    modifier = Modifier
                        .offset(bullet.x.dp, bullet.y.dp)
                        .size(10.dp, 10.dp)
                ) {
                    drawCircle(
                        color = Color.Red,
                    )
                }
            }
        }
    } else {
        // Show game over dialog
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game Over") },
            text = { Text("Your final score: $score") },
            confirmButton = {
                Button(onClick = {
                    score = 0
                    healthPoints = 5
                    isGameOver = false
                    ducks.clear()
                    bullets.clear()
                    stars.clear()
                }) {
                    Text("Restart")
                }
            },
            dismissButton = {
                Button(onClick = {
                    (mContext as? Activity)?.finish()
                }) {
                    Text("Exit")
                }
            }
        )
    }
}

// Duck class
class Duck(var x: Float, var y: Float) {
    val width = 80f
    val height = 50f

    fun moveDown() {
        y += 5f
    }
}

// Bullet class
class Bullet(var x: Float, var y: Float) {
    fun moveUp() {
        y -= 15f
    }
}

// Star class for animated background
class Star(var x: Float, var y: Float) {
    fun moveDown(screenHeight: Float) {
        y += 3f // Move star downward
    }
}