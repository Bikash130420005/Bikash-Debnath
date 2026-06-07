package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.bot.BotEngine
import com.example.data.local.ChatDatabase
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.User
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class ChatScreen {
    LOGIN,
    INBOX,
    CHAT_THREAD,
    PROFILE_EDIT
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    // Current Session / Logged In profile
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Screen State
    private val _activeScreen = MutableStateFlow(ChatScreen.LOGIN)
    val activeScreen: StateFlow<ChatScreen> = _activeScreen.asStateFlow()

    // Selected Chat Partner
    private val _selectedPartner = MutableStateFlow<User?>(null)
    val selectedPartner: StateFlow<User?> = _selectedPartner.asStateFlow()

    // Search query for contacts
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Typing Indicators for bots (key: botEmail, value: isTyping)
    private val _typingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingState: StateFlow<Map<String, Boolean>> = _typingState.asStateFlow()

    // Flow of all registered users
    val allUsers: StateFlow<List<User>> = repositoryFlow { repository.allUsersFlow }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active message list for currently selected thread
    val activeChatMessages: StateFlow<List<Message>> = _currentUser.combine(_selectedPartner) { current, partner ->
        if (current == null || partner == null) emptyList()
        else repository.observeMessagesBetween(current.email, partner.email).firstOrNull() ?: emptyList()
    }.flatMapLatest { initialList ->
        val current = _currentUser.value
        val partner = _selectedPartner.value
        if (current == null || partner == null) flowOf(emptyList())
        else repository.observeMessagesBetween(current.email, partner.email)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Inbox Conversations aggregation
    val conversations: StateFlow<List<Conversation>> = combine(
        _currentUser,
        allUsers,
        _currentUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.observeAllMessagesForUser(user.email)
        }
    ) { current, users, messages ->
        if (current == null) return@combine emptyList()
        val grouped = messages.groupBy { msg ->
            if (msg.senderEmail == current.email) msg.receiverEmail else msg.senderEmail
        }
        grouped.mapNotNull { (partnerEmail, msgs) ->
            val lastMsg = msgs.firstOrNull() ?: return@mapNotNull null
            val contact = users.find { it.email == partnerEmail } ?: User(
                email = partnerEmail,
                name = partnerEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                avatarColorIndex = partnerEmail.hashCode().coerceAtLeast(0) % 6,
                bio = "Direct Offline Contact"
            )
            val unreadCount = msgs.count { msg ->
                msg.receiverEmail == current.email && !msg.isRead
            }
            Conversation(contact = contact, lastMessage = lastMsg, unreadCount = unreadCount)
        }.sortedByDescending { it.lastMessage.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val database = ChatDatabase.getDatabase(application)
        repository = ChatRepository(database.userDao(), database.messageDao())

        // Initial provisioning checklist: ensure bots are registered in DB
        viewModelScope.launch(Dispatchers.IO) {
            val dbBots = repository.getAllUsers().filter { it.isBot }
            if (dbBots.size < BotEngine.presetBots.size) {
                BotEngine.presetBots.forEach { bot ->
                    repository.insertUser(bot)
                }
            }
            
            // Auto sign in with last active or default if any
            val usersInDb = repository.getAllUsers().filter { !it.isBot }
            if (usersInDb.isNotEmpty()) {
                val lastActiveUser = usersInDb.maxByOrNull { it.lastActive }
                lastActiveUser?.let {
                    _currentUser.value = it
                    _activeScreen.value = ChatScreen.INBOX
                }
            }
        }
    }

    private fun <T> repositoryFlow(block: () -> Flow<T>): Flow<T> {
        return flow {
            emitAll(block())
        }
    }

    // --- Action Methods ---

    fun loginOrRegister(email: String, name: String) {
        val trimmedEmail = email.trim().lowercase(Locale.ROOT)
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) return

        val trimmedName = name.trim().ifEmpty { trimmedEmail.substringBefore("@") }

        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserByEmail(trimmedEmail)
            val user = if (existing != null) {
                // If bot profile, don't modify lastActive as main session directly
                val updated = existing.copy(lastActive = System.currentTimeMillis())
                repository.insertUser(updated)
                updated
            } else {
                val newUser = User(
                    email = trimmedEmail,
                    name = trimmedName,
                    avatarColorIndex = trimmedEmail.hashCode().coerceAtLeast(0) % 6,
                    bio = "Newly Registered Mind",
                    isBot = false,
                    lastActive = System.currentTimeMillis()
                )
                repository.insertUser(newUser)
                newUser
            }

            _currentUser.value = user
            _activeScreen.value = ChatScreen.INBOX
        }
    }

    fun logout() {
        _currentUser.value = null
        _selectedPartner.value = null
        _activeScreen.value = ChatScreen.LOGIN
    }

    fun selectScreen(screen: ChatScreen) {
        _activeScreen.value = screen
    }

    fun startChatWithEmail(email: String, onComplete: (Boolean) -> Unit = {}) {
        val targetEmail = email.trim().lowercase(Locale.ROOT)
        if (targetEmail.isEmpty() || targetEmail == _currentUser.value?.email) {
            onComplete(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getUserByEmail(targetEmail)
            val partner = if (existing != null) {
                existing
            } else {
                // Provision local contact on-the-fly to prevent a crash
                val defaultName = targetEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                val newContact = User(
                    email = targetEmail,
                    name = defaultName,
                    avatarColorIndex = targetEmail.hashCode().coerceAtLeast(0) % 6,
                    bio = "Discovered User",
                    isBot = false
                )
                repository.insertUser(newContact)
                newContact
            }

            _selectedPartner.value = partner
            _activeScreen.value = ChatScreen.CHAT_THREAD
            // mark messages as read
            val cur = _currentUser.value
            if (cur != null) {
                repository.markMessagesAsRead(partner.email, cur.email)
            }
            onComplete(true)
        }
    }

    fun openChat(partner: User) {
        _selectedPartner.value = partner
        _activeScreen.value = ChatScreen.CHAT_THREAD
        val cur = _currentUser.value
        if (cur != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.markMessagesAsRead(partner.email, cur.email)
            }
        }
    }

    fun updateProfile(name: String, bio: String) {
        val cur = _currentUser.value ?: return
        val updatedName = name.trim().ifEmpty { cur.name }
        val updatedBio = bio.trim()

        viewModelScope.launch(Dispatchers.IO) {
            val updated = cur.copy(name = updatedName, bio = updatedBio)
            repository.insertUser(updated)
            _currentUser.value = updated
            _activeScreen.value = ChatScreen.INBOX
        }
    }

    fun sendMessage(text: String) {
        val textBody = text.trim()
        if (textBody.isEmpty()) return

        val current = _currentUser.value ?: return
        val partner = _selectedPartner.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = Message(
                senderEmail = current.email,
                receiverEmail = partner.email,
                content = textBody
            )
            repository.insertMessage(userMsg)

            // Trigger Bot Response if partner is simulated BOT
            if (partner.isBot) {
                _typingState.value = _typingState.value + (partner.email to true)

                // Generate response
                val answer = BotEngine.generateReply(partner.email, textBody, current.name)

                _typingState.value = _typingState.value - partner.email

                val botMsg = Message(
                    senderEmail = partner.email,
                    receiverEmail = current.email,
                    content = answer
                )
                repository.insertMessage(botMsg)
            }
        }
    }

    fun deleteCurrentChat() {
        val current = _currentUser.value ?: return
        val partner = _selectedPartner.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChatBetween(current.email, partner.email)
            _selectedPartner.value = null
            _activeScreen.value = ChatScreen.INBOX
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun switchActiveProfile(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = user.copy(lastActive = System.currentTimeMillis())
            repository.insertUser(updated)
            _currentUser.value = updated
            _selectedPartner.value = null
            _activeScreen.value = ChatScreen.INBOX
        }
    }
}
