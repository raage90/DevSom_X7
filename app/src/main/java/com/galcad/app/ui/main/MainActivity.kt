package com.galcad.app.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.galcad.app.R
import com.galcad.app.network.ApiClient
import com.galcad.app.ui.contact.ContactFragment
import com.galcad.app.ui.folders.FolderBrowserFragment
import com.galcad.app.ui.news.NewsFragment
import com.galcad.app.ui.video.AllVideosFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bottom nav is 4 main sections (Home, Video, News, Audio) -- Contact Us
 * moved into the toolbar's overflow menu instead, YouTube-style: primary
 * destinations get tab icons, secondary ones live under a menu.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var currentLabels: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        bottomNav = findViewById(R.id.bottomNav)

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_video -> AllVideosFragment()
                R.id.nav_audio -> FolderBrowserFragment.newInstance(mediaType = "audio", categoryId = null, title = currentLabels["audio"] ?: "Audio")
                R.id.nav_news -> NewsFragment()
                else -> HomeFragment()
            }
            supportActionBar?.title = item.title
            showFragment(fragment)
            true
        }

        applyDynamicMenuLabels(bottomNav)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_contact) {
            supportActionBar?.title = currentLabels["contact"] ?: "Contact Us"
            bottomNav.menu.setGroupCheckable(0, false, true) // deselect bottom tabs visually
            for (i in 0 until bottomNav.menu.size()) bottomNav.menu.getItem(i).isChecked = false
            bottomNav.menu.setGroupCheckable(0, true, true)
            showFragment(ContactFragment())
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFragment(fragment: Fragment) {
        // Each bottom-tab press replaces the whole stack for that tab area --
        // simple, predictable navigation matching most feed apps.
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun applyDynamicMenuLabels(bottomNav: BottomNavigationView) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = ApiClient.get().getSettings()
                val settings = response.body() ?: return@launch
                val labels = mutableMapOf<String, String>()
                settings.menuVideoLabel?.let { bottomNav.menu.findItem(R.id.nav_video).title = it; labels["video"] = it }
                settings.menuAudioLabel?.let { bottomNav.menu.findItem(R.id.nav_audio).title = it; labels["audio"] = it }
                settings.menuNewsLabel?.let { bottomNav.menu.findItem(R.id.nav_news).title = it; labels["news"] = it }
                settings.menuContactLabel?.let { labels["contact"] = it }
                settings.menuHomeLabel?.let { bottomNav.menu.findItem(R.id.nav_home).title = it; labels["home"] = it }
                currentLabels = labels
            } catch (e: Exception) {
                // menu just keeps its default English labels if this fails -- never crash over it
            }
        }
    }
}
