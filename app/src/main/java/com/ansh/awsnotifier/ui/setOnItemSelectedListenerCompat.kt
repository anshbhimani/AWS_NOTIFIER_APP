package com.ansh.awsnotifier.ui

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

fun Spinner.setOnItemSelectedListenerCompat(onSelected: (String) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            val value = parent?.getItemAtPosition(position)?.toString() ?: return
            onSelected(value)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}
