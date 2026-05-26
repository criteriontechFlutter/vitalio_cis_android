package com.critetiontech.ctvitalio.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
 import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
 import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import okhttp3.ResponseBody
import java.time.LocalTime

object ToastUtils {

    // ---------------- Compose Success Popup ----------------
    @Composable
    fun SuccessPopup(message: String, onDismiss: () -> Unit) {
        LaunchedEffect(message) {
            delay(2000)
            onDismiss()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color =  Color(0xFF000000) ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
//                backgroundColor = Color(0xFF4CAF50)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
//                    color = Color.WHITE
                )
            }
        }
    }

    // ---------------- Classic Android Toasts ----------------
//    fun showFailure(context: Context, message: String) {
//        showCustomToast(context, message, "#F44336") // Red
//    }
//
//    fun showInfo(context: Context, message: String) {
//        showCustomToast(context, message, "#2196F3") // Blue
//    }

//    fun showSuccessPopupLegacy(context: Context, message: String) {
//        val dialog = Dialog(context)
//        val view = LayoutInflater.from(context).inflate(R.layout.custom_success_popup, null)
//        dialog.setContentView(view)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.setCancelable(false)
//
//        val textView = view.findViewById<TextView>(R.id.tvPopupMessage)
//        textView.text = message
//
//        dialog.show()
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (dialog.isShowing) dialog.dismiss()
//        }, 2000)
//    }

//    @SuppressLint("InflateParams")
//    private fun showCustomToast(context: Context, message: String, bgColor: String) {
//        val toast = Toast(context)
//        val inflater = LayoutInflater.from(context)
//        val view = inflater.inflate(R.layout.custom_toast_layout, null)
//        val textView = view.findViewById<TextView>(R.id.toastText)
//        textView.text = message
//        textView.setBackgroundColor(Color.parseColor(bgColor))
//
//        toast.duration = Toast.LENGTH_SHORT
//        toast.view = view
//        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
//        toast.show()
//    }

    // ---------------- Greeting Utility ----------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSimpleGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
    }
}

// ---------------- Error Parsing ----------------
object ErrorUtils {
    fun parseErrorMessage(errorBody: ResponseBody?): String {
        return try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val errorMap: Map<String, Any> = gson.fromJson(errorBody?.charStream(), type)
            errorMap["message"]?.toString() ?: "Something went wrong"
        } catch (e: Exception) {
            "Unable to parse error"
        }
    }
}
