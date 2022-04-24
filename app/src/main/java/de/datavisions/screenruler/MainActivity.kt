package de.datavisions.screenruler

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.button.MaterialButton
import kotlin.math.abs
import kotlin.math.round


class MainActivity : AppCompatActivity() {

    private var scrollDisabled = false
    private var calibrationActive = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Check if Pro Version
        val isProVersion = packageName == getString(R.string.pro_package)
        findViewById<TextView>(R.id.pro_badge).visibility = if (isProVersion) View.VISIBLE else
            View.INVISIBLE

        // Setup Google Ads
        val adView: AdView = findViewById(R.id.adView)
        if (!isProVersion) {
            MobileAds.initialize(this) {}
            adView.loadAd(AdRequest.Builder().build())
        } else {
            adView.visibility = View.GONE
        }

        val btnReset: MaterialButton = findViewById(R.id.btn_reset)
        val btnTop: MaterialButton = findViewById(R.id.btn_top)
        val btnUnit: MaterialButton = findViewById(R.id.btn_unit)
        val btnCalibration: MaterialButton = findViewById(R.id.btn_calibrate)
        val btnLock: MaterialButton = findViewById(R.id.btn_lock)
        val btnInfo: MaterialButton = findViewById(R.id.btn_info)
        val seekBar: SeekBar = findViewById(R.id.seekBar)
        val scaleTv: TextView = findViewById(R.id.scale_factor)
        val calibrateTv: TextView = findViewById(R.id.calibrate)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        seekBar.visibility = View.GONE
        calibrateTv.visibility = View.GONE
        scaleTv.visibility = View.GONE
        btnReset.visibility = View.GONE


        val ruler = (-20..50).toList().toIntArray()
        recyclerView.adapter = MeasureAdapter(ruler, this)
        recyclerView.scrollToPosition(20)

        btnTop.setOnClickListener {
            recyclerView.suppressLayout(false)
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(20, 0)
            recyclerView.suppressLayout(scrollDisabled)
        }

        btnUnit.tag = if (getPreferences(MODE_PRIVATE).getBoolean("use_inches", false))
            "in" else "cm"
        btnUnit.text = btnUnit.tag as String
        btnUnit.setOnClickListener {
            it.tag = if (it.tag == "in") "cm" else "in"
            (it as MaterialButton).text = it.tag as String
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putBoolean("use_inches", it.tag == "in")
                apply()
            }
            recyclerView.adapter = MeasureAdapter(ruler, this)
            recyclerView.scrollToPosition(20)
            recyclerView.suppressLayout(scrollDisabled)
        }

        btnLock.setOnClickListener {
            scrollDisabled = !scrollDisabled
            recyclerView.suppressLayout(scrollDisabled)
            (it as MaterialButton).setIconResource(
                if (scrollDisabled) R.drawable.ic_lock else
                    R.drawable.ic_lock_open
            )
            it.background.setTint(
                ContextCompat.getColor(
                    this,
                    if (scrollDisabled) R.color.fgRed else R.color.fgBlue
                )
            )
        }

        btnInfo.setOnClickListener {
            startActivity(Intent(this, Impressum::class.java))
        }

        btnReset.setOnClickListener {
            seekBar.progress = 70
            scaleTv.text = getString(R.string.factor, 1f)
            // Update SharedPreferences
            with(getPreferences(Context.MODE_PRIVATE).edit()) {
                putFloat("calibration_factor", 1f)
                apply()
            }
            val adapter = recyclerView.adapter
            recyclerView.suppressLayout(false)
            adapter?.notifyDataSetChanged()
            (recyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(20, 0)
            recyclerView.suppressLayout(scrollDisabled)
        }

        btnCalibration.setOnClickListener {
            calibrationActive = !calibrationActive
            seekBar.visibility = if (calibrationActive) View.VISIBLE else View.GONE
            scaleTv.visibility = if (calibrationActive) View.VISIBLE else View.GONE
            calibrateTv.visibility = if (calibrationActive) View.VISIBLE else View.GONE
            btnReset.visibility = if (calibrationActive) View.VISIBLE else View.GONE
            btnTop.visibility = if (calibrationActive) View.INVISIBLE else View.VISIBLE
            btnUnit.visibility = if (calibrationActive) View.INVISIBLE else View.VISIBLE
            btnLock.visibility = if (calibrationActive) View.INVISIBLE else View.VISIBLE
            btnInfo.visibility = if (calibrationActive) View.INVISIBLE else View.VISIBLE
            (it as MaterialButton).setIconResource(if (calibrationActive) R.drawable.ic_back else
                R.drawable.ic_calibrate)
            seekBar.progress = round(100f * getPreferences(MODE_PRIVATE)
                .getFloat("calibration_factor", 1f) - 50).toInt()
            scaleTv.text = getString(R.string.factor, (seekBar.progress + 50)
                .toFloat() / 100)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                scaleTv.text = getString(R.string.factor, (i + 50).toFloat() / 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Update SharedPreferences
                with(getPreferences(Context.MODE_PRIVATE).edit()) {
                    putFloat("calibration_factor", (seekBar.progress + 50).toFloat()/100)
                    apply()
                }
                val adapter = recyclerView.adapter
                recyclerView.suppressLayout(false)
                adapter?.notifyDataSetChanged()
                (recyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPositionWithOffset(20, 0)
                recyclerView.suppressLayout(scrollDisabled)
            }
        })

        // Only on first start: Store the device default px for one cm
        val loading: RelativeLayout = findViewById(R.id.loading)
        if (!getPreferences(MODE_PRIVATE).getBoolean("initialized", false)) {
            loading.visibility = View.VISIBLE
            // Really not optimal, but it's working... Wait a second for views to be drawn, before
            // obtaining the height of the unit cube. ViewTreeObserver stuff didn't work on first
            // tries.
            Handler(Looper.getMainLooper()).postDelayed({
                val unitCube: View = findViewById(R.id.unit)
                with(getPreferences(Context.MODE_PRIVATE).edit()) {
                    putInt("base_cm", unitCube.height)
                    putBoolean("initialized", true)
                    commit()
                }
                recyclerView.adapter = MeasureAdapter(ruler, this)
                recyclerView.scrollToPosition(20)
                recyclerView.suppressLayout(scrollDisabled)
                loading.visibility = View.GONE
            }, 1000)
        } else loading.visibility = View.GONE

    }


    class MeasureAdapter(private val dataSet: IntArray, private val context: Activity) :
        RecyclerView.Adapter<MeasureAdapter.ViewHolder>() {

        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder).
         */
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val frame: View = view
            val leftMarker: TextView = view.findViewById(R.id.text_left)
            val rightMarker: TextView = view.findViewById(R.id.text_right)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_measure, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(vh: ViewHolder, position: Int) {

            val sp: SharedPreferences = context.getPreferences(MODE_PRIVATE)

            // Set the height of the element
            val baseCm: Int = sp.getInt("base_cm", 38)
            val inchFactor: Float = if (sp.getBoolean("use_inches", false)) 2.54f else 1f
            val calibrationFactor: Float = sp.getFloat("calibration_factor", 1f)

            val lp: ViewGroup.LayoutParams = vh.frame.layoutParams
            lp.height = (baseCm * calibrationFactor * inchFactor).toInt()
            vh.frame.layoutParams = lp


            vh.leftMarker.text = "${abs(dataSet[position])}"
            vh.rightMarker.text = context.getString(if (inchFactor == 1f) R.string.cm else
                R.string.inch, abs(dataSet[position]))
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = dataSet.size
    }
}