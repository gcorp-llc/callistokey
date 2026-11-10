package com.example.ime.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseArray
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
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance, zero-allocation custom View that renders the GitHub Dark keyboard
 * with native Vazirmatn typography and sub-millisecond touch dispatch.
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

    var listener: OnKeyboardActionListener? = null

    // Keyboard state
    var currentMode: KeyboardMode = KeyboardMode.EN_ALPHA
        private set
    var shiftState: ShiftState = ShiftState.OFF
        private set
    var usePersianDigits: Boolean = false
        private set
    var enterActionLabel: String = "⏎"
        private set
    var lastAlphaMode: KeyboardMode = KeyboardMode.EN_ALPHA
        private set

    // Dimensions
    private val horizontalPadding = dpToPx(3f)
    private val verticalPadding = dpToPx(4f)
    private val keyGapX = dpToPx(3f)
    private val keyGapY = dpToPx(4f)
    private val cornerRadius = dpToPx(5f)
    private val borderWidth = dpToPx(1f)

    // Cached Typeface
    private var vazirmatnTypeface: Typeface? = null

    // Pre-allocated Paints (ZERO GC allocations during drawing)
    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEYBOARD_SURFACE
    }
    private val keyNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEY_BACKGROUND
    }
    private val keySpecialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.SPECIAL_KEY
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

    // Key structures
    private var rows: List<List<KeyModel>> = emptyList()

    // Multi-touch tracking: pointerId -> pressed KeyModel
    private val activePointers = SparseArray<KeyModel>()

    // Backspace continuous repeat
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var isBackspaceRepeating = false
    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceRepeating) {
                listener?.onBackspace()
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                repeatHandler.postDelayed(this, 45) // Continuous repeat interval
            }
        }
    }

    // Shift double-tap detection for Caps Lock
    private var lastShiftPressTime = 0L

    // Reusable text bounds rect
    private val textBounds = Rect()

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
        requestLayout()
        invalidate()
    }

    fun setShift(state: ShiftState) {
        shiftState = state
        if (currentMode == KeyboardMode.EN_ALPHA) {
            rebuildLayout()
            invalidate()
        }
    }

    fun setEnterLabel(label: String) {
        enterActionLabel = label
        rebuildLayout()
        invalidate()
    }

    fun togglePersianDigits() {
        usePersianDigits = !usePersianDigits
        if (currentMode == KeyboardMode.NUMERIC) {
            rebuildLayout()
            invalidate()
        }
    }

    fun switchLanguage() {
        if (currentMode == KeyboardMode.EN_ALPHA) {
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
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        // Default comfortable touch height for keyboards
        val desiredHeightDp = if (isLandscape) 180f else 240f
        val desiredHeightPx = dpToPx(desiredHeightDp).toInt()

        val height = resolveSize(desiredHeightPx, heightMeasureSpec)
        setMeasuredDimension(width, height)
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

        // Draw keyboard surface background (#161B22)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw keys
        val isNumeric = currentMode == KeyboardMode.NUMERIC
        val baseFontSize = if (isNumeric) dpToPx(24f) else dpToPx(17.5f)
        val specialFontSize = dpToPx(13.5f)

        for (row in rows) {
            for (key in row) {
                // Background & border
                val fillPaint = when {
                    key.isPressed -> keyPressedPaint
                    key.isSpecial -> keySpecialPaint
                    else -> keyNormalPaint
                }

                canvas.drawRoundRect(key.bounds, cornerRadius, cornerRadius, fillPaint)
                canvas.drawRoundRect(key.bounds, cornerRadius, cornerRadius, keyBorderPaint)

                // Select text paint and size
                val paintToUse = when {
                    key.type == KeyType.ENTER && key.label != "⏎" -> accentTextPaint
                    key.type == KeyType.SHIFT && shiftState != ShiftState.OFF -> accentTextPaint
                    key.isSpecial && key.type != KeyType.BACKSPACE && key.type != KeyType.SHIFT -> secondaryTextPaint
                    else -> textPaint
                }

                paintToUse.textSize = when {
                    key.type == KeyType.CHARACTER && isNumeric -> dpToPx(24f)
                    key.type == KeyType.SPACE || key.type == KeyType.ZWNJ -> dpToPx(13.5f)
                    key.type == KeyType.ENTER && key.label.length > 2 -> dpToPx(13f)
                    key.isSpecial -> specialFontSize
                    key.label.length > 1 -> dpToPx(14f)
                    else -> baseFontSize
                }

                // Center text vertically and horizontally
                val label = key.label
                paintToUse.getTextBounds(label, 0, label.length, textBounds)
                val textX = key.bounds.centerX()
                val textY = key.bounds.centerY() - textBounds.exactCenterY()

                canvas.drawText(label, textX, textY, paintToUse)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val key = findKeyAt(x, y)
                if (key != null) {
                    key.isPressed = true
                    activePointers.put(pointerId, key)
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    invalidate()

                    handleKeyDown(key)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val px = event.getX(i)
                    val py = event.getY(i)
                    val oldKey = activePointers.get(pId)
                    val newKey = findKeyAt(px, py)

                    if (oldKey != newKey) {
                        oldKey?.isPressed = false
                        newKey?.isPressed = true
                        activePointers.put(pId, newKey)
                        if (oldKey?.type == KeyType.BACKSPACE && newKey?.type != KeyType.BACKSPACE) {
                            stopBackspaceRepeat()
                        }
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val key = activePointers.get(pointerId)
                if (key != null) {
                    key.isPressed = false
                    activePointers.remove(pointerId)
                    invalidate()

                    handleKeyUp(key)
                }
                if (action == MotionEvent.ACTION_UP) {
                    clearAllPressedStates()
                    stopBackspaceRepeat()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                clearAllPressedStates()
                stopBackspaceRepeat()
                invalidate()
            }
        }
        return true
    }

    private fun handleKeyDown(key: KeyModel) {
        if (key.type == KeyType.BACKSPACE) {
            // Immediate deletion on touch down
            listener?.onBackspace()
            startBackspaceRepeat()
        }
    }

    private fun handleKeyUp(key: KeyModel) {
        when (key.type) {
            KeyType.CHARACTER -> {
                listener?.onText(key.output)
                if (currentMode == KeyboardMode.EN_ALPHA && shiftState == ShiftState.ON) {
                    setShift(ShiftState.OFF)
                }
            }
            KeyType.SPACE -> {
                listener?.onText(" ")
            }
            KeyType.ZWNJ -> {
                listener?.onZwnj()
            }
            KeyType.BACKSPACE -> {
                stopBackspaceRepeat()
            }
            KeyType.ENTER -> {
                listener?.onEnter()
            }
            KeyType.SHIFT -> {
                val now = System.currentTimeMillis()
                if (now - lastShiftPressTime < 320) {
                    // Double-tap detected: Caps Lock
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

    private fun startBackspaceRepeat() {
        isBackspaceRepeating = true
        repeatHandler.removeCallbacks(backspaceRepeatRunnable)
        repeatHandler.postDelayed(backspaceRepeatRunnable, 380) // Initial hold delay before repeat
    }

    private fun stopBackspaceRepeat() {
        isBackspaceRepeating = false
        repeatHandler.removeCallbacks(backspaceRepeatRunnable)
    }

    private fun clearAllPressedStates() {
        for (i in 0 until activePointers.size()) {
            activePointers.valueAt(i)?.isPressed = false
        }
        activePointers.clear()
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

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
