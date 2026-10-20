package com.example.mapaygps; // Cambia esto por el nombre del paquete de tu proyecto

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapaActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Lista en memoria para almacenar las coordenadas guardadas por el GPS
    private final List<LatLng> listaCoordenadas = new ArrayList<>();
    private Polyline polylineRuta;
    private Marker markerActual;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Creamos un contenedor visual dinámico por código (Evita usar XML y la clase R)
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(FrameLayout.generateViewId());
        setContentView(frameLayout);

        // Inicializar el cliente de ubicación GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Insertar el fragmento del mapa de manera dinámica en la pantalla
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(frameLayout.getId(), mapFragment);
        fragmentTransaction.commit();

        mapFragment.getMapAsync(this);

        crearConfiguracionDeUbicacion();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        verificarYPedirPermisos();
    }

    private void verificarYPedirPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            activarMonitoreoUbicacion();
        }
    }

    private void activarMonitoreoUbicacion() {
        try {
            mMap.setMyLocationEnabled(true);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            LatLng nuevaPosicion = new LatLng(location.getLatitude(), location.getLongitude());

                            // 1. Guardar la coordenada actual en la lista en memoria
                            listaCoordenadas.add(nuevaPosicion);

                            // 2. Crear o actualizar el marcador de la posición actual del usuario
                            if (markerActual == null) {
                                markerActual = mMap.addMarker(new MarkerOptions().position(nuevaPosicion).title("Mi posición actual"));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, 17f));
                            } else {
                                markerActual.setPosition(nuevaPosicion);
                            }

                            // 3. Dibujar o actualizar la ruta recorrida en el mapa
                            actualizarRutaEnMapa();
                        }
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void crearConfiguracionDeUbicacion() {
        // Configurar la frecuencia del GPS (cada 10 segundos)
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
    }

    private void actualizarRutaEnMapa() {
        if (polylineRuta == null) {
            // Crear la línea de ruta uniendo los puntos guardados
            PolylineOptions options = new PolylineOptions()
                    .addAll(listaCoordenadas)
                    .width(12)
                    .color(getResources().getColor(android.R.color.holo_blue_dark));
            polylineRuta = mMap.addPolyline(options);
        } else {
            // Actualizar la traza de la ruta con los nuevos puntos agregados
            polylineRuta.setPoints(listaCoordenadas);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activarMonitoreoUbicacion();
            } else {
                Toast.makeText(this, "Se necesitan permisos de ubicación para usar el mapa.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}