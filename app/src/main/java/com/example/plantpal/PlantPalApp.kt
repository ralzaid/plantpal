package com.example.plantpal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var plants by remember { mutableStateOf(previewPlants) }
    var currentScreen by remember {
        mutableStateOf<Screen>(if (isLoggedIn) Screen.Home else Screen.Login)
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
                onLoggedIn = {
                    isLoggedIn = true
                    currentScreen = Screen.Home
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
            isLoggedIn = false
            currentScreen = Screen.Login
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
                    onSave = { name, nickname, species, location, light, wateringDays, imageUrl ->
                        val nextId = (plants.maxOfOrNull { it.id } ?: 0) + 1
                        plants = plants + UiPlant(
                            id = nextId,
                            name = name,
                            nickname = nickname,
                            species = species,
                            location = location,
                            lightNeeds = light,
                            wateringFrequencyDays = wateringDays,
                            imageUrl = imageUrl
                        )
                        currentScreen = Screen.Home
                    }
                )

                is Screen.PlantDetail -> PlantDetailScreen(
                    plant = plants.firstOrNull { it.id == screen.plantId },
                    onDelete = {
                        plants = plants.filterNot { it.id == screen.plantId }
                        currentScreen = Screen.Home
                    }
                )

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
                        isLoggedIn = false
                        currentScreen = Screen.Login
                    }
                )

                Screen.Login, Screen.SignUp -> Unit
            }
        }
    }
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentScreen == Screen.Profile) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
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
            onNavigateHome = { },
            onNavigateAdd = { },
            onNavigateProfile = { },
            onLogout = { }
        ) { padding ->
            HomeDashboardContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                profile = previewProfile,
                plants = previewPlants,
                onAddPlant = { },
                onPlantClick = { },
                onProfileClick = { }
            )
        }
    }
}
