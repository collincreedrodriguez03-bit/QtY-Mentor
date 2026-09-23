package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.components.BottomNavBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val currentTab by viewModel.currentTab.collectAsState()

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            BottomNavBar(currentTab = currentTab) { tab ->
              viewModel.setTab(tab)
            }
          }
        ) { innerPadding ->
          val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

          when (currentTab) {
            AppTab.HOME -> HomeScreen(viewModel)
            AppTab.CHART -> ChartScreen(viewModel)
            AppTab.PRACTICE -> PracticeScreen(viewModel)
            AppTab.REPLAY -> ReplayScreen(viewModel)
            AppTab.JOURNAL -> JournalScreen(viewModel)
            AppTab.LEARN -> LearnScreen(viewModel)
          }
        }
      }
    }
  }
}
