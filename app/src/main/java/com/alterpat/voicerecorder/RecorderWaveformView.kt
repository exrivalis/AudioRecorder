package com.alterpat.voicerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class RecorderWaveformView : View {

    private lateinit var amplitudes: ArrayList<Int>
    private lateinit var spikes: ArrayList<RectF>
    private lateinit var paintRead: Paint
    private var w: Float = 9f
    private var d: Float = 4f
    private var sw: Int = 0
    private var maxSpikes: Int = 0
    private var maxAmp: Int = 200

    constructor(context: Context?) : super(context) {
        init(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    // this function is to avoid duplicating code in every constructor
    // indeed each constructor is called in a specific situation
    // and we want the View to de the same thing no matter what
    private fun init(attrs: AttributeSet?) {
        amplitudes = ArrayList()
        paintRead = Paint() //Paint.ANTI_ALIAS_FLAG
        paintRead.color = Color.rgb(244, 81, 30) // orange

        // get screen width
        val displayMetrics = resources.displayMetrics
        sw = displayMetrics.widthPixels

        maxSpikes = (sw / (w + d)).toInt()
        spikes = ArrayList()
    }

    fun reset() {
        amplitudes.clear()
        spikes.clear()
        invalidate()
    }

    fun updateAmps(amp: Int) {

        var norm = Math.min(amp / 7, maxAmp) // 100*abs(Math.log10(1.0*amp/(sqrt(amp*1.0)+1)))
        amplitudes.add(norm)
        var amps = amplitudes.takeLast(maxSpikes)

        spikes.clear()

        for (i in amps.indices) {
            val delta = maxAmp.toFloat()
            val top = delta - amps[i]
            var bottom = top + amps[i] as Int
            var rectUp = RectF(sw - i * (w + d), top, sw - i * (w + d) - w, bottom)
            var rectDown = RectF(sw - i * (w + d), delta - 2, sw - i * (w + d) - w, delta + amps[i])
            spikes.add(rectUp)
            spikes.add(rectDown)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        // this may be called several times on start or create
        // therefore we shouldn't initialize objects here

        spikes.forEach {
            canvas?.drawRoundRect(it, 6f, 6f, paintRead)
        }
    }
}