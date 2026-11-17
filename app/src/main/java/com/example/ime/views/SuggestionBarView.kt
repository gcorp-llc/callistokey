package com.example.ime.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ime.GitHubDarkPalette
import com.example.ime.nlp.SuggestionChip
import kotlin.math.max

/**
 * High-speed native suggestion strip above the keyboard.
 * Renders spell corrections, phrase/sentence predictions, and quick language indicator
 * with zero per-frame allocations.
 */
class SuggestionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnSuggestionListener {
        fun onSuggestionClicked(chip: SuggestionChip)
        fun onLanguageBadgeClicked()
    }

    var listener: OnSuggestionListener? = null
    var hapticEnabled: Boolean = true

    private var chips: List<SuggestionChip> = emptyList()
    private var isSensitiveMode: Boolean = false
    private var activeLanguageTag: String = "EN"

    // Pre-allocated hitboxes for up to 4 chips + 1 language badge
    private val chipRects = Array(4) { RectF() }
    private val langBadgeRect = RectF()
    private var pressedChipIndex = -1
    private var pressedLangBadge = false

    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.SUGGESTION_SURFACE
    }
    private val dividerPaint = Paint().apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.BORDER
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.SUGGESTION_PILL_BG
    }
    private val pillAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.ACCENT_SURFACE
    }
    private val pillPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.KEY_PRESSED
    }
    private val pillAccentBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        color = GitHubDarkPalette.ACCENT_BORDER
    }
    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(0.8f)
        color = GitHubDarkPalette.BORDER
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.PRIMARY_TEXT
        textSize = dpToPx(13.5f)
    }
    private val accentTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.ACCENT
        textSize = dpToPx(13.5f)
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.SECONDARY_TEXT
        textSize = dpToPx(12f)
    }

    private val textBounds = Rect()
    private val pillRadius = dpToPx(8f)
    private val badgeRadius = dpToPx(7f)
    private val padH = dpToPx(6f)
    private val padV = dpToPx(5f)
    private val gap = dpToPx(5f)

    init {
        loadFont()
    }

    private fun loadFont() {
        val tf = try {
            ResourcesCompat.getFont(context, R.font.vazirmatn)
        } catch (_: Exception) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/vazirmatn.ttf")
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
        } ?: Typeface.DEFAULT
        textPaint.typeface = tf
        accentTextPaint.typeface = Typeface.create(tf, Typeface.BOLD)
        secondaryTextPaint.typeface = tf
    }

    fun updateState(
        newChips: List<SuggestionChip>,
        sensitive: Boolean,
        languageTag: String
    ) {
        chips = if (sensitive) emptyList() else newChips.take(4)
        isSensitiveMode = sensitive
        activeLanguageTag = languageTag
        pressedChipIndex = -1
        calculateBounds()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = dpToPx(42f).toInt()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBounds()
    }

    private fun calculateBounds() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val badgeW = dpToPx(48f)
        langBadgeRect.set(w - padH - badgeW, padV, w - padH, h - padV)

        val count = chips.size
        if (count == 0) return

        val chipsAreaWidth = w - (padH * 2) - badgeW - gap
        val chipW = max(1f, (chipsAreaWidth - gap * (count - 1)) / count)

        var curX = padH
        for (i in 0 until count) {
            chipRects[i].set(curX, padV, curX + chipW, h - padV)
            curX += chipW + gap
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawRect(0f, h - dpToPx(0.8f), w, h, dividerPaint)

        // Draw right-side language switch badge
        val badgeFill = if (pressedLangBadge) pillPressedPaint else pillPaint
        canvas.drawRoundRect(langBadgeRect, badgeRadius, badgeRadius, badgeFill)
        canvas.drawRoundRect(langBadgeRect, badgeRadius, badgeRadius, pillBorderPaint)

        accentTextPaint.textSize = dpToPx(11.5f)
        accentTextPaint.getTextBounds(activeLanguageTag, 0, activeLanguageTag.length, textBounds)
        canvas.drawText(
            activeLanguageTag,
            langBadgeRect.centerX(),
            langBadgeRect.centerY() - textBounds.exactCenterY(),
            accentTextPaint
        )

        if (isSensitiveMode) {
            val msg = "🔒 Private input • History & learning paused"
            secondaryTextPaint.textAlign = Paint.Align.LEFT
            secondaryTextPaint.getTextBounds(msg, 0, msg.length, textBounds)
            canvas.drawText(
                msg,
                dpToPx(12f),
                h / 2f - textBounds.exactCenterY(),
                secondaryTextPaint
            )
            secondaryTextPaint.textAlign = Paint.Align.CENTER
            return
        }

        if (chips.isEmpty()) {
            val brand = "CallistoKey  •  Swipe Space ⇄"
            secondaryTextPaint.textAlign = Paint.Align.LEFT
            secondaryTextPaint.getTextBounds(brand, 0, brand.length, textBounds)
            canvas.drawText(
                brand,
                dpToPx(12f),
                h / 2f - textBounds.exactCenterY(),
                secondaryTextPaint
            )
            secondaryTextPaint.textAlign = Paint.Align.CENTER
            return
        }

        // Draw suggestion pills
        for (i in chips.indices) {
            val chip = chips[i]
            val rect = chipRects[i]

            val bg = when {
                pressedChipIndex == i -> pillPressedPaint
                chip.isCorrection -> pillAccentPaint
                else -> pillPaint
            }
            val border = if (chip.isCorrection) pillAccentBorderPaint else pillBorderPaint

            canvas.drawRoundRect(rect, pillRadius, pillRadius, bg)
            canvas.drawRoundRect(rect, pillRadius, pillRadius, border)

            val paint = if (chip.isCorrection) accentTextPaint else textPaint
            paint.textSize = if (chip.text.length > 12) dpToPx(11.5f) else dpToPx(13.5f)

            val label = ellipsizeText(chip.text, paint, rect.width() - dpToPx(10f))
            paint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawText(
                label,
                rect.centerX(),
                rect.centerY() - textBounds.exactCenterY(),
                paint
            )
        }
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 2 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (langBadgeRect.contains(x, y)) {
                    pressedLangBadge = true
                    invalidate()
                    return true
                }
                if (!isSensitiveMode) {
                    for (i in chips.indices) {
                        if (chipRects[i].contains(x, y)) {
                            pressedChipIndex = i
                            invalidate()
                            return true
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pressedLangBadge && langBadgeRect.contains(x, y)) {
                    if (hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    listener?.onLanguageBadgeClicked()
                } else if (pressedChipIndex in chips.indices && chipRects[pressedChipIndex].contains(x, y)) {
                    if (hapticEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    listener?.onSuggestionClicked(chips[pressedChipIndex])
                }
                pressedChipIndex = -1
                pressedLangBadge = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedChipIndex = -1
                pressedLangBadge = false
                invalidate()
            }
        }
        return true
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
