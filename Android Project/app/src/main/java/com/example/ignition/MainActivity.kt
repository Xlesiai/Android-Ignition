package com.example.ignition

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ignition.ui.theme.IgnitionTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity(), SensorEventListener {
    var sensorData = mutableStateMapOf<String, ArrayDeque<*>>()
    val sensorManager: SensorManager by lazy {
        getSystemService(SENSOR_SERVICE) as SensorManager
    }
    val maxGraphPoints = 5
    val mqttHelper = SimpleMqttHelper(BuildConfig.MQTT_BROKER_URI)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IgnitionTheme(
                darkTheme = false
            ) {
                val navController = rememberNavController()
                val modifier = Modifier.padding(8.dp)
                val permission =
                    rememberPermissionState(android.Manifest.permission.ACTIVITY_RECOGNITION)



                when {
                    permission.status.isGranted -> {
                        // If permission is already granted, do something
                        // Sensors
                        registerSensors()

                    }

                    else -> {
                        BasicAlertDialog(
                            onDismissRequest = {
                                permission.launchPermissionRequest()
                            },
                            properties = DialogProperties(),
                            content = {
                                Surface(
                                    modifier = Modifier.size(150.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Text("Permission required")
                                        Button(
                                            onClick = {
                                                permission.launchPermissionRequest()
                                            },
                                            content = {
                                                Text("Grant Permission")
                                            }
                                        )

                                    }
                                }

                            }
                        )
                    }
                }


                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(navController, modifier)
                    }
                    composable("ignition") {
                        IgnitionScreen(sensorData, modifier)
                    }

                }

            }
        }
    }

    private fun registerSensors() {
        val sensors = listOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_GRAVITY
        )

        sensors.forEach { type ->
            sensorManager.getDefaultSensor(type)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onResume() {
        super.onResume()
        registerSensors()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { e ->
            when (e.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val data = e.values.toList()
                    val vector = Vector3D(data[0], data[1], data[2])
                    val name = "Accelerometer"
                    val current = sensorData[name] ?: ArrayDeque<Vector3D>()
                    val updated = ArrayDeque(current)
                    if (updated.size >= maxGraphPoints) updated.removeFirst()
                    updated.add(vector)

                    sensorData[name] = updated

                    try {
                        mqttHelper.publish("Pixel6a/${name}/x", "${vector.x}")
                        mqttHelper.publish("Pixel6a/${name}/y", "${vector.y}")
                        mqttHelper.publish("Pixel6a/${name}/z", "${vector.z}")

                    }
                    catch (ex: Exception) {
                        Log.e("MQTT", "Error publishing message", ex)
                    }

                }

                Sensor.TYPE_GYROSCOPE -> {
                    val data = e.values.toList()
                    val vector = Vector3D(data[0], data[1], data[2])
                    val name = "Gyroscope"
                    val current = sensorData[name] ?: ArrayDeque<Vector3D>()
                    val updated = ArrayDeque(current)
                    if (updated.size >= maxGraphPoints) updated.removeFirst()
                    updated.add(vector)

                    sensorData[name] = updated

                    try {
                        mqttHelper.publish("Pixel6a/${name}/x", "${vector.x}")
                        mqttHelper.publish("Pixel6a/${name}/y", "${vector.y}")
                        mqttHelper.publish("Pixel6a/${name}/z", "${vector.z}")

                    }
                    catch (ex: Exception) {
                        Log.e("MQTT", "Error publishing message", ex)
                    }
                }

                Sensor.TYPE_PRESSURE -> {
                    val data = e.values.toList()[0]
                    val name = "Pressure"
                    val current = sensorData[name] ?: ArrayDeque<Float>()
                    val updated = ArrayDeque(current)
                    if (updated.size >= maxGraphPoints) updated.removeFirst()
                    updated.add(data)

                    sensorData[name] = updated

                    try {
                        mqttHelper.publish("Pixel6a/${name}/x", "$data")

                    }
                    catch (ex: Exception) {
                        Log.e("MQTT", "Error publishing message", ex)
                    }
                }

                Sensor.TYPE_LIGHT -> {
                    val data = e.values.toList()[0]
                    val name = "Light"
                    val current = sensorData[name] ?: ArrayDeque<Float>()
                    val updated = ArrayDeque(current)
                    if (updated.size >= maxGraphPoints) updated.removeFirst()
                    updated.add(data)

                    sensorData[name] = updated

                    try {
                        mqttHelper.publish("Pixel6a/${name}/x", "$data")

                    }
                    catch (ex: Exception) {
                        Log.e("MQTT", "Error publishing message", ex)
                    }
                }

                Sensor.TYPE_GRAVITY -> {
                    val data = e.values.toList()[0]
                    val name = "Gravity"
                    val current = sensorData[name] ?: ArrayDeque<Float>()
                    val updated = ArrayDeque(current)
                    if (updated.size >= maxGraphPoints) updated.removeFirst()
                    updated.add(data)

                    sensorData[name] = updated

                    try {
                        mqttHelper.publish("Pixel6a/${name}/x", "$data")

                    }
                    catch (ex: Exception) {
                        Log.e("MQTT", "Error publishing message", ex)
                    }
                }

            }
        }
    }


    @Composable
    fun HomeScreen(
        navController: androidx.navigation.NavController,
        modifier: Modifier = Modifier
    ) {
        Scaffold(
            modifier = modifier,
            content = {
                Column(modifier = modifier.verticalScroll(rememberScrollState())) {
                    Image(
                        painter = painterResource(id = R.drawable.ignition_logo),
                        contentDescription = "ignition logo",
                        modifier = modifier.padding(it)
                    )
                    Text(
                        text = "What is Ignition?",
                        modifier = modifier.padding(it)
                    )
                    Text(
                        text = "Ignition is a powerful, modern SCADA (Supervisory Control and Data Acquisition) platform used across industries to connect devices, collect real-time data, and build interactive dashboards. It allows engineers and developers to monitor, control, and analyze systems efficiently, from factory floors to energy fields.",
                        modifier = modifier.padding(it)
                    )
                    Text(
                        text = "How I’m Using It in My App",
                        modifier = modifier.padding(it)
                    )
                    Text(
                        text = "In this project, I built an Android application in Kotlin that streams live sensor data from my phone (such as GPS, battery, and accelerometer readings) directly into Ignition. Inside Ignition, I created a dashboard that visualizes this data in real time, adds alarms for thresholds, and logs historical trends for analysis." +
                                "\n" + "This integration demonstrates how mobile devices can act as data sources for SCADA systems, bridging everyday hardware with industrial-grade monitoring tools.",
                        modifier = modifier.padding(it)
                    )
                }
            },
            floatingActionButton = {
                Button(
                    onClick = { navController.navigate("ignition") },
                    content = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "arrow forward"
                        )
                    }
                )
            }
        )


    }

    @Composable
    fun IgnitionScreen(
        sensorData: SnapshotStateMap<String, ArrayDeque<*>>,
        modifier: Modifier = Modifier
    ) {
        val size = 200.dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            Text("Sensors", modifier = modifier.padding(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(size),
                modifier = modifier,
                content = {
                    items(items = sensorData.keys.toList()) { sensor ->
                        Surface(
                            modifier = Modifier.size(size, size)
                        ) {
                            val data: ArrayDeque<*> = sensorData[sensor] ?: return@Surface

                            when (data.firstOrNull()) {
                                is Vector3D -> {

                                    Graph(
                                        sensor,
                                        data as ArrayDeque<Vector3D>
                                    )
                                }

                                is Float -> {

                                    Graph1D(
                                        sensor,
                                        data as ArrayDeque<Float>
                                    )
                                }
                            }

                        }
                    }
                }
            )
        }

    }


    @Composable
    fun Graph(title: String, data: ArrayDeque<Vector3D>) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(8.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                onDraw = {
                    val width = size.width
                    val height = size.height
                    val maxPoints = data.size
                    if (maxPoints == 0) return@Canvas

                    // Extract x, y, z components
                    val xPoints = data.map { it.x }
                    val yPoints = data.map { it.y }
                    val zPoints = data.map { it.z }

                    // Find max absolute value for scaling
                    val maxVal =
                        (xPoints + yPoints + zPoints).maxOfOrNull { abs(it) }?.coerceAtLeast(1f)
                            ?: 1f

                    // Helper to convert value to canvas y-coordinate
                    fun mapY(value: Float) = height - ((value + maxVal) / (2 * maxVal) * height)

                    // Draw X component
                    val pathX = Path().apply {
                        moveTo(0f, mapY(xPoints[0]))
                        xPoints.forEachIndexed { i, v ->
                            lineTo(i * width / maxPoints, mapY(v))
                        }
                    }
                    drawPath(pathX, color = Color.Red, style = Stroke(3.dp.toPx()))

                    // Draw Y component
                    val pathY = Path().apply {
                        moveTo(0f, mapY(yPoints[0]))
                        yPoints.forEachIndexed { i, v ->
                            lineTo(i * width / maxPoints, mapY(v))
                        }
                    }
                    drawPath(pathY, color = Color.Green, style = Stroke(3.dp.toPx()))

                    // Draw Z component
                    val pathZ = Path().apply {
                        moveTo(0f, mapY(zPoints[0]))
                        zPoints.forEachIndexed { i, v ->
                            lineTo(i * width / maxPoints, mapY(v))
                        }
                    }
                    drawPath(pathZ, color = Color.Blue, style = Stroke(3.dp.toPx()))

                    // Draw axes
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, height / 2),
                        end = Offset(width, height / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, 0f),
                        end = Offset(0f, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            )
        }
    }


    @Composable
    fun Graph1D(title: String, data: ArrayDeque<Float>) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(8.dp)
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                onDraw = {
                    val width = size.width
                    val height = size.height
                    val maxPoints = data.size
                    if (maxPoints == 0) return@Canvas

                    // Extract x, y, z components
                    val points = data.toList()

                    // Find max absolute value for scaling
                    val maxVal = points.maxOfOrNull { abs(it) }?.coerceAtLeast(1f) ?: 1f

                    // Helper to convert value to canvas y-coordinate
                    fun mapY(value: Float) = height - ((value + maxVal) / (2 * maxVal) * height)

                    // Draw
                    val pathX = Path().apply {
                        moveTo(0f, mapY(points[0]))
                        points.forEachIndexed { i, v ->
                            lineTo(i * width / maxPoints, mapY(v))
                        }
                    }
                    drawPath(pathX, color = Color.Red, style = Stroke(3.dp.toPx()))

                    // Draw axes
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, height / 2),
                        end = Offset(width, height / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, 0f),
                        end = Offset(0f, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            )
        }
    }
}


