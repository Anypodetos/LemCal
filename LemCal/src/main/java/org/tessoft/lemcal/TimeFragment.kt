package org.tessoft.lemcal

/*
Copyright 2022 Anypodetos (Michael Weber)

This file is part of LemCal.

LemCal is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

LemCal is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LemCal. If not, see <https://www.gnu.org/licenses/>.

Contact: <https://lemizh.conlang.org/home/contact.php>
*/

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.*

class TimeView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mainActivity = if (context is MainActivity) context else null
    private var landscape = false
    private var centre1 = 0f
    private var centre2 = 0f
    private var radius = 0f
    private var lemHourMarks = Array(16) { Pair(0f, 0f) }
    private var ourHourMarks = Array(12) { Pair(0f, 0f) }
    private var lemHands = Array(3) { Pair(0f, 0f) }
    private var lemBackHands = Array(3) { Pair(0f, 0f) }
    private var ourHands = Array(3) { Pair(0f, 0f) }
    private var ourBackHands = Array(3) { Pair(0f, 0f) }
    private var lemTimeText = ""
    private var ourTimeText = ""
    private var textX = 0f
    private var textY = 0f

    private val textColor  = mainActivity?.resolveColor(R.attr.editTextColor) ?: 0
    private val clockColor = mainActivity?.resolveColor(R.attr.colorPrimaryVariant) ?: 0

    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = clockColor
    }

    private val markPaint = Array(2) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (if (it == 0) 18 else 24) * resources.displayMetrics.scaledDensity
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, if (it == 0) Typeface.NORMAL else Typeface.BOLD)
            color = clockColor
        }
    }

    private val handPaint = Array(3) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (4 - it) * 5f
            strokeCap = Paint.Cap.ROUND
            color = textColor
        }
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36 * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        color = textColor
    }

    init {
        timer(period = 1318) {
            calcLemClock()
            invalidate()
        }
        timer(period = 1000) {
            calcOurClock()
            invalidate()
        }
        setOnClickListener {
            if (mainActivity != null) {
                val lemZone = mainActivity.timeZone * 90 /* in minutes */
                val ourZone = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
                AlertDialog.Builder(mainActivity)
                    .setTitle(R.string.timezone_title)
                    .setMessage(HtmlCompat.fromHtml(resources.getString(R.string.timezone_text, lemZone, ourZone, lemZone - ourZone + 113)
                        .replace("+-", "−"), HtmlCompat.FROM_HTML_MODE_LEGACY))
                    .setNegativeButton(R.string.close) { _, _ -> }
                    .create().show()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        centre1 = min(w, h) / 2f
        centre2 = w - centre1
        radius = // min(centre1 * 0.8f, max(w, h) / 6f) //
                 //if (landscape) w / 6f else (centre1 * 0.8f).coerceAtMost(10 * markPaint[0].textSize)
                (centre1 * 0.8f).coerceAtMost(if (landscape) w / 6f else 10 * markPaint[0].textSize)
        textX = if (landscape) w / 2f else centre1
        textY = if (landscape) centre1 else 2.1f * centre1
        calcLemClock()
        calcOurClock()
    }

    private fun calcLemClock() {
        val time = (Calendar.getInstance().let {
            it.get(Calendar.HOUR_OF_DAY) / 24f + it.get(Calendar.MINUTE) / 1440f + it.get(Calendar.SECOND) / 86_400f +
                (it.get(Calendar.MILLISECOND) - TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86_400_000f +
                (mainActivity?.timeZone ?: 0) / 16f + 1f + /* avoid negative result */
                0.0785f /* Δ (UTC, LUT) */
        } * 0x10000).roundToInt() % 0x10000

        val marksRadius = radius - markPaint[0].textSize
        for (i in 0..15) lemHourMarks[i] = Pair(
            centre1 - sin(i / 8f * Math.PI.toFloat()) * marksRadius,
            centre1 + cos(i / 8f * Math.PI.toFloat()) * marksRadius + markPaint[if (i % 4 != 0) 0 else 1].textSize / 2.5f
        )

        var timeFloat = time.toFloat() / 0x10000 * 2 * Math.PI.toFloat()
        for (i in 0..2) {
            lemHands[i] = Pair(
                centre1 - sin(timeFloat) * radius * (i + 4) * 0.13f,
                centre1 + cos(timeFloat) * radius * (i + 4) * 0.13f)
            lemBackHands[i] = Pair(
                centre1 + sin(timeFloat) * radius * (i + 4) * 0.02f,
                centre1 - cos(timeFloat) * radius * (i + 4) * 0.02f)
            timeFloat *= 16
        }

        lemTimeText = time.toString(16).uppercase().padStart(4, '0')
    }

    private fun calcOurClock() {
        val (hours, minutes, seconds) = Calendar.getInstance().let {
            Triple(it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE), it.get(Calendar.SECOND))
        }

        if (landscape) {
            val marksRadius = radius - markPaint[0].textSize
            for (i in 1..12) ourHourMarks[i - 1] = Pair(
                centre2 + sin(i / 6f * Math.PI.toFloat()) * marksRadius,
                centre1 - cos(i / 6f * Math.PI.toFloat()) * marksRadius + markPaint[if (i % 3 != 0) 0 else 1].textSize / 2.5f
            )

            var timeFloat = (hours / 24f + minutes / 1440f + seconds / 86_400f) * 4 * Math.PI.toFloat()
            for (i in 0..2) {
                ourHands[i] = Pair(
                    centre2 + sin(timeFloat) * radius * (i + 4) * 0.13f,
                    centre1 - cos(timeFloat) * radius * (i + 4) * 0.13f)
                ourBackHands[i] = Pair(
                    centre2 - sin(timeFloat) * radius * (i + 4) * 0.02f,
                    centre1 + cos(timeFloat) * radius * (i + 4) * 0.02f)
                timeFloat *= if (i == 0) 12 else 60
            }
        }
        else ourTimeText = hours.toString().padStart(2, '0') + ":" + minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            it.drawCircle(centre1, centre1, radius, clockPaint)
            for (i in 0..15) it.drawText(i.toString(16).uppercase(), lemHourMarks[i].first, lemHourMarks[i].second,
                markPaint[if (i % 4 != 0) 0 else 1])
            for (i in 0..2) it.drawLine(lemBackHands[i].first, lemBackHands[i].second, lemHands[i].first, lemHands[i].second, handPaint[i])
            if (landscape) {
                it.drawCircle(centre2, centre1, radius, clockPaint)
                for (i in 1..12) it.drawText(i.toString(), ourHourMarks[i - 1].first, ourHourMarks[i - 1].second,
                    markPaint[if (i % 3 != 0) 0 else 1])
                for (i in 0..2) it.drawLine(ourBackHands[i].first, ourBackHands[i].second, ourHands[i].first, ourHands[i].second, handPaint[i])
            }
            it.drawText(lemTimeText + if (!landscape) "•$ourTimeText" else "", textX, textY, textPaint)
        }
    }
}

class TimeFragment : Fragment() {

    private lateinit var timeView: TimeView
    private lateinit var zoneButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_time, container, false)
        timeView = root.findViewById(R.id.timeView)
        zoneButton = root.findViewById(R.id.zoneButton)
        zoneButton.text = timezoneString((activity as MainActivity).timeZone)
        zoneButton.setOnClickListener { button ->
            val popupMenu = PopupMenu(activity, button)
            for (i in 7 downTo -8) popupMenu.menu.add(1, i, 7 - i, timezoneString(i)).isChecked = i == (activity as MainActivity).timeZone
            popupMenu.menu.setGroupCheckable(1, true, true)
            popupMenu.setOnMenuItemClickListener { item ->
                (activity as MainActivity).timeZone = item.itemId
                zoneButton.text = item.title
                timeView.invalidate()
                true
            }
            popupMenu.show()
        }
        return root
    }

    private fun timezoneString(i: Int) = "LUT" + when (i) {
        in -8..-1 -> "−${-i}"
        0         -> ""
        else      -> "+$i"
    }
}
