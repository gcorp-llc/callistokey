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
import com.example.ime.GitHubDarkPalette
import com.example.ime.KeyModel
import com.example.ime.KeyType
import com.example.ime.KeyboardLayout
import com.example.ime.KeyboardMode
import com.example.ime.ShiftState
import kotlin.math.abs
import kotlin.math.max

/**
 * Ultra-fast, zero-allocation custom Canvas View for CallistoKey.
 * Features multi-touch rollover, Spacebar swipe language switching,
 * long-press alternates, crisp vector icon paths, and modern GitHub Dark keycaps.
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

    // Geometry constants
    private val horizontalPadding = dpToPx(4f)
    private val verticalPadding = dpToPx(6f)
    private val keyGapX = dpToPx(4.5f)
    private val keyGapY = dpToPx(6f)
    private val cornerRadius = dpToPx(7.5f)
    private val shadowOffsetY = dpToPx(1.8f)
    private val borderWidth = dpToPx(0.9f)
    private val spaceSwipeThreshold = dpToPx(28f)

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
        strokeWidth = borderWidth
        color = GitHubDarkPalette.ACCENT_BORDER
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
        strokeWidth = dpToPx(1.75f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = GitHubDarkPalette.PRIMARY_TEXT
    }
    private val iconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.PRIMARY_TEXT
    }

    // Reusable drawing rects and paths
    private val shadowRect = RectF()
    private val textBounds = Rect()
    private val iconPath = Path()

    private var rows: List<List<KeyModel>> = emptyList()

    // Multi-touch tracking
    private val activePointers = SparseArray<KeyModel>()
    private val alreadyCommittedPointers = SparseBooleanArray()

    // Spacebar swipe tracking
    private var spacePointerId = -1
    private var spaceDownX = 0f
    private var spaceSwiped = false

    // Long-press handling (alternates + backspace repeat)
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
                handler.postDelayed(this, 40)
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
        rows = KeyboardLayout.buildLayout(
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
        val desiredHeightDp = if (isLandscape) 176f else 238f
        val desiredHeightPx = dpToPx(desiredHeightDp).toInt()
        setMeasuredDimension(width, resolveSize(desiredHeightPx, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateKeyBounds(w, h)
    }

    private fun calculateKeyBounds(w: Int, h: Int) {
        if (w <= 0 || h <= 0 || rows.isEmpty()) return

        val numRows = rows.size
        val availableH = h - (verticalPadding * 2) - (keyGapY * (numRows - 1))
        val rowH = max(1f, availableH / numRows)
        var curY = verticalPadding

        for (row in rows) {
            val numKeys = row.size
            if (numKeys == 0) continue

            val totalWeight = row.fold(0f) { acc, key -> acc + key.weight }
            val availableW = w - (horizontalPadding * 2) - (keyGapX * (numKeys - 1))
            val baseKeyW = max(1f, availableW / totalWeight)

            var curX = horizontalPadding
            for (key in row) {
                val keyW = key.weight * baseKeyW
                key.bounds.set(curX, curY, curX + keyW, curY + rowH)
                curX += keyW + keyGapX
            }
            curY += rowH + keyGapY
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val isNumeric = currentMode == KeyboardMode.NUMERIC
        val baseFontSize = if (isNumeric) dpToPx(25f) else dpToPx(18f)
        val specialFontSize = dpToPx(13f)

        for (row in rows) {
            for (key in row) {
                val isEnterAction = key.type == KeyType.ENTER
                val isShiftActive = key.type == KeyType.SHIFT && shiftState != ShiftState.OFF

                // Subtle bottom shadow for modern tactile depth (skipped when pressed)
                if (!key.isPressed) {
                    shadowRect.set(
                        key.bounds.left,
                        key.bounds.top + shadowOffsetY,
                        key.bounds.right,
                        key.bounds.bottom + shadowOffsetY
                    )
                    canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, keyShadowPaint)
                }

                val fillPaint = when {
                    key.isPressed -> keyPressedPaint
                    isEnterAction -> keyAccentBgPaint
                    isShiftActive -> keyAccentBgPaint
                    key.isSpecial -> keySpecialPaint
                    else -> keyNormalPaint
                }
                val borderPaint = if (isEnterAction || isShiftActive) keyAccentBorderPaint else keyBorderPaint

                canvas.drawRoundRect(key.bounds, cornerRadius, cornerRadius, fillPaint)
                canvas.drawRoundRect(key.bounds, cornerRadius, cornerRadius, borderPaint)

                // Draw top-right long-press hint if present
                val hint = key.hintLabel
                if (hint != null) {
                    canvas.drawText(
                        hint,
                        key.bounds.right - dpToPx(4.5f),
                        key.bounds.top + dpToPx(11.5f),
                        hintTextPaint
                    )
                }

                // Draw vector icons for functional keys or crisp Vazirmatn text
                when (key.type) {
                    KeyType.SHIFT -> drawShiftIcon(canvas, key.bounds, shiftState)
                    KeyType.BACKSPACE -> drawBackspaceIcon(canvas, key.bounds)
                    KeyType.ENTER -> {
                        if (key.label == "ENTER_ICON") {
                            drawEnterIcon(canvas, key.bounds)
                        } else {
                            drawKeyLabel(canvas, key, accentTextPaint, dpToPx(12.5f))
                        }
                    }
                    KeyType.ZWNJ -> drawZwnjIcon(canvas, key.bounds)
                    KeyType.SPACE -> drawSpaceLabel(canvas, key)
                    else -> {
                        val paintToUse = when {
                            key.isSpecial -> secondaryTextPaint
                            else -> textPaint
                        }
                        val size = when {
                            key.type == KeyType.CHARACTER && isNumeric -> dpToPx(25f)
                            key.isSpecial -> specialFontSize
                            key.label.length > 1 -> dpToPx(14f)
                            else -> baseFontSize
                        }
                        drawKeyLabel(canvas, key, paintToUse, size)
                    }
                }
            }
        }
    }

    private fun drawKeyLabel(canvas: Canvas, key: KeyModel, paint: Paint, textSizePx: Float) {
        paint.textSize = textSizePx
        val label = key.label
        paint.getTextBounds(label, 0, label.length, textBounds)
        val x = key.bounds.centerX()
        val y = key.bounds.centerY() - textBounds.exactCenterY()
        canvas.drawText(label, x, y, paint)
    }

    private fun drawSpaceLabel(canvas: Canvas, key: KeyModel) {
        secondaryTextPaint.textSize = dpToPx(11.5f)
        val label = key.label
        secondaryTextPaint.getTextBounds(label, 0, label.length, textBounds)
        val cx = key.bounds.centerX()
        val cy = key.bounds.centerY() - textBounds.exactCenterY()
        canvas.drawText(label, cx, cy, secondaryTextPaint)

        // Subtle left/right chevron hints on Spacebar edges indicating swipe language switch
        if (currentMode == KeyboardMode.EN_ALPHA || currentMode == KeyboardMode.FA_ALPHA) {
            hintTextPaint.textAlign = Paint.Align.CENTER
            val chevronY = key.bounds.centerY() + dpToPx(4f)
            canvas.drawText("‹", key.bounds.left + dpToPx(12f), chevronY, hintTextPaint)
            canvas.drawText("›", key.bounds.right - dpToPx(12f), chevronY, hintTextPaint)
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
                canvas.drawLine(
                    cx - s * 0.55f,
                    cy + s * 1.15f,
                    cx + s * 0.55f,
                    cy + s * 1.15f,
                    iconStrokePaint.apply { color = GitHubDarkPalette.ACCENT }
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

        // Inner X
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

        // Arrow head left
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

        // Visual ZWNJ join-barrier indicator underneath
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
                // Multi-touch rollover: commit any earlier held character key immediately
                if (action == MotionEvent.ACTION_POINTER_DOWN) {
                    commitEarlierHeldCharacterKeys(exceptPointerId = pointerId)
                }

                val key = findKeyAt(x, y)
                if (key != null) {
                    key.isPressed = true
                    activePointers.put(pointerId, key)
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
                if (isShowingAlternates && pointerId == longPressPointerId || (isShowingAlternates && event.findPointerIndex(longPressPointerId) >= 0)) {
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
                    val currentKey = activePointers.get(pId)

                    // Spacebar horizontal swipe detection
                    if (pId == spacePointerId && currentKey?.type == KeyType.SPACE) {
                        if (!spaceSwiped && abs(px - spaceDownX) >= spaceSwipeThreshold) {
                            spaceSwiped = true
                            triggerHaptic()
                            switchLanguage()
                            listener?.onLanguageSwitch()
                        }
                        continue
                    }

                    val newKey = findKeyAt(px, py)
                    if (currentKey != newKey && !alreadyCommittedPointers.get(pId)) {
                        cancelLongPressAlternates()
                        currentKey?.isPressed = false
                        newKey?.isPressed = true
                        activePointers.put(pId, newKey)

                        if (currentKey?.type == KeyType.BACKSPACE && newKey?.type != KeyType.BACKSPACE) {
                            stopBackspaceRepeat()
                        }
                        if (newKey?.type == KeyType.CHARACTER && keyPreviewEnabled && currentMode != KeyboardMode.NUMERIC) {
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

                val key = activePointers.get(pointerId)
                if (key != null) {
                    key.isPressed = false
                    activePointers.remove(pointerId)
                    invalidate()

                    if (pIdWasCommitted(pointerId)) {
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

    private fun pIdWasCommitted(pointerId: Int): Boolean {
        return alreadyCommittedPointers.get(pointerId, false)
    }

    private fun commitEarlierHeldCharacterKeys(exceptPointerId: Int) {
        for (i in 0 until activePointers.size()) {
            val pId = activePointers.keyAt(i)
            if (pId == exceptPointerId || pIdWasCommitted(pId)) continue
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
        handler.postDelayed(longPressAlternatesRunnable, 310)
    }

    private fun cancelLongPressAlternates() {
        handler.removeCallbacks(longPressAlternatesRunnable)
        longPressKey = null
        longPressPointerId = -1
    }

    private fun startBackspaceRepeat() {
        isBackspaceRepeating = true
        handler.removeCallbacks(backspaceRepeatRunnable)
        handler.postDelayed(backspaceRepeatRunnable, 360)
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

    private fun findKeyAt(x: Float, y: Float): KeyModel? {
        for (row in rows) {
            for (key in row) {
                if (key.bounds.contains(x, y)) {
                    return key
                }
            }
        }
        return null
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
