package com.cloudchef.assignment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ScanMode
import com.cloudchef.assignment.databinding.ActivityMainBinding
import com.cloudchef.assignment.model.DataModel
import com.cloudchef.assignment.util.Constants
import com.cloudchef.assignment.util.DataUtil
import com.cloudchef.assignment.util.PermissionUtil
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket.EVENT_CONNECT
import io.socket.client.Socket.EVENT_CONNECT_ERROR
import io.socket.client.Socket.EVENT_DISCONNECT
import io.socket.client.SocketOptionBuilder
import io.socket.emitter.Emitter.Listener

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CloudChef-MainActivity"
        const val CHART_DATA_AMOUNT = 20
        const val PERMISSION_REQUEST_CODE = 1000
    }

    // View binding
    private lateinit var binding: ActivityMainBinding

    // QR code scanner
    private lateinit var codeScanner: CodeScanner

    // Data Set received from server
    private var socketData: ArrayList<DataModel> = ArrayList()

    // Web Socket Client
    private var socketOption = SocketOptionBuilder.builder()
        .setTimeout(5000)
        .setReconnectionDelay(1000)
        .setReconnectionDelayMax(5000)
        .setReconnectionAttempts(Int.MAX_VALUE)
        .build()

    private var webSocket = IO.socket(Constants.SOCKET_URI, socketOption)

    // Init Websocket Listeners
    private val onConnect = Listener {
        appendLog("\n--------Connection Established--------")
        webSocket.emit(Constants.INIT, lastUpdated)
        runOnUiThread {
            binding.netStatus.setText(R.string.net_connected)
            binding.netConnectingIndicator.visibility = View.GONE
        }
    }

    private val onDisconnect = Listener {
        appendLog("\n--------Socket Disconnected--------")
        runOnUiThread {
            binding.netConnectingIndicator.visibility = View.GONE
        }
    }

    private val onConnectError = Listener { args ->
        appendLog("\n--------Socket Error--------")
        appendLog("$args[0]")
        runOnUiThread {
            binding.netStatus.setText("Socket Error")
            binding.netConnectingIndicator.visibility = View.GONE
        }
    }

    private val onReconnectAttempt = Listener {
        appendLog("\n--------Reconnecting--------")
        runOnUiThread {
            binding.netStatus.setText(R.string.net_connecting)
            binding.netConnectingIndicator.visibility = View.VISIBLE
        }
    }

    private val onData = Listener { args ->
        val message = args[0].toString()
        appendLog("\n--------Socket Data--------\n")
        appendLog(message)
        runOnUiThread { addData(message) }
    }

    // Indicate the last received data
    private var lastUpdated = 0L;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle permission
        val deniedPermission = PermissionUtil.getDeniedPermission(this)
        if (deniedPermission.size != 0) {
            requestPermissions(deniedPermission.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        // Resume QRCode Scanning on resume
        codeScanner.startPreview()
    }

    override fun onPause() {
        // Pause QRCode Scanning before pause
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.disconnect()
        webSocket.off(EVENT_CONNECT, onConnect)
        webSocket.off(EVENT_DISCONNECT, onConnectError)
        webSocket.off(EVENT_CONNECT_ERROR, onConnectError)
        webSocket.off(Manager.EVENT_RECONNECT, onConnect)
        webSocket.off(Manager.EVENT_RECONNECT_ATTEMPT, onReconnectAttempt)
        webSocket.off(Constants.DATA, onData)
    }

    private fun initActivity() {
        // Initialize QRCode View Container
        initQRContainer()

        // Initialize Socket Data View
        initSocketDataView()

        // Connect web socket
        appendLog("\n--------Connecting to the Server--------\n")
        webSocket.on(EVENT_CONNECT, onConnect)
        webSocket.on(EVENT_DISCONNECT, onDisconnect)
        webSocket.on(EVENT_CONNECT_ERROR, onConnectError)
        webSocket.on(Manager.EVENT_RECONNECT, onConnect)
        webSocket.on(Manager.EVENT_RECONNECT_ATTEMPT, onReconnectAttempt)
        webSocket.on(Constants.DATA, onData)
        webSocket.connect()
    }

    private fun initSocketDataView() {
        // Init CharView; Can be replace by any forms to show socket data
        binding.chart1.setViewPortOffsets(0.0f, 0.0f, 0.0f, 0.0f)
        binding.chart1.setBackgroundColor(Color.rgb(75, 43, 127))
        // no description text
        binding.chart1.description.isEnabled = false
        // enable touch gestures
        binding.chart1.setTouchEnabled(true)
        // enable scaling and dragging
        binding.chart1.isDragEnabled = false
        binding.chart1.setScaleEnabled(false)
        // if disabled, scaling can be done on x- and y-axis separately
        binding.chart1.setPinchZoom(false)
        binding.chart1.setDrawGridBackground(false)
        binding.chart1.maxHighlightDistance = 300f
        binding.chart1.xAxis.isEnabled = false

        val y = binding.chart1.axisLeft
        y.setLabelCount(6, false)
        y.textColor = Color.WHITE
        y.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        y.setDrawGridLines(false)
        y.axisLineColor = Color.WHITE

        binding.chart1.axisRight.isEnabled = false
        binding.chart1.legend.isEnabled = false

        // Init Log TextView to Scroll bottom automatically
        binding.logView.movementMethod = ScrollingMovementMethod()
    }

    private fun initQRContainer() {
        // Setup QR code scan engine for the front camera
        codeScanner = CodeScanner(this, binding.scannerView)
        codeScanner.camera = CodeScanner.CAMERA_FRONT // Set the front camera as active
        codeScanner.formats = CodeScanner.ALL_FORMATS // Scan all QR formats
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            // Add scan result to the log view
            appendLog("\n--------QR Code Scan Result--------\n")
            appendLog(it.text)

            // Send the scan result to the server
            if (webSocket.connected())
                webSocket.emit(Constants.QRCODE, it.text)

            runOnUiThread {
                // Restart capture
                Thread.sleep(2000)
                codeScanner.startPreview()
            }
        }
    }

    private fun addData(message: String?) {
        socketData.addAll(DataUtil.parseMessage(message))
        if (socketData.size > 0) {
            lastUpdated = socketData[socketData.size - 1].timestamp
            invalidateChart()
        }
    }

    private fun invalidateChart() {
        // Show the last #CHART_DATA_AMOUNT data on the chart
        if (socketData.size > CHART_DATA_AMOUNT) {
            socketData =
                ArrayList(socketData.subList(socketData.size - CHART_DATA_AMOUNT, socketData.size))
        }

        val dataSet: ArrayList<Entry> = ArrayList()
        for (i in 0 until socketData.size) {
            dataSet.add(Entry(i.toFloat(), socketData[i].value.toFloat()))
        }

        val set1: LineDataSet

        if (binding.chart1.data != null &&
            binding.chart1.data.dataSetCount > 0
        ) {
            set1 = binding.chart1.data.getDataSetByIndex(0) as LineDataSet
            set1.values = dataSet
            binding.chart1.data.notifyDataChanged()
            binding.chart1.notifyDataSetChanged()
        } else {
            // create a dataset and give it a type
            set1 = LineDataSet(dataSet, "DataSet 1")

            set1.axisDependency = AxisDependency.LEFT
            set1.color = Color.rgb(254, 18, 128)
            set1.setCircleColor(Color.WHITE)
            set1.lineWidth = 6f
            set1.circleRadius = 3f
            set1.fillAlpha = 90
            set1.fillColor = Color.rgb(254, 18, 128)
            set1.highLightColor = Color.rgb(254, 18, 128)
            set1.setDrawCircleHole(false)
            set1.setDrawFilled(true)
            set1.mode = LineDataSet.Mode.CUBIC_BEZIER

            // create a data object with the data sets
            val data = LineData(set1)
            data.setValueTextColor(Color.WHITE)
            data.setValueTextSize(9f)

            // set data
            binding.chart1.data = data
        }
        binding.chart1.invalidate()
    }

    private fun appendLog(log: String) {
        runOnUiThread {
            binding.logView.text = StringBuilder(binding.logView.text).append(log).toString()
        }

    }
}