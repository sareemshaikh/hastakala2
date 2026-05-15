package com.hastakala.testshop.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.hastakala.testshop.R
import com.hastakala.testshop.databinding.ActivityMainBinding
import com.hastakala.testshop.ui.auth.LoginActivity
import com.hastakala.testshop.util.AlertHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auth guard (Req 1.4): redirect to login if not authenticated
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Wire BottomNavigationView to NavController (Req 7.5)
        binding.bottomNav.setupWithNavController(navController)

        // Handle deep link from low-stock notification — navigate to Inventory tab
        if (intent?.getStringExtra(AlertHelper.EXTRA_NAVIGATE_TO) == AlertHelper.DEST_INVENTORY) {
            navController.navigate(R.id.inventoryFragment)
        }
    }
}
