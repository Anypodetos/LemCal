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

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.AttributeSet
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

const val UNIX_DAY_START   = -140_264 /* must be changed along with SOLAR_YEAR_START */
const val SOLAR_YEAR_START =  3049
const val SOLAR_YEAR_RANGE =  1200 /* lunar year gets inexact at about solar year 4263 */

const val MARK_TODAY = 3f
val BACK_COLORS = arrayOf(0, 0x60003cff, 0x600095ff, 0x6000d0ff, 0x6000ffbb, 0x6000ff22, 0x60b7ff00, 0x60ffdd00,
                             0x60ffae00, 0x60ff7700, 0x60ff4000, 0x60ff005e, 0x60e100ff, 0x60b700ff, 0x606f00ff)

@SuppressLint("ClickableViewAccessibility")
class WeekView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    var unixDay = 0
    var today = 0
    var solarWeek = 0
    var solarYear = 0
    var lunarYear = 0
    val lunarMonths = arrayOf(0, 0)
    val lunarBackLimits = arrayOf(1, 1, 1)
    val gregorianMonths = arrayOf(0, 0)
    val gregorianBackLimits = arrayOf(1, 1, 1)
    val cellTexts = Array(8) { arrayOf("", "") }

    private val mainActivity = if (context is MainActivity) context else null
    private val daysOfWeek = resources.getStringArray(R.array.days_of_week)
    private var colWidth = 0f
    private var rowHeight = 0f
    private var textX = arrayOf(0f, 0f)
    private var textY = arrayOf(0f, 0f)
    private var isWeekdays = false

    private val textPaints = Array(2) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (if (it == 0) 14 else 11) * resources.displayMetrics.scaledDensity
            textAlign = if (it == 0) Paint.Align.CENTER else Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, if (it == 0) Typeface.BOLD else Typeface.NORMAL)
            color = mainActivity?.resolveColor(R.attr.editTextColor) ?: 0
        }
    }
    private val backPaint = Paint(0).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(0).apply {
        color = textPaints[0].color
    }
    private val todayPaint = Paint(0).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * MARK_TODAY
        color = textPaints[0].color
    }

    init {
        setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP && mainActivity != null) {
                val cell = event.x.toInt() / colWidth.toInt()
                if (isWeekdays) {
                    if (cell > 0) {
                        playSoundEffect(SoundEffectConstants.CLICK)
                        Toast.makeText(mainActivity, cellTexts[cell][0] + " – " + daysOfWeek[cell], Toast.LENGTH_SHORT).show()
                        true
                    } else false
                } else {
                    val solarMessage = when (lunarBackLimits[2]) {
                        8    -> cellTexts[0][0] + " " + daysOfWeek[cell]
                        2    -> if (cell in 0..1) daysOfWeek[10] else ""
                        else -> if (cell in 0..2) daysOfWeek[cell + 8] else ""
                    }
                    if (solarMessage.isNotBlank()) {
                        playSoundEffect(SoundEffectConstants.CLICK)
                        val title = mainActivity.getString(if (cell == 0) R.string.week_title else R.string.day_title)
                        val lunarMessage = if (cell > 0)
                            "\n◆ " + resources.getString(R.string.lunar_template, lunarYear(cell), lunarMonths[if (cell < lunarBackLimits[1]) 0 else 1],
                                cellTexts[cell][0].substringAfter('–')) else ""
                        val gregorianMessage = if (cell > 0)
                            "\n◆ " + DateUtils.formatDateTime(context, (unixDay + cell - 1) * 86_400_000.toLong(),
                                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR) else ""
                        val unixMessage = if (cell > 0)
                            "\n◆ " + resources.getString(R.string.unix_template, DecimalFormat("#,###,##0").format(unixDay + cell - 1))
                                .replace('-', '−') else ""
                        val message = "◆ $solarMessage $solarYear$lunarMessage$gregorianMessage$unixMessage"
                        AlertDialog.Builder(mainActivity)
                            .setTitle(title)
                            .setMessage(message)
                            .setPositiveButton(R.string.copy_my) { _, _ ->
                                mainActivity.clipboard?.setPrimaryClip(ClipData.newPlainText(null, title + "\n\n" + message))
                                Toast.makeText(mainActivity.applicationContext, mainActivity.getString(R.string.clipboard_ok), Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton(R.string.close) { _, _ -> }
                            .create().show()
                        true
                    } else false
                }
            } else false
        }
    }

    fun makeWeekdays() {
        isWeekdays = true
        textPaints[0].textSize *= 2
        textPaints[0].textAlign = Paint.Align.CENTER
        for (i in 0..7) cellTexts[i][0] = " ⛢♄♂☿♃♀♁"[i].toString()
    }

    fun lunarYear(cell: Int) = lunarYear + if (cell >= lunarBackLimits[1] && lunarMonths[1] == 1) 1 else 0

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        colWidth = w / 8f
        rowHeight = h.toFloat()
        textX[0] = colWidth / 2f - MARK_TODAY
        textY[0] = rowHeight / 2f + textPaints[0].textSize / 3f + MARK_TODAY
        textX[1] = 0.95f * colWidth - MARK_TODAY
        textY[1] = textPaints[1].textSize + MARK_TODAY
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            when (mainActivity?.monthColors) {
                MonthColors.LUNAR -> for (i in 0..1) if (lunarBackLimits[i] < lunarBackLimits[i + 1])
                    drawBackground(it, BACK_COLORS[lunarMonths[i]], lunarBackLimits[i], lunarBackLimits[i + 1])
                MonthColors.GREGORIAN -> for (i in 0..1) if (gregorianBackLimits[i] < gregorianBackLimits[i + 1])
                    drawBackground(it, BACK_COLORS[gregorianMonths[i] + when (gregorianMonths[i]) {
                        in 0..5 -> 0
                        in 6..11 -> 1
                        else -> 2
                    }], gregorianBackLimits[i], gregorianBackLimits[i + 1])
                MonthColors.SEASONS -> drawBackground(it, BACK_COLORS[(solarWeek.coerceAtMost(52) - 1) / 13 * 4 + 1])
                MonthColors.GREEN -> drawBackground(it, 0x40009958)
                else -> { }
            }
            it.drawLine(0f, rowHeight - 1, 8 * colWidth, rowHeight - 1, linePaint)
            for (i in 0..7) {
                val yearStart = cellTexts[i][0].length !in 1..4
                for (j in 0..1) it.drawText(cellTexts[i][j], i * colWidth + textX[j], textY[j], textPaints[j])
                if (!yearStart) it.drawLine((i + 1) * colWidth, 0f, (i + 1) * colWidth, rowHeight, linePaint)
            }
            if (today > 0) it.drawRect(today * colWidth + MARK_TODAY,  MARK_TODAY, (today + 1) * colWidth - MARK_TODAY,
                rowHeight - MARK_TODAY, todayPaint)
        }
    }

    private fun drawBackground(canvas: Canvas, color: Int, left: Int = lunarBackLimits[0], right: Int = lunarBackLimits[2]) {
        backPaint.color = color
        canvas.drawRect(left * colWidth, 0f, right * colWidth, rowHeight, backPaint)
    }
}

class RecyclerAdapter internal constructor(private val mainActivity: MainActivity) :
        RecyclerView.Adapter<RecyclerAdapter.WeekViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(mainActivity)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder =
        WeekViewHolder(inflater.inflate(R.layout.fragment_week, parent, false))

    override fun getItemCount() = 53 * SOLAR_YEAR_RANGE

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.week.unixDay = 7 * position - 6 * (position / 53) + position / 212 - (position + 5512) / 6784 + UNIX_DAY_START

        /*  s o l a r   d a t e  */
        holder.week.solarYear = position / 53 + SOLAR_YEAR_START
        holder.week.solarWeek = position % 53 + 1
        val dayCount = when {
            holder.week.solarWeek < 53 -> 7
            holder.week.solarYear % 4 == 0 && holder.week.solarYear % 128 != 0 -> 2
            else -> 1
        }
        holder.week.today = with (Calendar.getInstance().timeInMillis / 86_400_000 - (holder.week.unixDay - 1).toLong()) {
            if (this in 1..dayCount) this.toInt() else 0
        }
        holder.week.cellTexts[0][0] = when (holder.week.solarWeek) {
            1, 21, 31, 41, 51 -> "${holder.week.solarWeek}st"
            2, 22, 32, 42, 52 -> "${holder.week.solarWeek}nd"
            3, 23, 33, 43     -> "${holder.week.solarWeek}rd"
            53                -> if (dayCount == 1) "♆" else "ℓ/♆"
            else              -> "${holder.week.solarWeek}th"
        }

        /*  l u n a r   d a t e  */
        val ld1 = holder.week.unixDay + 2_119_763
        val ld2 = ld1 % 19756
        var lunarDay = if (ld2 <= 6585) ld2 else (ld2 - 1) % 6585
        var ly = 48 * (ld1 / 19756) + 16 * ((ld2 - 1) / 6585).coerceAtLeast(0)
        while (lunarDay >= 413 || (lunarDay >= 383 && ly % 16 == 0)) {
            lunarDay -= if (ly % 16 == 0) (if (ly % 3 == 0) 384 else 383) else (if (ly % 2 == 0) 414 else 413)
            ly++
        }
        if (lunarDay == -1) {
            lunarDay += if ((ly - 1) % 16 == 0) 384 else 414
            ly--
        }
        var lunarMonth = 1
        while (lunarDay >= 29) {
            lunarDay -= if (lunarMonth % 2 == 0 || (ly % 2 == 0 && (ly % 16 != 0 || ly % 3 == 0) && lunarMonth == 3)) 30 else 29
            lunarMonth++
        }
        if (lunarDay == -1) {
            lunarDay += 30
            lunarMonth--
        }
        holder.week.lunarYear = ly - 3072

        for (i in 0..1) {
            holder.week.lunarMonths[i] = (lunarMonth + i - 1) % (if (holder.week.lunarYear % 16 == 0) 13 else 14) + 1
            holder.week.lunarBackLimits[i + 1] = dayCount + 1
        }
        val lunarMonthLength = if (lunarMonth % 2 == 0 ||
            ((holder.week.lunarYear % 2 == 0 && holder.week.lunarYear % 16 != 0 || holder.week.lunarYear % 48 == 0) && lunarMonth == 3))
                30 else 29
        for (i in 1..dayCount) with((lunarDay + i - 1) % lunarMonthLength + 1) {
            holder.week.cellTexts[i][0] = (if (this == 1) {
                if (i > 1) holder.week.lunarBackLimits[1] = i
                "${holder.week.lunarMonths[if (i > 1) 1 else 0]}–"
            } else "") + toString()
        }

        /*  G r e g o r i a n   d a t e  */
        for (i in 0..1) {
            val unixStamp = holder.week.unixDay * 86_400_000.toLong()
            holder.week.gregorianMonths[i] = ((SimpleDateFormat("M").format(unixStamp).toIntOrNull() ?: 0) + i - 1) % 12 + 1
            holder.week.gregorianBackLimits[i + 1] = dayCount + 1
        }
        for (i in 1..dayCount) {
            val unixStamp = (holder.week.unixDay + i - 1) * 86_400_000.toLong()
            val gregorianDay = SimpleDateFormat("d").format(unixStamp)
            holder.week.cellTexts[i][1] = (if (gregorianDay == "1") SimpleDateFormat("MMM ").format(unixStamp) else "") + gregorianDay
            if (i > 1 && gregorianDay == "1") holder.week.gregorianBackLimits[1] = i
        }

        for (i in dayCount + 1..7) for (j in 0..1) holder.week.cellTexts[i][j] = ""

        /*  s o l a r   y e a r   s t a r t  */
        if (holder.week.solarWeek == 53 && holder.week.solarYear + 1 < SOLAR_YEAR_START + SOLAR_YEAR_RANGE)
            holder.week.cellTexts[3 + dayCount][0] = mainActivity.getString(R.string.solar_year_start, holder.week.solarYear + 1)
    }

    inner class WeekViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val week: WeekView = itemView.findViewById(R.id.week)

        init {
            if (mainActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                week.layoutParams.height = (40 * mainActivity.resources.displayMetrics.scaledDensity).toInt()
        }
    }
}

class DateFragment : Fragment() {

    private lateinit var solarYear: TextView
    private lateinit var lunarYear: TextView
    private lateinit var gregorianYear: TextView
    private lateinit var todayButton: ImageButton
    private lateinit var recycler: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_date, container, false)
        root.findViewById<WeekView>(R.id.weekdays).makeWeekdays()

        solarYear = root.findViewById(R.id.solarYear)
        lunarYear = root.findViewById(R.id.lunarYear)
        gregorianYear = root.findViewById(R.id.gregorianYear)
        todayButton = root.findViewById(R.id.todayButton)
        recycler = root.findViewById(R.id.recycler)

        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = RecyclerAdapter(activity as MainActivity)
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recycler.layoutManager is LinearLayoutManager) {
                    val first = (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    val last  = (recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val firstSolar = (first / 53 + SOLAR_YEAR_START).toString()
                    val lastSolar  = (last  / 53 + SOLAR_YEAR_START).toString()
                    solarYear.text = if (firstSolar == lastSolar) getString(R.string.solar_year, firstSolar) else
                        getString(R.string.solar_years, firstSolar, lastSolar.removePrefix(lastSolar.commonPrefixWith(firstSolar)))
                    recycler.findViewHolderForAdapterPosition(first)?.let { firstHolder ->
                        recycler.findViewHolderForAdapterPosition(last)?.let { lastHolder ->
                            val firstLunar = (if (firstHolder is RecyclerAdapter.WeekViewHolder) firstHolder.week.lunarYear    else 0).toString()
                            val lastLunar  = (if (lastHolder  is RecyclerAdapter.WeekViewHolder) lastHolder .week.lunarYear(7) else 0).toString()
                            lunarYear.text = if (firstLunar == lastLunar) getString(R.string.lunar_year, firstLunar) else
                                getString(R.string.lunar_years, firstLunar, lastLunar.removePrefix(lastLunar.commonPrefixWith(firstLunar)))
                        }
                    }
                    val firstGregorian = ((first - 2) / 53 - 1463 + SOLAR_YEAR_START).toString()
                    val lastGregorian  = ((last  - 1) / 53 - 1463 + SOLAR_YEAR_START).toString()
                    gregorianYear.text = if (firstGregorian == lastGregorian) getString(R.string.gregorian_year, firstGregorian) else
                        getString(R.string.gregorian_years, firstGregorian, lastGregorian.removePrefix(lastGregorian.commonPrefixWith(firstGregorian)))
                    (activity as MainActivity).datePosition = first
                }
            }
        })

        solarYear.setOnClickListener {
            gotoYearDialog(R.string.go_solar_year) {
                (it - SOLAR_YEAR_START) * 53 - 1
            }
        }
        lunarYear.setOnClickListener {
            gotoYearDialog(R.string.go_lunar_year) {
                ((it - 2078) * 59.7245 - UNIX_DAY_START / 6.89136).roundToInt() - 20
            }
        }
        gregorianYear.setOnClickListener {
            gotoYearDialog(R.string.go_gregorian_year) {
                (it + 1463 - SOLAR_YEAR_START) * 53 + 1
            }
        }

        todayButton.setOnClickListener {
            val (thisPos, size) = (recycler.layoutManager as LinearLayoutManager).let {
                Pair((it.findLastVisibleItemPosition() + it.findFirstVisibleItemPosition()) / 2,
                      it.findLastVisibleItemPosition() - it.findFirstVisibleItemPosition())
            }
            with (Calendar.getInstance()) {
                val todayPos = (get(Calendar.YEAR) + 1463 - SOLAR_YEAR_START) * 53 + get(Calendar.WEEK_OF_YEAR)
                scrollTo(todayPos + if (todayPos < thisPos) - size / 2 else size / 2)
            }
        }

        with((activity as MainActivity).datePosition) {
            if (this == -1) todayButton.performClick() else recycler.scrollToPosition(this)
        }
        return root
    }

    private fun gotoYearDialog(titleRes: Int, calcPos: (Int) -> Int) {
        val edit = EditText(activity)
        edit.inputType = InputType.TYPE_CLASS_NUMBER

        activity?.let {
            val dialog = AlertDialog.Builder(it)
                .setTitle(titleRes)
                .setView(edit)
                .setPositiveButton(android.R.string.ok) { _, _ -> gotoYearDialogOk(edit, calcPos) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .create()

            edit.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    gotoYearDialogOk(edit, calcPos)
                    dialog.cancel()
                    true
                } else false
            }
            edit.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            dialog.show()
        }
    }
    private fun gotoYearDialogOk(edit: EditText, calcPos: (Int) -> Int) {
        edit.text.toString().toIntOrNull()?.let {
            scrollTo(calcPos(it).coerceIn(0, (recycler.adapter?.itemCount ?: 1) - 1))
        }
    }

    private fun scrollTo(position: Int) {
      //  val distance = position - (activity as MainActivity).datePosition
      //  if (abs(distance) > 10) recycler.scrollToPosition(position - 10 * distance.sign)
        recycler.scrollToPosition(position)
    }
}
