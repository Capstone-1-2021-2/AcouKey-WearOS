package com.example.acoukey

import android.Manifest
import android.app.Activity
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
import android.widget.TextView
import com.github.squti.androidwaverecorder.WaveRecorder
import io.wavebeans.lib.io.input
import io.wavebeans.lib.io.wave
import io.wavebeans.lib.stream.FiniteStream
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.window.window
import org.tensorflow.lite.Interpreter
import java.io.*
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.system.measureTimeMillis
import kotlin.time.*

private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity() {

    private var fileName: String = ""

//    private var recorder: MediaRecorder? = null
//    private var player: MediaPlayer? = null

    private var waveRecorder: WaveRecorder? = null

    // 워치앱으로 학습 데이터 수집할 때
//    private var count: Int = 0
//    private var alphabet = arrayOf('a', 'b', 'c', 'd' ,'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q' ,'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')
//    private var index: Int = 0

    // result TextView와 보여질 String
    private var resultTextView: TextView? = null
    private var resultString: String = ""

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

    @ExperimentalTime
    private fun onRecord(start: Boolean) = if (start) {
        // WAV - 학습 데이터 수집
//       fileName = "${this.externalMediaDirs.first()}/${alphabet[index]}${count}.wav"
        fileName = "${this.externalMediaDirs.first()}/audioFile.wav"
        waveRecorder = WaveRecorder(fileName)
        waveRecorder!!.noiseSuppressorActive = false
        waveRecorder!!.waveConfig.sampleRate = 44100
        waveRecorder!!.waveConfig.channels = AudioFormat.CHANNEL_IN_MONO
        waveRecorder!!.waveConfig.audioEncoding = AudioFormat.ENCODING_PCM_16BIT

        Log.d(LOG_TAG, "fileName=" + fileName)

        waveRecorder?.startRecording()
//        startRecording()

    } else {

        waveRecorder?.stopRecording()

        // 워치앱으로 학습 데이터 수집할 때
//        count++
//        if (count == 10) {
//            count = 0
//            index++
//        } else {
//            // empty
//        }

        val timedValue: TimedValue<ArrayList<Array<Array<Float>>>> = measureTimedValue {
            AudioProcessing(fileName)
        }
        var duration: Duration = timedValue.duration
        val returnResult = timedValue.value
        Log.d("Speed", "speed_AudioProcessing=" + duration.inSeconds)

//        RunModel(returnResult)

        duration = measureTime {
            run_kNN(returnResult)
        }
        Log.d("Speed", "speed_run_kNN=" + duration.inSeconds)

//        stopRecording()
    }

//    private fun startRecording() {
//        recorder = MediaRecorder().apply {
//            setAudioSource(MediaRecorder.AudioSource.MIC)
//
//            // 3GP
////            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
////            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//
//            // M4A
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // HE_AAC? - 근데 Bit rate이 64.0 kbps 이상 안 나오는 듯
//
//            setAudioEncodingBitRate(128*1000) // Bit rate: 128 kbps - to overcome the poor audio quality
//            // https://stackoverflow.com/questions/36984853/mediarecorder-recording-audio-and-video-has-very-low-volume
//
//            // Sample rate: 44.1kHz
//            setAudioSamplingRate(44100)
//
//            setOutputFile(fileName)
//
//            try {
//                prepare()
//            } catch (e: IOException) {
//                Log.e(LOG_TAG, "prepare() failed")
//            }
//
//            start()
//        }
//    }
//
//    private fun stopRecording() {
//        recorder?.apply {
//            stop()
//            release()
//        }
//        recorder = null
//    }
//
//    private fun onPlay(start: Boolean) = if (start) {
//        startPlaying()
//    } else {
//        stopPlaying()
//    }
//
//    private fun startPlaying() {
//        player = MediaPlayer().apply {
//            try {
//                setDataSource(fileName)
//                prepare()
//                start()
//            } catch (e: IOException) {
//                Log.e(LOG_TAG, "prepare() failed")
//            }
//        }
//    }
//
//    private fun stopPlaying() {
//        player?.release()
//        player = null
//    }

    @ExperimentalTime
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

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        setContentView(R.layout.activity_main)

        // UI
        resultTextView = findViewById(R.id.result)

        // 녹음 버튼
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

        // 재생 버튼
//        var mStartPlaying = true
//
//        val testButton: Button = findViewById(R.id.test_button)
//        testButton.setOnClickListener {
//            onPlay(mStartPlaying)
//            testButton.text = when (mStartPlaying) {
//                true -> "Stop"
//                false -> "Test"
//            }
//            mStartPlaying = !mStartPlaying
////            Log.d(LOG_TAG, "mStartPlaying=" + mStartPlaying)
//        }

        // audioFile_abc.wav 테스트
//        fileName = "${this.externalMediaDirs.first()}/audioFile_abc.wav"
//        val returnResult = AudioProcessing(fileName)
//        RunModel(returnResult)
    }

//    override fun onStop() {
//        super.onStop()
//        recorder?.release()
//        recorder = null
//        player?.release()
//        player = null
//    }

    // Peak Detection & Segmentation
    // input: filePath (wav 파일이 위치한 경로)
    // output: returnResult (segmentation된 행렬 목록)
    private fun AudioProcessing(filePath: String): ArrayList<Array<Array<Float>>> {

        // wavebeans로 input 데이터 생성
        val originalData = wave("file://${filePath}")
        val originalList = originalData.asSequence(44100.0f).toList()

        // window form으로 변환
        val dataWindow = originalData.window(256, 64)

        // FFT 적용 -> beanstream<fftSample>이 생성됨 -> 3660개의 구간의 fft결과 발생
        var myFFT = dataWindow.fft(256)

        // segmentation 하기 위해 list형태로 변환
        var FFTList = myFFT.asSequence(44100.0f).toList()

        // 8000~12000구간 주파수에 대하여 시간별 magnitude합 저장
        var totalMag = ArrayList<Double>()
        var totalAvr = ArrayList<Double>()

        // magnitude 계산
        for (i in FFTList) {    // i=특정 시간 구간의 fft결과
            var cnt: Int = 0
            var tempList = ArrayList<Double>()  // 8000~12000구간 주파수의 magnitude 저장될 곳
            var tempFreq = i.frequency().toList()
            var tempMag = i.magnitude().toList()
            for (j in tempMag) {
                var currFreq = tempFreq[cnt]
                if (currFreq >= 8000 && currFreq <= 12000) {
                    if (j.isFinite())
                        tempList.add(j.absoluteValue)
                    else {
                        tempList.add(0.0)
                    }
                }
                cnt += 1
            }
            var average = 0.0
            average = tempList.average()
            if (!average.isNaN()) {
                totalAvr.add(average)
            }
            totalMag.add(tempList.sum())
        }

        // totalMag에 테스트 데이터 기준 3660개의 시간구간에 대한 각각의 magnitude 합이 저장됨
        var totalAverage = 0.0
        totalAverage = totalMag.average() // 전체 평균 -> peak의 기준으로 쓰일 것임.

        // Segmentation으로 잘린 오디오 조각들이 저장될 것임
        var SegResult = ArrayList<FiniteStream<Double>>()

        //Segmentation
        var cnt: Int = 0
        var timecnt = 0 //원본데이터에서의 Sampling index
        for (i in totalMag) {

            if (i <= 0.67 * totalAverage && i > 0.0) { //peak 감지 -> 절댓값이 작을수록 peak임

                var start = FFTList[cnt].time() //nano초 단위임


                if ((start / 1000000000.0) <= (timecnt / 44100.0)) {
                    //이미 처리한 시간대임
                    cnt += 1
                    continue
                }

                if ((start / 1000000000.0) - 0.1 > 0) {
                    start -= 10000000
                }//시작지점의 0.01초 앞에서 자르기


                //시작시점을 기준으로 1.8초를 crop한 결과를 저장
                timecnt = ((((start+10000000) / 1000000000.0) * 44100) + (44100 * 1.8) ).toInt()

                if (timecnt < originalList.size) {
                    //println("[$cnt]$start:$i")
                    SegResult.add(
                        originalList.subList(
                            ((start / 1000000000.0) * 44100).toInt(),
                            timecnt
                        ).input()
                    )
                }
            }

            cnt += 1
        }

        // 행렬들이 담길 배열리스트 -> 나중에 return 값으로 쓰일 예정
        var returnResult = ArrayList<Array<Array<Float>>>()

        // 스펙트로그램이 반전된 모양의 행렬 -> 한 행이 (특정 시점의 주파수에 따른 magnitude 변화)
        for(i in SegResult){

            var tempFFT=i.window(256, 64).fft(256).asSequence(44100.0f).toList()
            var tempArray= Array<Array<Float>>(tempFFT.size){ Array<Float>(tempFFT[0].frequency().toList().size) { 0.0f }}
            var cnt=0
            for (j in tempFFT){
                tempArray[cnt]=(j.magnitude().map{it.toFloat()}.toList().map{ if(!it.isFinite()) -100.0f else it}.slice(47..70).toTypedArray())//tempArray[cnt]=(j.magnitude().map{it.toFloat()}.toList().toTypedArray())
                cnt+=1
            }
            returnResult.add(tempArray)
        }
        Log.d("Result", "returnResult.size=" + returnResult.size)
        Log.d("Result", "done")
//        println(returnResult.size)
//        println("done")

        //행렬 파일(.txt) 생성
//        var resultCnt=0
//        for (i in returnResult) {
//            var outPath="${this.externalMediaDirs.first()}/${resultCnt}.txt"
//            val outfile = File(outPath)
//            val printWriter = PrintWriter(outPath)
//
//            //println(i.size)
//
//            for (j in i.indices) {
//                //println("Array<Double> size:"+i[j].size.toString())
//                for (k in i[j].indices) {
//
//                    if(j==i.size-1 && k==i[j].size-1){
//                        printWriter.print(i[j][k].toString())
//                    }
//                    else{
//                        printWriter.print(i[j][k].toString()+",")
//                    }
//
//                }
//
//            }
//            printWriter.close()
//            resultCnt+=1
//
//        }

        return returnResult
    }

    private fun RunModel(returnResult: ArrayList<Array<Array<Float>>>) {

        resultString = ""   // 이전 결과 초기화

        // segmentation된 returnResult의 size만큼 반복
        for (item in returnResult) {

            // tensorflow-lite 모델

            //모델 준비
            val tflite: Interpreter? = getTfliteInterpreter("cs-m4a-wav.tflite")

            // [1][827][128][1] 사이즈로 모델에 입력
            var input = Array(1) { Array(item.size) { Array(item[0].size) { FloatArray(1) } } }

            // item: Array<Array<Double>>에서 모델의 입력 형식인 input으로 변환
            for (i in item.indices) {
                for (j in item[i].indices) {
                    input[0][i][j][0] = item[i][j]
                }
            }

            // output - 각 알파벳(26개) 별로 추정치가 들어있는 행렬
            var output = Array(1) { FloatArray(26) }

            //모델 예측
            if (tflite != null) {
                tflite.run(input, output)
            }

            // 어떤 알파벳으로 추정했는지
            var max: Float = -1f
            var idx: Int = 0
            output[0].forEachIndexed { index, value ->
                if (value > max) {
                    max = value
                    idx = index
                }
            }

            val alphabet: Char = (97+idx).toChar()

            Log.d("Result", "output[0]=" + output[0].contentToString())
            Log.d("Result", "alphabet=" + alphabet)

            resultString += alphabet
            resultTextView?.text = resultString
        }
    }

    private fun getTfliteInterpreter(modelPath: String): Interpreter? {
        try {
            return Interpreter(loadModelFile(this@MainActivity, modelPath)!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity, modelPath: String): MappedByteBuffer? {
        val fileDescriptor = activity.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun loadCentroid():ArrayList<Array<Array<Float>>> {

        val path2 = "${this.externalMediaDirs.first()}/centroid/" //centroid text파일 폴더 경로
        var result = ArrayList<Array<Array<Float>>>()
        for (alphabet in 'a'..'z') {
            val filename: String = path2 + "$alphabet.txt"
            val file2 = File(filename)

            //text파일에서 centroid 배열 가져오기
            val ls: InputStream = file2.inputStream()
            val lsReader = ls.reader()
            val testStr: List<String> = lsReader.readLines()
            //println(testStr[0].length)
            lsReader.close()
            ls.close()

            var token = testStr[0].split(',');
            //println(token.size)


            val tempArray = token.map { it.toFloat() }.toTypedArray()

            var input = Array<Array<Float>>(1248) { Array<Float>(24) { 0.0f } }

            // item: Array<Array<Double>>에서 모델의 입력 형식인 input으로 변환
            for (i in input.indices) {
                for (j in input[i].indices) {
                    //println(i * 24 + j)
                    input[i][j] = tempArray[i * 24 + j]
                }
            }


            result.add(input)

        }

        return result
    }

    @ExperimentalTime
    fun run_kNN(returnResult: ArrayList<Array<Array<Float>>>){

        var centroid=loadCentroid()

        resultString = ""   // 이전 결과 초기화

        // word_correction을 위한 후보 알파벳 모음
        var candidatesAll = ArrayList<CharArray>()

        for (item in returnResult) { //for문 한 번에 한글자씩 예측

            var distAll: MutableMap<Char, Float> = mutableMapOf('a' to 0f)

            var minDist=0.0f
            var minIdx = 0
            for (j in centroid.indices){
                //for문 한 번에 centroid 하나와의 거리 계산
                var distList= mutableListOf<Float>(0.0f)
                for(row in centroid[j].indices){
                    for(col in centroid[j][row].indices){
                        distList.add(abs(centroid[j][row][col]-item[row][col]))
                    }
                }
                val dist=distList.sum()

                // 각 centroid와의 거리값을 보관
                val chr = (97+j).toChar()
                distAll.put(chr, dist)

                if(j==0){
                    minDist=dist
                    minIdx=j
                }
                else{
                    if(minDist>dist){
                        minDist=dist
                        minIdx=j
                    }
                }
            }

            // 알파벳 별로 예측치를 뽑기
            distAll = distAll.toList().sortedBy { it.second }.toMap().toMutableMap()
            Log.d("Result", "distAll=" + distAll)

            var depth = 3   // 3순위까지 본다
            var candidates = CharArray(depth)
            distAll.onEachIndexed loop@ { index, entry ->
                if (index >= depth) return@loop
                candidates[index] = entry.key
            }

            candidatesAll.add(candidates)

            val alphabet: Char = (97+minIdx).toChar() //minIdx에 예측된 알파벳 인덱스가 저장됨
            Log.d("Result", "alphabet=" + alphabet)

            // word_correction 없이 바로 화면에 출력할 때
            resultString += alphabet
        }

        // word_correction
        if (returnResult.size >= 3) {
            // 시간 측정
            val timedValue: TimedValue<String> = measureTimedValue {
                word_correction(candidates = candidatesAll, num = returnResult.size)
            }
            val duration: Duration = timedValue.duration
            resultString = timedValue.value

            Log.d("Speed", "speed_word_correction=" + duration.inSeconds)
        }

        resultTextView?.text = resultString

        // 나중에 resultString을 반환하는 함수로 만드는 것이 더 좋겠음
        // 최종적으로 resultTextView에 반영하는 것은 onRecord에서..?
    }

    fun word_correction(candidates: ArrayList<CharArray>, num: Int): String {

        val word_path = "${this.externalMediaDirs.first()}/word${num}.txt" // * letter words dictionary 파일 경로

        val file = File(word_path)

        val ls: InputStream = file.inputStream()
        val lsReader = ls.reader()
        val wordDictionary: List<String> = lsReader.readLines() // List는 검색에 느리다?
        lsReader.close()
        ls.close()

        // words
        var words = Array<String>((3f).pow(num).toInt(), {_->""})

        // 최적화 필요.. cartesian product
        var idx = 0
        candidates[0].forEach { w ->
            candidates[1].forEach { x ->
                candidates[2].forEach { y ->
                    if (num >= 4) {
                        candidates[3].forEach { z ->
                            words[idx] = "$w$x$y$z"
                            idx++
                        }
                    } else {
                        words[idx] = "$w$x$y"
                        idx++
                    }
                }
            }
        }

        var result: String = "" // 반환할 결과 문자열

        var flag = false
        words.forEach { word ->
            if (wordDictionary.contains(word)) {
                result = word // 만약 correction 될 수 있는 단어가 2개 이상이면?
                flag = true
            }
        }

        if (!flag) {
            // 만약 사전에 없다면 가장 높게 추정한 알파벳의 조합으로..
            candidates.forEach { candidate ->
                result += candidate[0]
            }
        }

        return result
    }
}