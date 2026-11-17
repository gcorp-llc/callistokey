package com.example.ime.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ime.GitHubDarkPalette
import com.example.ime.KeyModel
import kotlin.math.max
import kotlin.math.min

/**
 * Container hosting CallistoKey's SuggestionBarView and CustomKeyboardView,
 * rendering lag-free floating Key Preview capsules and Long-Press Alternates strips
 * directly in dispatchDraw without Window/PopupWindow overhead.
 */
class KeyboardContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), CustomKeyboardView.OnOverlayListener {

    val suggestionBar: SuggestionBarView
    val keyboardView: CustomKeyboardView

    private var previewKey: KeyModel? = null
    private var alternatesAnchor: KeyModel? = null
    private var alternatesList: List<String> = emptyList()
    private var selectedAltIndex: Int = 0

    private val previewRect = RectF()
    private val altBoxRect = RectF()
    private val altItemRect = RectF()
    private val textBounds = Rect()

    private val popupBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.POPUP_BACKGROUND
    }
    private val popupBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.2f)
        color = GitHubDarkPalette.ACCENT_BORDER
    }
    private val altHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = GitHubDarkPalette.ACCENT_SURFACE
    }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = GitHubDarkPalette.PRIMARY_TEXT
        textSize = dpToPx(24f)
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(GitHubDarkPalette.KEYBOARD_SURFACE)
        clipChildren = false
        clipToPadding = false

        val tf = try {
            ResourcesCompat.getFont(context, R.font.vazirmatn)
        } catch (_: Exception) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/vazirmatn.ttf")
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
        } ?: Typeface.DEFAULT
        popupTextPaint.typeface = tf

        suggestionBar = SuggestionBarView(context)
        keyboardView = CustomKeyboardView(context)
        keyboardView.overlayListener = this

        addView(
            suggestionBar,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
        addView(
            keyboardView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    override fun onShowPreview(key: KeyModel) {
        previewKey = key
        invalidate()
    }

    override fun onHidePreview() {
        if (previewKey != null) {
            previewKey = null
            invalidate()
        }
    }

    override fun onShowAlternates(anchorKey: KeyModel, alternates: List<String>) {
        alternatesAnchor = anchorKey
        alternatesList = alternates
        selectedAltIndex = 0
        computeAlternatesBox()
        invalidate()
    }

    override fun onMoveAlternates(touchXInKeyboard: Float) {
        if (alternatesList.isEmpty()) return
        val itemW = altBoxRect.width() / alternatesList.size
        val idx = ((touchXInKeyboard - altBoxRect.left) / itemW).toInt()
        val clamped = max(0, min(alternatesList.size - 1, idx))
        if (clamped != selectedAltIndex) {
            selectedAltIndex = clamped
            invalidate()
        }
    }

    override fun onCommitAlternateAndHide(): String? {
        val result = alternatesList.getOrNull(selectedAltIndex)
        alternatesAnchor = null
        alternatesList = emptyList()
        invalidate()
        return result
    }

    override fun onCancelAlternates() {
        alternatesAnchor = null
        alternatesList = emptyList()
        invalidate()
    }

    private fun computeAlternatesBox() {
        val anchor = alternatesAnchor ?: return
        val count = alternatesList.size
        if (count == 0) return

        val itemW = dpToPx(38f)
        val boxW = itemW * count + dpToPx(8f)
        val boxH = dpToPx(44f)

        val anchorTopInContainer = keyboardView.top + anchor.bounds.top
        var left = anchor.bounds.centerX() - boxW / 2f
        left = max(dpToPx(4f), min(width - boxW - dpToPx(4f), left))
        val bottom = max(boxH + dpToPx(2f), anchorTopInContainer - dpToPx(6f))
        val top = bottom - boxH

        altBoxRect.set(left, top, left + boxW, bottom)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        // Draw floating alternates strip if active
        if (alternatesList.isNotEmpty()) {
            val radius = dpToPx(10f)
            canvas.drawRoundRect(altBoxRect, radius, radius, popupBgPaint)
            canvas.drawRoundRect(altBoxRect, radius, radius, popupBorderPaint)

            val pad = dpToPx(4f)
            val innerW = altBoxRect.width() - pad * 2
            val itemW = innerW / alternatesList.size
            popupTextPaint.textSize = dpToPx(18f)

            for (i in alternatesList.indices) {
                val itemLeft = altBoxRect.left + pad + i * itemW
                altItemRect.set(itemLeft, altBoxRect.top + pad, itemLeft + itemW, altBoxRect.bottom - pad)
                if (i == selectedAltIndex) {
                    canvas.drawRoundRect(altItemRect, dpToPx(6f), dpToPx(6f), altHighlightPaint)
                }
                val ch = alternatesList[i]
                popupTextPaint.getTextBounds(ch, 0, ch.length, textBounds)
                canvas.drawText(
                    ch,
                    altItemRect.centerX(),
                    altItemRect.centerY() - textBounds.exactCenterY(),
                    popupTextPaint
                )
            }
            return
        }

        // Draw lightweight floating key preview bubble
        val key = previewKey ?: return
        val keyTopInContainer = keyboardView.top + key.bounds.top
        val previewW = max(key.bounds.width() * 1.32f, dpToPx(42f))
        val previewH = dpToPx(46f)

        var left = key.bounds.centerX() - previewW / 2f
        left = max(dpToPx(4f), min(width - previewW - dpToPx(4f), left))
        val bottom = max(previewH + dpToPx(2f), keyTopInContainer - dpToPx(4f))
        val top = bottom - previewH

        previewRect.set(left, top, left + previewW, bottom)
        val radius = dpToPx(10f)
        canvas.drawRoundRect(previewRect, radius, radius, popupBgPaint)
        canvas.drawRoundRect(previewRect, radius, radius, popupBorderPaint)

        popupTextPaint.textSize = dpToPx(24f)
        val label = key.label
        popupTextPaint.getTextBounds(label, 0, label.length, textBounds)
        canvas.drawText(
            label,
            previewRect.centerX(),
            previewRect.centerY() - textBounds.exactCenterY(),
            popupTextPaint
        )
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
