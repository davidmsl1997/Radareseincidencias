package com.example.david.radareseincidencias.Controladores;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.david.radareseincidencias.R;
import com.example.david.radareseincidencias.sampledata.Incidencia;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import de.hdodenhof.circleimageview.CircleImageView;

public class DisplayMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, TextToSpeech.OnInitListener, GoogleMap.OnMarkerDragListener {
    /*PARA EL MAPA*/
    private MapView mapView;
    private GoogleMap gmap;
    private FusedLocationProviderClient fusedLocationClient; //Para acceder a la API de localización
    LocationRequest mLocationRequest;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private Location lastSpeedLocation; //Servirá para calcular la velocidad

    /*PARA EL MENÚ*/
    private TextView tvUserName;
    private CircleImageView ivProfilePhotoPhoto;
    private boolean delete_user;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    /*PARA SALIR DE LA APP CUANDO SE PULSA DOS VECES EL BOTON ATRAS*/
    private boolean canExitApp;

    /*BOTONES*/
    private ImageButton traficoButton, obrasButton, radarButton, otrosButton;
    private FloatingActionButton startButton, stopButton;
    /*PARA LAS INCIDENCIAS*/
    private TextToSpeech speak;
    private FirebaseAuth mAuth;
    private Geocoder geo;
    private List<Address> direccionUsuario;
    private ArrayList<String> incidenciasAvisadas; //Para no avisar la misma incidencia dos veces
    private String[] fieldsMarker;
    private  AlertDialog dialog2, dialog_velocidad, dialog_otros;
    private LatLng currentLocation; //Almacena la última ubicación recibida para poder guardar las incidencias
    private int [] idSenales;
    private HashMap<String, Integer> hmDistancias; //Para saber a qué distancia avisar de cada incidencia
    private HashMap<String, Boolean> hmAvisos; //Para saber de qué incidencias quiere el usuario que se le avise
    private HashMap<String, Integer> imgIncidencias;
    private ArrayList<MarkerOptions> markers;

    /*PARA TRADUCIR*/
    private HashMap<String, String> hmTraduccion;

    /*NOMBRE DEL USUARIO ACTUAL*/
    private String currentUser;

    /*PARA EL PROGRESS BAR*/
    private AlertDialog dialog_progressBar;

    /*PARA EL DOBLE CLICK DE LOS MARCADORES*/
    private Date clickOneTime;
    private String markerClickID;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_map);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //La pantalla no se puede bloquear sola mientras se conduce
        this.idSenales = new int [] {R.id.treinta, R.id.cuarenta, R.id.cincuenta, R.id.sesenta, R.id.setenta,
                R.id.ochenta, R.id.noventa, R.id.cien, R.id.cientodiez, R.id.cientoveinte};

        mapView = findViewById(R.id.map_view);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navview);
        View headerView = navigationView.getHeaderView(0);
        tvUserName = (TextView) headerView.findViewById(R.id.headerUserName);
        startButton = (FloatingActionButton)findViewById(R.id.startButton);
        stopButton = (FloatingActionButton)findViewById(R.id.stopButton);
        traficoButton = (ImageButton)findViewById(R.id.traficoButton);
        obrasButton = (ImageButton)findViewById(R.id.obrasButton);
        radarButton = (ImageButton)findViewById(R.id.radarButton);
        otrosButton = (ImageButton)findViewById(R.id.otrosButton);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_display_map);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
            getSupportActionBar().setTitle(R.string.titulo_mapa);
        }

        traficoButton.animate().translationX(-500);
        obrasButton.animate().translationX(-500);
        radarButton.animate().translationX(-500);
        otrosButton.animate().translationX(-500);
        stopButton.animate().translationX(-500);

        mAuth = FirebaseAuth.getInstance();
        this.currentLocation = null;

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) { //Ha existido otra actividad que se detuvo por situaciones anormales
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        //CREAMOS EL CLIENTE DE LOS SERVICIOS DE LOCALIZACIÓN
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); //Se conecta a la API

        //CREAMOS EL MAPA
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        lastSpeedLocation = null;

        delete_user = false;

        final SwitchCompat drawerSwitch = (SwitchCompat) navigationView.getMenu().findItem(R.id.switch_item).getActionView();
        final SwitchCompat drawerSwitchNight = (SwitchCompat) navigationView.getMenu().findItem(R.id.switch_item_dark).getActionView();
        drawerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    gmap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else {
                    gmap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    if(drawerSwitchNight.isChecked()){
                        gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(DisplayMapActivity.this, R.raw.dark_map));
                    } else {
                        gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(DisplayMapActivity.this, R.raw.style_json));
                    }
                }
            }
        });
        drawerSwitchNight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(DisplayMapActivity.this, R.raw.dark_map));
                } else {
                    if(drawerSwitch.isChecked()){
                        gmap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    } else {
                        gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(DisplayMapActivity.this, R.raw.style_json));
                    }
                }
            }
        });

        //CONFIGURAMOS EL NAVIGATION VIEW PARA QUE REACCIONES A LAS INTERACCIONES DEL USUARIO => PARA QUE MUESTRE EL MENÚ
        final FirebaseUser user = mAuth.getCurrentUser();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_configuracion:
                        Intent intentConfiguracion = new Intent(DisplayMapActivity.this, ConfigurarAlertasActivity.class);
                        startActivity(intentConfiguracion);
                        break;
                    case R.id.menu_verPerfil:
                        Intent intentPerfil = new Intent(DisplayMapActivity.this, PerfilActivity.class);
                        startActivity(intentPerfil);
                        break;
                    case R.id.menu_logOut: //Cierra sesión
                        FirebaseAuth.getInstance().signOut();
                        File f = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
                        if (f.exists()){
                            f.delete();
                        }
                        finish(); //Se vuelve a la vista anterior
                        break;
                    case R.id.menu_removeUser:
                        AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMapActivity.this, R.style.CustomDialogTheme);
                        builder.setTitle(R.string.title_delete_account);
                        builder.setMessage(R.string.ask_delete_account);


                        builder.setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                dialog2 = null;
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(DisplayMapActivity.this, R.style.CustomDialogTheme);
                                builder2.setTitle(R.string.title_delete_account);
                                builder2.setMessage(R.string.ask_delete_data);

                                builder2.setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        delete_user = true;
                                        final String uid = user.getUid();
                                        final FirebaseDatabase dataBase = FirebaseDatabase.getInstance();
                                        final StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                                        StorageReference ficheroRef = mStorageRef.child(uid + "/" + "fichero_configuracion.xml");
                                        /*ELIMINAMOS EL FICHERO DE CONFIGURACIÓN*/
                                        ficheroRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                DatabaseReference refDB = dataBase.getReference("/database/userData"); //Lo borra de la base de datos
                                                final DatabaseReference refChild = refDB.child(uid);

                                                refChild.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if (!dataSnapshot.child("profilePhoto").getValue().toString().equals(" ")) { //Si hay almacenada una foto suya, la borramos
                                                            StorageReference imgRef = mStorageRef.child(uid + "/" + "profilePhoto");
                                                            /*ELIMINAMOS LA FOTO DE PERFIL, SI LA HAY*/
                                                            imgRef.delete();
                                                        }

                                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                        /*CERRAMOS SESIÓN*/
                                                        FirebaseAuth.getInstance().signOut();
                                                        /*ELIMINAMOS EL USUARIO*/
                                                        user.delete().addOnCompleteListener(new OnCompleteListener<Void>() { //Lo borra de authentication
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Log.d("ELIMINAR_CUENTA", "User account deleted.");
                                                                    finish();
                                                                }
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(DisplayMapActivity.this, R.string.user_error, Toast.LENGTH_SHORT)
                                                                        .show();
                                                            }
                                                        });
                                                        /*BORRAMOS LOS DATOS DE LA BASE DE DATOS*/
                                                        refChild.setValue(null);
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception exception) {
                                                Toast.makeText(DisplayMapActivity.this, R.string.user_error , Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });

                                builder2.setNeutralButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog2.dismiss();
                                    }
                                });

                                dialog2 = builder2.create();

                                dialog2.show();
                            }
                        });

                        builder.setNeutralButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();

                        dialog.show();

                        break;
                    case R.id.switch_item:
                    case R.id.switch_item_dark:
                        break;

                    case R.id.menu_verRanking:
                        Intent intentRanking = new Intent(DisplayMapActivity.this, RankingActivity.class);
                        startActivity(intentRanking);
                }

                return false; //Para que no se queden marcadas las opciones cuando se seleccionan.
            }
        });
        //RELLENAMOS EL MAPA CON LAS INCIDENCIAS DE LA BASE DE DATOS
        showIncidencias();

        //PARA PODER ACCEDER A LAS DIRECCIONES DE LAS INCIDENCIAS
        this.geo = new Geocoder(this.getApplicationContext(), Locale.getDefault());

        //ARRAYLIST QUE GUARDA LAS INCIDENCIAS DE LAS QUE YA SE HA AVISADO PARA NO VOLVER A AVISAR
        this.incidenciasAvisadas = new ArrayList<String>();

        //ARRAYLIST QUE GUARDA LAS INCIDENCIAS QUE HAY PUESTAS EN EL MAPA PARA PODER AVISAR DE ELLAS
        this.markers = new ArrayList<MarkerOptions>();

        //DESCARGA/COGE EL FICHERO DE CONFIGURACIÓN DE LAS ALERTAS
        File f = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
        if (f.exists()) {
            Boolean valorLeido;
            SharedPreferences prefs = getSharedPreferences("fichero_configuracion", Context.MODE_PRIVATE);

            String fileUser = prefs.getString("user", null);

            if (!fileUser.equals(user.getUid())){ //El fichero de configuracion guardado no es el del usuario que inicia sesión
                f.delete();
                StorageReference pathReference = FirebaseStorage.getInstance().getReference().child(user.getUid()).child("fichero_configuracion.xml");
                File localFile = null;
                try {
                    localFile = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");

                } catch (Exception e) {
                    e.printStackTrace();
                }
                pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        SharedPreferences prefs2 = getSharedPreferences("fichero_configuracion", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs2.edit();
                        File newFile = new File ("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
                        //Leemos el fichero descargado para generar un nuevo SharedPreferences que sustituya al anterior
                        try {
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document doc = dBuilder.parse(newFile);

                            editor.putString("user", doc.getElementsByTagName("string").item(0).getTextContent());

                            NodeList listSwitch = doc.getElementsByTagName("boolean");
                            NodeList listEditText = doc.getElementsByTagName("int");

                            for (int i = 0; i<listSwitch.getLength(); i++){
                                Node nNode = listSwitch.item(i); //Coge un elemento de la lista
                                if(nNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eElement = (Element) nNode;
                                    editor.putBoolean(eElement.getAttribute("name"), Boolean.parseBoolean(eElement.getAttribute("value")));
                                }
                            }

                            for (int i = 0; i<listEditText.getLength(); i++){
                                Node nNode = listEditText.item(i); //Coge un elemento de la lista
                                if(nNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element eElement = (Element) nNode;
                                    editor.putInt(eElement.getAttribute("name"), Integer.parseInt(eElement.getAttribute("value")));
                                }
                            }

                            editor.commit();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                    }
                });

            }
        } else {
            StorageReference pathReference = FirebaseStorage.getInstance().getReference().child(user.getUid()).child("fichero_configuracion.xml");
            File localFile = null;
            try {
                localFile = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
            } catch (Exception e) {
                e.printStackTrace();
            }
            pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    SharedPreferences prefs2 = getSharedPreferences("fichero_configuracion", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs2.edit();

                    File newFile = new File ("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
                    //Leemos el fichero descargado para generar un nuevo SharedPreferences
                    try {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(newFile);

                        editor.putString("user", doc.getElementsByTagName("string").item(0).getTextContent());

                        NodeList listSwitch = doc.getElementsByTagName("boolean");
                        NodeList listEditText = doc.getElementsByTagName("int");

                        for (int i = 0; i<listSwitch.getLength(); i++){
                            Node nNode = listSwitch.item(i); //Coge un elemento de la lista
                            if(nNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element eElement = (Element) nNode;
                                editor.putBoolean(eElement.getAttribute("name"), Boolean.parseBoolean(eElement.getAttribute("value")));
                            }
                        }

                        for (int i = 0; i<listEditText.getLength(); i++){
                            Node nNode = listEditText.item(i); //Coge un elemento de la lista
                            if(nNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element eElement = (Element) nNode;
                                editor.putInt(eElement.getAttribute("name"), Integer.parseInt(eElement.getAttribute("value")));
                            }
                        }

                        editor.commit();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    Log.d("INICIO", "ERROR");
                }
            });

        }

        hmDistancias = new HashMap<String, Integer>();
        hmAvisos = new HashMap<String, Boolean>();

        /*RELLENAMOS EL HASHMAP PARA PODER TRADUCIR EL TITULO DE LOS CUADROS DE DIALOGO DE LAS ALERTAS*/
        if(Locale.getDefault().getLanguage().equals("en")){
            Log.d("INGLES", "Voy a rellenar el hashmap");

            hmTraduccion = new HashMap<String, String>();
            hmTraduccion.put("Policía", "Police");
            hmTraduccion.put("Radar", "Radar");
            hmTraduccion.put("Obras", "Works");
            hmTraduccion.put("Nieve", "Snow");
            hmTraduccion.put("Accidente", "Accident");
            hmTraduccion.put("Animales", "Animals");
            hmTraduccion.put("Tráfico", "Traffic");
            hmTraduccion.put("Lluvia", "Rain");
            hmTraduccion.put("Asfalto en mal estado", "Asphalt in poor condition");
            hmTraduccion.put("Calle cortada", "Cut street");
        }

        /*RELLENAMOS EL HASHMAP DE LAS IMAGENES DE LAS INCIDENCIAS PARA CUANDO SE MUESTREN EN EL CUADRO DE DIALOGO AL PULSAR EN ELLAS*/
        //Las de los radares no se ponen, se eligen más abajo en función de la velocidad
        imgIncidencias = new HashMap<String, Integer>();
        imgIncidencias.put("Policía", R.drawable.policia_100);
        imgIncidencias.put("Accidente", R.drawable.accidente_100);
        imgIncidencias.put("Lluvia", R.drawable.lluvia_100);
        imgIncidencias.put("Calle cortada", R.drawable.calle_cortada_original);
        imgIncidencias.put("Nieve", R.drawable.nieve_100);
        imgIncidencias.put("Animales", R.drawable.animales_100);
        imgIncidencias.put("Asfalto en mal estado", R.drawable.bache_100);
        imgIncidencias.put("Tráfico", R.drawable.desfile_de_coches_original);
        imgIncidencias.put("Obras", R.drawable.obras_original);

        /*INICIALIZAMOS EL SPEAKER QUE HABLARA CUANDO SE REPORTEN LAS INCIDENCIAS*/
        speak = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //Locale loc = new Locale("es", "ES");
            Locale loc = new Locale(Locale.getDefault().getLanguage(), Locale.getDefault().getCountry());
            int result = speak.setLanguage(loc);

            //Si el idioma no está instalado, le ponemos al usuario la pantalla para que lo instale
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("ALERTA", "This Language is not supported");
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        } else {
            Log.e("ALERTA", "Initilization Failed! " + "Hola");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        SharedPreferences prefs = getSharedPreferences("fichero_configuracion", Context.MODE_PRIVATE);
        /*RELLENAMOS EL HASHMAP DE LAS DISTANCIAS PARA SABER CUANDO HAY QUE AVISAR EN CADA UNA*/
        hmDistancias.clear();
        hmDistancias.put("Policía", prefs.getInt("etPolicia", 200));
        hmDistancias.put("Accidente", prefs.getInt("etAccidente", 200));
        hmDistancias.put("Lluvia", prefs.getInt("etLluvia", 200));
        hmDistancias.put("Calle cortada", prefs.getInt("etCalleCortada", 200));
        hmDistancias.put("Radar", prefs.getInt("etRadar", 200));
        hmDistancias.put("Nieve", prefs.getInt("etNieve", 200));
        hmDistancias.put("Animales", prefs.getInt("etAnimales", 200));
        hmDistancias.put("Asfalto en mal estado", prefs.getInt("etAsfalto", 200));
        hmDistancias.put("Tráfico", prefs.getInt("etTrafico", 200));
        hmDistancias.put("Obras", prefs.getInt("etObras", 200));

        /*RELLENAMOS EL HASHMAP DE LOS AVISOS PARA SABER DE QUÉ INCIDENCIAS QUIERE SER AVISADO EL USUARIO*/
        hmAvisos.clear();
        hmAvisos.put("Policía", prefs.getBoolean("Policia", true));
        hmAvisos.put("Accidente", prefs.getBoolean("Accidente", true));
        hmAvisos.put("Lluvia", prefs.getBoolean("Lluvia", true));
        hmAvisos.put("Calle cortada", prefs.getBoolean("CalleCortada", true));
        hmAvisos.put("Radar", prefs.getBoolean("Radar", true));
        hmAvisos.put("Nieve", prefs.getBoolean("Nieve", true));
        hmAvisos.put("Animales", prefs.getBoolean("Animales", true));
        hmAvisos.put("Asfalto en mal estado", prefs.getBoolean("Asfalto", true));
        hmAvisos.put("Tráfico", prefs.getBoolean("Trafico", true));
        hmAvisos.put("Obras", prefs.getBoolean("Obras", true));

        Log.d("AVISOS", ""+hmAvisos);

        //CONFIGURAMOS EL HEADER DEL NAVIGATION VIEW CON LOS DATOS DEL USUARIO ACTUAL
        View headerView = navigationView.getHeaderView(0);
        ivProfilePhotoPhoto = (CircleImageView) headerView.findViewById(R.id.profilePhoto);
        final FirebaseUser user = mAuth.getCurrentUser();
        DatabaseReference dbUsers = FirebaseDatabase.getInstance().getReference().child("database").child("userData").child(user.getUid());
        dbUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    tvUserName.setText(dataSnapshot.child("userName").getValue().toString());
                } catch (Exception e){
                    Log.d("ELIMINAR_CUENTA", "Estoy en el catch");

                    FirebaseAuth.getInstance().signOut();
                    File f = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
                    if (f.exists()){
                        f.delete();
                    }
                    finish();
                    return; //Para que no siga ejecutando el método y no "pete"
                }

                /*RELLENAMOS EL NOMBRE DE USUARIO*/
                currentUser = dataSnapshot.child("userName").getValue().toString();

                /*PONEMOS LA FOTO DE PERFIL QUE CORRESPONDA*/
                StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                StorageReference imgRef = null;
                if (dataSnapshot.child("profilePhoto").getValue().toString().equals(" ")) { //Si no hay nada, se muestra la imagen por defecto
                    Log.d("IMAGEN", "Voy a poner la imagen por defecto");
                    imgRef = mStorageRef.child("general/user.png");
                    Glide.with(DisplayMapActivity.this).using(new FirebaseImageLoader()).load(imgRef).into(ivProfilePhotoPhoto);
                } else {
                    Log.d("IMAGEN", "Voy a poner la imagen de FireBase");
                    File f = new File("/data/data/" + getPackageName() + "/profilePhoto.jpg");
                    if (f.exists()){
                        if(f.delete()){
                            //Lo borramos porque puede ser antiguo
                            Log.d("IMAGEN", "He borrado la imagen");
                        }
                    }

                    StorageReference pathReference = FirebaseStorage.getInstance().getReference().child(user.getUid()).child("profilePhoto");
                    File localFile = null;
                    try {
                        localFile = new File("/data/data/" + getPackageName() + "/profilePhoto.jpg");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            /*YA HA DESCARGADO LA IMAGEN*/
                            Log.d("IMAGEN", "Voy a actualizar la imagen en el iv");
                            ivProfilePhotoPhoto.setImageURI(Uri.fromFile(new File("/data/data/" + getPackageName() + "/profilePhoto.jpg")));
                            Log.d("IMAGEN", "He actualizado la imagen en el iv");
                            //ivProfilePhotoPhoto = null;
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ERRORDATO", "Error!", databaseError.toException());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        /*COMRPUEBA SI HAY PERMISOS DE LOCALIZACIÓN*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gmap = googleMap;
            gmap.setMinZoomPreference(18);
            gmap.setOnMarkerClickListener(this);
            gmap.setMyLocationEnabled(true); //MUESTRA EL CIRCULO DE LA LOCALIZACIÓN
            gmap.setMapStyle(MapStyleOptions.loadRawResourceStyle(DisplayMapActivity.this, R.raw.style_json));

            //Cogemos la ubicación actual del usuario
            Task<Location> lastLocationTask = fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        if (ActivityCompat.checkSelfPermission(DisplayMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(DisplayMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LatLng ny = new LatLng(location.getLatitude(), location.getLongitude());
                            gmap.moveCamera(CameraUpdateFactory.newLatLng(ny));
                        }
                    }
                }
            });
        }
    }


    //Método que controla el comportamiento del botón de atrás
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (!canExitApp) {
            canExitApp = true;
            Toast.makeText(this, R.string.exit, Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    canExitApp = false;
                }
            }, 2000);
        } else {
            //super.onBackPressed();
            moveTaskToBack(true);
        }
    }

    //Para cuando se pulse el icono del menú que se abra el navigation drawer
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showIncidencias() {
        DatabaseReference refDB = FirebaseDatabase.getInstance().getReference().child("database").child("incidencias");
        refDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) { //Recorremos todas las incidencias que hay
                    boolean flag_antigua = false;

                    //Comprobamos la fecha de las incidencias, si se pasa de fecha, no se pone en el mapa
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    Date fecha_actual = new Date();
                    Date fecha_incidencia = null;
                    try {
                        fecha_incidencia = formatter.parse(ds.child("fecha").getValue().toString());
                    } catch (Exception e ){
                        //MANEJAR EL ERROR
                    }

                    int diferencia = (int) ((fecha_actual.getTime()-fecha_incidencia.getTime()));
                    int horas = 0;
                    int dias = 0;

                    switch (ds.child("tipo").getValue().toString()){
                        case "Tráfico":
                            horas = diferencia/3600000;
                            if (horas >= 3){
                                flag_antigua = true;
                            }
                            break;
                        case "Accidente":
                        case "Policía":
                        case "Radar":
                        case "Nieve":
                        case "Animales":
                        case "Lluvia":
                            dias = diferencia/86400000;
                            if (dias >= 1){
                                flag_antigua = true;
                            }
                            break;
                        case "Obras":
                        case "Calle cortada":
                        case "Asfalto en mal estado":
                            dias = diferencia/86400000;
                            if (dias >= 7){
                                flag_antigua = true;
                            }
                            break;
                    }

                    if (flag_antigua == true){
                        Log.d("INCIDENCIAA", "Es antigua una incidencia de tipo: " + ds.child("tipo").getValue().toString());
                        continue; //Se considera que la incidencia es antigua y no hace falta mostrarla en el mapa
                    }

                    Log.d("INCIDENCIAA", "Voy a mostrar una incidencia de tipo: " + ds.child("tipo").getValue().toString());

                    double latitud = Double.valueOf(ds.child("latitud").getValue().toString());
                    double longitud = Double.valueOf(ds.child("longitud").getValue().toString());

                    List<Address> addresses;
                    Geocoder geo = new Geocoder(DisplayMapActivity.this.getApplicationContext(), Locale.getDefault());
                    addresses = null;
                    String calle = null;
                    String [] elementosDireccion;
                    try {
                        addresses = geo.getFromLocation(latitud, longitud, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (addresses.isEmpty()) {
                        //Poner toast de error
                    }
                    else {
                        if (addresses.size() > 0) {
                            elementosDireccion = addresses.get(0).getAddressLine(0).split(",");
                            calle = elementosDireccion[0]; //El primer elemento es la calle
                            Log.d("INCIDENCIAM", "La calle es: " + calle);
                        }
                    }

                    String tipo = ds.child("tipo").getValue().toString();
                    LatLng point = new LatLng(latitud, longitud);
                    MarkerOptions marker = new MarkerOptions();
                    marker.position(point);
                    marker.draggable(true);
                    String velocidad = "0";
                    if (tipo.equals("Radar")){
                        velocidad = ds.child("velocidad").getValue().toString();
                    }
                    //Se pone como título al marcador la Clave de la base de datos, para poder localizarlo fácilmente en ella y la calle para cuando se avisa
                    marker.title(tipo + ":" + ds.getKey() + ":" + calle + ":" + velocidad + ":" + ds.child("usuario").getValue().toString());

                    switch (tipo) {
                        case "Tráfico":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.desfile_de_coches_original_32));
                            break;
                        case "Radar":
                            switch (ds.child("velocidad").getValue().toString()){
                                case "30":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.treinta_32));
                                    break;
                                case "40":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.cuarenta_32));
                                    break;
                                case "50":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.cincuenta_32));
                                    break;
                                case "60":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.sesenta_32));
                                    break;
                                case "70":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.setenta_32));
                                    break;
                                case "80":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ochenta_32));
                                    break;
                                case "90":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.noventa_32));
                                    break;
                                case "100":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.cien_32));
                                    break;
                                case "110":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.cientodiez_32));
                                    break;
                                case "120":
                                    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.cientoveinte_32));
                                    break;
                            }
                            break;
                        case "Calle cortada":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.calle_cortada_32));
                            break;
                        case "Obras":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.obras_original_32));
                            break;
                        case "Accidente":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.accidente_32));
                            break;
                        case "Policía":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.policia_32));
                            break;
                        case "Nieve":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.nieve_32));
                            break;
                        case "Lluvia":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.lluvia_32));
                            break;
                        case "Asfalto en mal estado":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.bache_32));
                            break;
                        case "Animales":
                            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.animales_32));
                            break;

                    }
                    gmap.addMarker(marker);
                    markers.add(marker);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        DatabaseReference refDB = null;
        fieldsMarker = marker.getTitle().split(":"); //El primer elemento es el tipo de incidencia y el segundo es el ID que tiene en la base de datos
        Log.d("ALERTA", "Voy a mostrar una alerta de tipo: " + fieldsMarker[0]);
        refDB = FirebaseDatabase.getInstance().getReference().child("database").child("incidencias");

        refDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayMapActivity.this, R.style.CustomDialogTheme);
                View dialogView = getLayoutInflater().inflate(R.layout.cuadro_dialogo_incidencias, null); //Cogemos la vista personalizada del cuadro de diálogo

                builder.setView(dialogView);
                final AlertDialog dialog;

                TextView tvTitle = dialogView.findViewById(R.id.dialogoTitle);
                TextView tvDireccion = dialogView.findViewById(R.id.dialogoDireccion);
                TextView tvUsuario = dialogView.findViewById(R.id.dialogoUser);
                TextView tvInfo = dialogView.findViewById(R.id.dialogoInfo);
                ImageView img = dialogView.findViewById(R.id.imageDialogo);
                Button b = dialogView.findViewById(R.id.buttonAceptar);

                /*MIRAMOS A VER EN QUE IDIOMA ESTA LA APP PARA PONER EL TITULO BIEN*/
                if (Locale.getDefault().getLanguage().equals("en")){
                    tvTitle.setText(hmTraduccion.get(fieldsMarker[0]));
                } else {
                    tvTitle.setText(fieldsMarker[0]);
                }

                Resources res = getResources();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (ds.getKey().equals(fieldsMarker[1])) { //Hemos encontrado la incidencia que nos interesaba
                        tvDireccion.setText(res.getString(R.string.calle) + " " + ds.child("calle").getValue().toString());
                        tvUsuario.setText(res.getString(R.string.usuario_incidencia) + " " + ds.child("usuario").getValue().toString());

                        if (fieldsMarker[0].equals("Radar")){
                            tvInfo.setText(res.getString(R.string.velocidad) + " " + ds.child("velocidad").getValue().toString() + " Km/h");
                            switch (ds.child("velocidad").getValue().toString()){
                                case "30":
                                    img.setImageResource(R.drawable.treinta_100);
                                    break;
                                case "40":
                                    img.setImageResource(R.drawable.cuarenta_100);
                                    break;
                                case "50":
                                    img.setImageResource(R.drawable.cincuenta_100);
                                    break;
                                case "60":
                                    img.setImageResource(R.drawable.sesenta_100);
                                    break;
                                case "70":
                                    img.setImageResource(R.drawable.setenta_100);
                                    break;
                                case "80":
                                    img.setImageResource(R.drawable.ochenta_100);
                                    break;
                                case "90":
                                    img.setImageResource(R.drawable.noventa_100);
                                    break;
                                case "100":
                                    img.setImageResource(R.drawable.cien_100);
                                    break;
                                case "110":
                                    img.setImageResource(R.drawable.cientodiez_100);
                                    break;
                                case "120":
                                    img.setImageResource(R.drawable.cientoveinte_100);
                                    break;
                            }
                        } else {
                            tvInfo.setVisibility(View.INVISIBLE);
                            img.setImageResource(imgIncidencias.get(fieldsMarker[0]));
                        }

                        dialog = builder.create();
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return true;
    }

    @SuppressLint("RestrictedApi")
    public void start(View view) {
        AlertDialog.Builder builder_progressBar = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        final View dialogViewProgressBar = getLayoutInflater().inflate(R.layout.cuadro_dialogo_progressbar, null);
        builder_progressBar.setView(dialogViewProgressBar).setCancelable(false);
        dialog_progressBar = builder_progressBar.create();
        dialog_progressBar.show();

        stopButton.setVisibility(View.VISIBLE);
        traficoButton.setVisibility(View.VISIBLE);
        obrasButton.setVisibility(View.VISIBLE);
        radarButton.setVisibility(View.VISIBLE);
        otrosButton.setVisibility(View.VISIBLE);

        startButton.animate().translationX(500).setDuration(1000);
        stopButton.animate().translationX(0).setDuration(1000);
        traficoButton.animate().translationX(0).setDuration(1000);
        obrasButton.animate().translationX(0).setDuration(1000);
        radarButton.animate().translationX(0).setDuration(1000);
        otrosButton.animate().translationX(0).setDuration(1000);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10);
        mLocationRequest.setFastestInterval(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) { //ESTO ES LO QUE HACE CADA VEZ QUE RECIBE UNA ACTUALIZACIÓN DE LOCALIZACIÓN
            dialog_progressBar.dismiss();
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);

                if (currentLocation == null){
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                } else {
                    currentLocation = null;
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                }
                //move map camera
                gmap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));

                //Avisamos de las incidencias que pueda haber cerca
                avisarIncidencias(location);
            }
        }
    };

    private void avisarIncidencias (final Location currentLocation) {
        DecimalFormat decimalFormat = new DecimalFormat("###.##");
        /*RECORREMOS EL ARRAY DE LAS INCIDENCIAS QUE ESTAN PUESTAS PARA SABER DE CUALES AVISAR => NO NECESITA ACCEDER A LA BBDD*/
        Location obj = new Location("");
        for (MarkerOptions m : this.markers){
            Location userLoc = new Location(""); //Localización actual del usuario
            userLoc.setLatitude(currentLocation.getLatitude());
            userLoc.setLongitude(currentLocation.getLongitude());

            obj.setLatitude(m.getPosition().latitude);
            obj.setLongitude(m.getPosition().longitude);

            float distance = userLoc.distanceTo(obj); //Distancia del usuario a la incidencia en cuestión

            String title = m.getTitle();
            String tipo = title.split(":")[0];
            String key = title.split(":")[1];
            String calle = title.split(":")[2];
            String velocidad = title.split(":")[3];
            String usuarioIncidencia = title.split(":")[4];
            Resources res = getResources();
            if (tipo.equals("Radar")){ //AVISA SOLO SI EL USUARIO ESTA EN LA MISMA CALLE QUE LA INCIDENCIA
                try {
                    direccionUsuario = geo.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String [] elementosDireccionUsuario = direccionUsuario.get(0).getAddressLine(0).split(",");

                if ((distance < hmDistancias.get(tipo) &&
                        calle.equals(elementosDireccionUsuario[0]) &&
                        !incidenciasAvisadas.contains(key) &&
                        hmAvisos.get(tipo) == true) &&
                        !usuarioIncidencia.equals(this.currentUser)){
                    String text = String.format(res.getString(R.string.radar1) +" " + velocidad + " " + res.getString(R.string.radar2) + decimalFormat.format(distance) + " " + res.getString(R.string.radar3));
                    Log.d("ALERTA", text);
                    speak.speak(text, TextToSpeech.QUEUE_ADD, null);

                    incidenciasAvisadas.add(key);
                }
            } else {
                Log.d("AVISOS", "Alerta de tipo: " + tipo);
                Log.d("AVISOS", "El valor del hashmap es: " + hmAvisos.get(tipo));
                if (distance < Integer.valueOf(hmDistancias.get(tipo)) &&
                        !incidenciasAvisadas.contains(key) &&
                        (hmAvisos.get(tipo) == true) &&
                        !usuarioIncidencia.equals(this.currentUser)){
                    Log.d("ALERTA", "El usuario de la alerta es: " + usuarioIncidencia + " y el actual es: " + this.currentUser);
                    String text;
                    if (Locale.getDefault().getLanguage().equals("en")){
                        text = String.format(res.getString(R.string.alerta1) + " " + decimalFormat.format(distance) + " " + res.getString(R.string.alerta2 )+ " " + hmTraduccion.get(tipo) + " " + res.getString(R.string.alerta3) + " " + calle);
                    } else {
                        text = String.format(res.getString(R.string.alerta1) + " " + decimalFormat.format(distance) + " " + res.getString(R.string.alerta2 )+ " " + tipo + " " + res.getString(R.string.alerta3) + " " + calle);
                    }
                    Log.d("ALERTA", text);
                    speak.speak(text, TextToSpeech.QUEUE_ADD, null);
                    incidenciasAvisadas.add(key);
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void stop (View view){
        startButton.animate().translationX(0).setDuration(1000);
        stopButton.animate().translationX(-500).setDuration(1000);
        traficoButton.animate().translationX(-500).setDuration(1000);
        obrasButton.animate().translationX(-500).setDuration(1000);
        radarButton.animate().translationX(-500).setDuration(1000);
        otrosButton.animate().translationX(-500).setDuration(1000);

        incidenciasAvisadas.clear();

        fusedLocationClient.removeLocationUpdates(mLocationCallback);

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(R.string.titulo_mapa);
        }
    }

    public void reportar (View view){
        if (view.getTag().toString().equals("Otros")){
            AlertDialog.Builder builder_otros = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
            final View dialogViewOtros = getLayoutInflater().inflate(R.layout.cuadro_dialogo_otras_incidencias, null); //Cogemos la vista personalizada del cuadro de diálogo

            //Ponemos el onClick a los botones
            int[] idOtrasIncidencias = {R.id.animalesButton, R.id.calleCortadaButton, R.id.policiaButton,
            R.id.nieveButton, R.id.lluviaButton, R.id.malAsfaltoButton, R.id.accidenteButton};
            ImageButton [] botonesOtrasIncidencias = new ImageButton[idOtrasIncidencias.length];

            for (int i = 0; i<idOtrasIncidencias.length; i++){
                botonesOtrasIncidencias[i] = dialogViewOtros.findViewById(idOtrasIncidencias[i]);
                botonesOtrasIncidencias[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reportarInicidencia(v.getTag().toString());
                        dialog_otros.dismiss();
                    }
                });
            }

            builder_otros.setView(dialogViewOtros);

            dialog_otros = builder_otros.create();
            dialog_otros.show();
        } else {
            view.animate().rotation(view.getRotation()+360).setDuration(500);
        }

        if (view.getTag().toString().equals("Radar")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
            View dialogView = getLayoutInflater().inflate(R.layout.cuadro_dialogo_reportar_radar, null); //Cogemos la vista personalizada del cuadro de diálogo

            //Configuramos los botones de las señales para reportar radares
            ImageButton [] botonesSenales = new ImageButton[this.idSenales.length];

            for (int i = 0; i<idSenales.length; i++){
                botonesSenales[i] = dialogView.findViewById(this.idSenales[i]);
                botonesSenales[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reportarInicidencia(v.getTag().toString());
                        dialog_velocidad.dismiss();
                    }
                });
            }

            builder.setView(dialogView);
            dialog_velocidad = builder.create();
            dialog_velocidad.show();
        } else if (!view.getTag().toString().equals("Otros")) { //IGUAL SOBRA
            //Colocamos la incidencia en el punto donde se encuentra el usuario
            this.reportarInicidencia(view.getTag().toString());
        }

    }

    private void reportarInicidencia(final String tipo) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser user = mAuth.getCurrentUser();

        DatabaseReference dbUsers = FirebaseDatabase.getInstance().getReference().child("database").child("userData").child(user.getUid()).child("userName");

        dbUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Ahora guardamos el objeto incidencia en la base de datos
                final DatabaseReference refDB;
                FirebaseDatabase dataBase = FirebaseDatabase.getInstance();

                final String userName = dataSnapshot.getValue().toString();

                DatabaseReference refIncidencia = null;

                //La referencia a la base de datos cambia en función del tipo de incidencia
                refDB = dataBase.getReference("/database/incidencias");
                refDB.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int flag_while = 0;
                        int flag_repetido = 0;
                        int clave_aleatoria = 0;

                        //Hay que ver que la clave de la incidencia no esté repetida
                        while (flag_while == 0) {
                            Random generador = new Random();
                            clave_aleatoria = generador.nextInt();
                            if (clave_aleatoria < 0){
                                clave_aleatoria = clave_aleatoria*(-1);
                            }
                            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                                if(Integer.parseInt(ds.getKey()) == clave_aleatoria){
                                    flag_repetido = 1;
                                    break; //No tiene sentido seguir buscando más
                                }
                            }

                            if (flag_repetido == 0){
                                break; //sale del while porque la clave no está repetida.
                            }
                        }
                        final int clave_incidencia = clave_aleatoria;
                        final String currentUserUID = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();

                        DatabaseReference userDB = FirebaseDatabase.getInstance().getReference().child("database").child("userData");
                        userDB.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Incidencia incidencia = null; //PORQUE TIENE QUE ESTAR INICIALIZADA A ALGO

                                //Cogemos la dirección de la ubicación actual del usuario
                                List<Address> addresses;
                                Geocoder geo = new Geocoder(DisplayMapActivity.this.getApplicationContext(), Locale.getDefault());
                                addresses = null;
                                String calle = null;
                                String [] elementosDireccion;
                                try {
                                    addresses = geo.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (addresses.isEmpty()) {
                                    //Poner toast de error
                                }
                                else {
                                    if (addresses.size() > 0) {
                                        elementosDireccion = addresses.get(0).getAddressLine(0).split(",");
                                        calle = elementosDireccion[0]; //El primer elemento es la calle
                                        Log.d("INCIDENCIAM", "La calle es: " + calle);
                                    }
                                }

                                for (DataSnapshot ds : dataSnapshot.getChildren()){
                                    if (ds.getKey().equals(currentUserUID)){
                                        DatabaseReference refChild = refDB.child(String.valueOf(clave_incidencia));
                                        String userName = ds.child("userName").getValue().toString();
                                        switch (tipo){
                                            case "30":
                                            case "40":
                                            case "50":
                                            case "60":
                                            case "70":
                                            case "80":
                                            case "90":
                                            case "100":
                                            case "110":
                                            case "120":
                                                incidencia = new Incidencia("Radar", userName, calle, currentLocation.latitude, currentLocation.longitude, Integer.valueOf(tipo));
                                                break;
                                            default:
                                                incidencia = new Incidencia(tipo, userName, calle, currentLocation.latitude, currentLocation.longitude);
                                                break;
                                        }
                                        refChild.setValue(incidencia);

                                        /*CADA VEZ QUE EL USUARIO REGISTRA UNA INCIDENCIA SE LE DA UN PUNTO*/
                                        int userReputation = Integer.valueOf(ds.child("reputation").getValue().toString());
                                        userReputation += 1;
                                        DatabaseReference dbUser = FirebaseDatabase.getInstance().getReference().child("database").child("userData").child(user.getUid());
                                        dbUser.child("reputation").setValue(userReputation);
                                        Log.d("PUNTOS", "La variable tipo es: " + tipo);
                                        String tipoPunto = null;
                                        switch (tipo){
                                            case "30":
                                            case "40":
                                            case "50":
                                            case "60":
                                            case "70":
                                            case "80":
                                            case "90":
                                            case "100":
                                            case "110":
                                            case "120":
                                                tipoPunto = "radar";
                                                break;
                                            case "Obras":
                                            case "Animales":
                                            case "Nieve":
                                            case "Lluvia":
                                            case "Accidente":
                                                tipoPunto = tipo.toLowerCase();
                                                break;
                                            case "Calle cortada":
                                                tipoPunto = "calleCortada";
                                                break;
                                            case "Policía":
                                                tipoPunto = "policia";
                                                break;
                                            case "Asfalto en mal estado":
                                                tipoPunto = "asfalto";
                                                break;
                                            case "Tráfico":
                                                tipoPunto = "trafico";
                                                break;
                                        }
                                        Log.d("PUNTOS", "La variable es: " + tipoPunto);
                                        int puntos_incidencia = Integer.valueOf(ds.child(tipoPunto).getValue().toString());
                                        Log.d("PUNTOS", "Ya he cogido los puntos");
                                        puntos_incidencia += 1;
                                        dbUser.child(tipoPunto).setValue(puntos_incidencia);
                                    }
                                }


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d("REPORTARR", "ERROR"+databaseError.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ERRORDATO", "Error!", databaseError.toException());
            }
        });
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.d("PULSACIONLARGA", "Se ha seleccionado un elemento");
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }
}
