package com.buddy.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.buddy.app.databinding.ActivitySettingsBinding
import com.buddy.app.memory.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var memoryRepository: MemoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        memoryRepository = (application as BuddyApplication).memoryRepository

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.etApiKey.setText(getApiKey())
            binding.etUserName.setText(
                memoryRepository.getProfile(MemoryRepository.KEY_USER_NAME) ?: ""
            )
            binding.etUserAge.setText(
                memoryRepository.getProfile(MemoryRepository.KEY_USER_AGE) ?: ""
            )
            binding.etUserOccupation.setText(
                memoryRepository.getProfile(MemoryRepository.KEY_USER_OCCUPATION) ?: ""
            )
            binding.etUserCity.setText(
                memoryRepository.getProfile(MemoryRepository.KEY_USER_CITY) ?: ""
            )
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveSettings() {
        val apiKey = binding.etApiKey.text.toString().trim()
        val userName = binding.etUserName.text.toString().trim()
        val userAge = binding.etUserAge.text.toString().trim()
        val occupation = binding.etUserOccupation.text.toString().trim()
        val city = binding.etUserCity.text.toString().trim()

        if (apiKey.isBlank()) {
            Toast.makeText(this, "API key can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        saveApiKey(apiKey)

        CoroutineScope(Dispatchers.IO).launch {
            if (userName.isNotBlank()) memoryRepository.setProfile(MemoryRepository.KEY_USER_NAME, userName)
            if (userAge.isNotBlank()) memoryRepository.setProfile(MemoryRepository.KEY_USER_AGE, userAge)
            if (occupation.isNotBlank()) memoryRepository.setProfile(MemoryRepository.KEY_USER_OCCUPATION, occupation)
            if (city.isNotBlank()) memoryRepository.setProfile(MemoryRepository.KEY_USER_CITY, city)
        }

        Toast.makeText(this, "Settings saved. Buddy's ready.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getApiKey(): String {
        val prefs = getSharedPreferences("buddy_prefs", MODE_PRIVATE)
        return prefs.getString("api_key", "") ?: ""
    }

    private fun saveApiKey(key: String) {
        getSharedPreferences("buddy_prefs", MODE_PRIVATE)
            .edit()
            .putString("api_key", key)
            .apply()
    }
}
