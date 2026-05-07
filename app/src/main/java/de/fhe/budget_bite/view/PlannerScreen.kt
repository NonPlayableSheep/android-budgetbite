package de.fhe.budget_bite.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import de.fhe.budget_bite.R

@Composable
fun PlannerScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.title_planner),
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}

@Composable
@Preview(
    showBackground = true
)
fun PlannerScreenPreview() {
    PlannerScreen()
}