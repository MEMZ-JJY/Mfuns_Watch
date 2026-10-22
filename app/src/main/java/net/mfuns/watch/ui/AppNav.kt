package net.mfuns.watch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import net.mfuns.watch.data.SessionStore
import net.mfuns.watch.ui.screens.AboutScreen
import net.mfuns.watch.ui.screens.CategoryScreen
import net.mfuns.watch.ui.screens.CommentsScreen
import net.mfuns.watch.ui.screens.DetailScreen
import net.mfuns.watch.ui.screens.FavoriteScreen
import net.mfuns.watch.ui.screens.HistoryScreen
import net.mfuns.watch.ui.screens.HomeScreen
import net.mfuns.watch.ui.screens.MessagesScreen
import net.mfuns.watch.ui.screens.PlayerScreen
import net.mfuns.watch.ui.screens.SearchScreen
import net.mfuns.watch.ui.screens.UserScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATEGORY = "category"
    const val SIGN = "sign"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val MESSAGES = "messages"
    const val ABOUT = "about"
    const val DETAIL = "detail/{type}/{id}"
    const val PLAYER = "player/{videoId}"
    const val COMMENTS = "comments/{areaId}"
    const val USER = "user/{userId}"

    fun detail(type: Int, id: Int) = "detail/$type/$id"
    fun player(videoId: Int) = "player/$videoId"
    fun comments(areaId: Int) = "comments/$areaId"
    fun user(userId: Int) = "user/$userId"
}

@Composable
fun AppNavHost(
    appViewModel: AppViewModel,
    session: SessionStore.Session,
) {
    val navController = rememberNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel(),
                session = session,
                onOpenDetail = { type, id -> navController.navigate(Routes.detail(type, id)) },
                onOpenUser = { id -> navController.navigate(Routes.user(id)) },
                onNavigate = { route ->
                    when (route) {
                        "about" -> navController.navigate(Routes.ABOUT)
                        else -> navController.navigate(route)
                    }
                },
                onLogout = {
                    appViewModel.logout()
                },
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = viewModel(),
                onOpenDetail = { type, id -> navController.navigate(Routes.detail(type, id)) },
            )
        }

        composable(Routes.CATEGORY) {
            CategoryScreen(
                viewModel = viewModel(),
                onOpenDetail = { type, id -> navController.navigate(Routes.detail(type, id)) },
            )
        }

        composable(Routes.SIGN) {
            net.mfuns.watch.ui.screens.SignScreen(viewModel = viewModel())
        }

        composable(Routes.FAVORITES) {
            FavoriteScreen(
                viewModel = viewModel(),
                onOpenDetail = { type, id -> navController.navigate(Routes.detail(type, id)) },
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel(),
                onOpenDetail = { type, id -> navController.navigate(Routes.detail(type, id)) },
            )
        }

        composable(Routes.MESSAGES) {
            MessagesScreen(
                viewModel = viewModel(),
                onOpenUser = { id -> navController.navigate(Routes.user(id)) },
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onLogout = { appViewModel.logout() },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("type") { type = NavType.IntType },
                navArgument("id") { type = NavType.IntType },
            ),
        ) { entry ->
            val type = entry.arguments?.getInt("type") ?: 0
            val id = entry.arguments?.getInt("id") ?: 0
            DetailScreen(
                viewModel = viewModel(),
                type = type,
                id = id,
                onPlayVideo = { videoId -> navController.navigate(Routes.player(videoId)) },
                onOpenComments = { areaId -> navController.navigate(Routes.comments(areaId)) },
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("videoId") { type = NavType.IntType },
            ),
        ) { entry ->
            val videoId = entry.arguments?.getInt("videoId") ?: 0
            PlayerScreen(
                viewModel = viewModel(),
                videoId = videoId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.COMMENTS,
            arguments = listOf(
                navArgument("areaId") { type = NavType.IntType },
            ),
        ) { entry ->
            val areaId = entry.arguments?.getInt("areaId") ?: 0
            CommentsScreen(
                viewModel = viewModel(),
                areaId = areaId,
            )
        }

        composable(
            route = Routes.USER,
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
            ),
        ) { entry ->
            val userId = entry.arguments?.getInt("userId") ?: 0
            UserScreen(
                viewModel = viewModel(),
                userId = userId,
                onOpenDetail = { type, id -> navController.navigate(Routes.detail(type, id)) },
            )
        }
    }
}
