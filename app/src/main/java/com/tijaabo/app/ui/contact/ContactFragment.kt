package com.tijaabo.app.ui.contact

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tijaabo.app.data.ContactMessageRequest
import com.tijaabo.app.databinding.FragmentContactBinding
import com.tijaabo.app.network.ApiClient
import kotlinx.coroutines.launch

class ContactFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.contactSendButton.setOnClickListener { sendMessage() }
    }

    private fun sendMessage() {
        val message = binding.contactMessageInput.text.toString().trim()
        if (message.isEmpty()) {
            binding.contactStatusText.text = "Please write a message first."
            return
        }

        val name = binding.contactNameInput.text.toString().trim().ifEmpty { null }
        val contactInfo = binding.contactInfoInput.text.toString().trim().ifEmpty { null }

        binding.contactSendButton.isEnabled = false
        binding.contactStatusText.text = "Sending..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.get().sendContactMessage(
                    ContactMessageRequest(name = name, contactInfo = contactInfo, message = message)
                )
                if (response.isSuccessful) {
                    binding.contactStatusText.text = "Message sent. Thank you!"
                    binding.contactNameInput.text.clear()
                    binding.contactInfoInput.text.clear()
                    binding.contactMessageInput.text.clear()
                } else {
                    binding.contactStatusText.text = "Couldn't send. Please try again."
                }
            } catch (e: Exception) {
                binding.contactStatusText.text = "Couldn't send. Check your connection and try again."
            } finally {
                binding.contactSendButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
