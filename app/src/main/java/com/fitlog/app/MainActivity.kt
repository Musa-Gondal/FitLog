@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitlog.app.notify.AppState
import com.fitlog.app.notify.Notifications
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.LocalSnackbar
import com.fitlog.app.ui.screens.ActiveWorkoutScreen
import com.fitlog.app.ui.screens.AddFoodScreen
import com.fitlog.app.ui.screens.CustomFoodScreen
import com.fitlog.app.ui.screens.ExercisePickerScreen
import com.fitlog.app.ui.screens.FinishSummaryDialog
import com.fitlog.app.ui.screens.FoodScreen
import com.fitlog.app.ui.screens.NewBadgeDialog
import com.fitlog.app.ui.screens.ProfileForm
import com.fitlog.app.ui.screens.ProgressScreen
import com.fitlog.app.ui.screens.RoutineEditorScreen
import com.fitlog.app.ui.screens.SettingsScreen
import com.fitlog.app.ui.screens.TodayScreen
import com.fitlog.app.ui.screens.WorkoutDetailScreen
import com.fitlog.app.ui.screens.WorkoutScreen
import com.fitlog.app.ui.theme.FitLogTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { FitLogTheme { FitLogApp(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        i?.getStringExtra(Notifications.EXTRA_ROUTE)?.let {
            vm.pendingRoute.value = it
            i.removeExtra(Notifications.EXTRA_ROUTE)
        }
    }

    override fun onStart() {
        super.onStart()
        AppState.foreground = true
    }

    override fun onStop() {
        AppState.foreground = false
        super.onStop()
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Today", Icons.Filled.Home),
    Tab("food", "Food", Icons.Filled.Restaurant),
    Tab("workout", "Workout", Icons.Filled.FitnessCenter),
    Tab("progress", "Progress", Icons.AutoMirrored.Filled.ShowChart),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun FitLogApp(vm: AppViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()

    if (!profile.onboarded) {
        ProfileForm(initial = profile, firstRun = true, onBack = null) { vm.saveProfile(it.copy(onboarded = true)) }
        return
    }

    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val active by vm.active.collectAsStateWithLifecycle()
    val pending by vm.pendingRoute.collectAsStateWithLifecycle()
    val summary by vm.finishSummary.collectAsStateWithLifecycle()
    val newBadges by vm.newBadges.collectAsStateWithLifecycle()
    val checkInDue by vm.checkInDue.collectAsStateWithLifecycle()

    // Navigation requested by a notification tap.
    LaunchedEffect(pending) {
        val r = pending ?: return@LaunchedEffect
        vm.pendingRoute.value = null
        when (r) {
            "active" -> if (vm.active.value != null) nav.navigate("active") { launchSingleTop = true } else nav.goTab("workout")
            "coach" -> { vm.progressTab.value = 0; nav.goTab("progress") }
            "food", "workout", "today", "progress", "settings" -> nav.goTab(r)
        }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (tabs.any { it.route == route }) {
                    NavigationBar {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = route == t.route,
                                onClick = { nav.goTab(t.route) },
                                icon = {
                                    val dot = (t.route == "workout" && active != null) || (t.route == "progress" && checkInDue)
                                    if (dot) BadgedBox(badge = { Badge() }) { Icon(t.icon, contentDescription = t.label) }
                                    else Icon(t.icon, contentDescription = t.label)
                                },
                                label = { Text(t.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "today",
                modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
            ) {
                composable("today") { TodayScreen(vm, nav) }
                composable("food") { FoodScreen(vm, nav) }
                composable("workout") { WorkoutScreen(vm, nav) }
                composable("progress") { ProgressScreen(vm, nav) }
                composable("settings") { SettingsScreen(vm, nav) }

                composable("addFood/{meal}", arguments = listOf(navArgument("meal") { type = NavType.IntType })) {
                    AddFoodScreen(vm, nav, it.arguments?.getInt("meal") ?: 0)
                }
                composable("customFood") { CustomFoodScreen(vm, nav) }
                composable("active") { ActiveWorkoutScreen(vm, nav) }
                composable("pick/{target}") { PickerRoute(vm, nav, it.arguments?.getString("target") ?: "workout") }
                composable("routine") { RoutineEditorScreen(vm, nav) }
                composable("history/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    WorkoutDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0L)
                }
                composable("profile") {
                    ProfileForm(initial = profile, firstRun = false, onBack = { nav.popBackStack() }) {
                        vm.saveProfile(it)
                        nav.popBackStack()
                    }
                }
            }
        }
    }

    summary?.let { FinishSummaryDialog(it, profile.useLb) { vm.dismissSummary() } }
    if (summary == null && newBadges.isNotEmpty()) {
        NewBadgeDialog(newBadges) { vm.acknowledgeBadges() }
    }
}

@Composable
private fun PickerRoute(vm: AppViewModel, nav: NavHostController, target: String) {
    ExercisePickerScreen(vm, onBack = { nav.popBackStack() }) { names ->
        if (target == "routine") {
            val d = vm.routineDraft.value
            if (d != null) {
                val merged = (d.exerciseList() + names).distinct()
                vm.routineDraft.value = d.copy(exercises = merged.joinToString("\n"))
            }
        } else {
            vm.addExercises(names)
        }
        nav.popBackStack()
    }
}
