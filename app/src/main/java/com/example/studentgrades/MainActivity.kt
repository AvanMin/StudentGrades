package com.example.studentgrades

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.studentgrades.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Автоматический отступ от верхней шторки (статус-бара) и нижней панели жестов
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // Переопределяем клик по нижнему меню: вкладка «Оценки» всегда возвращает на главный экран
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.homeFragment, inclusive = false, saveState = false)
                    .build()
                navController.navigate(R.id.homeFragment, null, options)
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }

        // Если повторно нажать на уже выбранную вкладку «Оценки» (находясь в фильтрах или предмете) — тоже вернуть на главный экран
        binding.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                navController.popBackStack(R.id.homeFragment, inclusive = false)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomBar = destination.id != R.id.addEditGradeFragment
            binding.bottomNav.visibility = if (showBottomBar) View.VISIBLE else View.GONE
            binding.bottomNavDivider.visibility = if (showBottomBar) View.VISIBLE else View.GONE
        }
    }
}