package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

interface EqualizerController {
    fun getNumberOfBands(): Short
    fun getCenterFrequency(band: Short): Int
    fun getBandLevelRange(): ShortArray
    fun getBandLevel(band: Short): Short
    fun setBandLevel(band: Short, level: Short)
    fun getPresets(): List<String>
    fun usePreset(presetName: String)
    val currentLevels: State<Map<Short, Short>>
}

class MockEqualizerController : EqualizerController {
    private val _bands: Short = 5
    private val _freqs = listOf(60000, 230000, 910000, 3600000, 14000000)
    private val _minLevel: Short = -1500
    private val _maxLevel: Short = 1500
    private val _presets = mapOf(
        "Normal" to listOf<Short>(0, 0, 0, 0, 0),
        "Classical" to listOf<Short>(500, 300, -200, 400, 400),
        "Dance" to listOf<Short>(600, 0, 200, 400, 100),
        "Flat" to listOf<Short>(0, 0, 0, 0, 0),
        "Rock" to listOf<Short>(500, 300, -100, 300, 500)
    )
    private val _levels = mutableStateOf<Map<Short, Short>>((0 until _bands).associate { it.toShort() to 0.toShort() })

    override fun getNumberOfBands() = _bands
    override fun getCenterFrequency(band: Short) = _freqs[band.toInt()]
    override fun getBandLevelRange() = shortArrayOf(_minLevel, _maxLevel)
    override fun getBandLevel(band: Short) = _levels.value[band] ?: 0.toShort()
    
    override fun setBandLevel(band: Short, level: Short) {
        _levels.value = _levels.value.toMutableMap().apply { put(band, level) }
    }
    
    override fun getPresets() = _presets.keys.toList()
    
    override fun usePreset(presetName: String) {
        val levelsList = _presets[presetName] ?: return
        val newMap = mutableMapOf<Short, Short>()
        for (i in 0 until _bands) {
            newMap[i.toShort()] = levelsList[i]
        }
        _levels.value = newMap
    }
    
    override val currentLevels: State<Map<Short, Short>> get() = _levels
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val placeable = measurables.first().measure(
            androidx.compose.ui.unit.Constraints(
                minWidth = h,
                maxWidth = h,
                minHeight = w,
                maxHeight = w
            )
        )
        layout(w, h) {
            placeable.placeWithLayer(
                x = (w - placeable.width) / 2,
                y = (h - placeable.height) / 2,
                layerBlock = {
                    rotationZ = -90f
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerComponent(controller: EqualizerController) {
    val bands = controller.getNumberOfBands()
    val range = controller.getBandLevelRange()
    val minDb = range[0].toFloat() / 100f
    val maxDb = range[1].toFloat() / 100f
    
    val currentLevels by controller.currentLevels

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until bands) {
                val band = i.toShort()
                val freqHz = controller.getCenterFrequency(band) / 1000
                val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}k" else "$freqHz"
                val currentVal = (currentLevels[band] ?: 0).toFloat() / 100f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+${maxDb.toInt()}dB",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp)
                    ) {
                        VerticalSlider(
                            value = currentVal,
                            onValueChange = { controller.setBandLevel(band, (it * 100).toInt().toShort()) },
                            valueRange = minDb..maxDb,
                            modifier = Modifier.width(36.dp).height(160.dp)
                        )
                    }

                    Text(
                        text = "${minDb.toInt()}dB",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        var expanded by remember { mutableStateOf(false) }
        var selectedPreset by remember { mutableStateOf("Normal") }
        val presets = controller.getPresets()
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPreset,
                onValueChange = {},
                readOnly = true,
                label = { Text("Presets") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset) },
                        onClick = {
                            controller.usePreset(preset)
                            selectedPreset = preset
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
