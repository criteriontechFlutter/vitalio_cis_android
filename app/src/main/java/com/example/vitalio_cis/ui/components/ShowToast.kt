package com.example.vitalio_cis.ui.components

import android.content.Context
import android.widget.Toast

fun showToast(context: Context, message: String) {

    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}