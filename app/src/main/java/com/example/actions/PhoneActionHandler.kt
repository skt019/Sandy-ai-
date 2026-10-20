package com.example.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast

object PhoneActionHandler {

    fun executeAction(context: Context, action: SandyAction): String {
        return try {
            when (action) {
                is SandyAction.WhatsApp -> openWhatsApp(context, action.recipient, action.message)
                is SandyAction.Call -> makeCall(context, action.phoneNumber)
                is SandyAction.Sms -> sendSms(context, action.recipient, action.message)
                is SandyAction.YouTube -> openYouTube(context, action.query)
                is SandyAction.Alarm -> setAlarm(context, action.hour, action.minute, action.label)
                is SandyAction.Timer -> setTimer(context, action.seconds, action.label)
                is SandyAction.Note -> "Note saved to Sandy's memory"
                is SandyAction.CameraQuestion -> "Opening camera for visual analysis"
                is SandyAction.FileQuery -> "Retrieved document details"
                is SandyAction.WhatsAppStatus -> postWhatsAppStatus(context, action.statusText)
                is SandyAction.InstagramMessage -> sendInstagramMessage(context, action.recipient, action.message)
                is SandyAction.InstagramPost -> postOnInstagram(context, action.caption)
            }
        } catch (e: Exception) {
            "Action could not be completed: ${e.localizedMessage}"
        }
    }

    fun openWhatsApp(context: Context, recipient: String?, message: String): String {
        val encodedMessage = Uri.encode(message)
        val cleanPhone = recipient?.filter { it.isDigit() || it == '+' }

        val uri = if (!cleanPhone.isNullOrBlank() && cleanPhone.length >= 7) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            "Opening WhatsApp to send your message..."
        } catch (e: Exception) {
            // Fallback to browser or generic share intent if WhatsApp app is not installed
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
            "Opening WhatsApp via web link..."
        }
    }

    fun makeCall(context: Context, phoneNumber: String): String {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        val uri = Uri.parse("tel:${if (cleanNumber.isNotBlank()) cleanNumber else phoneNumber}")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Opening dialer for $phoneNumber..."
    }

    fun sendSms(context: Context, recipient: String, message: String): String {
        val cleanRecipient = recipient.filter { it.isDigit() || it == '+' }
        val uri = Uri.parse("smsto:${if (cleanRecipient.isNotBlank()) cleanRecipient else ""}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "Composing SMS to $recipient: \"$message\"..."
    }

    fun openYouTube(context: Context, query: String): String {
        val encodedQuery = Uri.encode(query)
        val uri = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.youtube")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            "Playing \"$query\" on YouTube..."
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            "Searching \"$query\" on YouTube..."
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, label: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val displayMinute = String.format("%02d", minute)
            "Alarm set for $displayHour:$displayMinute $amPm"
        } catch (e: Exception) {
            "Unable to set alarm: ${e.localizedMessage}"
        }
    }

    fun setTimer(context: Context, seconds: Int, label: String): String {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            "Timer set for ${seconds / 60} minutes"
        } catch (e: Exception) {
            "Unable to set timer: ${e.localizedMessage}"
        }
    }

    fun postWhatsAppStatus(context: Context, statusText: String): String {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, statusText)
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            "Opening WhatsApp to post your status: \"$statusText\""
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, statusText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(fallback, "Post Status").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            "Opening share options for your WhatsApp status..."
        }
    }

    fun sendInstagramMessage(context: Context, recipient: String, message: String): String {
        val cleanUser = recipient.trim().replace("@", "")
        // Deep link into Instagram Direct DM conversation
        val directUri = Uri.parse("https://ig.me/m/$cleanUser")
        val intent = Intent(Intent.ACTION_VIEW, directUri).apply {
            setPackage("com.instagram.android")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            "Opening Instagram to send personal message to @$cleanUser: \"$message\""
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/direct/inbox/")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            "Opening Instagram Direct in browser..."
        }
    }

    fun postOnInstagram(context: Context, caption: String): String {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption)
            setPackage("com.instagram.android")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            "Opening Instagram to share post: \"$caption\""
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            "Opening Instagram web to share your update..."
        }
    }
}
