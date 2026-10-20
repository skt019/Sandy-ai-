package com.example.actions

sealed class SandyAction {
    data class WhatsApp(
        val recipient: String? = null,
        val message: String
    ) : SandyAction()

    data class Call(
        val phoneNumber: String,
        val contactName: String? = null
    ) : SandyAction()

    data class Sms(
        val recipient: String,
        val message: String
    ) : SandyAction()

    data class YouTube(
        val query: String
    ) : SandyAction()

    data class Alarm(
        val hour: Int,
        val minute: Int,
        val label: String = "Sandy Alarm"
    ) : SandyAction()

    data class Timer(
        val seconds: Int,
        val label: String = "Sandy Timer"
    ) : SandyAction()

    data class Note(
        val title: String,
        val content: String
    ) : SandyAction()

    data class CameraQuestion(
        val question: String = "Describe what you see"
    ) : SandyAction()
}
