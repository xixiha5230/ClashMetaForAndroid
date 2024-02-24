package com.github.kr328.clash

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*

class ExternalControlService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                val url = uri?.getQueryParameter("url")
                if (url != null) {
                    serviceScope.launch {
                        val uuid = withProfile {
                            val type = when (uri.getQueryParameter("type")
                                ?.lowercase(Locale.getDefault())) {
                                "url" -> Profile.Type.Url
                                "file" -> Profile.Type.File
                                else -> Profile.Type.Url
                            }
                            val name =
                                uri.getQueryParameter("name") ?: getString(R.string.new_profile)
                            create(type, name).also {
                                patch(it, name, url, 0)
                            }
                        }
                        startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                        stopSelf()
                    }
                }
            }

            Intents.ACTION_TOGGLE_CLASH -> if (Remote.broadcasts.clashRunning) {
                stopClash()
            } else {
                startClash()
            }

            Intents.ACTION_START_CLASH -> if (!Remote.broadcasts.clashRunning) {
                startClash()
            } else {
                Toast.makeText(this, R.string.external_control_started, Toast.LENGTH_LONG).show()
            }

            Intents.ACTION_STOP_CLASH -> if (Remote.broadcasts.clashRunning) {
                stopClash()
            } else {
                Toast.makeText(this, R.string.external_control_stopped, Toast.LENGTH_LONG).show()
            }
        }

        return START_REDELIVER_INTENT
    }

    private fun startClash() {
        val vpnRequest = startClashService()
        if (vpnRequest != null) {
            Toast.makeText(this, R.string.unable_to_start_vpn, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, R.string.external_control_started, Toast.LENGTH_LONG).show()
    }

    private fun stopClash() {
        stopClashService()
        Toast.makeText(this, R.string.external_control_stopped, Toast.LENGTH_LONG).show()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}