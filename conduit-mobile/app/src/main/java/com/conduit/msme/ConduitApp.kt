package com.conduit.msme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.conduit.msme.data.model.ConduitLanguage
import com.conduit.msme.data.repository.ConduitRepository
import com.conduit.msme.ui.screens.DigitalTwinScreen
import com.conduit.msme.ui.screens.HomeScreen
import com.conduit.msme.ui.screens.RepaymentScreen
import com.conduit.msme.ui.screens.VoiceApplyScreen
import com.conduit.msme.ui.screens.VoiceOnboardingScreen
import com.conduit.msme.ui.theme.ConduitTheme
import com.conduit.msme.ui.theme.ConduitThemeTokens
import com.conduit.msme.ui.viewmodel.HomeViewModel
import com.conduit.msme.ui.viewmodel.RepaymentViewModel
import com.conduit.msme.ui.viewmodel.VoiceApplyViewModel
import com.example.R
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data object VoiceOnboardingRoute

@Serializable
data object HomeRoute

@Serializable
data object VoiceApplyRoute

@Serializable
data object PassportRoute

@Serializable
data object RepaymentRoute

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private fun Context.localizedContextFor(language: ConduitLanguage): Context {
    val locale = Locale.forLanguageTag(language.code)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

@Composable
fun ConduitApp(
    repository: ConduitRepository
) {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val voiceApplyViewModel: VoiceApplyViewModel = viewModel(factory = VoiceApplyViewModel.Factory(repository))
    val repaymentViewModel: RepaymentViewModel = viewModel(factory = RepaymentViewModel.Factory(repository))

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by voiceApplyViewModel.uiState.collectAsStateWithLifecycle()
    val waveform by voiceApplyViewModel.waveformAmplitudes.collectAsStateWithLifecycle()
    val repaymentState by repaymentViewModel.uiState.collectAsStateWithLifecycle()

    val baseContext = LocalContext.current
    val localizedContext = remember(homeState.language) {
        baseContext.localizedContextFor(homeState.language)
    }
    val isKannada = homeState.language == ConduitLanguage.KANNADA

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration
    ) {
        ConduitTheme(isKannada = isKannada, darkTheme = false) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val extended = ConduitThemeTokens.extendedColors

            val destinations = remember {
                listOf(
                    TopLevelDestination(
                        route = HomeRoute,
                        labelRes = R.string.nav_home,
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        testTag = "nav_home"
                    ),
                    TopLevelDestination(
                        route = VoiceApplyRoute,
                        labelRes = R.string.nav_voice_apply,
                        selectedIcon = Icons.Filled.Mic,
                        unselectedIcon = Icons.Outlined.Mic,
                        testTag = "nav_voice_apply"
                    ),
                    TopLevelDestination(
                        route = PassportRoute,
                        labelRes = R.string.nav_passport,
                        selectedIcon = Icons.Filled.VerifiedUser,
                        unselectedIcon = Icons.Outlined.VerifiedUser,
                        testTag = "nav_passport"
                    ),
                    TopLevelDestination(
                        route = RepaymentRoute,
                        labelRes = R.string.nav_repayment,
                        selectedIcon = Icons.Filled.Payments,
                        unselectedIcon = Icons.Outlined.Payments,
                        testTag = "nav_repayment"
                    )
                )
            }

            fun navigateToTab(route: Any) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            val isOnboardingCompleted = homeState.profile.onboardingCompleted
            val startDest: Any = if (isOnboardingCompleted) HomeRoute else VoiceOnboardingRoute

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val useNavigationRail = maxWidth >= 600.dp && currentDestination?.hasRoute<VoiceOnboardingRoute>() != true
                val showBottomBar = currentDestination?.hasRoute<VoiceOnboardingRoute>() != true

                if (useNavigationRail) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentDestination?.hasRoute(dest.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    modifier = Modifier.testTag(dest.testTag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = extended.cyanAccent,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = extended.cyanContainer
                                    )
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            ConduitNavHost(
                                navController = navController,
                                repository = repository,
                                homeState = homeState,
                                voiceState = voiceState,
                                waveform = waveform,
                                repaymentState = repaymentState,
                                onToggleLanguage = homeViewModel::toggleLanguage,
                                onNavigateToTab = ::navigateToTab,
                                voiceApplyViewModel = voiceApplyViewModel,
                                repaymentViewModel = repaymentViewModel,
                                startDestination = startDest
                            )
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    destinations.forEach { dest ->
                                        val selected = currentDestination?.hasRoute(dest.route::class) == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateToTab(dest.route) },
                                            modifier = Modifier.testTag(dest.testTag),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(dest.labelRes),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = extended.cyanAccent,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = extended.cyanContainer,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        ConduitNavHost(
                            navController = navController,
                            repository = repository,
                            homeState = homeState,
                            voiceState = voiceState,
                            waveform = waveform,
                            repaymentState = repaymentState,
                            onToggleLanguage = homeViewModel::toggleLanguage,
                            onNavigateToTab = ::navigateToTab,
                            voiceApplyViewModel = voiceApplyViewModel,
                            repaymentViewModel = repaymentViewModel,
                            startDestination = startDest,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConduitNavHost(
    navController: androidx.navigation.NavHostController,
    repository: ConduitRepository,
    homeState: com.conduit.msme.ui.viewmodel.HomeUiState,
    voiceState: com.conduit.msme.ui.viewmodel.VoiceApplyUiState,
    waveform: List<Float>,
    repaymentState: com.conduit.msme.ui.viewmodel.RepaymentUiState,
    onToggleLanguage: () -> Unit,
    onNavigateToTab: (Any) -> Unit,
    voiceApplyViewModel: VoiceApplyViewModel,
    repaymentViewModel: RepaymentViewModel,
    startDestination: Any = HomeRoute,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize()
    ) {
        composable<VoiceOnboardingRoute> {
            VoiceOnboardingScreen(
                repository = repository,
                onOnboardingComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo(VoiceOnboardingRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                uiState = homeState,
                onToggleLanguage = onToggleLanguage,
                onNavigateToVoiceApply = { onNavigateToTab(VoiceApplyRoute) },
                onNavigateToPassport = { onNavigateToTab(PassportRoute) },
                onNavigateToRepayment = { onNavigateToTab(RepaymentRoute) }
            )
        }
        composable<VoiceApplyRoute> {
            VoiceApplyScreen(
                uiState = voiceState,
                waveformAmplitudes = waveform,
                onStartRecording = voiceApplyViewModel::startRecording,
                onStopRecording = voiceApplyViewModel::stopRecording,
                onOpenAAConsent = voiceApplyViewModel::openAAConsentDialog,
                onDismissAAConsent = voiceApplyViewModel::dismissAAConsentDialog,
                onApproveAAConsent = voiceApplyViewModel::approveAAConsent,
                onAcceptLoanOffer = voiceApplyViewModel::acceptLoanOffer,
                onResetFlow = voiceApplyViewModel::resetFlow,
                onNavigateToRepayment = { onNavigateToTab(RepaymentRoute) }
            )
        }
        composable<PassportRoute> {
            DigitalTwinScreen(
                profile = homeState.profile,
                digitalTwin = homeState.digitalTwin,
                language = homeState.language
            )
        }
        composable<RepaymentRoute> {
            RepaymentScreen(
                uiState = repaymentState,
                language = homeState.language,
                onSimulateDailySweep = repaymentViewModel::simulateDailyUpiSweep,
                onDownloadCertificate = repaymentViewModel::triggerCibilCertificateDownload
            )
        }
    }
}
