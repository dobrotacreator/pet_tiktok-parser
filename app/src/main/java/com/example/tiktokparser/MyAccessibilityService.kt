package com.example.tiktokparser

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class MyAccessibilityService : AccessibilityService() {
    private val stepHandlers: Map<Step, (AccessibilityNodeInfo) -> Boolean> = mapOf(
        Step.START to ::handleStart,
        Step.OPEN_SEARCH_TAB to ::handleOpenSearchTab,
        Step.SEARCH to ::handleSearch,
        Step.SHARE to ::handleShare,
        Step.MORE to ::handleMore,
        Step.URL to ::handleUrl,
    )

    enum class Step {
        START, OPEN_SEARCH_TAB, SEARCH, SHARE, MORE, URL;

        fun next(): Step = when (this) {
            START -> OPEN_SEARCH_TAB
            OPEN_SEARCH_TAB -> SEARCH
            SEARCH -> START
            SHARE -> MORE
            MORE -> URL
            URL -> START
        }
    }

    private var currentStep = Step.START
    private var retryCount = 0
    private val maxRetries = 5
    private var isRestarting = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (isRestarting) {
            Log.d("TikTokParser", "Пропуск события — идёт перезапуск TikTok")
            return
        }
        val root = rootInActiveWindow ?: return

        if (retryCount > maxRetries) {
            Log.e(
                "TikTokParser", "Превышено число попыток на шаге $currentStep. Перезапускаю TikTok."
            )
            restartTiktok()
            return
        }

        Log.d("TikTokParser", "Поиск обработчика $currentStep, попытка $retryCount")
        val handler = stepHandlers[currentStep]
        if (handler != null) {
            val success = handler(root)
            if (success) {
                retryCount = 0
                currentStep = currentStep.next()
            } else {
                Log.d("TikTokParser", "Ошибка на $currentStep, попытка $retryCount")
                retryCount++
                sleepRandom(5000, 6000)
            }
        }
    }

    private fun handleStart(root: AccessibilityNodeInfo): Boolean {
        Log.d("TikTokParser", "Handle START")
        sleepRandom(5000, 6000)
        return true
    }

    private fun handleOpenSearchTab(root: AccessibilityNodeInfo): Boolean {
        Log.d("TikTokParser", "Handle SEARCH")
        clickSearchImage(root)
        sleepRandom(5000, 6000)
        return true
    }

//    private fun handleInput(root: AccessibilityNodeInfo): Boolean {
//        Log.d("TikTokParser", "Handle INPUT")
//        val inputNodes = findAllNodesByClass(root, "android.widget.EditText")
//        if (inputNodes.isNotEmpty()) {
//            val inputNode = inputNodes[0]
//            setTextToInputField(inputNode, "мем")
//            sleepRandom(5000, 6000)
////            root.performAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
////            inputNode.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
////            inputNode.performAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION)
////            inputNode.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
////            inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
////            if (inputNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) {
////                Log.d("TikTokParser", "Поиск инициирован с помощью ACTION_IME_ENTER")
////                sleepRandom(5000, 6000)
////                return true
////            } else {
////                Log.w("TikTokParser", "Не удалось выполнить ACTION_IME_ENTER")
////                return false
////            }
//            // performGlobalAction(GLOBAL_ACTION_BACK)
//            inputNode.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
//            sleepRandom(5000, 6000)
//            return true
//        } else {
//            Log.w("TikTokParser", "Поле ввода не найдено — ввод текста пропущен")
//            return false
//        }
//    }

    private fun handleSearch(root: AccessibilityNodeInfo): Boolean {
        performGlobalAction(GLOBAL_ACTION_BACK)
        root.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
        sleepRandom(1000, 1100)
        logAllNodes(root)
        val node = root.getChild(5).getChild(0)
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.d("TikTokParser", "${node.className} ${node.contentDescription} ${node.text}")
//        if (clickByText(root, "мем")) {
//            sleepRandom(5000, 6000)
//            return true
//        } else {
//            Log.w("TikTokParser", "Недавний поиск с текстом 'мем' не найден")
//            return false
//        }
        return true
    }

    private fun handleShare(root: AccessibilityNodeInfo): Boolean {
        Log.d("TikTokParser", "Handle SHARE")
        if (clickByDescription(root, "Share")) {
            Log.d("TikTokParser", "Шаг 1: Нажали Share")
            sleepRandom(5000, 6000)
            return true
        } else {
            return false
        }
    }

    private fun handleMore(root: AccessibilityNodeInfo): Boolean {
        Log.d("TikTokParser", "Handle MORE")
        if (clickByDescription(root, "More")) {
            Log.d("TikTokParser", "Шаг 2: Нажали More")
            sleepRandom(5000, 6000)
            return true
        } else {
            return false
        }
    }

    private fun handleUrl(root: AccessibilityNodeInfo): Boolean {
        Log.d("TikTokParser", "Handle URL")
        val url = findFirstUrl(root)
        if (url != null) {
            Log.d("TikTokParser", "Шаг 3: Достали URL и запустили отправку")
            extractTiktokVideo(url)
            sleepRandom(60000, 61000)
            return true
        } else {
            return false
        }
    }

    private fun clickSearchImage(root: AccessibilityNodeInfo): Boolean {
        val frameLayouts = findAllNodesByClass(root, "android.widget.FrameLayout")
        for (frame in frameLayouts) {
            var scrollViewFound = false
            var imageClicked = false

            for (i in 0 until frame.childCount) {
                val child = frame.getChild(i) ?: continue

                if (!scrollViewFound && child.className == "android.widget.HorizontalScrollView") {
                    if (hasExpectedTabsInScrollView(child)) {
                        scrollViewFound = true
                    }
                } else if (scrollViewFound && child.className == "android.widget.ImageView" && child.isVisibleToUser) {
                    imageClicked = child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (imageClicked) {
                        Log.d(
                            "TikTokParser",
                            "Клик по ImageView после нужного HorizontalScrollView успешен"
                        )
                        return true
                    }
                }
            }
        }
        Log.d(
            "TikTokParser",
            "Не удалось найти подходящий ImageView после нужного HorizontalScrollView"
        )
        return false
    }

    private fun hasExpectedTabsInScrollView(scrollView: AccessibilityNodeInfo): Boolean {
        val expected = listOf("Explore", "Following", "Shop", "For You")
        val actual = mutableListOf<String>()

        fun collectText(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.className == "android.widget.TextView" && node.text != null) {
                actual.add(node.text.toString().trim())
            }
            for (i in 0 until node.childCount) {
                collectText(node.getChild(i))
            }
        }

        collectText(scrollView)

        val indexStart = actual.indexOf("Explore")
        if (indexStart == -1 || indexStart + expected.size > actual.size) return false

        return actual.subList(indexStart, indexStart + expected.size) == expected
    }

    private fun findAllNodesByClass(
        root: AccessibilityNodeInfo, className: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.className == className) result.add(node)
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }
        traverse(root)
        return result
    }

    private fun clickByDescription(node: AccessibilityNodeInfo, description: String): Boolean {
        val list = mutableListOf<AccessibilityNodeInfo>()
        findNodesByContentDescription(node, description, list)
        for (n in list) {
            if (n.isClickable) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    private fun clickByText(node: AccessibilityNodeInfo, text: String): Boolean {
        val list = mutableListOf<AccessibilityNodeInfo>()
        findNodesByText(node, text, list)
        for (n in list) {
            if (n.isClickable) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    private fun findNodesByContentDescription(
        node: AccessibilityNodeInfo, description: String, result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.contentDescription != null && node.contentDescription.toString()
                .contains(description)
        ) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            findNodesByContentDescription(node.getChild(i), description, result)
        }
    }

    private fun findNodesByText(
        node: AccessibilityNodeInfo, text: String, result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.text != null && node.text.toString().contains(text)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            findNodesByText(node.getChild(i), text, result)
        }
    }

    fun setTextToInputField(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.className == "android.widget.EditText" && node.isFocusable && node.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d("TikTokParser", "Ввод текста в поле: ${if (success) "успешен" else "не удался"}")
            return success
        }

        for (i in 0 until node.childCount) {
            if (setTextToInputField(node.getChild(i), text)) return true
        }

        return false
    }

    private fun findFirstUrl(root: AccessibilityNodeInfo): String? {
        val urls = mutableListOf<AccessibilityNodeInfo>()
        findTextMatching(root, Regex("https?://[^\\s]+"), urls)
        return urls.firstOrNull()?.text?.toString()
    }

    private fun findTextMatching(
        node: AccessibilityNodeInfo?, regex: Regex, result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return
        val text = node.text?.toString() ?: ""
        if (regex.containsMatchIn(text)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            findTextMatching(node.getChild(i), regex, result)
        }
    }

    private fun resolveFinalUrl(shortUrl: String): String? {
        return try {
            val url = URL(shortUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connect()
            connection.getHeaderField("Location") ?: shortUrl
        } catch (e: Exception) {
            Log.e("TikTokParser", "Ошибка при разрешении редиректа", e)
            null
        }
    }

    private fun extractTiktokVideo(url: String) {
        Thread {
            val fullUrl = resolveFinalUrl(url)
            if (fullUrl != null) {
                Log.d("TikTokParser", "Полный URL: $fullUrl")
                try {
                    val outputFile = File(cacheDir, "tiktok_video.mp4")
                    if (outputFile.exists()) outputFile.delete()
                    val outputPath = outputFile.absolutePath
                    val request = YoutubeDLRequest(fullUrl).apply { addOption("-o", outputPath) }
                    YoutubeDL.getInstance().execute(request) { progress, eta, line ->
                        Log.d("TikTokParser", "Progress: $progress%, ETA: $eta, Line: $line")
                    }
                    val videoFile = File(outputPath)
                    if (videoFile.exists()) {
                        Log.d("TikTokParser", "Видео скачано: ${videoFile.length()} байт")
                        processVideo(videoFile)
                    }
                } catch (e: Exception) {
                    Log.e("YT-DLP", "Ошибка при скачивании", e)
                }
            } else {
                Log.d("TikTokParser", "Не удалось получить полный вариант ссылки")
            }
        }.start()
    }

    private fun processVideo(rawVideo: File) {
        val outputFile = File(cacheDir, "unique_video.mp4")
        if (outputFile.exists()) outputFile.delete()

        val inputPath = rawVideo.absolutePath
        val outputPath = outputFile.absolutePath

        val cmd = arrayOf(
            "-i",
            inputPath,
            "-vf",
            "eq=contrast=1.01:brightness=0.02:saturation=1.05," + "noise=alls=10:allf=t+u",
            "-c:v",
            "libx264",
            "-preset",
            "ultrafast",
            "-crf",
            "23",
            "-map_metadata",
            "-1",
            "-y",
            outputPath
        )

        val rc = FFmpegKit.execute(cmd.joinToString(" "))
        if (rc.returnCode.isValueSuccess) {
            Log.d("TikTokParser", "Видео успешно уникализировано")
            rawVideo.delete()
            sendVideoToTelegram(outputFile)
            outputFile.delete()
        } else {
            Log.e("TikTokParser", "Ошибка при уникализации видео: ${rc.returnCode}")
        }
    }

    private fun sendVideoToTelegram(videoFile: File) {
        val botToken = ""
        val chatId = ""
        val apiUrl = "https://api.telegram.org/bot$botToken/sendDocument"

        val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            doOutput = true
            connectTimeout = 30000
            readTimeout = 30000
        }

        DataOutputStream(connection.outputStream).use { output ->
            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n$chatId\r\n")

            output.writeBytes("--$boundary\r\n")
            output.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"${videoFile.name}\"\r\n")
            output.writeBytes("Content-Type: application/octet-stream\r\n\r\n")

            FileInputStream(videoFile).use { input ->
                input.copyTo(output)
            }

            output.writeBytes("\r\n--$boundary--\r\n")
        }

        val responseCode = connection.responseCode
        Log.d("TikTokParser", "Код ответа Telegram: $responseCode")

        if (responseCode == 200) {
            Log.d("TikTokParser", "Файл успешно отправлен")
        } else {
            Log.e("TikTokParser", "Ошибка: ${connection.responseMessage}")
        }
    }

    private fun resetSteps() {
        currentStep = Step.START
        retryCount = 0
    }

    private fun restartTiktok() {
        isRestarting = true

        Thread {
            for (i in 1..10) {
                Log.d("TikTokParser", "Нажатие назад #$i")
                performGlobalAction(GLOBAL_ACTION_BACK)
                Thread.sleep(800)
                if (isNotInTikTok()) {
                    break
                }
            }

            val intent = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                startActivity(intent)
                Log.d("TikTokParser", "TikTok перезапущен")
            } else {
                Log.e("TikTokParser", "Не удалось получить Intent для TikTok")
            }

            Thread.sleep(4000)
            resetSteps()
            isRestarting = false
        }.start()
    }

    private fun isNotInTikTok(): Boolean {
        val pkg = rootInActiveWindow?.packageName?.toString()
        return pkg != "com.zhiliaoapp.musically"
    }

    private fun sleepRandom(minMillis: Long = 300, maxMillis: Long = 1000) {
        val delay = (minMillis..maxMillis).random()
        Log.d("TikTokParser", "Пауза $delay мс")
        Thread.sleep(delay)
    }

    private fun logAllNodes(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return

        val indent = "  ".repeat(depth)
        Log.d(
            "TikTokParser",
            "$indent[${node.className}] " + "text='${node.text}' " + "desc='${node.contentDescription}' " + "id='${node.viewIdResourceName}' "
        )

        for (i in 0 until node.childCount) {
            logAllNodes(node.getChild(i), depth + 1)
        }
    }

    override fun onInterrupt() {}
}
