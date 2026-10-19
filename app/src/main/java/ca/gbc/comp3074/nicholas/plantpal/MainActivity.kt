package ca.gbc.comp3074.nicholas.plantpal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ca.gbc.comp3074.nicholas.plantpal.databinding.ActivityMainBinding

/**
 * Single activity. The Navigation Component swaps fragments inside the NavHost;
 * My Plants, Weather Tips and Settings are top-level destinations in the bottom bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration
    private var bottomInset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        appBarConfig = AppBarConfiguration(
            setOf(R.id.plantsFragment, R.id.weatherFragment, R.id.settingsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfig)
        binding.bottomNav.setupWithNavController(navController)

        // Hide the bottom bar on the Add/Edit and Detail screens.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id in appBarConfig.topLevelDestinations
            applyBottomInset()
        }

        // Edge-to-edge: pad the app bar under the status bar, and the content above the
        // navigation bar / keyboard when the bottom bar is hidden.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.appBar.updatePadding(top = bars.top)
            bottomInset = maxOf(bars.bottom, ime.bottom)
            applyBottomInset()
            insets
        }
    }

    private fun applyBottomInset() {
        binding.navHostFragment.updatePadding(
            bottom = if (binding.bottomNav.isVisible) 0 else bottomInset
        )
    }

    /** Lets a screen switch bottom-bar tabs, e.g. the weather banner opening Weather Tips. */
    fun showTab(destinationId: Int) {
        binding.bottomNav.selectedItemId = destinationId
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
}
