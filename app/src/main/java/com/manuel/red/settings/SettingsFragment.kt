package com.manuel.red.settings

import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.manuel.red.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        val switchPreferenceCompat =
            findPreference<SwitchPreferenceCompat>(getString(R.string.offers_and_promotions_key))
        switchPreferenceCompat?.setOnPreferenceChangeListener { _, newValue ->
            (newValue as? Boolean)?.let { isChecked ->
                val topic = getString(R.string.main_topic)
                if (isChecked) {
                    Firebase.messaging.subscribeToTopic(topic).addOnSuccessListener {
                        Toast.makeText(
                            context,
                            getString(R.string.notifications_activated),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Firebase.messaging.unsubscribeFromTopic(topic).addOnSuccessListener {
                        Toast.makeText(
                            context,
                            getString(R.string.notifications_disabled),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            true
        }
    }
}