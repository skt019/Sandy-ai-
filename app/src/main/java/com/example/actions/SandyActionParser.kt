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

        // 1. WhatsApp Intent (Message or Status)
        // Can be triggered by "Sandy send message on WhatsApp...", "Sandy send status on WhatsApp...",
        // or by saying the recipient's name first: "<Name> send message on WhatsApp: <message>"
        if (lower.contains("whatsapp")) {
            val isStatus = lower.contains("status") || lower.contains("story")
            return if (isStatus) {
                val statusText = extractWhatsAppStatusText(cleaned)
                SandyAction.WhatsAppStatus(statusText = statusText)
            } else {
                val messagePart = extractWhatsAppDetails(cleaned)
                SandyAction.WhatsApp(
                    recipient = messagePart.first,
                    message = messagePart.second
                )
            }
        }

        // 2. Instagram Intent (Personal Message / DM or Post)
        // Can be triggered by "Sandy send personal message to <Name> on Instagram...",
        // "Sandy post something on Instagram...", or by saying their name: "<Name> send message on Instagram: <message>"
        if (lower.contains("instagram") || lower.contains("insta dm") || lower.contains("insta message") || lower.contains("insta post")) {
            return extractInstagramDetails(cleaned)
        }

        // 3. Phone Call Intent
        if (lower.startsWith("call ") || lower.startsWith("phone call ") || lower.startsWith("dial ")) {
            val target = cleaned
                .replace(Regex("^(call|phone call|dial)\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
            val phoneOnly = target.filter { it.isDigit() || it == '+' }
            val number = if (phoneOnly.length >= 3) phoneOnly else target
            return SandyAction.Call(phoneNumber = number, contactName = target)
        }

        // 4. SMS Intent
        if (lower.startsWith("send sms") || lower.startsWith("sms ") || lower.startsWith("text ")) {
            val (recipient, msg) = extractSmsDetails(cleaned)
            return SandyAction.Sms(recipient = recipient, message = msg)
        }

        // 5. YouTube Intent
        if (lower.contains("youtube") || lower.startsWith("play ")) {
            val query = cleaned
                .replace(Regex("^(search youtube for|play on youtube|play|open youtube and play|youtube)\\s+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+on youtube$", RegexOption.IGNORE_CASE), "")
                .trim()
            return SandyAction.YouTube(query = query.ifBlank { "Trending" })
        }

        // 6. Alarm Intent
        if (lower.contains("alarm") || lower.startsWith("wake me up")) {
            val (hour, minute) = extractTime(lower)
            return SandyAction.Alarm(hour = hour, minute = minute, label = "Sandy Alarm")
        }

        // 7. Timer Intent
        if (lower.contains("timer")) {
            val seconds = extractTimerSeconds(lower)
            return SandyAction.Timer(seconds = seconds, label = "Sandy Timer")
        }

        // 8. Note Intent ("note this down", "take a note", "remember that")
        if (lower.startsWith("note this down") || lower.startsWith("take a note") || lower.startsWith("note ") || lower.startsWith("remember that")) {
            val noteContent = cleaned
                .replace(Regex("^(note this down[:\\s]*|take a note[:\\s]*|note[:\\s]*|remember that[:\\s]*)", RegexOption.IGNORE_CASE), "")
                .trim()
            if (noteContent.isNotBlank()) {
                val title = if (noteContent.length > 25) noteContent.take(25) + "..." else noteContent
                return SandyAction.Note(title = title, content = noteContent)
            }
        }

        // 9. Camera Intent
        if (lower.contains("open camera") || lower.contains("take picture") || lower.contains("take photo") || lower.contains("look at this with camera")) {
            val question = cleaned
                .replace(Regex("^(open camera|take picture|take photo|look at this with camera)[:\\s]*", RegexOption.IGNORE_CASE), "")
                .trim()
            return SandyAction.CameraQuestion(question = question.ifBlank { "Describe what you see in this image" })
        }

        // 10. Uploaded File Query Intent (PDF or JPG info, summaries, details)
        if (isFileQueryIntent(lower)) {
            val targetType = when {
                lower.contains("pdf") -> "PDF"
                lower.contains("jpg") || lower.contains("jpeg") || lower.contains("image") || lower.contains("photo") -> "JPG"
                else -> null
            }
            return SandyAction.FileQuery(query = cleaned, targetFileType = targetType)
        }

        return null
    }

    private fun isFileQueryIntent(lower: String): Boolean {
        // Direct phrases
        if (lower.contains("tell me about") && (lower.contains("file") || lower.contains("pdf") || lower.contains("jpg") || lower.contains("upload") || lower.contains("document"))) {
            return true
        }
        if (lower.contains("information about") && (lower.contains("file") || lower.contains("pdf") || lower.contains("jpg") || lower.contains("upload") || lower.contains("document"))) {
            return true
        }
        if (lower.contains("what is in") || lower.contains("what's in") || lower.contains("whats in")) {
            if (lower.contains("file") || lower.contains("pdf") || lower.contains("jpg") || lower.contains("upload") || lower.contains("document")) {
                return true
            }
        }
        if (lower.startsWith("summarize") || lower.startsWith("summary of")) {
            if (lower.contains("file") || lower.contains("pdf") || lower.contains("jpg") || lower.contains("document")) {
                return true
            }
        }
        if (lower.contains("uploaded file") || lower.contains("uploaded pdf") || lower.contains("uploaded jpg")) {
            return true
        }
        if (lower.contains("my files") || lower.contains("my uploaded") || lower.contains("show files") || lower.contains("list files") || lower.contains("file details") || lower.contains("file info")) {
            return true
        }
        return false
    }

    private fun extractWhatsAppStatusText(text: String): String {
        val colonIdx = text.indexOf(':')
        if (colonIdx != -1 && colonIdx < text.length - 1) {
            val content = text.substring(colonIdx + 1).trim()
            if (content.isNotBlank()) return content
        }
        val cleaned = text
            .replace(Regex("(?i)^(?:hey|hi|hello|ok)?\\s*sandy[,\\s]*"), "")
            .replace(Regex("(?i)^(?:send|post|share|update)?\\s*(?:a\\s+)?(?:status|story)(?:\\s+on\\s+whatsapp)?(?:\\s+by\\s+saying\\s+sandy)?[:\\s]*"), "")
            .replace(Regex("(?i)^whatsapp\\s+(?:status|story)[:\\s]*"), "")
            .trim()
        return cleaned.ifBlank { "Sharing a new status update via Sandy!" }
    }

    private fun extractWhatsAppDetails(text: String): Pair<String?, String> {
        val trimmed = text.trim()

        // Name-first pattern: "<Name> send (personal )?message on whatsapp: <msg>" or "<Name> whatsapp: <msg>"
        val nameFirstRegex = Regex("(?i)^([a-zA-Z0-9_ ]{2,30})\\s+(?:send\\s+)?(?:personal\\s+)?(?:message\\s+)?(?:on\\s+)?whatsapp[:\\s]+(.*)")
        val nameFirstMatch = nameFirstRegex.find(trimmed)
        if (nameFirstMatch != null) {
            val nameCandidate = nameFirstMatch.groupValues[1].trim()
            val msg = nameFirstMatch.groupValues[2].trim()
            val lowerName = nameCandidate.lowercase()
            if (!lowerName.startsWith("can") && !lowerName.startsWith("please") && !lowerName.startsWith("i want") && !lowerName.startsWith("send")) {
                return Pair(nameCandidate, msg.ifBlank { "Hello from Sandy!" })
            }
        }

        // Standard: "send (personal )?message to <Name> on whatsapp: <msg>"
        val toColonRegex = Regex("(?i)(?:send\\s+)?(?:personal\\s+)?(?:message\\s+)?(?:to\\s+)?([^:]+?)\\s+(?:on\\s+whatsapp|whatsapp)[:\\s]+(.*)")
        val toColonMatch = toColonRegex.find(trimmed)
        if (toColonMatch != null) {
            val recipient = toColonMatch.groupValues[1].replace(Regex("(?i)^(send\\s+)?(personal\\s+)?(message\\s+)?(to\\s+)?"), "").trim()
            val msg = toColonMatch.groupValues[2].trim()
            return Pair(recipient, msg.ifBlank { "Hello from Sandy!" })
        }

        // Standard with colon: "send (personal )?whatsapp (message )?to <Name>: <msg>"
        val standardColonRegex = Regex("(?i)(?:send\\s+)?(?:personal\\s+)?whatsapp(?:\\s+message)?\\s+(?:to\\s+)?([^:]+):\\s*(.*)")
        val matchColon = standardColonRegex.find(trimmed)
        if (matchColon != null) {
            val recipient = matchColon.groupValues[1].trim()
            val msg = matchColon.groupValues[2].trim()
            return Pair(recipient, msg.ifBlank { "Hello from Sandy!" })
        }

        // Fallback
        val fallbackMsg = trimmed
            .replace(Regex("(?i)^(send\\s+)?(personal\\s+)?whatsapp(\\s+message)?(\\s+to)?"), "")
            .trim()
        return Pair(null, fallbackMsg.ifBlank { "Hello from Sandy!" })
    }

    private fun extractInstagramDetails(text: String): SandyAction {
        val lower = text.lowercase()
        val isPost = lower.contains("post") || lower.contains("share") || lower.contains("caption") || lower.contains("feed") || (lower.contains("post something") && !lower.contains("message"))

        if (isPost) {
            val colonIdx = text.indexOf(':')
            val caption = if (colonIdx != -1 && colonIdx < text.length - 1) {
                text.substring(colonIdx + 1).trim()
            } else {
                text.replace(Regex("(?i)^(?:hey|hi|hello|ok)?\\s*sandy[,\\s]*"), "")
                    .replace(Regex("(?i)^(?:post|share)?\\s*(?:something)?\\s*(?:on\\s+instagram|to\\s+instagram|instagram\\s+post)[:\\s]*"), "")
                    .trim()
            }
            return SandyAction.InstagramPost(caption = caption.ifBlank { "Sharing a new moment via Sandy!" })
        }

        // Instagram Personal Message / DM
        // Pattern 1: "<Name> send (personal )?message on instagram: <msg>" or "<Name> instagram: <msg>"
        val nameFirstRegex = Regex("(?i)^([a-zA-Z0-9_ @]{2,30})\\s+(?:send\\s+)?(?:personal\\s+)?(?:message|dm)?\\s*(?:on\\s+instagram|to\\s+instagram)[:\\s]+(.*)")
        val nameMatch = nameFirstRegex.find(text)
        if (nameMatch != null) {
            val recipient = nameMatch.groupValues[1].trim()
            val msg = nameMatch.groupValues[2].trim()
            val lowerName = recipient.lowercase()
            if (!lowerName.startsWith("can") && !lowerName.startsWith("please") && !lowerName.startsWith("i want") && !lowerName.startsWith("send")) {
                return SandyAction.InstagramMessage(recipient = recipient, message = msg.ifBlank { "Hello via Instagram Direct!" })
            }
        }

        // Pattern 2: "send (personal )?message to <Name> on instagram: <msg>"
        val toColonRegex = Regex("(?i)(?:send\\s+)?(?:personal\\s+)?(?:message|dm)\\s+(?:to\\s+)?([^:]+?)\\s+(?:on\\s+instagram|to\\s+instagram)[:\\s]+(.*)")
        val toMatch = toColonRegex.find(text)
        if (toMatch != null) {
            val recipient = toMatch.groupValues[1].replace(Regex("(?i)^(send\\s+)?(personal\\s+)?(message\\s+)?(to\\s+)?"), "").trim()
            val msg = toMatch.groupValues[2].trim()
            return SandyAction.InstagramMessage(recipient = recipient, message = msg.ifBlank { "Hello via Instagram Direct!" })
        }

        // Pattern 3: "instagram (message|dm) to <Name>: <msg>"
        val standardColonRegex = Regex("(?i)(?:send\\s+)?(?:personal\\s+)?instagram(?:\\s+message|\\s+dm)?\\s+(?:to\\s+)?([^:]+):\\s*(.*)")
        val standardMatch = standardColonRegex.find(text)
        if (standardMatch != null) {
            val recipient = standardMatch.groupValues[1].trim()
            val msg = standardMatch.groupValues[2].trim()
            return SandyAction.InstagramMessage(recipient = recipient, message = msg.ifBlank { "Hello via Instagram Direct!" })
        }

        // Fallback message with colon
        val colonIdx = text.indexOf(':')
        if (colonIdx != -1 && colonIdx < text.length - 1) {
            val target = text.substring(0, colonIdx).replace(Regex("(?i)(send|personal|message|instagram|to|on)"), "").trim()
            val content = text.substring(colonIdx + 1).trim()
            return SandyAction.InstagramMessage(
                recipient = target.ifBlank { "Friend" },
                message = content.ifBlank { "Hello from Sandy!" }
            )
        }

        return SandyAction.InstagramMessage(
            recipient = "Friend",
            message = text.replace(Regex("(?i)^(send|personal|message|instagram|to|on)\\s*"), "").ifBlank { "Hello from Sandy on Instagram!" }
        )
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
