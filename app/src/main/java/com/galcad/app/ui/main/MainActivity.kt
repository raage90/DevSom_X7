package com.galcad.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.galcad.app.R
import com.galcad.app.network.ApiClient
import com.galcad.app.ui.contact.ContactFragment
import com.galcad.app.ui.folders.FolderBrowserFragment
import com.galcad.app.ui.news.NewsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_video -> FolderBrowserFragment.newInstance(mediaType = "video", categoryId = null, title = "Video")
                R.id.nav_audio -> FolderBrowserFragment.newInstance(mediaType = "audio", categoryId = null, title = "Audio")
                R.id.nav_news -> NewsFragment()
                R.id.nav_contact -> ContactFragment()
                else -> HomeFragment()
            }
            showFragment(fragment)
            true
        }

        applyDynamicMenuLabels(bottomNav)
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
                settings.menuVideoLabel?.let { bottomNav.menu.findItem(R.id.nav_video).title = it }
                settings.menuAudioLabel?.let { bottomNav.menu.findItem(R.id.nav_audio).title = it }
                settings.menuNewsLabel?.let { bottomNav.menu.findItem(R.id.nav_news).title = it }
                settings.menuContactLabel?.let { bottomNav.menu.findItem(R.id.nav_contact).title = it }
                settings.menuHomeLabel?.let { bottomNav.menu.findItem(R.id.nav_home).title = it }
            } catch (e: Exception) {
                // menu just keeps its default English labels if this fails -- never crash over it
            }
        }
    }
}
