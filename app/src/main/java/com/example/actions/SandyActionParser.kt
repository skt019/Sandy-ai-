package com.example.actions

object SandyActionParser {

    /**
     * Parses user command into a structured SandyAction if an action intent is present.
     */
    fun parseAction(input: String): SandyAction? {
        val trimmed = input.trim()
        val cleaned = trimmed
            .replace(Regex("^(hey|hi|hello|ok)?\\s*sandy[,\\s]*", RegexOption.IGNORE_CASE), "")
            .trim()

        val lower = cleaned.lowercase()

        // 1. WhatsApp Intent
        if (lower.contains("whatsapp")) {
            val messagePart = extractWhatsAppDetails(cleaned)
            return SandyAction.WhatsApp(
                recipient = messagePart.first,
                message = messagePart.second
            )
        }

        // 2. Phone Call Intent
        if (lower.startsWith("call ") || lower.startsWith("phone call ") || lower.startsWith("dial ")) {
            val target = cleaned
                .replace(Regex("^(call|phone call|dial)\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
            val phoneOnly = target.filter { it.isDigit() || it == '+' }
            val number = if (phoneOnly.length >= 3) phoneOnly else target
            return SandyAction.Call(phoneNumber = number, contactName = target)
        }

        // 3. SMS Intent
        if (lower.startsWith("send sms") || lower.startsWith("sms ") || lower.startsWith("text ")) {
            val (recipient, msg) = extractSmsDetails(cleaned)
            return SandyAction.Sms(recipient = recipient, message = msg)
        }

        // 4. YouTube Intent
        if (lower.contains("youtube") || lower.startsWith("play ")) {
            val query = cleaned
                .replace(Regex("^(search youtube for|play on youtube|play|open youtube and play|youtube)\\s+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+on youtube$", RegexOption.IGNORE_CASE), "")
                .trim()
            return SandyAction.YouTube(query = query.ifBlank { "Trending" })
        }

        // 5. Alarm Intent
        if (lower.contains("alarm") || lower.startsWith("wake me up")) {
            val (hour, minute) = extractTime(lower)
            return SandyAction.Alarm(hour = hour, minute = minute, label = "Sandy Alarm")
        }

        // 6. Timer Intent
        if (lower.contains("timer")) {
            val seconds = extractTimerSeconds(lower)
            return SandyAction.Timer(seconds = seconds, label = "Sandy Timer")
        }

        // 7. Note Intent ("note this down", "take a note", "remember that")
        if (lower.startsWith("note this down") || lower.startsWith("take a note") || lower.startsWith("note ") || lower.startsWith("remember that")) {
            val noteContent = cleaned
                .replace(Regex("^(note this down[:\\s]*|take a note[:\\s]*|note[:\\s]*|remember that[:\\s]*)", RegexOption.IGNORE_CASE), "")
                .trim()
            if (noteContent.isNotBlank()) {
                val title = if (noteContent.length > 25) noteContent.take(25) + "..." else noteContent
                return SandyAction.Note(title = title, content = noteContent)
            }
        }

        // 8. Camera Intent
        if (lower.contains("open camera") || lower.contains("take picture") || lower.contains("take photo") || lower.contains("look at this with camera")) {
            val question = cleaned
                .replace(Regex("^(open camera|take picture|take photo|look at this with camera)[:\\s]*", RegexOption.IGNORE_CASE), "")
                .trim()
            return SandyAction.CameraQuestion(question = question.ifBlank { "Describe what you see in this image" })
        }

        return null
    }

    private fun extractWhatsAppDetails(text: String): Pair<String?, String> {
        // e.g. "send whatsapp message to John: hello there" or "whatsapp John hello"
        val regexWithColon = Regex("(?i)(?:send\\s+)?whatsapp(?:\\s+message)?\\s+(?:to\\s+)?([^:]+):\\s*(.*)")
        val matchColon = regexWithColon.find(text)
        if (matchColon != null) {
            val recipient = matchColon.groupValues[1].trim()
            val msg = matchColon.groupValues[2].trim()
            return Pair(recipient, msg)
        }

        val fallbackMsg = text
            .replace(Regex("(?i)^(send\\s+)?whatsapp(\\s+message)?(\\s+to)?"), "")
            .trim()
        return Pair(null, fallbackMsg.ifBlank { "Hello from Sandy!" })
    }

    private fun extractSmsDetails(text: String): Pair<String, String> {
        val regexWithColon = Regex("(?i)(?:send\\s+)?(?:sms|text)(?:\\s+to\\s+)?([^:]+):\\s*(.*)")
        val match = regexWithColon.find(text)
        if (match != null) {
            return Pair(match.groupValues[1].trim(), match.groupValues[2].trim())
        }

        val cleaned = text.replace(Regex("(?i)^(send\\s+)?(sms|text)(\\s+to)?"), "").trim()
        val parts = cleaned.split(" ", limit = 2)
        val recipient = parts.getOrNull(0) ?: "Contact"
        val msg = parts.getOrNull(1) ?: "Hello from Sandy"
        return Pair(recipient, msg)
    }

    private fun extractTime(text: String): Pair<Int, Int> {
        // matches 7:30, 7:30 am, 7 am, 19:00
        val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 7
            val min = match.groupValues[2].toIntOrNull() ?: 0
            val amPm = match.groupValues[3].lowercase()

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return Pair(hour.coerceIn(0, 23), min.coerceIn(0, 59))
        }
        return Pair(7, 0)
    }

    private fun extractTimerSeconds(text: String): Int {
        val minMatch = Regex("(\\d+)\\s*(?:min|minute)", RegexOption.IGNORE_CASE).find(text)
        if (minMatch != null) {
            return (minMatch.groupValues[1].toIntOrNull() ?: 5) * 60
        }
        val secMatch = Regex("(\\d+)\\s*(?:sec|second)", RegexOption.IGNORE_CASE).find(text)
        if (secMatch != null) {
            return secMatch.groupValues[1].toIntOrNull() ?: 30
        }
        return 300 // default 5 minutes
    }
}
