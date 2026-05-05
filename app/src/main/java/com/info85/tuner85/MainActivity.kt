package com.info85.tuner85

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.info85.tuner85.databinding.ActivityMainBinding
import com.info85.tuner85.ui.adapters.RadioListAdapter
import com.info85.tuner85.ui.player.PlayerFragment
import com.info85.tuner85.ui.player.PlayerViewModel
import java.text.Normalizer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var drawerAdapter: RadioListAdapter
    private var fullDrawerList: List<com.info85.tuner85.data.model.RadioStation> = emptyList()

    private fun normalizeText(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").lowercase()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigationDrawer()
        setupFragment(savedInstanceState)
        setupBackNavigation()
        requestNotificationPermission()

        viewModel.bindService(this)
    }

    private fun setupNavigationDrawer() {
        drawerAdapter = RadioListAdapter { station, _ ->
            val index = fullDrawerList.indexOf(station).coerceAtLeast(0)
            viewModel.selectStation(station, index)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.rvDrawerRadios.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = drawerAdapter
        }

        viewModel.radioList.observe(this) { stations ->
            fullDrawerList = stations
            applyDrawerSearch(binding.etSearchRadio.text?.toString() ?: "")
        }

        viewModel.currentStation.observe(this) { station ->
            drawerAdapter.setCurrentStation(station)
        }

        binding.etSearchRadio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyDrawerSearch(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyDrawerSearch(query: String) {
        val filtered = if (query.isBlank()) {
            fullDrawerList
        } else {
            val q = normalizeText(query)
            fullDrawerList.filter { normalizeText(it.name).contains(q) }
        }
        drawerAdapter.submitList(filtered)
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun setupFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlayerFragment())
                .commit()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}
