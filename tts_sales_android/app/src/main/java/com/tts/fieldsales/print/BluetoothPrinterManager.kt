package com.tts.fieldsales.print

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnect()
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothAdapter?.cancelDiscovery()
            s.connect()
            socket = s
            outputStream = s.outputStream
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching {
            outputStream?.close()
            socket?.close()
            outputStream = null
            socket = null
        }
    }

    val isConnected: Boolean get() = socket?.isConnected == true

    suspend fun sendBytes(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = outputStream ?: throw IOException("Printer not connected")
            out.write(data)
            out.flush()
        }
    }

    // Paper width in mm: 58mm = 3inch, 80mm = 4inch
    fun getPaperWidthMm(paperSetting: String): Int = when (paperSetting) {
        "4inch" -> 80
        else -> 58 // default 3inch
    }
}
