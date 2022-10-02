package dev.tendo.qrcode

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


class Scanner : Activity() {
    private val requestCodeCameraPermission = 1001
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""
    private var scannerJobDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            askForCameraPermission()
        setupControls()
    }
    private fun askForCameraPermission() {
        this.requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            requestCodeCameraPermission
        )
    }

    /**
     * défini ce qu'on va faire de la caméra,
     * en particulier on enregistre la valeur du premier code détécté dans 'scannedValue'
     * puis on sort de la caméra
     */
    private fun setupControls() {
        barcodeDetector =
            BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true) //you should add this feature
            .build()

        val holder = findViewById<SurfaceView>(R.id.cameraSurfaceView).holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    //Start preview after 1s delay
                    if (checkSelfPermission(
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        askForCameraPermission()
                        Thread.sleep(100)
                        surfaceCreated(holder)
                        return
                    }
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                try {
                    if (checkSelfPermission(
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        askForCameraPermission()
                        Thread.sleep(100)
                        surfaceChanged(holder, format, width, height)
                        return
                    }
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })


        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
                Toast.makeText(applicationContext, "Scanner has been closed", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun receiveDetections(detections: Detections<Barcode>) {
                if (scannerJobDone) // don't open several windows
                    finish()

                val barcodes = detections.detectedItems
                var url:String
                if (barcodes.size() == 1) { // on a détecté un unique QRCode
                    scannedValue = barcodes.valueAt(0).rawValue

                    // on repasse à l'ancienne activitée
                    runOnUiThread {
                        cameraSource.stop()

                        Toast.makeText(applicationContext, scannedValue , Toast.LENGTH_SHORT)
                            .show()
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("text", scannedValue)
                        clipboardManager.setPrimaryClip(clipData)

                        url = scannedValue
                        if (scannedValue.startsWith("www", true)) {
                            url = "http://" + scannedValue;
                        }
                        if (url.startsWith("https://", true) || url.startsWith("http://", true)){
                            scannerJobDone = true
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(Intent.createChooser(intent, "Browse with"))
                        }
                        finish()
                    }
                }
            }
        })
    }
}