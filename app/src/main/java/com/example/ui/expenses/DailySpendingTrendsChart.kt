package com.example.ui.expenses

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailySpendingTrend
import com.example.data.model.Expense
import com.example.data.model.TrendChartType
import com.example.data.model.TrendTimeRange
import com.example.ui.components.getCategoryColor
import com.example.ui.util.DateTimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * Recharts & Canvas-based high-performance data visualization component
 * for interactive daily spending trends analysis.
 */
@Composable
fun DailySpendingTrendsChart(
    dailyTrends: List<DailySpendingTrend>,
    selectedDate: LocalDate,
    monthlyBudget: Double = 0.0,
    timeRange: TrendTimeRange = TrendTimeRange.FULL_MONTH,
    onTimeRangeChange: (TrendTimeRange) -> Unit = {},
    onDaySelected: (LocalDate) -> Unit = {},
    onNavigateToDailyLog: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (dailyTrends.isEmpty()) return

    var chartType by remember { mutableStateOf(TrendChartType.AREA_CURVE) }
    var selectedIndex by remember(dailyTrends) {
        val initialIdx = dailyTrends.indexOfFirst { it.date == selectedDate }
            .takeIf { it >= 0 } ?: (dailyTrends.size - 1).coerceAtLeast(0)
        mutableIntStateOf(initialIdx)
    }

    val selectedDayTrend = dailyTrends.getOrNull(selectedIndex)

    // Key trend metrics
    val totalPeriodSpend = remember(dailyTrends) { dailyTrends.sumOf { it.totalAmount } }
    val activeDays = remember(dailyTrends) { dailyTrends.filter { it.totalAmount > 0.0 } }
    val activeDaysCount = activeDays.size
    val dailyAverage = remember(dailyTrends, totalPeriodSpend) {
        if (dailyTrends.isNotEmpty()) totalPeriodSpend / dailyTrends.size else 0.0
    }
    val activeDaysAverage = remember(activeDays, totalPeriodSpend) {
        if (activeDays.isNotEmpty()) totalPeriodSpend / activeDays.size else 0.0
    }
    val peakDay = remember(dailyTrends) { dailyTrends.maxByOrNull { it.totalAmount } }
    val maxSpend = remember(dailyTrends) { (dailyTrends.maxOfOrNull { it.totalAmount } ?: 100.0).coerceAtLeast(10.0) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_spending_trends_chart"),
        shape = RoundedCornerShape(24.dp),
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
            // Header: Title, Technology Badge, and Chart Style Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Daily Spending Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Recharts / Canvas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Interactive daily expenditure flow & baseline tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Chart mode selector (Area / Bars / Cumulative)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TrendChartType.entries.forEach { type ->
                            val isSelected = chartType == type
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp,
                                modifier = Modifier
                                    .clickable { chartType = type }
                                    .testTag("trend_chart_type_${type.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (type) {
                                            TrendChartType.AREA_CURVE -> Icons.Default.Timeline
                                            TrendChartType.BARS -> Icons.Default.ViewColumn
                                            TrendChartType.CUMULATIVE -> Icons.Default.StackedBarChart
                                        },
                                        contentDescription = type.label,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Range Tabs (Full Month, 7D, 14D, 30D)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrendTimeRange.entries.forEach { range ->
                    val isSelected = timeRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTimeRangeChange(range) },
                        label = {
                            Text(
                                text = range.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key Metrics Pill Row (Daily Average, Peak Day, Active Days)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Period Total",
                    value = DateTimeUtils.formatCurrency(totalPeriodSpend),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Daily Avg",
                    value = DateTimeUtils.formatCurrency(dailyAverage),
                    color = Color(0xFF0284C7), // Cyan/Blue
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Peak Day",
                    value = if (peakDay != null && peakDay.totalAmount > 0) "${peakDay.dayLabel} (${DateTimeUtils.formatCurrency(peakDay.totalAmount)})" else "None",
                    color = Color(0xFFD97706), // Amber
                    modifier = Modifier.weight(1.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart Rendering Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                when (chartType) {
                    TrendChartType.AREA_CURVE -> {
                        DailyTrendAreaCanvas(
                            dailyTrends = dailyTrends,
                            selectedIndex = selectedIndex,
                            dailyAverage = dailyAverage,
                            maxSpend = maxSpend,
                            onSelectIndex = { idx ->
                                selectedIndex = idx
                                dailyTrends.getOrNull(idx)?.let { onDaySelected(it.date) }
                            }
                        )
                    }
                    TrendChartType.BARS -> {
                        DailyTrendBarsCanvas(
                            dailyTrends = dailyTrends,
                            selectedIndex = selectedIndex,
                            dailyAverage = dailyAverage,
                            maxSpend = maxSpend,
                            onSelectIndex = { idx ->
                                selectedIndex = idx
                                dailyTrends.getOrNull(idx)?.let { onDaySelected(it.date) }
                            }
                        )
                    }
                    TrendChartType.CUMULATIVE -> {
                        DailyTrendCumulativeCanvas(
                            dailyTrends = dailyTrends,
                            selectedIndex = selectedIndex,
                            monthlyBudget = monthlyBudget,
                            onSelectIndex = { idx ->
                                selectedIndex = idx
                                dailyTrends.getOrNull(idx)?.let { onDaySelected(it.date) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Day Telemetry Card / Inspector
            AnimatedVisibility(visible = selectedDayTrend != null) {
                selectedDayTrend?.let { trend ->
                    DayInspectorCard(
                        trend = trend,
                        dailyAverage = dailyAverage,
                        onNavigateToDailyLog = onNavigateToDailyLog
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DayInspectorCard(
    trend: DailySpendingTrend,
    dailyAverage: Double,
    onNavigateToDailyLog: (LocalDate) -> Unit
) {
    val isAboveAverage = trend.totalAmount > dailyAverage && dailyAverage > 0
    val ratio = if (dailyAverage > 0) (trend.totalAmount / dailyAverage) else 1.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_inspector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (trend.totalAmount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${trend.dayOfMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (trend.totalAmount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trend.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (trend.isToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DateTimeUtils.formatCurrency(trend.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (trend.totalAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${trend.transactionCount} txn${if (trend.transactionCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    if (trend.totalAmount > 0 && dailyAverage > 0) {
                        Text(
                            text = if (isAboveAverage) "${String.format("%.1fx", ratio)} above daily avg" else "${String.format("%.0f%%", (1 - ratio) * 100)} below avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAboveAverage) Color(0xFFD97706) else Color(0xFF059669),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { onNavigateToDailyLog(trend.date) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "View Log",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Recharts-style Smooth Bezier Area Chart with Gradient fill, Average reference line,
 * and touch scrubbing inspector
 */
@Composable
private fun DailyTrendAreaCanvas(
    dailyTrends: List<DailySpendingTrend>,
    selectedIndex: Int,
    dailyAverage: Double,
    maxSpend: Double,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val peakDay = remember(dailyTrends) { dailyTrends.maxByOrNull { it.totalAmount } }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "area_anim"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daily_trend_area_canvas")
            .pointerInput(dailyTrends) {
                detectTapGestures { offset ->
                    val width = size.width
                    val paddingHorizontal = 16f
                    val chartWidth = width - (paddingHorizontal * 2)
                    if (dailyTrends.size > 1 && offset.x in paddingHorizontal..(width - paddingHorizontal)) {
                        val step = chartWidth / (dailyTrends.size - 1)
                        val idx = ((offset.x - paddingHorizontal + (step / 2)) / step).toInt().coerceIn(0, dailyTrends.size - 1)
                        onSelectIndex(idx)
                    }
                }
            }
            .pointerInput(dailyTrends) {
                detectDragGestures { change, _ ->
                    val width = size.width
                    val paddingHorizontal = 16f
                    val chartWidth = width - (paddingHorizontal * 2)
                    if (dailyTrends.size > 1 && change.position.x in paddingHorizontal..(width - paddingHorizontal)) {
                        val step = chartWidth / (dailyTrends.size - 1)
                        val idx = ((change.position.x - paddingHorizontal + (step / 2)) / step).toInt().coerceIn(0, dailyTrends.size - 1)
                        onSelectIndex(idx)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 32f
        val paddingRight = 16f
        val paddingTop = 20f
        val paddingBottom = 24f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val numPoints = dailyTrends.size
        if (numPoints <= 1) return@Canvas

        val safeMax = (maxSpend * 1.15).coerceAtLeast(1.0)
        val stepX = chartWidth / (numPoints - 1)

        // 1. Draw Grid Lines (0%, 33%, 66%, 100%)
        val gridLevels = listOf(0.0, 0.33, 0.66, 1.0)
        val textPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.6f).toArgb()
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        gridLevels.forEach { lvl ->
            val yPos = paddingTop + (chartHeight * (1.0 - lvl)).toFloat()
            drawLine(
                color = outlineColor.copy(alpha = 0.25f),
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Y-axis Currency Tick Label
            val tickValue = safeMax * lvl
            val tickText = if (tickValue >= 1000) "${(tickValue / 1000).toInt()}k" else "${tickValue.toInt()}"
            drawContext.canvas.nativeCanvas.drawText(
                "৳$tickText",
                paddingLeft - 6f,
                yPos + 7f,
                textPaint
            )
        }

        // 2. Average Daily Spending Reference Line (Recharts ReferenceLine style)
        if (dailyAverage > 0) {
            val avgRatio = (dailyAverage / safeMax).coerceIn(0.0, 1.0)
            val avgY = (paddingTop + (chartHeight * (1.0 - avgRatio))).toFloat()
            val avgColor = Color(0xFF0284C7)

            drawLine(
                color = avgColor.copy(alpha = 0.8f),
                start = Offset(paddingLeft, avgY),
                end = Offset(width - paddingRight, avgY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )

            val avgPaint = Paint().apply {
                color = avgColor.toArgb()
                textSize = 20f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            drawContext.canvas.nativeCanvas.drawText(
                "Avg ৳${dailyAverage.toInt()}",
                width - paddingRight,
                avgY - 4f,
                avgPaint
            )
        }

        // 3. Compute Coordinates for Area & Line
        val points = mutableListOf<Offset>()
        dailyTrends.forEachIndexed { i, trend ->
            val x = paddingLeft + (i * stepX)
            val normalizedSpend = (trend.totalAmount / safeMax).coerceIn(0.0, 1.0) * animProgress
            val y = (paddingTop + (chartHeight * (1.0 - normalizedSpend))).toFloat()
            points.add(Offset(x, y))
        }

        // 4. Construct Smooth Bezier Path
        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(points[0].x, points[0].y)
        fillPath.moveTo(points[0].x, paddingTop + chartHeight)
        fillPath.lineTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlPointX1 = p0.x + (p1.x - p0.x) / 2
            val controlPointY1 = p0.y
            val controlPointX2 = p0.x + (p1.x - p0.x) / 2
            val controlPointY2 = p1.y

            strokePath.cubicTo(controlPointX1, controlPointY1, controlPointX2, controlPointY2, p1.x, p1.y)
            fillPath.cubicTo(controlPointX1, controlPointY1, controlPointX2, controlPointY2, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, paddingTop + chartHeight)
        fillPath.close()

        // 5. Draw Area Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.40f),
                    primaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        // 6. Draw Stroke Curve
        drawPath(
            path = strokePath,
            color = primaryColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 7. Draw Data Points & Peak Indicator
        points.forEachIndexed { i, pt ->
            val trend = dailyTrends[i]
            val isSelected = i == selectedIndex
            val isPeak = trend == peakDay && trend.totalAmount > 0

            if (trend.totalAmount > 0 || isSelected || trend.isToday) {
                val nodeRadius = when {
                    isSelected -> 6.dp.toPx()
                    isPeak -> 5.dp.toPx()
                    else -> 3.dp.toPx()
                }

                // Outer Halo for Selected or Peak
                if (isSelected || isPeak) {
                    drawCircle(
                        color = if (isSelected) primaryColor.copy(alpha = 0.25f) else Color(0xFFD97706).copy(alpha = 0.25f),
                        radius = nodeRadius + 5.dp.toPx(),
                        center = pt
                    )
                }

                // Inner Dot
                drawCircle(
                    color = surfaceColor,
                    radius = nodeRadius,
                    center = pt
                )
                drawCircle(
                    color = when {
                        isPeak -> Color(0xFFD97706)
                        isSelected -> primaryColor
                        else -> secondaryColor
                    },
                    radius = nodeRadius - 1.5.dp.toPx(),
                    center = pt
                )
            }
        }

        // 8. Scrubbing / Selected Cursor Line
        if (selectedIndex in points.indices) {
            val selectedPoint = points[selectedIndex]
            drawLine(
                color = primaryColor.copy(alpha = 0.7f),
                start = Offset(selectedPoint.x, paddingTop),
                end = Offset(selectedPoint.x, paddingTop + chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }

        // 9. X-axis Day Labels
        val labelStep = when {
            numPoints <= 8 -> 1
            numPoints <= 15 -> 2
            numPoints <= 22 -> 3
            else -> 4
        }

        val axisTextPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.75f).toArgb()
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until numPoints step labelStep) {
            val pt = points[i]
            val trend = dailyTrends[i]
            val label = if (numPoints <= 10) trend.dayLabel else "${trend.dayOfMonth}"
            drawContext.canvas.nativeCanvas.drawText(
                label,
                pt.x,
                height - 4f,
                axisTextPaint
            )
        }
    }
}

/**
 * Recharts-style Daily Columns / Bar Chart with gradient intensity and active selection
 */
@Composable
private fun DailyTrendBarsCanvas(
    dailyTrends: List<DailySpendingTrend>,
    selectedIndex: Int,
    dailyAverage: Double,
    maxSpend: Double,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val peakDay = remember(dailyTrends) { dailyTrends.maxByOrNull { it.totalAmount } }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bars_anim"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daily_trend_bars_canvas")
            .pointerInput(dailyTrends) {
                detectTapGestures { offset ->
                    val width = size.width
                    val paddingLeft = 32f
                    val paddingRight = 16f
                    val chartWidth = width - paddingLeft - paddingRight
                    val stepX = chartWidth / dailyTrends.size
                    if (offset.x in paddingLeft..(width - paddingRight)) {
                        val idx = ((offset.x - paddingLeft) / stepX).toInt().coerceIn(0, dailyTrends.size - 1)
                        onSelectIndex(idx)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 32f
        val paddingRight = 16f
        val paddingTop = 20f
        val paddingBottom = 24f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val numBars = dailyTrends.size
        if (numBars == 0) return@Canvas

        val safeMax = (maxSpend * 1.15).coerceAtLeast(1.0)
        val barSlotWidth = chartWidth / numBars
        val barWidth = (barSlotWidth * 0.65f).coerceIn(4f, 28f)

        // 1. Grid Lines
        val gridLevels = listOf(0.0, 0.5, 1.0)
        val textPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.6f).toArgb()
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        gridLevels.forEach { lvl ->
            val yPos = paddingTop + (chartHeight * (1.0 - lvl)).toFloat()
            drawLine(
                color = outlineColor.copy(alpha = 0.25f),
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            val tickValue = safeMax * lvl
            val tickText = if (tickValue >= 1000) "${(tickValue / 1000).toInt()}k" else "${tickValue.toInt()}"
            drawContext.canvas.nativeCanvas.drawText(
                "৳$tickText",
                paddingLeft - 6f,
                yPos + 7f,
                textPaint
            )
        }

        // 2. Average Line
        if (dailyAverage > 0) {
            val avgRatio = (dailyAverage / safeMax).coerceIn(0.0, 1.0)
            val avgY = (paddingTop + (chartHeight * (1.0 - avgRatio))).toFloat()
            val avgColor = Color(0xFF0284C7)

            drawLine(
                color = avgColor.copy(alpha = 0.8f),
                start = Offset(paddingLeft, avgY),
                end = Offset(width - paddingRight, avgY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }

        // 3. Draw Daily Bars
        dailyTrends.forEachIndexed { i, trend ->
            val slotCenterX = paddingLeft + (i * barSlotWidth) + (barSlotWidth / 2)
            val barLeft = slotCenterX - (barWidth / 2)
            val isSelected = i == selectedIndex
            val isPeak = trend == peakDay && trend.totalAmount > 0

            // Highlight column background for selected
            if (isSelected) {
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.12f),
                    topLeft = Offset(slotCenterX - (barSlotWidth / 2), paddingTop),
                    size = Size(barSlotWidth, chartHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            val barHeight = ((trend.totalAmount / safeMax) * chartHeight * animProgress).toFloat().coerceAtLeast(if (trend.totalAmount > 0) 4.dp.toPx() else 1.5.dp.toPx())
            val barTop = paddingTop + chartHeight - barHeight

            val barBrush = when {
                isPeak -> Brush.verticalGradient(
                    listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                    startY = barTop,
                    endY = paddingTop + chartHeight
                )
                isSelected -> Brush.verticalGradient(
                    listOf(primaryColor, primaryColor.copy(alpha = 0.7f)),
                    startY = barTop,
                    endY = paddingTop + chartHeight
                )
                trend.totalAmount > dailyAverage && dailyAverage > 0 -> Brush.verticalGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    startY = barTop,
                    endY = paddingTop + chartHeight
                )
                trend.totalAmount > 0 -> Brush.verticalGradient(
                    listOf(secondaryColor.copy(alpha = 0.85f), secondaryColor.copy(alpha = 0.5f)),
                    startY = barTop,
                    endY = paddingTop + chartHeight
                )
                else -> Brush.verticalGradient(
                    listOf(outlineColor.copy(alpha = 0.3f), outlineColor.copy(alpha = 0.15f)),
                    startY = barTop,
                    endY = paddingTop + chartHeight
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }

        // 4. X-axis Labels
        val labelStep = when {
            numBars <= 8 -> 1
            numBars <= 15 -> 2
            numBars <= 22 -> 3
            else -> 4
        }

        val axisTextPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.75f).toArgb()
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until numBars step labelStep) {
            val slotCenterX = paddingLeft + (i * barSlotWidth) + (barSlotWidth / 2)
            val trend = dailyTrends[i]
            val label = if (numBars <= 10) trend.dayLabel else "${trend.dayOfMonth}"
            drawContext.canvas.nativeCanvas.drawText(
                label,
                slotCenterX,
                height - 4f,
                axisTextPaint
            )
        }
    }
}

/**
 * Cumulative Spending Pace Trajectory against Monthly Budget Baseline
 */
@Composable
private fun DailyTrendCumulativeCanvas(
    dailyTrends: List<DailySpendingTrend>,
    selectedIndex: Int,
    monthlyBudget: Double,
    onSelectIndex: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "cum_anim"
    )

    // Compute cumulative running totals
    val cumulativeSpend = remember(dailyTrends) {
        var runningSum = 0.0
        dailyTrends.map {
            runningSum += it.totalAmount
            runningSum
        }
    }

    val totalSpent = cumulativeSpend.lastOrNull() ?: 0.0
    val maxChartVal = max(totalSpent, monthlyBudget).coerceAtLeast(10.0) * 1.15

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("daily_trend_cumulative_canvas")
            .pointerInput(dailyTrends) {
                detectTapGestures { offset ->
                    val width = size.width
                    val paddingLeft = 32f
                    val paddingRight = 16f
                    val chartWidth = width - paddingLeft - paddingRight
                    if (dailyTrends.size > 1 && offset.x in paddingLeft..(width - paddingRight)) {
                        val step = chartWidth / (dailyTrends.size - 1)
                        val idx = ((offset.x - paddingLeft + (step / 2)) / step).toInt().coerceIn(0, dailyTrends.size - 1)
                        onSelectIndex(idx)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 32f
        val paddingRight = 16f
        val paddingTop = 20f
        val paddingBottom = 24f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val numPoints = dailyTrends.size
        if (numPoints <= 1) return@Canvas

        val stepX = chartWidth / (numPoints - 1)

        // 1. Grid Lines
        val gridLevels = listOf(0.0, 0.5, 1.0)
        val textPaint = Paint().apply {
            color = onSurfaceVariant.copy(alpha = 0.6f).toArgb()
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        gridLevels.forEach { lvl ->
            val yPos = paddingTop + (chartHeight * (1.0 - lvl)).toFloat()
            drawLine(
                color = outlineColor.copy(alpha = 0.25f),
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            val tickValue = maxChartVal * lvl
            val tickText = if (tickValue >= 1000) "${(tickValue / 1000).toInt()}k" else "${tickValue.toInt()}"
            drawContext.canvas.nativeCanvas.drawText(
                "৳$tickText",
                paddingLeft - 6f,
                yPos + 7f,
                textPaint
            )
        }

        // 2. Budget Pace Benchmark Line if Budget is Set
        if (monthlyBudget > 0) {
            val budgetEndY = (paddingTop + (chartHeight * (1.0 - (monthlyBudget / maxChartVal)))).toFloat()
            val budgetPaceColor = Color(0xFF10B981) // Emerald Green

            drawLine(
                color = budgetPaceColor.copy(alpha = 0.75f),
                start = Offset(paddingLeft, paddingTop + chartHeight),
                end = Offset(width - paddingRight, budgetEndY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            val budgetPaint = Paint().apply {
                color = budgetPaceColor.toArgb()
                textSize = 20f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            drawContext.canvas.nativeCanvas.drawText(
                "Budget ৳${monthlyBudget.toInt()}",
                width - paddingRight,
                budgetEndY - 4f,
                budgetPaint
            )
        }

        // 3. Compute Cumulative Points
        val points = mutableListOf<Offset>()
        cumulativeSpend.forEachIndexed { i, cumAmount ->
            val x = paddingLeft + (i * stepX)
            val normalizedSpend = (cumAmount / maxChartVal).coerceIn(0.0, 1.0) * animProgress
            val y = (paddingTop + (chartHeight * (1.0 - normalizedSpend))).toFloat()
            points.add(Offset(x, y))
        }

        // 4. Construct Paths
        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(points[0].x, points[0].y)
        fillPath.moveTo(points[0].x, paddingTop + chartHeight)
        fillPath.lineTo(points[0].x, points[0].y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2
            strokePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }

        fillPath.lineTo(points.last().x, paddingTop + chartHeight)
        fillPath.close()

        // 5. Fill & Stroke
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.35f),
                    primaryColor.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        drawPath(
            path = strokePath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // 6. Selected node
        if (selectedIndex in points.indices) {
            val pt = points[selectedIndex]
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = 10.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = surfaceColor,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = primaryColor,
                radius = 3.5.dp.toPx(),
                center = pt
            )
        }
    }
}
