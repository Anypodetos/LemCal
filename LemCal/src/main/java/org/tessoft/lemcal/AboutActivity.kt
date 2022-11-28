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

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(findViewById(R.id.aboutToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<WebView>(R.id.webView).loadData(
            getString(R.string.css) +
            (if (isNightModeActive()) getString(R.string.css_dark) else "") +
            "<h1>" + getString(R.string.about_title, packageManager?.getPackageInfo(packageName ?: "", 0)?.versionName ?: "…") + "</h1>" +
            getString(R.string.about_text),
            "text/html", "UTF-8")
    }

    private fun isNightModeActive() = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> false
        AppCompatDelegate.MODE_NIGHT_YES -> true
        else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
