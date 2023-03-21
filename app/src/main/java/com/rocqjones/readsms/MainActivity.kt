package com.rocqjones.readsms

import android.content.Context
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rocqjones.readsms.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private val readSmsPermissionCode = 98

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        requestReadSmsPermission()
    }

    private fun requestReadSmsPermission() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_SMS),
                    readSmsPermissionCode
                )
            } else {
                // previously granted
                showMessageToUI(readLastIncomingSms(this))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            readSmsPermissionCode -> {
                when {
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                        // Permission granted, do something here
                        showMessageToUI(readLastIncomingSms(this))
                    }
                    else -> {
                        // Permission denied, handle the user's response
                        requestReadSmsPermission()
                    }
                }
                return
            }
            // Handle other permission request results here
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun readLastIncomingSms(context: Context): String {
        val smsUri = Uri.parse("content://sms/inbox")
        val cursor = context.contentResolver.query(smsUri, null, null, null, null)
        var message = ""
        try {
            cursor?.use {
                if (it.moveToFirst()) {
                    val senderIndex = it.getColumnIndex("address")
                    val bodyIndex = it.getColumnIndex("body")
                    do {
                        if (it.getString(senderIndex) == "MPESA") {
                            message = it.getString(bodyIndex)
                            break
                        }
                    } while (it.moveToNext())
                }
            }
            return message
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMessageToUI(message: String) {
        try {
            if (message.isNotEmpty()) {
                binding.mPesaMessage.text = message

                if (message.length > 5) {
                    val index = message.indexOf("Confirmed.") // find the index of "Confirmed."
                    Log.d("output", "Index: $index")
                    val result = message.substring(0, index)  // extract the text before "Confirmed."
                    Log.d("output", "Result: $result")
                    binding.mPesaCode.text = "CODE: $result"
                    Toast.makeText(this, "CODE: $result", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Handle incoming SMS with BroadcastReceiver if needed
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
                val bundle = intent.extras
                if (bundle != null) {
                    val pdus = bundle["pdus"] as Array<Any>
                    for (pdu in pdus) {
                        val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                        val messageBody = smsMessage.messageBody
                        val messageSender = smsMessage.originatingAddress

                        // Check if the message is from the expected sender
                        if (messageSender == "MPESA") {
                            // Perform actions with the message here

                            // Store the state of the message using SharedPreferences
                            val preferences = context?.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            val editor = preferences?.edit()
                            editor?.putBoolean("messageReceived", true)
                            editor?.apply()

                            Log.d("SmsReceiver", " messageReceived Successfully")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
