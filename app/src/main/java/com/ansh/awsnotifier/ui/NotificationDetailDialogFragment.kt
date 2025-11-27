package com.ansh.awsnotifier.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ansh.awsnotifier.data.NotificationEntity
import com.ansh.awsnotifier.databinding.DialogNotificationDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationDetailDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_NOTIFICATION = "notification"

        fun newInstance(notification: NotificationEntity): NotificationDetailDialogFragment {
            val fragment = NotificationDetailDialogFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(ARG_NOTIFICATION, notification)
            }
            return fragment
        }
    }

    private lateinit var binding: DialogNotificationDetailBinding

    @Suppress("DEPRECATION") // for getParcelable(String)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogNotificationDetailBinding.inflate(layoutInflater)

        val notification =
            requireArguments().getParcelable<NotificationEntity>(ARG_NOTIFICATION)
                ?: throw IllegalStateException("Notification argument missing")

        binding.title.text = notification.title
        binding.message.text = notification.message
        binding.topic.text = notification.topic

        val fmt = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())
        binding.time.text = fmt.format(Date(notification.timestamp))

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
    }
}
