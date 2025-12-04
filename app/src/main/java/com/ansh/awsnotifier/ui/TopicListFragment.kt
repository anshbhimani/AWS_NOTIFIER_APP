package com.ansh.awsnotifier.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ansh.awsnotifier.App
import com.ansh.awsnotifier.R
import com.ansh.awsnotifier.databinding.FragmentTopicListBinding
import com.ansh.awsnotifier.session.UserSession
import com.ansh.awsnotifier.ui.adapters.TopicAdapter
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TopicListFragment : Fragment() {

    private var _binding: FragmentTopicListBinding? = null
    private val binding get() = _binding!!

    private lateinit var topicAdapter: TopicAdapter

    private val TAG = "SNS_DEBUG"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopicListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SNS_DEBUG", "onViewCreated() executed in TopicListFragment")

        setupRecycler()
        setupPullToRefresh()
        loadTopics()
        Log.d(TAG, "=== loadTopics() called ===")
    }

    override fun onResume() {
        super.onResume()
        loadTopics()
    }

    private fun setupRecycler() {
        topicAdapter = TopicAdapter(
            onSubscribe = { topicArn -> handleSubscribe(topicArn) },
            onUnsubscribe = { subscriptionArn -> handleUnsubscribe(subscriptionArn) },
            onDelete = { _ -> },
            onSendMessage = { topicArn -> showSendMessageDialog(topicArn) }
        )

        binding.recyclerView.apply {
            adapter = topicAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupPullToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadTopics {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadTopics(done: (() -> Unit)? = null) {
        lifecycleScope.launch {

            Log.d(TAG, "=== loadTopics() called ===")

            val app = requireActivity().application as App

            Log.d(TAG, "App hasCredentials=${app.hasCredentials()} awsProvider=${app.awsCredentialsProvider}")

            if (app.snsManager == null && app.hasCredentials()) {
                Log.d(TAG, "snsManager is NULL -> Initializing lazily")
                app.initSnsManager()
            }

            val sns = app.snsManager
            Log.d(TAG, "snsManager = $sns")

            if (sns == null) {
                Log.e(TAG, "snsManager is STILL null → CANNOT load topics")
                done?.invoke()
                return@launch
            }

            try {
                Log.d(TAG, "Calling sns.listAllTopics() ...")
                val topicArns = sns.listAllTopics()

                Log.d(TAG, "AWS returned topics (${topicArns.size}): $topicArns")

                val localSubs = UserSession.getAllSubscriptions(requireContext())
                Log.d(TAG, "Local subscriptions: $localSubs")

                val topicItems = topicArns.map { arn ->
                    val sub = localSubs.find { it.topicArn == arn }
                    TopicItem(
                        topicArn = arn,
                        topicName = arn.substringAfterLast(":"),
                        isSubscribed = sub != null,
                        subscriptionArn = sub?.subscriptionArn
                    )
                }

                Log.d(TAG, "Final list for RecyclerView: $topicItems")

                topicAdapter.submitList(topicItems)

                binding.emptyState.visibility =
                    if (topicItems.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Log.e(TAG, "EXCEPTION while loading topics:", e)
            }

            done?.invoke()
        }
    }

    private fun handleSubscribe(topicArn: String) {
        lifecycleScope.launch {
            val app = requireActivity().application as App
            val sns = app.snsManager ?: return@launch

            val endpoint = UserSession.getDeviceEndpointArn(requireContext())
                ?: return@launch

            try {
                val subArn = sns.subscribe(topicArn, endpoint)
                val region = topicArn.split(":")[3]

                UserSession.saveSubscription(requireContext(), subArn, topicArn, region)
                loadTopics()
            } catch (e: Exception) {
                Log.e(TAG, "Subscribe failed", e)
            }
        }
    }

    private fun handleUnsubscribe(subscriptionArn: String) {
        lifecycleScope.launch {
            val app = requireActivity().application as App
            val sns = app.snsManager ?: return@launch

            try {
                sns.unsubscribe(subscriptionArn)
                UserSession.removeSubscription(requireContext(), subscriptionArn)
                loadTopics()
            } catch (e: Exception) {
                Log.e(TAG, "Unsubscribe failed", e)
            }
        }
    }

    private fun showSendMessageDialog(topicArn: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_send_message, null)
        val messageInput = dialogView.findViewById<EditText>(R.id.messageInput)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val message = messageInput.text.toString()
                if (message.isNotEmpty()) {
                    publishMessage(topicArn, message)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun publishMessage(topicArn: String, message: String) {
        lifecycleScope.launch {
            val app = requireActivity().application as App
            val sns = app.snsManager ?: return@launch
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy | HH:mm:ss", Locale.getDefault())

                val innerJson = JSONObject().apply {
                    put("Message", message)
                    put("Timestamp", sdf.format(Date()))
                }.toString()

                val envelope = JSONObject().apply {
                    put("default", innerJson)
                }.toString()

                sns.publish(topicArn, envelope)
                Toast.makeText(context, "Message published", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to publish message", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Publish failed", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
