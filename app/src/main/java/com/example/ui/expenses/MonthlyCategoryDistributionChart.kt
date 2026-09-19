package com.example.ui.expenses

import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySpending
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.getCategoryColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.util.DateTimeUtils
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ChartDisplayType(val label: String, val icon: ImageVector) {
    DONUT("Donut Arc", Icons.Default.DonutLarge),
    BARS("Bar Levels", Icons.Default.ViewColumn),
    PROPORTION("Share Strip", Icons.Default.StackedBarChart)
}

/**
 * Data structure representing calculated D3-style Arc angles and coordinates
 */
data class D3ArcSlice(
    val categorySpending: CategorySpending,
    val startAngle: Float,
    val sweepAngle: Float,
    val endAngle: Float,
    val color: Color,
    val isSelected: Boolean
)

/**
 * High-performance, D3/Recharts inspired visualization component for displaying
 * monthly expense distributions by category with smooth animations, interactive arc selection,
 * center telemetry hub, and multiple visualization modes.
 */
@Composable
fun MonthlyCategoryDistributionChart(
    categorySpending: List<CategorySpending>,
    monthlyTotal: Double,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categorySpending.isEmpty()) return

    var chartType by remember { mutableStateOf(ChartDisplayType.DONUT) }
    val activeCategoryItem = categorySpending.firstOrNull { it.category == selectedCategory }
    val topCategory = categorySpending.maxByOrNull { it.amount }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_category_distribution_chart"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & Chart View Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Category Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "D3 / Recharts",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = "${categorySpending.size} categories • ${DateTimeUtils.formatCurrency(monthlyTotal)} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini Tab Switcher (Donut / Bars / Proportion)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ChartDisplayType.entries.forEach { type ->
                            val isChosen = chartType == type
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isChosen) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isChosen) 1.dp else 0.dp,
                                modifier = Modifier
                                    .clickable { chartType = type }
                                    .testTag("chart_type_${type.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = type.icon,
                                        contentDescription = type.label,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Visualizer Area based on selected ChartDisplayType
            AnimatedContent(
                targetState = chartType,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "chart_visual_transition"
            ) { targetType ->
                when (targetType) {
                    ChartDisplayType.DONUT -> {
                        D3DonutArcVisualizer(
                            categorySpending = categorySpending,
                            monthlyTotal = monthlyTotal,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory
                        )
                    }
                    ChartDisplayType.BARS -> {
                        D3BarLevelsVisualizer(
                            categorySpending = categorySpending,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory
                        )
                    }
                    ChartDisplayType.PROPORTION -> {
                        D3ProportionShareVisualizer(
                            categorySpending = categorySpending,
                            monthlyTotal = monthlyTotal,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recharts-style Interactive Tooltip / Detail Card
            RechartsDetailTooltipCard(
                activeCategoryItem = activeCategoryItem,
                topCategory = topCategory,
                monthlyTotal = monthlyTotal,
                onClearSelection = { onSelectCategory(null) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Category Legend Chips Row
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "INTERACTIVE LEGEND",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorySpending.forEach { item ->
                    val isSelected = selectedCategory == item.category
                    val categoryColor = getCategoryColor(item.category)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) categoryColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) categoryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clickable {
                                onSelectCategory(if (isSelected) null else item.category)
                            }
                            .testTag("legend_chip_${item.category.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", item.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive D3 Arc & Donut Chart Canvas with touch point angle hit-testing,
 * animated sweep angles, and central telemetry hub.
 */
@Composable
fun D3DonutArcVisualizer(
    categorySpending: List<CategorySpending>,
    monthlyTotal: Double,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation progress for chart entry
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut_draw_anim"
    )

    // Calculate D3 Arc slices (start angle, sweep angle, end angle with pad angle)
    val padAngleDeg = 2.5f
    val totalPad = padAngleDeg * categorySpending.size
    val availableDegrees = (360f - totalPad).coerceAtLeast(0f)

    var currentStart = -90f // Start at top (12 o'clock)
    val arcSlices = remember(categorySpending, selectedCategory) {
        val slices = mutableListOf<D3ArcSlice>()
        categorySpending.forEach { item ->
            val sweep = (item.percentage / 100f) * availableDegrees
            val isSelected = selectedCategory == item.category
            slices.add(
                D3ArcSlice(
                    categorySpending = item,
                    startAngle = currentStart,
                    sweepAngle = sweep,
                    endAngle = currentStart + sweep,
                    color = getCategoryColor(item.category),
                    isSelected = isSelected
                )
            )
            currentStart += sweep + padAngleDeg
        }
        slices
    }

    val activeSlice = arcSlices.firstOrNull { it.isSelected }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        // D3 Canvas Drawing
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(arcSlices) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        val outerR = size.width / 2f
                        val innerR = outerR * 0.58f

                        // Check if tap falls within donut ring or center
                        if (distance >= innerR * 0.7f && distance <= outerR * 1.15f) {
                            var angleDeg = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())
                            // Normalize angle to match start at -90 deg
                            if (angleDeg < -90f) angleDeg += 360f

                            val hitSlice = arcSlices.firstOrNull { slice ->
                                val s = slice.startAngle
                                val e = slice.startAngle + slice.sweepAngle + padAngleDeg
                                angleDeg in s..e || (s < -90f && angleDeg + 360f in s..e)
                            }
                            if (hitSlice != null) {
                                onSelectCategory(if (selectedCategory == hitSlice.categorySpending.category) null else hitSlice.categorySpending.category)
                            } else {
                                onSelectCategory(null)
                            }
                        } else if (distance < innerR * 0.7f) {
                            // Tapping center resets selection
                            onSelectCategory(null)
                        }
                    }
                }
        ) {
            val strokeWidthNormal = 34.dp.toPx()
            val strokeWidthSelected = 42.dp.toPx()
            val radius = (size.minDimension - strokeWidthSelected) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Draw each D3 Arc slice
            arcSlices.forEach { slice ->
                val strokeW = if (slice.isSelected) strokeWidthSelected else strokeWidthNormal
                val sliceAlpha = if (selectedCategory == null || slice.isSelected) 1f else 0.35f
                val sweepAnimated = slice.sweepAngle * animationProgress

                drawArc(
                    color = slice.color.copy(alpha = sliceAlpha),
                    startAngle = slice.startAngle,
                    sweepAngle = sweepAnimated,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Center Telemetry Hub (D3/Recharts Center Metric)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(116.dp)
                .clickable { onSelectCategory(null) }
                .padding(6.dp)
        ) {
            if (activeSlice != null) {
                Icon(
                    imageVector = getCategoryIcon(activeSlice.categorySpending.category),
                    contentDescription = activeSlice.categorySpending.category,
                    tint = activeSlice.color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activeSlice.categorySpending.category,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateTimeUtils.formatCurrency(activeSlice.categorySpending.amount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = activeSlice.color,
                    fontSize = 13.sp
                )
                Text(
                    text = "${String.format("%.1f", activeSlice.categorySpending.percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            } else {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = DateTimeUtils.formatCurrency(monthlyTotal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tap slice to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * D3 Horizontal Bar Levels Visualizer with proportional value scale and rank index
 */
@Composable
fun D3BarLevelsVisualizer(
    categorySpending: List<CategorySpending>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxAmount = categorySpending.maxOfOrNull { it.amount } ?: 1.0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categorySpending.forEachIndexed { index, item ->
            val isSelected = selectedCategory == null || selectedCategory == item.category
            val isExplicit = selectedCategory == item.category
            val categoryColor = getCategoryColor(item.category)
            val targetFraction = (item.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)

            val animatedWidth by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = tween(durationMillis = 600 + index * 50, easing = FastOutSlowInEasing),
                label = "bar_level_anim_${item.category}"
            )

            val itemAlpha = if (isSelected) 1f else 0.35f

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isExplicit) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                border = if (isExplicit) BorderStroke(1.dp, categoryColor) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCategory(if (isExplicit) null else item.category) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = itemAlpha),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = getCategoryIcon(item.category),
                                contentDescription = null,
                                tint = categoryColor.copy(alpha = itemAlpha),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isExplicit) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = itemAlpha)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = DateTimeUtils.formatCurrency(item.amount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = itemAlpha)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${String.format("%.1f", item.percentage)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor.copy(alpha = itemAlpha),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // D3 Bar Line with Gradient Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            categoryColor.copy(alpha = itemAlpha * 0.75f),
                                            categoryColor.copy(alpha = itemAlpha)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Continuous D3 Stacked Proportion Strip showing full 100% distribution share
 */
@Composable
fun D3ProportionShareVisualizer(
    categorySpending: List<CategorySpending>,
    monthlyTotal: Double,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Continuous Stacked Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                categorySpending.forEach { item ->
                    val isSelected = selectedCategory == null || selectedCategory == item.category
                    val categoryColor = getCategoryColor(item.category)
                    val alpha = if (isSelected) 1f else 0.3f
                    val weight = (item.percentage / 100f).coerceAtLeast(0.01f)

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(categoryColor.copy(alpha = alpha))
                            .clickable {
                                onSelectCategory(if (selectedCategory == item.category) null else item.category)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.percentage >= 12f) {
                            Text(
                                text = "${item.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Summary Proportion grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            categorySpending.take(3).forEach { item ->
                val categoryColor = getCategoryColor(item.category)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(categoryColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", item.percentage)}% • ${DateTimeUtils.formatCurrency(item.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style Tooltip Card with rich metric callouts
 */
@Composable
fun RechartsDetailTooltipCard(
    activeCategoryItem: CategorySpending?,
    topCategory: CategorySpending?,
    monthlyTotal: Double,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        if (activeCategoryItem != null) {
            val color = getCategoryColor(activeCategoryItem.category)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(
                        categoryName = activeCategoryItem.category,
                        size = 36
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${activeCategoryItem.category} Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activeCategoryItem.count} transactions logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = DateTimeUtils.formatCurrency(activeCategoryItem.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "${String.format("%.1f", activeCategoryItem.percentage)}% of month",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Top Expense Leader",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${topCategory?.category ?: "N/A"} (${String.format("%.1f", topCategory?.percentage ?: 0f)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = DateTimeUtils.formatCurrency(topCategory?.amount ?: 0.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (topCategory != null) getCategoryColor(topCategory.category) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
