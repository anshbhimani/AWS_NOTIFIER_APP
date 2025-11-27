package com.ansh.awsnotifier.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.databinding.FragmentNotificationListBinding
import com.ansh.awsnotifier.ui.adapters.NotificationAdapter
import com.ansh.awsnotifier.viewmodel.NotificationViewModel
import com.ansh.awsnotifier.viewmodel.NotificationViewModelFactory

class NotificationListFragment : Fragment() {

    private var _binding: FragmentNotificationListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationViewModel
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as App
        val factory = NotificationViewModelFactory(app.notificationDao)
        viewModel = ViewModelProvider(this, factory)[NotificationViewModel::class.java]

        setupRecyclerView()

        viewModel.allNotifications.observe(viewLifecycleOwner) {
            notificationAdapter.submitList(it)
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { /* handle click if needed */ }
        binding.recyclerView.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
