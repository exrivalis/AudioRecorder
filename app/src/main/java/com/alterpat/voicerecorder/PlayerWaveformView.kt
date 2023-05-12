package com.alterpat.voicerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class PlayerWaveformView : View {

    private lateinit var spikes: Array<RectF>
    private lateinit var paintRead: Paint
    private var w: Int = 18
    private var d: Int = 4
    private var sw: Int = 0
    private var maxAmp: Int = 200
    private var delta = 320

    private lateinit var rect: Rect
    private var nbSpikes = 30

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
        spikes = Array<RectF>(nbSpikes) { RectF() }

        paintRead = Paint() //Paint.ANTI_ALIAS_FLAG
        paintRead.color = Color.rgb(244, 81, 30) // orange

        // get screen width
        val displayMetrics = resources.displayMetrics
        sw = displayMetrics.widthPixels

        var i = 1
        var top = 20
        var bottom = 200
        //rect = RectF(i*(w+d), top, i*(w+d)+w, bottom)
    }


    fun updateAmps(amp: Int) {

        //var norm  = Math.min(amp/7, maxAmp) // 100*abs(Math.log10(1.0*amp/(sqrt(amp*1.0)+1)))
        var norm = amp
        for (i in spikes.indices) {
            var bottom: Float = (Math.random() * norm).toFloat()
            var top = delta - bottom

            var rectUp = RectF(i * (w + d) * 1f, top, i * (w + d) + w * 1f, bottom)
            spikes[i] = rectUp

        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        // this may be called several times on start or create
        // therefore we shouldn't initialize objects here

        spikes.forEach {
            Log.d("waveform", it.bottom.toString())
            canvas?.drawRoundRect(it, 10f, 10f, paintRead)
        }
        //Log.d("waveform", rect.bottom.toString())
        //canvas?.drawRect(rect, paintRead)
    }
}