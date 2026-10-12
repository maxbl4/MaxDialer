package com.maxbl4.maxdialer.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import com.maxbl4.maxdialer.R

/** Hands numbers to the system: calls, messages, contacts, clipboard, blocking. */
object PhoneActions {
    private const val TAG = "PhoneActions"

    @SuppressLint("MissingPermission")
    fun call(context: Context, number: String, sim: PhoneAccountHandle? = null) {
        val uri = Uri.fromParts("tel", number, null)
        if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            start(context, Intent(Intent.ACTION_DIAL, uri))
            return
        }
        val extras = Bundle()
        if (sim != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, sim)
        context.getSystemService(TelecomManager::class.java).placeCall(uri, extras)
    }

    fun sms(context: Context, number: String) =
        start(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)))

    fun createContact(context: Context, number: String) = start(
        context,
        Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
            .putExtra(ContactsContract.Intents.Insert.PHONE, number),
    )

    fun addToContact(context: Context, number: String) = start(
        context,
        Intent(Intent.ACTION_INSERT_OR_EDIT)
            .setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
            .putExtra(ContactsContract.Intents.Insert.PHONE, number),
    )

    fun openContact(context: Context, contactId: Long, lookupKey: String) = start(
        context,
        Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(contactId, lookupKey)),
    )

    fun copy(context: Context, number: String, toast: Boolean = true) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(number, number))
        if (toast) Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    fun pastedNumber(context: Context): String? {
        val text = context.getSystemService(ClipboardManager::class.java)
            .primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
            ?: return null
        return text.filter { it.isDigit() || it in "+*#,;" }.ifEmpty { null }
    }

    /**
     * Only the default phone app may write the blocked list. Until MaxDialer is one,
     * open the system list so the user can add the number there.
     */
    fun block(context: Context, number: String) {
        if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
            try {
                val values = ContentValues().apply {
                    put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                }
                context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
                Toast.makeText(context, R.string.blocked, Toast.LENGTH_SHORT).show()
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "Blocking denied", e)
            }
        }
        copy(context, number, toast = false)
        Toast.makeText(context, R.string.block_manually, Toast.LENGTH_LONG).show()
        start(context, context.getSystemService(TelecomManager::class.java).createManageBlockedNumbersIntent())
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity for $intent", e)
        }
    }
}
