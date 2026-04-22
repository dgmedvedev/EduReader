package com.example.edureader.presentation.reader.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

enum class ReaderDrawerAction {
    About,
    TableOfContents,
    PickFile
}

data class DrawerMenuItemUi(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    val action: ReaderDrawerAction
)

@Composable
fun ReaderDrawerMenuItems(
    items: List<DrawerMenuItemUi>,
    onItemClick: (ReaderDrawerAction) -> Unit
) {
    items.forEach { item ->
        NavigationDrawerItem(
            icon = {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null
                )
            },
            label = { Text(stringResource(item.titleRes)) },
            selected = false,
            onClick = { onItemClick(item.action) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ReaderDrawerSheet(
    title: String,
    items: List<DrawerMenuItemUi>,
    onItemClick: (ReaderDrawerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier.fillMaxHeight()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        )
        ReaderDrawerMenuItems(
            items = items,
            onItemClick = onItemClick
        )
    }
}
