package com.example.data.bot

import com.example.data.model.User
import kotlinx.coroutines.delay
import java.util.Locale

object BotEngine {

    val SPARKY_EMAIL = "sparky@bot.com"
    val ECHO_EMAIL = "echo@bot.com"
    val SUPPORT_EMAIL = "support@app.com"

    val presetBots = listOf(
        User(
            email = SPARKY_EMAIL,
            name = "Sparky Bot",
            avatarColorIndex = 3, // Teal / Accent
            bio = "I am Sparky, your cheerful offline AI buddy! Tell me a joke or ask how to chat.",
            isBot = true
        ),
        User(
            email = ECHO_EMAIL,
            name = "Echo Echo",
            avatarColorIndex = 1, // Orange
            bio = "I live in the mountains. I repeat back everything you say, but louder!",
            isBot = true
        ),
        User(
            email = SUPPORT_EMAIL,
            name = "Support Assistant",
            avatarColorIndex = 5, // Violet
            bio = "Official configuration helper. Ask me about testing, profiles, or navigation.",
            isBot = true
        )
    )

    suspend fun generateReply(botEmail: String, userMessage: String, userName: String): String {
        // Add a realistic simulated thinking/typing delay
        val delayTime = when (botEmail) {
            ECHO_EMAIL -> 800L
            else -> 1500L
        }
        delay(delayTime)

        val cleanMsg = userMessage.trim().lowercase(Locale.ROOT)

        return when (botEmail) {
            ECHO_EMAIL -> {
                "🔊 \"$userMessage... $userMessage...\""
            }
            SUPPORT_EMAIL -> {
                generateSupportReply(cleanMsg, userName)
            }
            SPARKY_EMAIL -> {
                generateSparkyReply(cleanMsg, userName)
            }
            else -> {
                "Hello $userName, I'm a simulated user! Nice to hear from you."
            }
        }
    }

    private fun generateSparkyReply(msg: String, user: String): String {
        return when {
            msg.contains("hello") || msg.contains("hi") || msg.contains("hey") -> {
                "Hey there, $user! 👋 I'm Sparky! How are you doing today? Ask me for a \"joke\" or type \"how to chat\" to learn how this cool application works!"
            }
            msg.contains("joke") || msg.contains("funny") -> {
                val jokes = listOf(
                    "Why do programmers prefer dark mode? Because light attracts bugs! 💻🪲",
                    "How many programmers does it take to change a light bulb? None, that's a hardware problem! 🔌",
                    "There are 10 types of people in the world: those who understand binary, and those who don't. 🔢",
                    "An SQL query walks into a bar, walks up to two tables and asks, 'Can I join you?' 📊"
                )
                "Here is a programmers' classic for you, $user:\n\n${jokes.random()}"
            }
            msg.contains("how to chat") || msg.contains("test") || msg.contains("how does") || msg.contains("multi") -> {
                "To chat with multiple profiles, do this:\n" +
                "1. Click the top-right profile button on the Inbox page.\n" +
                "2. Click 'Register New Email' and enter details (e.g., 'david@test.com').\n" +
                "3. In the switcher, select 'david@test.com' to log in as them.\n" +
                "4. Start a chat with your original email, send a reply, then swap back!\n\nThis makes testing full offline direct messaging on a single device incredibly fast and fully functional!"
            }
            msg.contains("color") || msg.contains("theme") || msg.contains("style") -> {
                "I absolutely love our Material 3 design! We've utilized dynamic background tone integration, spacious list margins, colored active chat bubbles, and micro-elevation transitions. What do you think?"
            }
            msg.contains("room") || msg.contains("database") || msg.contains("store") -> {
                "Yes! Everything you text and every user profile you register is permanently recorded in an offline SQLite database via Android Room. Unread badges are updated dynamically on-the-fly too."
            }
            msg.contains("bye") || msg.contains("goodbye") -> {
                "See you later, $user! Keep chatting! 🚀"
            }
            else -> {
                "That's super interesting, $user! 🌟 I am an offline AI, so my knowledge is local, but feel free to ask me for a 'joke', 'how to chat' instructions, or talk about our 'M3 theme'!"
            }
        }
    }

    private fun generateSupportReply(msg: String, user: String): String {
        return when {
            msg.contains("hello") || msg.contains("hi") || msg.contains("hey") || msg.contains("help") -> {
                "Welcome to Email Chat Support, $user. I can assist you with:\n" +
                "• 'registration' query details\n" +
                "• 'troubleshoot' tips\n" +
                "• 'export' features\n" +
                "How can I assist you today?"
            }
            msg.contains("register") || msg.contains("registration") || msg.contains("account") -> {
                "In Email Chat, registration is purely client-side! Anyone can sign in instantly by entering a valid email format. The system checks if the email already exists in Room. If not, it provisions a new user profile with a uniquely assigned material color."
            }
            msg.contains("troubleshoot") || msg.contains("reset") -> {
                "If you experience any transient UI lag, make sure to compile again via AI Studio! Since the app compiles incrementally, doing a refresh ensures the latest build states are loaded perfectly."
            }
            msg.contains("export") || msg.contains("zip") -> {
                "You can export this complete Android app! Go to the top settings menu in the browser to download the Project ZIP or compile a custom release APK/AAB package, complete with signed configurations."
            }
            else -> {
                "Thanks for contacting Support, $user. If you need details on 'registration', 'exporting', or 'troubleshooting', let me know! Have a wonderful day."
            }
        }
    }
}
