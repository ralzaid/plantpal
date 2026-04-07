package com.example.plantpal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plantpal.data.local.NotificationHelper
import com.example.plantpal.data.workers.scheduleReminderWorker
import com.example.plantpal.ui.state.PlantViewModel
import com.example.plantpal.ui.theme.PlantPalTheme

sealed class Screen {
    data object Login : Screen()
    data object SignUp : Screen()
    data object Home : Screen()
    data object AddPlant : Screen()
    data class PlantDetail(val plantId: Int) : Screen()
    data object Profile : Screen()
}

@Composable
fun PlantPalApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf(previewProfile) }

    val plantViewModel: PlantViewModel = viewModel()
    val dbPlants by plantViewModel.plants.collectAsState()
    val currentUserId by plantViewModel.currentUserId.collectAsState()
    val hasSavedHomeLocation by plantViewModel.hasSavedHomeLocation.collectAsState()

    val context = LocalContext.current

    var showLocationConsentDialog by rememberSaveable { mutableStateOf(false) }
    var locationConsentChecked by rememberSaveable { mutableStateOf(false) }
    var locationConsentHandledForSession by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            plantViewModel.captureAndSaveUserLocationIfMissing()
        }
        showLocationConsentDialog = false
        locationConsentHandledForSession = true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(currentUserId, hasSavedHomeLocation) {
        NotificationHelper.createChannel(context)

        currentUserId?.let { userId ->
            if (!hasSavedHomeLocation && !locationConsentHandledForSession) {
                showLocationConsentDialog = true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationsGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                if (!notificationsGranted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            scheduleReminderWorker(context, userId)
        }
    }

    var currentScreen by remember {
        mutableStateOf<Screen>(if (isLoggedIn) Screen.Home else Screen.Login)
    }

    val plants = dbPlants.map {
        UiPlant(
            id = it.id,
            name = it.name,
            nickname = "",
            species = it.species,
            location = it.plantType,
            lightNeeds = "",
            wateringFrequencyDays = it.wateringFrequencyDays,
            careInstructions = it.careInstructions,
            lastWateredDate = it.lastWateredDate,
            imageUrl = null
        )
    }

    if (showLocationConsentDialog && !hasSavedHomeLocation) {
        LocationConsentDialog(
            checked = locationConsentChecked,
            onCheckedChange = { locationConsentChecked = it },
            onAgree = {
                if (!locationConsentChecked) return@LocationConsentDialog

                val fineLocationGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                val coarseLocationGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                if (fineLocationGranted || coarseLocationGranted) {
                    plantViewModel.captureAndSaveUserLocationIfMissing()
                    showLocationConsentDialog = false
                    locationConsentHandledForSession = true
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onDismiss = { }
        )
    }

    if (!isLoggedIn) {
        when (currentScreen) {
            Screen.SignUp -> SignUpScreen(
                onSignedUp = { name, email ->
                    profile = profile.copy(name = name, email = email)
                    isLoggedIn = true
                    currentScreen = Screen.Home
                },
                onBackToLogin = { currentScreen = Screen.Login }
            )

            else -> LoginScreen(
                onLoggedIn = { email, password ->
                    plantViewModel.loginOrCreateLocalUser(email, password)
                    isLoggedIn = true
                    currentScreen = Screen.Home
                    locationConsentHandledForSession = false
                    locationConsentChecked = false
                },
                onGoToSignUp = { currentScreen = Screen.SignUp }
            )
        }
        return
    }

    PlantPalScaffold(
        currentScreen = currentScreen,
        onNavigateHome = { currentScreen = Screen.Home },
        onNavigateAdd = { currentScreen = Screen.AddPlant },
        onNavigateProfile = { currentScreen = Screen.Profile },
        onLogout = {
            plantViewModel.logout()
            isLoggedIn = false
            currentScreen = Screen.Login
            showLocationConsentDialog = false
            locationConsentChecked = false
            locationConsentHandledForSession = false
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val screen = currentScreen) {
                Screen.Home -> HomeDashboardScreen(
                    profile = profile,
                    plants = plants,
                    onAddPlant = { currentScreen = Screen.AddPlant },
                    onPlantClick = { plantId -> currentScreen = Screen.PlantDetail(plantId) },
                    onProfileClick = { currentScreen = Screen.Profile }
                )

                Screen.AddPlant -> AddPlantScreen(
                    onSave = { name, species, plantType, wateringDays, careInstructions ->
                        plantViewModel.addPlant(
                            name = name,
                            species = species,
                            plantType = plantType,
                            wateringFrequencyDays = wateringDays,
                            careInstructions = careInstructions
                        )
                        currentScreen = Screen.Home
                    }
                )

                is Screen.PlantDetail -> {
                    val selectedPlant = dbPlants.firstOrNull { it.id == screen.plantId }

                    PlantDetailScreen(
                        plant = plants.firstOrNull { it.id == screen.plantId },
                        onDelete = {
                            selectedPlant?.let { plantViewModel.deletePlant(it) }
                            currentScreen = Screen.Home
                        }
                    )
                }

                Screen.Profile -> ProfileScreen(
                    profile = profile,
                    onSave = { name, email, remindersEnabled ->
                        profile = profile.copy(
                            name = name,
                            email = email,
                            remindersEnabled = remindersEnabled
                        )
                    },
                    onLogout = {
                        plantViewModel.logout()
                        isLoggedIn = false
                        currentScreen = Screen.Login
                        showLocationConsentDialog = false
                        locationConsentChecked = false
                        locationConsentHandledForSession = false
                    }
                )

                Screen.Login, Screen.SignUp -> Unit
            }
        }
    }
}

@Composable
private fun LocationConsentDialog(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onAgree: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Location consent")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "PlantPal uses your location once to save your home city and provide personalized plant care reminders based on the weather conditions there."
                )
                Text(
                    "This saved city will continue to be used for reminders even if you travel somewhere else."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange
                    )
                    Text(
                        text = "I agree to let PlantPal use my location for personalized weather-based care reminders.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                enabled = checked
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = false
            ) {
                Text("Required")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
private fun screenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Login -> "Welcome"
        Screen.SignUp -> "Create Account"
        Screen.Home -> "PlantPal"
        Screen.AddPlant -> "Add Plant"
        is Screen.PlantDetail -> "Plant Details"
        Screen.Profile -> "Profile"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantPalScaffold(
    currentScreen: Screen,
    onNavigateHome: () -> Unit,
    onNavigateAdd: () -> Unit,
    onNavigateProfile: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val showBottomBar = currentScreen == Screen.Home || currentScreen == Screen.Profile

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(currentScreen)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    if (currentScreen == Screen.AddPlant || currentScreen is Screen.PlantDetail) {
                        IconButton(onClick = onNavigateHome) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.Profile) {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                Box {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Home,
                            onClick = onNavigateHome,
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f, fill = true))

                        NavigationBarItem(
                            selected = currentScreen == Screen.Profile,
                            onClick = onNavigateProfile,
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    FloatingActionButton(
                        onClick = onNavigateAdd,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .size(64.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add plant")
                    }
                }
            }
        },
        content = content
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
fun PlantPalAppPreview() {
    PlantPalTheme {
        PlantPalScaffold(
            currentScreen = Screen.Home,
            onNavigateHome = {},
            onNavigateAdd = {},
            onNavigateProfile = {},
            onLogout = {}
        ) { padding ->
            HomeDashboardContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                profile = previewProfile,
                plants = previewPlants,
                onAddPlant = {},
                onPlantClick = {},
                onProfileClick = {}
            )
        }
    }
}