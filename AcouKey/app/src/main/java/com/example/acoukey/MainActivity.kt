package com.example.acoukey

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
// import android.support.v4.app.ActivityCompat
import androidx.core.app.ActivityCompat
// import android.support.v7.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import com.github.squti.androidwaverecorder.WaveRecorder
import io.wavebeans.lib.io.wave
import java.io.IOException

private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity() {

    private var fileName: String = ""

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var waveRecorder: WaveRecorder? = null

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> =
        arrayOf(Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onRecord(start: Boolean) = if (start) {
        waveRecorder?.startRecording()
//        startRecording()
    } else {
        waveRecorder?.stopRecording()
//        stopRecording()
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)

            // 3GP
//            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            
            // M4A
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // HE_AAC? - 근데 Bit rate이 64.0 kbps 이상 안 나오는 듯

            setAudioEncodingBitRate(128*1000) // Bit rate: 128 kbps - to overcome the poor audio quality
            // https://stackoverflow.com/questions/36984853/mediarecorder-recording-audio-and-video-has-very-low-volume

            // Sample rate: 44.1kHz
            setAudioSamplingRate(44100)

            setOutputFile(fileName)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Record to the external cache directory for visibility
//        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        // externalCacheDir를 사용하는 경우
        // ls:/storage/emulated/0/Android/data/: Permission denied

//        fileName = "${this.externalMediaDirs.first()}/audiorecordtest.m4a"
        // externalMediaDirs를 사용하는 경우
        // /storage/emulated/0/Android/media/com.example.acoukey/audiorecordtest.3gp 로 접근하거나
        // /storage/self/primary/Android/media/com.example.acoukey/audiorecordtest.3gp 로 접

        // 접근하는 방법은 View -> Tool Windows -> Device File Explorer

        // 아래 2가지 방법은 E/AudioRecordTest: prepare() failed 발생 -> java.lang.IllegalStateException
//        fileName = "${Environment.getExternalStorageDirectory().absolutePath}/audiorecordtest.3gp"
//        fileName = "/sdcard/audiorecordtest.3gp"

        // WAV
        fileName = "${this.externalMediaDirs.first()}/audioFile.wav"
        waveRecorder = WaveRecorder(fileName)
        waveRecorder!!.noiseSuppressorActive = false
        waveRecorder!!.waveConfig.sampleRate = 44100
        waveRecorder!!.waveConfig.channels = AudioFormat.CHANNEL_IN_MONO
        waveRecorder!!.waveConfig.audioEncoding = AudioFormat.ENCODING_PCM_16BIT

        Log.d(LOG_TAG, "fileName=" + fileName)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        setContentView(R.layout.activity_main)

        // UI
        var mStartRecording = true

        val recordButton: Button = findViewById(R.id.record_button)
        recordButton.setOnClickListener {
            onRecord(mStartRecording)
            recordButton.text = when (mStartRecording) {
                true -> "Stop"
                false -> "Start"
            }
            mStartRecording = !mStartRecording
//            Log.d(LOG_TAG, "mStartRecording=" + mStartRecording)
        }

        var mStartPlaying = true

        val testButton: Button = findViewById(R.id.test_button)
        testButton.setOnClickListener {
            onPlay(mStartPlaying)
            testButton.text = when (mStartPlaying) {
                true -> "Stop"
                false -> "Test"
            }
            mStartPlaying = !mStartPlaying
//            Log.d(LOG_TAG, "mStartPlaying=" + mStartPlaying)
        }
    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }
}