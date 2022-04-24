package de.datavisions.screenruler

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class Impressum : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impressum)
        findViewById<View>(R.id.back).setOnClickListener { finish() }


        val appInfo: TextView = findViewById(R.id.app_info)
        appInfo.text = getString(R.string.info, packageManager.getPackageInfo(packageName, 0)
            .versionName)

        findViewById<View>(R.id.btn_pro).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "market://details?id=" + getString(R.string.pro_package))))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id="
                            + getString(R.string.pro_package))))
            }
        }

        val isProVersion = packageName == getString(R.string.pro_package)
        findViewById<View>(R.id.pro_badge).visibility = if (isProVersion) View.VISIBLE else
            View.GONE
        findViewById<View>(R.id.btn_pro).visibility = if (isProVersion) View.GONE else View.VISIBLE
        findViewById<View>(R.id.thanks).visibility = if (isProVersion) View.VISIBLE else View.GONE
        (findViewById<View>(R.id.title_pro) as TextView).setText(if (isProVersion)
            R.string.thanks else R.string.pro)
        (findViewById<View>(R.id.desc_pro) as TextView).setText(if (isProVersion)
            R.string.thanks_desc else R.string.pro_desc)

    }
}