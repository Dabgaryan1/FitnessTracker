package com.example.fitnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

object RepBotChatMemory {
    val messages = mutableListOf<ChatMessage>()
    var hasStartedConversation = false
    var workoutContext: String = ""

    fun clearConversation() {
        messages.clear()
        hasStartedConversation = false
        workoutContext = ""
    }
}

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userMessage: TextView = view.findViewById(R.id.userMessageText)
        val botMessage: TextView = view.findViewById(R.id.botMessageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.userMessage.visibility = View.VISIBLE
            holder.botMessage.visibility = View.GONE
            holder.userMessage.text = message.text
        } else {
            holder.userMessage.visibility = View.GONE
            holder.botMessage.visibility = View.VISIBLE
            holder.botMessage.text = message.text
        }
    }

    override fun getItemCount() = messages.size
}

class AiChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var adapter: ChatAdapter
    private lateinit var model: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aichat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.AiChat)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        recyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        adapter = ChatAdapter(RepBotChatMemory.messages)
        recyclerView.layoutManager = LinearLayoutManager(this).also {
            it.stackFromEnd = true
        }
        recyclerView.adapter = adapter

        adapter.notifyDataSetChanged()
        if (RepBotChatMemory.messages.isNotEmpty()) {
            recyclerView.scrollToPosition(RepBotChatMemory.messages.size - 1)
        }

        findViewById<ImageButton>(R.id.backArrow).setOnClickListener {
            RepBotChatMemory.clearConversation()
            adapter.notifyDataSetChanged()
            finish()
        }

        setupConversation()
        BottomNavHelper.bottomNav(this)
    }

    private fun setupConversation() {
        if (RepBotChatMemory.hasStartedConversation) {
            // Refresh active workout context in case it changed
            val activeWorkout = WorkoutStorage.loadCurrentWorkout(this)
            if (activeWorkout != null) {
                RepBotChatMemory.workoutContext = "The user is currently in an active workout:\n" + buildWorkoutContext(activeWorkout)
            }
            setupSendButton()
            return
        }

        WorkoutRepository.loadSavedWorkouts { workouts ->
            val activeWorkout = WorkoutStorage.loadCurrentWorkout(this)

            val workoutContext = if (activeWorkout != null) {
                "The user is currently in an active workout:\n" + buildWorkoutContext(activeWorkout)
            } else if (workouts.isEmpty()) {
                "The user has no saved workouts yet."
            } else {
                val latest = workouts.first()
                "The user's most recent completed workout:\n" +
                        buildWorkoutContext(
                            CurrentWorkout(
                                latest.name,
                                latest.time,
                                latest.exercises.toMutableList()
                            )
                        )
            }

            RepBotChatMemory.workoutContext = workoutContext
            RepBotChatMemory.hasStartedConversation = true

            runOnUiThread {
                sendInitialGreeting(workoutContext)
                setupSendButton()
            }
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val userText = messageEditText.text.toString().trim()
            if (userText.isNotEmpty()) {
                messageEditText.setText("")
                addMessage(ChatMessage(userText, isUser = true))
                sendToGemini(userText, RepBotChatMemory.workoutContext)
            }
        }
    }

    private fun buildWorkoutContext(workout: CurrentWorkout?): String {
        if (workout == null) return "The user has no active workout logged."
        val sb = StringBuilder()
        sb.append("The user's current workout is '${workout.name}' lasting ${workout.time}.\n")
        sb.append("Exercises:\n")
        for (exercise in workout.exercises) {
            sb.append("- ${exercise.name} (${exercise.muscleGroup}):\n")
            for (set in exercise.sets) {
                sb.append("  Set ${set.setNumber}: ${set.reps} reps @ ${set.weight}lbs\n")
            }
        }
        return sb.toString()
    }

    private fun sendInitialGreeting(workoutContext: String) {
        lifecycleScope.launch {
            val prompt = """
                You are RepBot, a fitness AI assistant inside a workout tracking app.
                Here is the user's current workout data:
                $workoutContext
                Greet the user warmly in 1-2 sentences and offer to give feedback on their workout.
            """.trimIndent()
            try {
                val response = model.generateContent(prompt)
                addMessage(ChatMessage(response.text ?: "Hey! I'm RepBot. How can I help?", isUser = false))
            } catch (e: Exception) {
                addMessage(ChatMessage("Hey! I'm RepBot, your fitness assistant. Ask me anything about your workout!", isUser = false))
            }
        }
    }

    private fun sendToGemini(userMessage: String, workoutContext: String) {
        lifecycleScope.launch {
            val previousConversation = RepBotChatMemory.messages.joinToString("\n") { message ->
                if (message.isUser) "User: ${message.text}" else "RepBot: ${message.text}"
            }
            val prompt = """
                You are RepBot, a fitness AI assistant inside a workout tracking app.
                
                Here is the user's workout data:
                $workoutContext
                
                Here is the conversation so far:
                $previousConversation
                
                The user just said: "$userMessage"
                
                Respond helpfully and concisely as a knowledgeable fitness coach.
            """.trimIndent()
            try {
                val response = model.generateContent(prompt)
                addMessage(ChatMessage(response.text ?: "Sorry, I couldn't process that.", isUser = false))
            } catch (e: Exception) {
                addMessage(ChatMessage("Error: ${e.message}", isUser = false))
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        RepBotChatMemory.messages.add(message)
        adapter.notifyItemInserted(RepBotChatMemory.messages.size - 1)
        recyclerView.scrollToPosition(RepBotChatMemory.messages.size - 1)
    }
}