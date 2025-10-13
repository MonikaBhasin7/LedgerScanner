package com.example.ledgerscanner.feature.scanner.exam.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.ledgerscanner.R
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.AssetUtils
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.ui.activity.CreateExamActivity
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@Composable
fun SelectTemplateScreen(navController: NavHostController) {

    val context = LocalContext.current
    var templateList by remember {
        mutableStateOf<UiState<MutableList<Template>>>(UiState.Loading())
    }
    LaunchedEffect(templateList) {
        if (templateList !is UiState.Loading) return@LaunchedEffect
        val templateNamesList = AssetUtils.listJsonAssets(context)
        val tempList = mutableListOf<Template>()
        templateNamesList.forEachIndexed { index, item ->
            val template = Template.Companion.loadOmrTemplateSafe(
                context,
                item
            )
            when (template) {
                is OperationResult.Error -> {}
                is OperationResult.Success -> template.data?.let {
                    tempList.add(
                        it
                    )
                }
            }
        }
        templateList = UiState.Success(tempList)
    }

    Scaffold(
        topBar = {
            GenericToolbar("Select Template", onBackClick = {
                navController.popBackStack()
            })
        }
    ) { innerPadding ->
        when (templateList) {
            is UiState.Error -> {}
            is UiState.Loading -> {
                GenericLoader()
            }

            is UiState.Success -> {
                val templateData =
                    (templateList as UiState.Success<MutableList<Template>>).data ?: mutableListOf()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    itemsIndexed(
                        items = templateData,
                        key = { index, template -> "${template.name}::$index" }
                    ) { index, template ->
                        TemplateCard(template) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(CreateExamActivity.SELECTED_TEMPLATE, template)
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: Template, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .genericClick { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Grey100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = template.imageUrl,
                    placeholder = painterResource(id = R.drawable.placeholder_image),
                    error = painterResource(id = R.drawable.placeholder_image)
                ),
                contentDescription = template.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(White),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = template.name ?: "",
                style = AppTypography.body2Medium,
                color = Grey500,
                maxLines = 1
            )
        }
    }
}