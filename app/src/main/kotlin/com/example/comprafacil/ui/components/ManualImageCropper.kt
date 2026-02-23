package com.example.comprafacil.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun ManualImageCropper(
    imageUri: Uri,
    onCropSuccess: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // UI state for transformation
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(imageUri) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        bitmap = BitmapFactory.decodeStream(inputStream)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { containerSize = it.size }
    ) {
        bitmap?.let { btm ->
            val imageBitmap = btm.asImageBitmap()

            // Draw the interactive image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale *= zoom
                            scale = scale.coerceIn(0.5f, 5f)
                            offset += pan
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val drawSize = Size(
                        btm.width.toFloat() * scale,
                        btm.height.toFloat() * scale
                    )

                    // Center the image initially
                    val startOffset = Offset(
                        (size.width - drawSize.width) / 2 + offset.x,
                        (size.height - drawSize.height) / 2 + offset.y
                    )

                    drawImage(
                        image = imageBitmap,
                        dstOffset = startOffset.toIntOffset(),
                        dstSize = drawSize.toIntSize()
                    )
                }
            }

            // Draw circular overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val circleRadius = min(size.width, size.height) * 0.4f
                val center = Offset(size.width / 2, size.height / 2)

                // Mask outside the circle
                val path = Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addOval(Rect(center, circleRadius))
                    fillType = PathFillType.EvenOdd
                }
                drawPath(path, Color.Black.copy(alpha = 0.6f))

                // Circle border
                drawCircle(
                    color = Color.White,
                    radius = circleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.background(Color.DarkGray, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
                }

                Button(
                    onClick = {
                        val cropped = cropBitmap(btm, scale, offset, containerSize)
                        onCropSuccess(cropped)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recortar")
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

private fun cropBitmap(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    containerSize: IntSize
): Bitmap {
    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()

    // The target crop area is a circle/square in the middle
    val cropSizePx = min(containerWidth, containerHeight) * 0.8f
    val cropLeft = (containerWidth - cropSizePx) / 2
    val cropTop = (containerHeight - cropSizePx) / 2

    // Current rendered image size
    val renderedWidth = source.width * scale
    val renderedHeight = source.height * scale

    // Top-left of the image on screen
    val imageLeft = (containerWidth - renderedWidth) / 2 + offset.x
    val imageTop = (containerHeight - renderedHeight) / 2 + offset.y

    // How much of the image is to the left/top of the crop area
    val xInRendered = cropLeft - imageLeft
    val yInRendered = cropTop - imageTop

    // Map back to source bitmap coordinates
    val xInSource = (xInRendered / scale).toInt()
    val yInSource = (yInRendered / scale).toInt()
    val sizeInSource = (cropSizePx / scale).toInt()

    // Bounds checking
    val finalX = max(0, min(xInSource, source.width - 1))
    val finalY = max(0, min(yInSource, source.height - 1))
    val finalSize = min(sizeInSource, min(source.width - finalX, source.height - finalY))

    return Bitmap.createBitmap(source, finalX, finalY, finalSize, finalSize)
}

private fun Offset.toIntOffset() = androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt())
private fun Size.toIntSize() = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())
