
package com.lifx.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.BufferedReader
import java.io.StringReader

class MainActivity : Activity() {

    private val bg = Color.rgb(9, 13, 28)
    private val panel = Color.rgb(20, 27, 48)
    private val accent = Color.rgb(91, 115, 255)
    private val muted = Color.rgb(158, 171, 201)

    private lateinit var status: TextView
    private lateinit var connect: Button
    private lateinit var configInput: EditText
    private lateinit var backend: GoBackend

    @Volatile
    private var connected = false

    private var pendingConfig: Config? = null

    private val tunnel = object : Tunnel {
        override fun getName(): String = "lifx"

        override fun onStateChange(newState: Tunnel.State) {
            runOnUiThread {
                connected = newState == Tunnel.State.UP

                status.text = if (connected) {
                    "●  TERHUBUNG"
                } else {
                    "●  TIDAK TERHUBUNG"
                }

                status.setTextColor(
                    if (connected) Color.rgb(75, 224, 166)
                    else muted
                )

                connect.text = if (connected) {
                    "Putuskan VPN"
                } else {
                    "Hubungkan VPN"
                }

                connect.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backend = GoBackend(this)
        buildUi()
    }

    private fun rounded(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(22),
                dp(22),
                dp(22),
                dp(18)
            )
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "✦"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(accent, dp(15).toFloat())
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(52), dp(52))
        )

        val brandCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        brandCol.addView(
            text("LIFX VPN", 21f, Color.WHITE, true)
        )

        brandCol.addView(
            text("PRIVATE CONNECTION", 10f, muted, true)
        )

        header.addView(
            brandCol,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        header.addView(
            text(
                "WIREGUARD",
                10f,
                Color.rgb(167, 180, 255),
                true
            ).apply {
                setPadding(
                    dp(10),
                    dp(8),
                    dp(10),
                    dp(8)
                )
                background = rounded(panel, dp(18).toFloat())
            }
        )

        content.addView(header)
        content.addView(space(28))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                dp(18),
                dp(24),
                dp(18),
                dp(24)
            )
            background = rounded(panel, dp(28).toFloat())
        }

        val shield = TextView(this).apply {
            text = "⬡"
            textSize = 76f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(125, 145, 255))
        }

        hero.addView(
            shield,
            LinearLayout.LayoutParams(-1, dp(112))
        )

        hero.addView(
            text("Koneksi Anda", 15f, muted).apply {
                gravity = Gravity.CENTER
            }
        )

        status = text(
            "●  TIDAK TERHUBUNG",
            14f,
            muted,
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(6))
        }

        hero.addView(status)

        hero.addView(
            text(
                "Aktifkan tunnel WireGuard untuk mengenkripsi\n" +
                    "lalu lintas jaringan perangkat ini.",
                12f,
                muted
            ).apply {
                gravity = Gravity.CENTER
                setLineSpacing(dp(3).toFloat(), 1f)
            }
        )

        connect = Button(this).apply {
            text = "Hubungkan VPN"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = rounded(accent, dp(18).toFloat())
            setOnClickListener {
                toggleVpn()
            }
        }

        val buttonLp = LinearLayout.LayoutParams(
            -1,
            dp(56)
        ).apply {
            topMargin = dp(22)
        }

        hero.addView(connect, buttonLp)
        content.addView(hero)

        content.addView(space(22))

        content.addView(
            text("KONFIGURASI SERVER", 12f, Color.WHITE, true)
        )

        content.addView(space(8))

        content.addView(
            text(
                "Tempel konfigurasi client WireGuard dari server Anda. " +
                    "Private key hanya digunakan lokal di perangkat.",
                12f,
                muted
            )
        )

        content.addView(space(12))

        configInput = EditText(this).apply {
            hint = """
                [Interface]
                PrivateKey = ...
                Address = ...

                [Peer]
                PublicKey = ...
                Endpoint = host:51820
                AllowedIPs = 0.0.0.0/0, ::/0
            """.trimIndent()

            setHintTextColor(Color.rgb(105, 119, 151))
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.TOP or Gravity.START
            minLines = 8
            maxLines = 14

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
            )

            background = rounded(
                Color.rgb(13, 19, 36),
                dp(16).toFloat()
            )
        }

        content.addView(
            configInput,
            LinearLayout.LayoutParams(-1, dp(210))
        )

        content.addView(space(10))

        val save = Button(this).apply {
            text = "Gunakan konfigurasi"
            isAllCaps = false
            setTextColor(Color.WHITE)

            background = rounded(
                Color.rgb(42, 54, 86),
                dp(14).toFloat()
            )

            setOnClickListener {
                hideKeyboard()

                try {
                    pendingConfig = parseConfig()

                    Toast.makeText(
                        this@MainActivity,
                        "Konfigurasi berhasil dibaca.",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Konfigurasi tidak valid: " +
                            (e.message ?: "periksa format"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        content.addView(
            save,
            LinearLayout.LayoutParams(-1, dp(48))
        )

        content.addView(space(18))

        content.addView(
            text("KEAMANAN", 12f, Color.WHITE, true)
        )

        content.addView(space(6))

        content.addView(
            text(
                "Lifx VPN tidak menyediakan server otomatis. " +
                    "Gunakan konfigurasi dari server WireGuard milik Anda " +
                    "atau penyedia tepercaya. Jangan bagikan file konfigurasi " +
                    "karena berisi private key.",
                12f,
                muted
            )
        )

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(
            text(
                "LIFX VPN  •  WIREGUARD",
                10f,
                Color.rgb(91, 105, 138),
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
            }
        )

        setContentView(root)
    }

    private fun parseConfig(): Config {
        val raw = configInput.text.toString().trim()

        require(
            raw.contains("[Interface]", ignoreCase = true) &&
                raw.contains("[Peer]", ignoreCase = true)
        ) {
            "Bagian [Interface] dan [Peer] wajib ada."
        }

        return Config.parse(
            BufferedReader(StringReader(raw))
        )
    }

    private fun toggleVpn() {
        if (connected) {
            connect.isEnabled = false

            Thread {
                try {
                    backend.setState(
                        tunnel,
                        Tunnel.State.DOWN,
                        null
                    )
                } catch (e: Exception) {
                    runOnUiThread {
                        connect.isEnabled = true
                        showError(e.message)
                    }
                }
            }.start()

            return
        }

        val config = try {
            parseConfig()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Masukkan konfigurasi WireGuard yang valid terlebih dahulu.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        pendingConfig = config

        val intent = VpnService.prepare(this)

        if (intent != null) {
            startActivityForResult(intent, 71)
        } else {
            startTunnel()
        }
    }

    @Deprecated(
        "Activity result API kept compatible with minimal project setup"
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 71) {
            if (resultCode == RESULT_OK) {
                startTunnel()
            } else {
                Toast.makeText(
                    this,
                    "Izin VPN diperlukan untuk membuat tunnel.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startTunnel() {
        val config = pendingConfig ?: run {
            showError("Konfigurasi belum tersedia.")
            return
        }

        connect.isEnabled = false
        status.text = "●  MENGHUBUNGKAN…"
        status.setTextColor(Color.rgb(255, 199, 92))

        Thread {
            try {
                backend.setState(
                    tunnel,
                    Tunnel.State.UP,
                    config
                )

                runOnUiThread {
                    connect.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    connect.isEnabled = true
                    status.text = "●  GAGAL TERHUBUNG"
                    status.setTextColor(
                        Color.rgb(255, 111, 128)
                    )
                    showError(e.message)
                }
            }
        }.start()
    }

    private fun showError(message: String?) {
        Toast.makeText(
            this,
            "VPN error: ${message ?: "operasi gagal"}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun hideKeyboard() {
        val manager = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        manager.hideSoftInputFromWindow(
            configInput.windowToken,
            0
        )

        configInput.clearFocus()
    }

    private fun space(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }
}
