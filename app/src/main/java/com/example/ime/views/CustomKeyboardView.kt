package com.example.ime.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.KeyboardPreferences
import com.example.ime.GitHubDarkPalette
import com.example.ime.KeyModel
import com.example.ime.KeyRow
import com.example.ime.KeyType
import com.example.ime.KeyboardLayout
import com.example.ime.KeyboardMode
import com.example.ime.ShiftState
import kotlin.math.abs
import kotlin.math.max

/**
 * Precision-engineered, zero-dead-zone native Canvas View for CallistoKey.
 *
 * Key touch & design guarantees:
 * - Zero dead zones: hitBounds cover 100% of view area via Voronoi midpoints between keys & rows.
 * - Touch hysteresis: prevents micro thumb-slips on fast taps from cancelling or mistyping keys.
 * - Multi-touch rollover: commits held character keys immediately when a second finger lands.
 * - Spacebar horizontal swipe: instant EN <-> FA switching without accidental spaces.
 * - Mechanical-inspired GitHub Dark keycaps: bevel shadow, top specular highlight, physical travel on press.
 */
class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnKeyboardActionListener {
        fun onText(text: String)
        fun onBackspace()
        fun onEnter()
        fun onShiftToggle()
        fun onShiftDoubleTap()
        fun onLanguageSwitch()
        fun onModeSwitch(target: String)
        fun onNumericScriptToggle()
        fun onZwnj()
    }

    interface OnOverlayListener {
        fun onShowPreview(key: KeyModel)
        fun onHidePreview()
        fun onShowAlternates(anchorKey: KeyModel, alternates: List<String>)
        fun onMoveAlternates(touchXInKeyboard: Float)
        fun onCommitAlternateAndHide(): String?
        fun onCancelAlternates()
    }

    var listener: OnKeyboardActionListener? = null
    var overlayListener: OnOverlayListener? = null

    var hapticEnabled: Boolean = true
    var keyPreviewEnabled: Boolean = true
    var longPressDelayMs: Int = 310
    var heightMode: String = KeyboardPreferences.HEIGHT_NORMAL
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    var currentMode: KeyboardMode = KeyboardMode.EN_ALPHA
        private set
    var shiftState: ShiftState = ShiftState.OFF
        private set
    var usePersianDigits: Boolean = false
        private set
    var enterActionLabel: String = "ENTER_ICON"
        private set
    var lastAlphaMode: KeyboardMode = KeyboardMode.EN_ALPHA
        private set

    // Tactile geometry constants
    private val horizontalPadding = dpToPx(3.5f)
    private val verticalPadding = dpToPx(5.5f)
    private val keyGapX = dpToPx(3.8f)
    private val keyGapY = dpToPx(5.5f)
    private val cornerRadius = dpToPx(8f)
    private val shadowOffsetY = dpToPx(1.8f)
    private val pressTravelY = dpToPx(1.0f)
    private val borderWidth = dpToPx(0.85f)
    private val spaceSwipeThreshold = dpToPx(28f)
    private val hysteresisSlop = dpToPx(11f)

    private var vazirmatnTypeface: Typeface? = null

    // Pre-allocated Paints (zero GC pressure during draw)
    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEYBOARD_SURFACE
    }
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEY_SHADOW
    }
    private val keyNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEY_BACKGROUND
    }
    private val keySpecialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.SPECIAL_KEY
    }
    private val keySpacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.SPACEBAR_BG
    }
    private val keyAccentBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.ACCENT_SURFACE
    }
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEY_PRESSED
    }
    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        color = GitHubDarkPalette.BORDER
    }
    private val keyAccentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.1f)
        color = GitHubDarkPalette.ACCENT_BORDER
    }
    private val topHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        strokeCap = Paint.Cap.ROUND
        color = GitHubDarkPalette.KEY_TOP_HIGHLIGHT
    }
    private val spacePillBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.BORDER
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.PRIMARY_TEXT
    }
    private val accentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.ACCENT
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.SECONDARY_TEXT
    }
    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        color = GitHubDarkPalette.HINT_TEXT
        textSize = dpToPx(9.5f)
    }
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = GitHubDarkPalette.PRIMARY_TEXT
    }
    private val iconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.PRIMARY_TEXT
    }

    // Reusable drawing rects & paths
    private val drawRect = RectF()
    private val shadowRect = RectF()
    private val textBounds = Rect()
    private val iconPath = Path()

    private var keyRows: List<KeyRow> = emptyList()

    // Multi-touch pointer tracking
    private val activePointers = SparseArray<KeyModel>()
    private val pointerDownX = FloatArray(16)
    private val pointerDownY = FloatArray(16)
    private val alreadyCommittedPointers = SparseBooleanArray()

    // Spacebar swipe tracking
    private var spacePointerId = -1
    private var spaceDownX = 0f
    private var spaceSwiped = false

    // Long-press handling (alternates + backspace continuous repeat)
    private val handler = Handler(Looper.getMainLooper())
    private var isBackspaceRepeating = false
    private var longPressKey: KeyModel? = null
    private var longPressPointerId = -1
    private var isShowingAlternates = false

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceRepeating) {
                listener?.onBackspace()
                triggerHaptic()
                handler.postDelayed(this, 38)
            }
        }
    }

    private val longPressAlternatesRunnable = Runnable {
        val key = longPressKey
        if (key != null && key.altOutputs.isNotEmpty() && !spaceSwiped) {
            isShowingAlternates = true
            triggerHaptic()
            overlayListener?.onHidePreview()
            overlayListener?.onShowAlternates(key, key.altOutputs)
        }
    }

    private var lastShiftPressTime = 0L

    init {
        isClickable = true
        isFocusable = false
        loadFont()
        rebuildLayout()
    }

    private fun loadFont() {
        try {
            vazirmatnTypeface = ResourcesCompat.getFont(context, R.font.vazirmatn)
        } catch (_: Exception) {
            try {
                vazirmatnTypeface = Typeface.createFromAsset(context.assets, "fonts/vazirmatn.ttf")
            } catch (_: Exception) {
                vazirmatnTypeface = Typeface.DEFAULT
            }
        }
        val tf = vazirmatnTypeface ?: Typeface.DEFAULT
        textPaint.typeface = tf
        accentTextPaint.typeface = Typeface.create(tf, Typeface.BOLD)
        secondaryTextPaint.typeface = tf
        hintTextPaint.typeface = tf
    }

    fun setMode(mode: KeyboardMode) {
        if (mode == KeyboardMode.EN_ALPHA || mode == KeyboardMode.FA_ALPHA) {
            lastAlphaMode = mode
        }
        currentMode = mode
        if (mode != KeyboardMode.EN_ALPHA) {
            shiftState = ShiftState.OFF
        }
        rebuildLayout()
        invalidate()
    }

    fun setShift(state: ShiftState) {
        if (shiftState == state) return
        shiftState = state
        if (currentMode == KeyboardMode.EN_ALPHA) {
            rebuildLayout()
            invalidate()
        }
    }

    fun setEnterLabel(label: String) {
        if (enterActionLabel == label) return
        enterActionLabel = label
        rebuildLayout()
        invalidate()
    }

    fun setUsePersianDigits(enabled: Boolean) {
        if (usePersianDigits == enabled) return
        usePersianDigits = enabled
        if (currentMode == KeyboardMode.NUMERIC) {
            rebuildLayout()
            invalidate()
        }
    }

    fun togglePersianDigits() {
        usePersianDigits = !usePersianDigits
        if (currentMode == KeyboardMode.NUMERIC) {
            rebuildLayout()
            invalidate()
        }
    }

    fun switchLanguage() {
        if (lastAlphaMode == KeyboardMode.EN_ALPHA) {
            setMode(KeyboardMode.FA_ALPHA)
        } else {
            setMode(KeyboardMode.EN_ALPHA)
        }
    }

    private fun rebuildLayout() {
        keyRows = KeyboardLayout.buildRowLayout(
            mode = currentMode,
            shiftState = shiftState,
            usePersianDigits = usePersianDigits,
            enterLabel = enterActionLabel,
            lastAlphaMode = lastAlphaMode
        )
        calculateKeyBounds(width, height)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val desiredHeightDp = when (heightMode) {
            KeyboardPreferences.HEIGHT_COMPACT -> if (isLandscape) 158f else 218f
            KeyboardPreferences.HEIGHT_TALL -> if (isLandscape) 196f else 284f
            else -> if (isLandscape) 178f else 252f
        }
        val desiredHeightPx = dpToPx(desiredHeightDp).toInt()
        setMeasuredDimension(width, resolveSize(desiredHeightPx, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateKeyBounds(w, h)
    }

    /**
     * Computes visual bounds (with airy spacing) AND zero-dead-zone hitBounds
     * (seamless Voronoi midpoints covering 100% of the view rectangle).
     */
    private fun calculateKeyBounds(w: Int, h: Int) {
        if (w <= 0 || h <= 0 || keyRows.isEmpty()) return

        val numRows = keyRows.size
        val availableH = h - (verticalPadding * 2) - (keyGapY * (numRows - 1))
        val rowH = max(1f, availableH / numRows)

        val rowTops = FloatArray(numRows)
        val rowBottoms = FloatArray(numRows)
        var curY = verticalPadding
        for (r in 0 until numRows) {
            rowTops[r] = curY
            rowBottoms[r] = curY + rowH
            curY += rowH + keyGapY
        }

        for (r in 0 until numRows) {
            val keyRow = keyRows[r]
            val keys = keyRow.keys
            val numKeys = keys.size
            if (numKeys == 0) continue

            val totalWeight = keys.fold(0f) { acc, key -> acc + key.weight } + (keyRow.horizontalInsetWeight * 2f)
            val availableW = w - (horizontalPadding * 2) - (keyGapX * (numKeys - 1))
            val baseKeyW = max(1f, availableW / totalWeight)

            var curX = horizontalPadding + (keyRow.horizontalInsetWeight * baseKeyW)
            for (c in 0 until numKeys) {
                val key = keys[c]
                val keyW = key.weight * baseKeyW
                key.bounds.set(curX, rowTops[r], curX + keyW, rowBottoms[r])
                curX += keyW + keyGapX
            }

            // Compute seamless hitBounds for this row:
            // Top & bottom extend to row midpoints and outer view edges (0 .. h)
            val hitTop = if (r == 0) 0f else (rowBottoms[r - 1] + rowTops[r]) * 0.5f
            val hitBottom = if (r == numRows - 1) h.toFloat() else (rowBottoms[r] + rowTops[r + 1]) * 0.5f

            for (c in 0 until numKeys) {
                val key = keys[c]
                val hitLeft = if (c == 0) 0f else (keys[c - 1].bounds.right + key.bounds.left) * 0.5f
                val hitRight = if (c == numKeys - 1) w.toFloat() else (key.bounds.right + keys[c + 1].bounds.left) * 0.5f
                key.hitBounds.set(hitLeft, hitTop, hitRight, hitBottom)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val isNumeric = currentMode == KeyboardMode.NUMERIC
        val baseFontSize = if (isNumeric) dpToPx(25f) else dpToPx(18.5f)
        val specialFontSize = dpToPx(13f)

        for (keyRow in keyRows) {
            for (key in keyRow.keys) {
                val isEnterAction = key.type == KeyType.ENTER
                val isShiftActive = key.type == KeyType.SHIFT && shiftState != ShiftState.OFF

                val travel = if (key.isPressed) pressTravelY else 0f
                drawRect.set(
                    key.bounds.left,
                    key.bounds.top + travel,
                    key.bounds.right,
                    key.bounds.bottom + travel
                )

                // Subtle bottom mechanical bevel shadow when unpressed
                if (!key.isPressed) {
                    shadowRect.set(
                        drawRect.left,
                        drawRect.top + shadowOffsetY,
                        drawRect.right,
                        drawRect.bottom + shadowOffsetY
                    )
                    canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, keyShadowPaint)
                }

                val fillPaint = when {
                    key.isPressed -> keyPressedPaint
                    isEnterAction -> keyAccentBgPaint
                    isShiftActive -> keyAccentBgPaint
                    key.type == KeyType.SPACE -> keySpacePaint
                    key.isSpecial -> keySpecialPaint
                    else -> keyNormalPaint
                }
                val borderPaint = when {
                    key.isPressed -> keyAccentBorderPaint
                    isEnterAction || isShiftActive -> keyAccentBorderPaint
                    else -> keyBorderPaint
                }

                canvas.drawRoundRect(drawRect, cornerRadius, cornerRadius, fillPaint)
                canvas.drawRoundRect(drawRect, cornerRadius, cornerRadius, borderPaint)

                // Subtle specular top edge highlight on unpressed standard keycaps
                if (!key.isPressed && !isEnterAction && !isShiftActive) {
                    canvas.drawLine(
                        drawRect.left + cornerRadius * 0.7f,
                        drawRect.top + dpToPx(1.1f),
                        drawRect.right - cornerRadius * 0.7f,
                        drawRect.top + dpToPx(1.1f),
                        topHighlightPaint
                    )
                }

                // Draw top-right long-press hint if present
                val hint = key.hintLabel
                if (hint != null) {
                    canvas.drawText(
                        hint,
                        drawRect.right - dpToPx(4.2f),
                        drawRect.top + dpToPx(11.2f),
                        hintTextPaint
                    )
                }

                // Draw key icon or Vazirmatn label
                when (key.type) {
                    KeyType.SHIFT -> drawShiftIcon(canvas, drawRect, shiftState)
                    KeyType.BACKSPACE -> drawBackspaceIcon(canvas, drawRect)
                    KeyType.ENTER -> {
                        if (key.label == "ENTER_ICON") {
                            drawEnterIcon(canvas, drawRect)
                        } else {
                            drawKeyLabel(canvas, key.label, drawRect, accentTextPaint, dpToPx(12.5f))
                        }
                    }
                    KeyType.ZWNJ -> drawZwnjIcon(canvas, drawRect)
                    KeyType.SPACE -> drawSpaceLabel(canvas, key.label, drawRect)
                    else -> {
                        val paintToUse = if (key.isSpecial) secondaryTextPaint else textPaint
                        val size = when {
                            key.type == KeyType.CHARACTER && isNumeric -> dpToPx(25f)
                            key.isSpecial -> specialFontSize
                            key.label.length > 1 -> dpToPx(14f)
                            else -> baseFontSize
                        }
                        drawKeyLabel(canvas, key.label, drawRect, paintToUse, size)
                    }
                }
            }
        }
    }

    private fun drawKeyLabel(canvas: Canvas, label: String, rect: RectF, paint: Paint, textSizePx: Float) {
        paint.textSize = textSizePx
        paint.getTextBounds(label, 0, label.length, textBounds)
        val x = rect.centerX()
        val y = rect.centerY() - textBounds.exactCenterY()
        canvas.drawText(label, x, y, paint)
    }

    private fun drawSpaceLabel(canvas: Canvas, label: String, rect: RectF) {
        // Subtle tactile pill indicator near bottom of spacebar
        val pillW = rect.width() * 0.24f
        val pillH = dpToPx(2.2f)
        val pillTop = rect.bottom - dpToPx(6.5f)
        shadowRect.set(rect.centerX() - pillW / 2f, pillTop, rect.centerX() + pillW / 2f, pillTop + pillH)
        canvas.drawRoundRect(shadowRect, pillH, pillH, spacePillBarPaint)

        secondaryTextPaint.textSize = dpToPx(11f)
        secondaryTextPaint.getTextBounds(label, 0, label.length, textBounds)
        val cx = rect.centerX()
        val cy = rect.centerY() - textBounds.exactCenterY() - dpToPx(2f)
        canvas.drawText(label, cx, cy, secondaryTextPaint)

        if (currentMode == KeyboardMode.EN_ALPHA || currentMode == KeyboardMode.FA_ALPHA) {
            hintTextPaint.textAlign = Paint.Align.CENTER
            val chevronY = rect.centerY() + dpToPx(2f)
            canvas.drawText("‹", rect.left + dpToPx(12f), chevronY, hintTextPaint)
            canvas.drawText("›", rect.right - dpToPx(12f), chevronY, hintTextPaint)
            hintTextPaint.textAlign = Paint.Align.RIGHT
        }
    }

    private fun drawShiftIcon(canvas: Canvas, bounds: RectF, state: ShiftState) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val s = dpToPx(8f)

        iconPath.reset()
        iconPath.moveTo(cx, cy - s)
        iconPath.lineTo(cx + s, cy + s * 0.1f)
        iconPath.lineTo(cx + s * 0.42f, cy + s * 0.1f)
        iconPath.lineTo(cx + s * 0.42f, cy + s * 0.78f)
        iconPath.lineTo(cx - s * 0.42f, cy + s * 0.78f)
        iconPath.lineTo(cx - s * 0.42f, cy + s * 0.1f)
        iconPath.lineTo(cx - s, cy + s * 0.1f)
        iconPath.close()

        if (state == ShiftState.OFF) {
            iconStrokePaint.color = GitHubDarkPalette.PRIMARY_TEXT
            canvas.drawPath(iconPath, iconStrokePaint)
        } else {
            iconFillPaint.color = GitHubDarkPalette.ACCENT
            canvas.drawPath(iconPath, iconFillPaint)
            if (state == ShiftState.CAPS_LOCK) {
                iconStrokePaint.color = GitHubDarkPalette.ACCENT
                canvas.drawLine(
                    cx - s * 0.55f,
                    cy + s * 1.15f,
                    cx + s * 0.55f,
                    cy + s * 1.15f,
                    iconStrokePaint
                )
            }
        }
    }

    private fun drawBackspaceIcon(canvas: Canvas, bounds: RectF) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val w = dpToPx(9.5f)
        val h = dpToPx(6.8f)

        iconStrokePaint.color = GitHubDarkPalette.PRIMARY_TEXT
        iconPath.reset()
        iconPath.moveTo(cx - w, cy)
        iconPath.lineTo(cx - w * 0.42f, cy - h)
        iconPath.lineTo(cx + w * 0.85f, cy - h)
        iconPath.lineTo(cx + w * 0.85f, cy + h)
        iconPath.lineTo(cx - w * 0.42f, cy + h)
        iconPath.close()
        canvas.drawPath(iconPath, iconStrokePaint)

        val xs = dpToPx(2.8f)
        val xCenter = cx + w * 0.22f
        canvas.drawLine(xCenter - xs, cy - xs, xCenter + xs, cy + xs, iconStrokePaint)
        canvas.drawLine(xCenter + xs, cy - xs, xCenter - xs, cy + xs, iconStrokePaint)
    }

    private fun drawEnterIcon(canvas: Canvas, bounds: RectF) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val w = dpToPx(8.5f)
        val h = dpToPx(5f)

        iconStrokePaint.color = GitHubDarkPalette.ACCENT
        iconPath.reset()
        iconPath.moveTo(cx + w * 0.7f, cy - h)
        iconPath.lineTo(cx + w * 0.7f, cy + h * 0.45f)
        iconPath.lineTo(cx - w * 0.7f, cy + h * 0.45f)
        canvas.drawPath(iconPath, iconStrokePaint)

        val tipX = cx - w * 0.7f
        val tipY = cy + h * 0.45f
        val head = dpToPx(3.6f)
        iconPath.reset()
        iconPath.moveTo(tipX + head, tipY - head)
        iconPath.lineTo(tipX, tipY)
        iconPath.lineTo(tipX + head, tipY + head)
        canvas.drawPath(iconPath, iconStrokePaint)
    }

    private fun drawZwnjIcon(canvas: Canvas, bounds: RectF) {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        secondaryTextPaint.textSize = dpToPx(11f)
        val label = "نیم"
        secondaryTextPaint.getTextBounds(label, 0, label.length, textBounds)
        canvas.drawText(label, cx, cy - textBounds.exactCenterY() - dpToPx(2f), secondaryTextPaint)

        iconStrokePaint.color = GitHubDarkPalette.ACCENT
        val barY = cy + dpToPx(6.5f)
        canvas.drawLine(cx - dpToPx(7f), barY, cx - dpToPx(2f), barY, iconStrokePaint)
        canvas.drawLine(cx + dpToPx(2f), barY, cx + dpToPx(7f), barY, iconStrokePaint)
        canvas.drawLine(cx, barY - dpToPx(3f), cx, barY + dpToPx(2f), iconStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (action == MotionEvent.ACTION_POINTER_DOWN) {
                    commitEarlierHeldCharacterKeys(exceptPointerId = pointerId)
                }

                val key = findKeyAt(x, y)
                if (key != null) {
                    key.isPressed = true
                    activePointers.put(pointerId, key)
                    if (pointerId in 0..15) {
                        pointerDownX[pointerId] = x
                        pointerDownY[pointerId] = y
                    }
                    alreadyCommittedPointers.delete(pointerId)
                    triggerHaptic()
                    invalidate()

                    if (key.type == KeyType.SPACE) {
                        spacePointerId = pointerId
                        spaceDownX = x
                        spaceSwiped = false
                    } else if (key.type == KeyType.BACKSPACE) {
                        listener?.onBackspace()
                        alreadyCommittedPointers.put(pointerId, true)
                        startBackspaceRepeat()
                    } else if (key.type == KeyType.CHARACTER) {
                        if (keyPreviewEnabled && currentMode != KeyboardMode.NUMERIC) {
                            overlayListener?.onShowPreview(key)
                        }
                        if (key.altOutputs.isNotEmpty()) {
                            scheduleLongPressAlternates(pointerId, key)
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isShowingAlternates) {
                    val idx = event.findPointerIndex(longPressPointerId)
                    if (idx >= 0) {
                        overlayListener?.onMoveAlternates(event.getX(idx))
                    }
                    return true
                }

                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val px = event.getX(i)
                    val py = event.getY(i)
                    val currentKey = activePointers.get(pId) ?: continue

                    // Spacebar horizontal swipe detection
                    if (pId == spacePointerId && currentKey.type == KeyType.SPACE) {
                        if (!spaceSwiped && abs(px - spaceDownX) >= spaceSwipeThreshold) {
                            spaceSwiped = true
                            triggerHaptic()
                            switchLanguage()
                            listener?.onLanguageSwitch()
                        }
                        continue
                    }

                    if (alreadyCommittedPointers.get(pId, false)) continue

                    // Touch hysteresis: only switch key if finger deliberately moved far outside
                    // the currently pressed key's hitBounds + slop margin!
                    if (pId in 0..15) {
                        val dxDown = abs(px - pointerDownX[pId])
                        val dyDown = abs(py - pointerDownY[pId])
                        if (dxDown < hysteresisSlop && dyDown < hysteresisSlop) {
                            continue
                        }
                    }

                    val insideHysteresisBox =
                        px >= currentKey.hitBounds.left - hysteresisSlop * 0.4f &&
                        px <= currentKey.hitBounds.right + hysteresisSlop * 0.4f &&
                        py >= currentKey.hitBounds.top - hysteresisSlop * 0.4f &&
                        py <= currentKey.hitBounds.bottom + hysteresisSlop * 0.4f

                    if (insideHysteresisBox) {
                        continue
                    }

                    val newKey = findKeyAt(px, py)
                    if (newKey != null && newKey != currentKey) {
                        cancelLongPressAlternates()
                        currentKey.isPressed = false
                        newKey.isPressed = true
                        activePointers.put(pId, newKey)
                        if (pId in 0..15) {
                            pointerDownX[pId] = px
                            pointerDownY[pId] = py
                        }

                        if (currentKey.type == KeyType.BACKSPACE && newKey.type != KeyType.BACKSPACE) {
                            stopBackspaceRepeat()
                        }
                        if (newKey.type == KeyType.CHARACTER && keyPreviewEnabled && currentMode != KeyboardMode.NUMERIC) {
                            overlayListener?.onShowPreview(newKey)
                        } else {
                            overlayListener?.onHidePreview()
                        }
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (isShowingAlternates && pointerId == longPressPointerId) {
                    val altChar = overlayListener?.onCommitAlternateAndHide()
                    isShowingAlternates = false
                    longPressKey = null
                    longPressPointerId = -1
                    activePointers.get(pointerId)?.isPressed = false
                    activePointers.remove(pointerId)
                    if (!altChar.isNullOrEmpty()) {
                        listener?.onText(altChar)
                        if (currentMode == KeyboardMode.EN_ALPHA && shiftState == ShiftState.ON) {
                            setShift(ShiftState.OFF)
                        }
                    }
                    invalidate()
                    return true
                }

                cancelLongPressAlternates()
                overlayListener?.onHidePreview()

                // Fallback to findKeyAt if pointer wasn't mapped, ensuring zero missed taps!
                val key = activePointers.get(pointerId) ?: findKeyAt(x, y)
                activePointers.remove(pointerId)

                if (key != null) {
                    key.isPressed = false
                    invalidate()

                    if (alreadyCommittedPointers.get(pointerId, false)) {
                        alreadyCommittedPointers.delete(pointerId)
                    } else if (key.type == KeyType.SPACE) {
                        if (!spaceSwiped) {
                            listener?.onText(" ")
                        }
                        if (pointerId == spacePointerId) {
                            spacePointerId = -1
                            spaceSwiped = false
                        }
                    } else {
                        handleKeyCommit(key)
                    }
                }

                if (action == MotionEvent.ACTION_UP) {
                    clearAllPressedStates()
                    stopBackspaceRepeat()
                    spacePointerId = -1
                    spaceSwiped = false
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelLongPressAlternates()
                overlayListener?.onHidePreview()
                overlayListener?.onCancelAlternates()
                isShowingAlternates = false
                clearAllPressedStates()
                stopBackspaceRepeat()
                spacePointerId = -1
                spaceSwiped = false
                invalidate()
            }
        }
        return true
    }

    private fun commitEarlierHeldCharacterKeys(exceptPointerId: Int) {
        for (i in 0 until activePointers.size()) {
            val pId = activePointers.keyAt(i)
            if (pId == exceptPointerId || alreadyCommittedPointers.get(pId, false)) continue
            val heldKey = activePointers.valueAt(i)
            if (heldKey != null && heldKey.type == KeyType.CHARACTER) {
                cancelLongPressAlternates()
                overlayListener?.onHidePreview()
                heldKey.isPressed = false
                alreadyCommittedPointers.put(pId, true)
                handleKeyCommit(heldKey)
            }
        }
    }

    private fun handleKeyCommit(key: KeyModel) {
        when (key.type) {
            KeyType.CHARACTER -> {
                listener?.onText(key.output)
                if (currentMode == KeyboardMode.EN_ALPHA && shiftState == ShiftState.ON) {
                    setShift(ShiftState.OFF)
                }
            }
            KeyType.SPACE -> listener?.onText(" ")
            KeyType.ZWNJ -> listener?.onZwnj()
            KeyType.BACKSPACE -> stopBackspaceRepeat()
            KeyType.ENTER -> listener?.onEnter()
            KeyType.SHIFT -> {
                val now = System.currentTimeMillis()
                if (now - lastShiftPressTime < 320) {
                    listener?.onShiftDoubleTap()
                    setShift(ShiftState.CAPS_LOCK)
                } else {
                    val nextShift = when (shiftState) {
                        ShiftState.OFF -> ShiftState.ON
                        ShiftState.ON -> ShiftState.OFF
                        ShiftState.CAPS_LOCK -> ShiftState.OFF
                    }
                    listener?.onShiftToggle()
                    setShift(nextShift)
                }
                lastShiftPressTime = now
            }
            KeyType.LANGUAGE_SWITCH -> {
                switchLanguage()
                listener?.onLanguageSwitch()
            }
            KeyType.MODE_SWITCH -> {
                when (key.output) {
                    "NUMERIC" -> setMode(KeyboardMode.NUMERIC)
                    "SYMBOLS" -> setMode(KeyboardMode.SYMBOLS)
                    "SYMBOLS_EXT" -> setMode(KeyboardMode.SYMBOLS_EXTENDED)
                    "ALPHA" -> setMode(lastAlphaMode)
                    else -> setMode(lastAlphaMode)
                }
                listener?.onModeSwitch(key.output)
            }
            KeyType.NUMERIC_SCRIPT_TOGGLE -> {
                togglePersianDigits()
                listener?.onNumericScriptToggle()
            }
        }
    }

    private fun scheduleLongPressAlternates(pointerId: Int, key: KeyModel) {
        cancelLongPressAlternates()
        longPressPointerId = pointerId
        longPressKey = key
        handler.postDelayed(longPressAlternatesRunnable, longPressDelayMs.toLong())
    }

    private fun cancelLongPressAlternates() {
        handler.removeCallbacks(longPressAlternatesRunnable)
        longPressKey = null
        longPressPointerId = -1
    }

    private fun startBackspaceRepeat() {
        isBackspaceRepeating = true
        handler.removeCallbacks(backspaceRepeatRunnable)
        handler.postDelayed(backspaceRepeatRunnable, 340)
    }

    private fun stopBackspaceRepeat() {
        isBackspaceRepeating = false
        handler.removeCallbacks(backspaceRepeatRunnable)
    }

    private fun clearAllPressedStates() {
        for (i in 0 until activePointers.size()) {
            activePointers.valueAt(i)?.isPressed = false
        }
        activePointers.clear()
        alreadyCommittedPointers.clear()
    }

    /**
     * Zero-dead-zone key lookup:
     * 1) Checks seamless hitBounds (which cover 100% of view area).
     * 2) Falls back to nearest key center so no coordinate ever returns null.
     */
    private fun findKeyAt(x: Float, y: Float): KeyModel? {
        for (keyRow in keyRows) {
            for (key in keyRow.keys) {
                if (key.hitBounds.contains(x, y)) {
                    return key
                }
            }
        }

        // Fallback: nearest key center
        var bestKey: KeyModel? = null
        var bestDistSq = Float.MAX_VALUE
        for (keyRow in keyRows) {
            for (key in keyRow.keys) {
                val dx = x - key.bounds.centerX()
                val dy = y - key.bounds.centerY()
                val distSq = dx * dx + dy * dy
                if (distSq < bestDistSq) {
                    bestDistSq = distSq
                    bestKey = key
                }
            }
        }
        return bestKey
    }

    private fun triggerHaptic() {
        if (hapticEnabled) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
