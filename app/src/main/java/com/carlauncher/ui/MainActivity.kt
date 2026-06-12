package com.carlauncher.ui

import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.carlauncher.adapter.AppAdapter
import com.carlauncher.databinding.ActivityMainBinding
import com.carlauncher.model.AppInfo
import com.carlauncher.service.MediaNotificationService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val appList = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter
    private val clockHandler = Handler(Looper.getMainLooper())
    private var mediaReceiver: BroadcastReceiver? = null
    private var locationManager: LocationManager? = null

    // ── 生命周期 ────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏（兼容 API 14）
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // 隐藏导航栏（API 14 用 SYSTEM_UI_FLAG_LOW_PROFILE，API 19+ 才有真正沉浸）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppGrid()
        setupClock()
        setupNavBar()
        setupMediaSection()
        setupWeather()
        registerMediaReceiver()
    }

    // ── 应用网格 ────────────────────────────────────
    private fun setupAppGrid() {
        adapter = AppAdapter(appList) { app ->
            try {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                intent?.let { startActivity(it) }
            } catch (e: Exception) { e.printStackTrace() }
        }
        binding.rvApps.layoutManager = GridLayoutManager(this, 5)
        binding.rvApps.adapter = adapter
        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        // API 14 兼容：不传 MATCH_ALL flag，使用 0
        val resolveList = pm.queryIntentActivities(intent, 0)
        appList.clear()
        resolveList
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString() }
            .forEach {
                appList.add(AppInfo(
                    label       = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon        = it.loadIcon(pm)
                ))
            }
        adapter.notifyDataSetChanged()
    }

    // ── 时钟 ────────────────────────────────────────
    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()
            binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            binding.tvDate.text = SimpleDateFormat("MM月dd日 EEEE", Locale.CHINESE).format(now.time)
            clockHandler.postDelayed(this, 1000)
        }
    }
    private fun setupClock() = clockHandler.post(clockRunnable)

    // ── 导航栏 ──────────────────────────────────────
    private fun setupNavBar() {
        binding.btnBack.setOnClickListener { onBackPressed() }
        binding.btnHome.setOnClickListener {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        // 最近任务 API 21+ 才有公开 API，低版本隐藏
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            binding.btnRecents.visibility = View.GONE
        } else {
            binding.btnRecents.setOnClickListener {
                sendBroadcast(Intent("com.carlauncher.ACTION_RECENTS"))
            }
        }
    }

    // ── 媒体控制 ────────────────────────────────────
    private fun setupMediaSection() {
        // NotificationListenerService 需要 API 18+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            binding.llMediaBar.visibility = View.GONE
            return
        }

        binding.btnPrev.setOnClickListener {
            sendBroadcast(Intent(MediaNotificationService.ACTION_CMD).apply {
                putExtra(MediaNotificationService.EXTRA_CMD, "prev")
                setPackage(packageName)
            })
        }
        binding.btnPlayPause.setOnClickListener {
            sendBroadcast(Intent(MediaNotificationService.ACTION_CMD).apply {
                putExtra(MediaNotificationService.EXTRA_CMD, "play")
                setPackage(packageName)
            })
        }
        binding.btnNext.setOnClickListener {
            sendBroadcast(Intent(MediaNotificationService.ACTION_CMD).apply {
                putExtra(MediaNotificationService.EXTRA_CMD, "next")
                setPackage(packageName)
            })
        }

        if (!isNotificationListenerEnabled()) {
            binding.tvMediaHint.visibility = View.VISIBLE
            binding.tvMediaHint.setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return false
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun registerMediaReceiver() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return
        mediaReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != MediaNotificationService.ACTION_MEDIA_UPDATE) return
                binding.tvMediaTitle.text =
                    intent.getStringExtra(MediaNotificationService.EXTRA_TITLE)
                        .takeIf { !it.isNullOrEmpty() } ?: "未在播放"
                binding.tvMediaArtist.text =
                    intent.getStringExtra(MediaNotificationService.EXTRA_ARTIST) ?: ""
                val playing = intent.getBooleanExtra(MediaNotificationService.EXTRA_IS_PLAYING, false)
                binding.btnPlayPause.text = if (playing) "⏸" else "▶"
            }
        }
        registerReceiver(mediaReceiver, IntentFilter(MediaNotificationService.ACTION_MEDIA_UPDATE))
    }

    // ── 天气 ────────────────────────────────────────
    private fun setupWeather() {
        // API 14 没有运行时权限，AndroidManifest 声明即可
        // 但 API 23+ 需要运行时请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
                binding.tvWeather.text = "🌡️ 授权位置后显示天气"
                return
            }
        }
        startLocationAndFetch()
    }

    private fun startLocationAndFetch() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        // 先用缓存位置快速显示
        val lastLoc = getLastKnownLocationCompat()
        if (lastLoc != null) fetchWeather(lastLoc.latitude, lastLoc.longitude)
        else binding.tvWeather.text = "🌡️ 定位中…"

        // 再注册监听，有更新就刷新
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 60_000L, 500f, locationListener
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    @Suppress("DEPRECATION")
    private fun getLastKnownLocationCompat(): Location? {
        val lm = locationManager ?: return null
        return try {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) { null }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            fetchWeather(location.latitude, location.longitude)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        lifecycleScope.launch {
            val w = WeatherHelper.fetch(lat, lon)
            if (w != null) {
                binding.tvWeather.text = "${w.icon} ${w.tempC}°C  ${w.description}"
            } else {
                binding.tvWeather.text = "🌡️ 天气获取失败"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationAndFetch()
        }
    }

    // ── 生命周期 ────────────────────────────────────
    override fun onResume() {
        super.onResume()
        loadApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
        mediaReceiver?.let { unregisterReceiver(it) }
        try { locationManager?.removeUpdates(locationListener) } catch (e: Exception) {}
    }
}
