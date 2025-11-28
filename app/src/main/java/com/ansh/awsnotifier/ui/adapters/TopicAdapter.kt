package com.ansh.awsnotifier.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ansh.awsnotifier.databinding.ItemTopicBinding
import com.ansh.awsnotifier.ui.TopicItem
import com.google.android.material.color.MaterialColors

class TopicAdapter(
    private val onSubscribe: (String) -> Unit,
    private val onUnsubscribe: (String) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onSendMessage: (String) -> Unit
) : ListAdapter<TopicItem, TopicAdapter.TopicViewHolder>(DiffCallback) {

    private var originalItems: List<TopicItem> = emptyList()

    fun submitListWithBackup(list: List<TopicItem>) {
        originalItems = list
        submitList(list)
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) submitList(originalItems)
        else submitList(
            originalItems.filter {
                it.topicName.lowercase().contains(q) ||
                        it.topicArn.lowercase().contains(q)
            }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        return TopicViewHolder(
            ItemTopicBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TopicViewHolder(
        private val binding: ItemTopicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopicItem) {

            binding.topicName.text = item.topicName
            binding.topicArn.text = item.topicArn

            // Region Badge
            val region = item.topicArn.split(":")[3]
            binding.regionBadge.text = region

            val ctx = binding.root.context
            val dynamicColor = MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorPrimary
            )

            binding.regionBadge.setBackgroundColor(dynamicColor)

            // Subscribe / Unsubscribe
            if (item.isSubscribed) {
                binding.subscriptionStatus.text = "Subscribed"
                binding.btnAction.text = "Unsubscribe"
            } else {
                binding.subscriptionStatus.text = "Not subscribed"
                binding.btnAction.text = "Subscribe"
            }

            binding.btnAction.setOnClickListener {
                animateClick(binding.btnAction)
                if (item.isSubscribed)
                    item.subscriptionArn?.let(onUnsubscribe)
                else
                    onSubscribe(item.topicArn)
            }

            // Delete
            binding.btnDelete.setOnClickListener {
                animateClick(binding.btnDelete)
                onDelete(item.topicArn)
            }

            // Copy ARN button
            binding.btnCopyArn.setOnClickListener {
                copyArn(ctx, item.topicArn)
            }

            // Send Message
            binding.btnSendMessage.setOnClickListener {
                animateClick(binding.btnSendMessage)
                onSendMessage(item.topicArn)
            }

            // Long press card to copy ARN
            binding.root.setOnLongClickListener {
                copyArn(ctx, item.topicArn)
                true
            }
        }

        private fun animateClick(view: View) {
            val anim = AlphaAnimation(0.3f, 1.0f)
            anim.duration = 200
            view.startAnimation(anim)
        }

        private fun copyArn(context: Context, arn: String) {
            val clip = ClipData.newPlainText("topicArn", arn)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "ARN copied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TopicItem>() {
            override fun areItemsTheSame(oldItem: TopicItem, newItem: TopicItem) =
                oldItem.topicArn == newItem.topicArn

            override fun areContentsTheSame(oldItem: TopicItem, newItem: TopicItem) =
                oldItem == newItem
        }
    }
}
