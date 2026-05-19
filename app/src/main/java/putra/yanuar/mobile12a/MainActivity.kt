package putra.yanuar.mobile12a

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import mumayank.com.airlocationlibrary.AirLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import putra.yanuar.mobile12a.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var b: ActivityMainBinding
    lateinit var map: MapView
    val arrayItemPos = ArrayList<OverlayItem>()
    var updateLokasiSekaliSaja = false
    var lat = 0.0
    var lng = 0.0

    // Deklarasi AirLocation untuk manajemen lokasi dan runtime permission
    val airLocation = AirLocation(this, object : AirLocation.Callback {
        override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
            // Menangani kegagalan saat mengambil lokasi
        }

        override fun onSuccess(locations: ArrayList<Location>) {
            // Menampilkan latitude & longitude ke TextView saat berhasil mendapatkan koordinat
            lat = locations[0].latitude
            lng = locations[0].longitude
            b.textView.text = "Latitude = ${lat}\nLongitude = ${lng}"
        }
    }, false) // Argumen ketiga diatur berdasarkan variabel boolean updateLokasiSekaliSaja di runtime jika dinamis

    fun drawMap() {
        // Menambahkan kompas ke map
        val compass = CompassOverlay(
            applicationContext,
            InternalCompassOrientationProvider(applicationContext),
            map
        )
        compass.enableCompass()
        map.overlays.add(0, compass)

        // Menghapus isi arrayItemPos jika sudah ada sebelumnya agar marker tidak redundant
        if (arrayItemPos.size > 0) {
            for (i in 1 until map.overlays.size) {
                map.overlays.removeAt(1)
            }
        }
        arrayItemPos.clear()

        // Menampilkan lokasi pengguna di peta
        val gps = GpsMyLocationProvider(applicationContext)
        val me = MyLocationNewOverlay(gps, map)
        me.enableMyLocation()
        map.overlays.add(1, me)

        // Mengatur posisi zoom dan animasi perpindahan fokus peta
        val startPoint = GeoPoint(lat, lng)
        map.controller.setZoom(20.0)
        map.controller.animateTo(startPoint)

        // Menambahkan beberapa titik lokasi kustom sebagai marker
        arrayItemPos.add(
            OverlayItem("Polinema Kampus Kediri 2", "Maskumambang", GeoPoint(-7.80203, 111.97982))
        )
        arrayItemPos.add(
            OverlayItem("Polinema Kampus Kediri 1", "Semampir", GeoPoint(-7.801166, 112.008228))
        )

        // Memasang marker ke dalam map beserta event handler-nya
        map.overlays.add(
            ItemizedIconOverlay<OverlayItem>(
                arrayItemPos,
                object : OnItemGestureListener<OverlayItem> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                        Toast.makeText(applicationContext, item.title, Toast.LENGTH_SHORT).show()
                        return true
                    }

                    override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                        Toast.makeText(applicationContext, item.snippet, Toast.LENGTH_SHORT).show()
                        return true
                    }
                }, applicationContext
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        b.chip.setOnClickListener(this)

        // Penting! Diperlukan sharedPreferences default oleh osmdroid agar caching peta berjalan lancar
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        // Inisialisasi objek map
        map = b.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    // Melakukan forward hasil perizinan (permission) runtime ke pustaka AirLocation
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Melakukan forward hasil activity (seperti mengaktifkan GPS lewat dialog sistem) ke AirLocation
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation.onActivityResult(requestCode, resultCode, data)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.chip -> {
                // Toggle status tracking berdasarkan keadaan Chip (Live Update)
                updateLokasiSekaliSaja = b.chip.isChecked.not()
                airLocation.start()
                drawMap()
            }
        }
    }
}