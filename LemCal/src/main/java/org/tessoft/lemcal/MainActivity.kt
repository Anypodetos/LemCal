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

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class Adapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun createFragment(position: Int): Fragment = if (position == 0) TimeFragment() else DateFragment()
    override fun getItemCount() = 2
}

class MainActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2
    private lateinit var tabs: TabLayout

    var timeZone = 0
    var datePosition = 0
    var monthColors = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        setDayNight()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        pager = findViewById(R.id.pager)
        tabs = findViewById(R.id.tabs)
        pager.adapter = Adapter(this)
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.setText(if (position == 0) R.string.time else R.string.date)
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.aboutItem -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.settingsItem -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        pager.currentItem = preferences.getInt("page", 0)
        timeZone = preferences.getInt("timezone", 0)
        datePosition = preferences.getInt("datePosition", -1)
        monthColors = preferences.getString("monthColors", "")?.toIntOrNull() ?: 1
    }

    override fun onPause() {
        super.onPause()
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putInt("page", pager.currentItem)
        editor.putInt("timezone", timeZone)
        editor.putInt("datePosition", datePosition)
        editor.apply()
    }

    fun resolveColor(id: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(id, typedValue, true)
        return ContextCompat.getColor(applicationContext, typedValue.resourceId)
    }

    companion object {
        fun setDayNight(what: Char = 'S') {
            AppCompatDelegate.setDefaultNightMode(when (what) {
                'L'  -> AppCompatDelegate.MODE_NIGHT_NO
                'D'  -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
        }
    }
}
