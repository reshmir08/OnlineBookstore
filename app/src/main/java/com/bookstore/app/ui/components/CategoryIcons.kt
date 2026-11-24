package com.bookstore.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(key: String): ImageVector = when (key) {
    "fiction" -> Icons.Filled.AutoStories
    "nonfiction" -> Icons.Filled.History
    "tech" -> Icons.Filled.Biotech
    "business" -> Icons.AutoMirrored.Filled.TrendingUp
    "children" -> Icons.Filled.ChildCare
    "selfhelp" -> Icons.Filled.Lightbulb
    else -> Icons.Filled.Category
}
