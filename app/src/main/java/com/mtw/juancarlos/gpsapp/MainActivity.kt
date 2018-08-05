package com.mtw.juancarlos.gpsapp

import android.Manifest
import android.animation.AnimatorInflater
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.mtw.juancarlos.gpsapp.R.id.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        private val REQUEST_LOCATION_PERMISSION = 1
        private val REQUEST_CHECK_SETTINGS = 1
    }

    private lateinit var fusedLocationClient:FusedLocationProviderClient
    private var requestingLocationUpdates = false
    private lateinit var locationCallback: LocationCallback

    private val animRotat by lazy{
        AnimatorInflater.loadAnimator(this,R.animator.rotate)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.visibility = View.INVISIBLE

        fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(this)

        //Evalua si se tiene el permiso al GPS
        button_location.setOnClickListener{
            if (checkPermission()){
                Log.e("GPSANDROIDAPP", "Acceso Concedido")
                getLastLocation()
            } else {
                Log.e("GPSANDROIDAPP", "Acceso denegado")
            }
        }




        animRotat.setTarget(imageview_android)
        button_startTrack.setOnClickListener{
            if (checkPermission()){
                if (!requestingLocationUpdates){
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                }
            } else {
                Log.e("GPSANDROIDAPP", "Acceso denegado")
            }

        }
        locationCallback = object :LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                onLocationChanged(locationResult.lastLocation)
            }
        }
    }

    //Se inicia la animación
    private fun startLocationUpdates(){
        progressBar.visibility = View.VISIBLE
        animRotat.start()
        requestingLocationUpdates = true
        button_startTrack.text = "Detener"
        textview_location.text="Localizando..."

        val locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->

            Log.e("GPSANDROIDAPP", "OnSucessListener Task")
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)

            } catch (e: SecurityException) {
                Log.e("GPSANDROIDAPP", "SecEx "+ e.message)
                Toast.makeText(this@MainActivity,"secex:"+ e.message,Toast.LENGTH_LONG)
                progressBar.visibility = View.INVISIBLE
            }
        }


        task.addOnFailureListener { exception ->
            progressBar.visibility = View.INVISIBLE
            if (exception is ResolvableApiException) {

                try {

                    Log.e("GPSANDROIDAPP", "OnFailureListener")
                    exception.startResolutionForResult(this@MainActivity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {

                }
            }
        }

    }

    //Detiene la animación
    private fun stopLocationUpdates(){
        if (requestingLocationUpdates){
            requestingLocationUpdates = false
            progressBar.visibility = View.INVISIBLE
            button_startTrack.text = "Rastrear"
            textview_location.text ="Presiona el boton para obtener la última ubicación"
            animRotat.end()
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    //onResume y onPause comienzan y detienen las lecturas al GPS.
    override fun onResume() {
        if (requestingLocationUpdates) startLocationUpdates()
        super.onResume()
    }
    override fun onPause() {
        if (requestingLocationUpdates) {
            stopLocationUpdates()
            requestingLocationUpdates = false
        }
        super.onPause()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK){
                    Log.e("GPSANDROIDAPP","ACCESS GRANTED")
                    startLocationUpdates()
                }
                return
            }
        }
    }

    //Recupera la última posición GPS
    private fun getLastLocation(){
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                location: Location? ->
                Log.i("GPSAPP","OnSuccessListener lastlocation")
                //   Log.i("GPSAPP",location!!.latitude.toString() +""+ location!!.longitude.toString() )
                onLocationChanged(location )
            }
                    .addOnFailureListener {
                        Log.i("GPSAPP","OnFailureListener lastlocation")
                        Toast.makeText(this,"Error en la lectura del GPS", Toast.LENGTH_SHORT)
                    }
        }catch(e: SecurityException){}

    }

    //Muestra en el TextView las coordenadas obtenidas del GPS
    private fun onLocationChanged(location:Location?){

        if (location != null){
            textview_location.text = getString(R.string.location_text,
                    location?.latitude,
                    location?.longitude,
                    location?.time
            )
        } else {
            textview_location.text = "No se recuperó la ubicación"
        }
    }

    //Método que solicita expícitamente el permiso GPS
    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            return true
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return false
        }

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode){
            REQUEST_LOCATION_PERMISSION ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {}
                else
                    Toast.makeText(this,"Acceso al GPS denegado",Toast.LENGTH_SHORT).show()
                return
            }
        }
    }
}
