package com.uc.caffeine.ui.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.uc.caffeine.R
import com.uc.caffeine.ui.components.ExpressiveIconBadge
import com.uc.caffeine.ui.components.rememberAppHaptics
import com.uc.caffeine.ui.components.rememberImagePickerRequest
import com.uc.caffeine.util.BarcodeDecoder
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CameraAccess { Granted, NeedsExplaining, Denied, Unavailable }

private val ScanWindowMaxWidth = 320.dp
private val ScanWindowHeight = 170.dp

/** Longest edge a picked photo is decoded at: plenty for a barcode, a fraction of a 50 MP shot. */
private const val MAX_PHOTO_EDGE_PX = 1600

/**
 * Full-screen barcode scanner: a live viewfinder decoded on the device with ZXing, plus scanning a
 * photo picked with the system photo picker. It says what the camera is for before Android asks,
 * and falls back to photos when the camera is refused or missing. Frames go from the analyzer
 * straight to the decoder; nothing is recorded or kept.
 */
@Composable
fun BarcodeScanner(
    onBarcode: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()

    var access by remember { mutableStateOf(initialCameraAccess(context)) }
    // After one refusal Android still shows its dialog once more; after that only Settings can help.
    var canAskAgain by remember { mutableStateOf(true) }
    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    var detected by remember { mutableStateOf<String?>(null) }

    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        access = if (granted) CameraAccess.Granted else CameraAccess.Denied
        canAskAgain = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    // Back from the app's settings page with the camera now allowed.
    LifecycleResumeEffect(access) {
        if (access != CameraAccess.Granted && access != CameraAccess.Unavailable && context.hasCameraPermission()) {
            access = CameraAccess.Granted
        }
        onPauseOrDispose { }
    }

    val onDetected: (String) -> Unit = { code ->
        if (detected == null) {
            detected = code
            haptics.confirm()
        }
    }
    // A beat with the frame lit up before closing, so the scan visibly landed.
    LaunchedEffect(detected) {
        val code = detected ?: return@LaunchedEffect
        delay(350)
        onBarcode(code)
    }

    val photoRequest = rememberImagePickerRequest()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val code = decodeBarcodeFromPhoto(context, uri)
                photoFailed = code == null
                if (code != null) onDetected(code)
            }
        }
    }

    val showingCamera = access == CameraAccess.Granted
    if (showingCamera) LightStatusBarIcons()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (showingCamera) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (showingCamera) Color.White else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showingCamera) {
                CameraFeed(
                    torchOn = torchOn,
                    onTorchAvailable = { hasTorch = it },
                    onBarcode = onDetected,
                    onUnavailable = { access = CameraAccess.Unavailable },
                    modifier = Modifier.fillMaxSize(),
                )
                ScanWindow(lit = detected != null, modifier = Modifier.fillMaxSize())
                Text(
                    text = stringResource(R.string.scanner_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = ScanWindowHeight / 2 + 40.dp)
                        .padding(horizontal = 32.dp),
                )
            } else {
                CameraAccessPanel(
                    access = access,
                    canAskAgain = canAskAgain,
                    onAllow = { requestCamera.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Translucent dark on the camera so the controls read over any scene.
                val controlColor = if (showingCamera) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerHighest
                FilledIconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = controlColor,
                        contentColor = if (showingCamera) Color.White else MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.scanner_close_cd))
                }
                if (showingCamera && hasTorch) {
                    FilledIconToggleButton(
                        checked = torchOn,
                        onCheckedChange = {
                            haptics.toggle()
                            torchOn = it
                        },
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = controlColor,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                            contentDescription = stringResource(
                                if (torchOn) R.string.scanner_torch_off_cd else R.string.scanner_torch_on_cd
                            ),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (photoFailed) {
                    Text(
                        text = stringResource(R.string.scanner_no_barcode_in_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 360.dp),
                    )
                }
                FilledTonalButton(
                    onClick = {
                        photoFailed = false
                        pickPhoto.launch(photoRequest)
                    },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.scanner_from_photo))
                }
            }
        }
    }
}

@Composable
private fun CameraAccessPanel(
    access: CameraAccess,
    canAskAgain: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, body) = when (access) {
        CameraAccess.NeedsExplaining -> R.string.scanner_permission_title to R.string.scanner_permission_body
        CameraAccess.Denied -> R.string.scanner_permission_denied_title to R.string.scanner_permission_denied_body
        CameraAccess.Unavailable -> R.string.scanner_no_camera_title to R.string.scanner_no_camera_body
        CameraAccess.Granted -> return
    }
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExpressiveIconBadge(index = 0, size = 88.dp) {
            if (access == CameraAccess.NeedsExplaining) {
                Icon(
                    painter = painterResource(R.drawable.ic_barcode_scanner),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.NoPhotography,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        when {
            access == CameraAccess.NeedsExplaining || (access == CameraAccess.Denied && canAskAgain) ->
                Button(onClick = onAllow) { Text(stringResource(R.string.scanner_permission_allow)) }
            access == CameraAccess.Denied ->
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.scanner_open_settings)) }
        }
    }
}

/** Dims everything but a rounded window in the middle, which lights up when a code is read. */
@Composable
private fun ScanWindow(lit: Boolean, modifier: Modifier = Modifier) {
    val frameColor by animateColorAsState(
        targetValue = if (lit) MaterialTheme.colorScheme.primary else Color.White,
        label = "scan-window-color",
    )
    val scale by animateFloatAsState(
        targetValue = if (lit) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scan-window-scale",
    )
    // Offscreen so the cleared window punches through the scrim rather than the camera below.
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val width = minOf(size.width - 64.dp.toPx(), ScanWindowMaxWidth.toPx()) * scale
        val height = ScanWindowHeight.toPx() * scale
        val topLeft = Offset((size.width - width) / 2f, (size.height - height) / 2f)
        val windowSize = androidx.compose.ui.geometry.Size(width, height)
        val radius = CornerRadius(28.dp.toPx())
        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRoundRect(Color.Black, topLeft, windowSize, radius, blendMode = BlendMode.Clear)
        drawRoundRect(frameColor, topLeft, windowSize, radius, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun CameraFeed(
    torchOn: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onBarcode: (String) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val currentOnTorchAvailable by rememberUpdatedState(onTorchAvailable)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(lifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val selector = listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
            .firstOrNull { runCatching { provider.hasCamera(it) }.getOrDefault(false) }
        if (selector == null) {
            currentOnUnavailable()
            return@LaunchedEffect
        }

        val preview = Preview.Builder().build().apply { setSurfaceProvider { surfaceRequest = it } }
        // 720p keeps a barcode across the room readable without decoding a full-size frame.
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        analysis.setAnalyzer(analysisExecutor, BarcodeAnalyzer { code -> mainExecutor.execute { currentOnBarcode(code) } })

        try {
            val bound = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            camera = bound
            currentOnTorchAvailable(bound.cameraInfo.hasFlashUnit())
            awaitCancellation()
        } catch (_: IllegalArgumentException) {
            currentOnUnavailable()
        } catch (_: IllegalStateException) {
            currentOnUnavailable()
        } finally {
            provider.unbind(preview, analysis)
            analysis.clearAnalyzer()
            analysisExecutor.shutdown()
            camera = null
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    surfaceRequest?.let { request ->
        // Embedded (TextureView-style) rather than a SurfaceView, which can't follow the scale and
        // clip of the open/close morph.
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = modifier,
            implementationMode = ImplementationMode.EMBEDDED,
        )
    }
}

/** Decodes the luminance plane of each frame; reports a code once two reads agree. */
private class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val decoder = BarcodeDecoder()
    private var frame = ByteArray(0)
    private var candidate: String? = null
    private var reported = false

    override fun analyze(image: ImageProxy) {
        image.use {
            if (reported) return
            val luminance = it.planes[0]
            val buffer = luminance.buffer
            if (frame.size != buffer.remaining()) frame = ByteArray(buffer.remaining())
            buffer.get(frame)
            val code = decoder.decode(frame, it.width, it.height, luminance.rowStride) ?: return
            // A single frame can misread a scuffed code; two matching reads won't plausibly be wrong.
            if (code == candidate) {
                reported = true
                onBarcode(code)
            } else {
                candidate = code
            }
        }
    }
}

/** The camera is shown on black, so the status bar icons go light while it is up. */
@Composable
private fun LightStatusBarIcons() {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val wasLight = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = wasLight }
    }
}

private fun initialCameraAccess(context: Context): CameraAccess = when {
    !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> CameraAccess.Unavailable
    context.hasCameraPermission() -> CameraAccess.Granted
    else -> CameraAccess.NeedsExplaining
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private suspend fun decodeBarcodeFromPhoto(context: Context, uri: Uri): String? = withContext(Dispatchers.Default) {
    val bitmap = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            // Software pixels so they can be read back.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_PHOTO_EDGE_PX) {
                val scale = MAX_PHOTO_EDGE_PX.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt(),
                    (info.size.height * scale).roundToInt(),
                )
            }
        }
    } catch (_: IOException) {
        return@withContext null
    }
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    bitmap.recycle()
    val luminance = ByteArray(pixels.size) { i ->
        val pixel = pixels[i]
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        ((red * 299 + green * 587 + blue * 114) / 1000).toByte()
    }
    BarcodeDecoder(tryHarder = true).decode(luminance, width, height)
}
