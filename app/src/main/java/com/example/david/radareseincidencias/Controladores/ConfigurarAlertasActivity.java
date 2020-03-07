package com.example.david.radareseincidencias.Controladores;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import com.example.david.radareseincidencias.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;

public class ConfigurarAlertasActivity extends AppCompatActivity {
    private Switch sAccidente, sPolicia, sObras, sCalleCortada, sRadar, sNieve, sTrafico, sAnimales, sLluvia, sAsfalto;
    private EditText etAccidente, etPolicia, etObras, etCalleCortada, etRadar, etNieve, etTrafico, etAnimales, etLluvia, etAsfalto;
    private HashMap<String, Switch> switches;
    private HashMap<String, EditText> editTextHashMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_alertas);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_configuracion);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.configuracion);
        }

        switches = new HashMap<String, Switch>();
        editTextHashMap = new HashMap<String, EditText>();

        /*RELLENAMOS EL HASHMAP DE OS SWITCH*/
        sAccidente = findViewById(R.id.switchAccidente);
        switches.put("Accidente", sAccidente);

        sPolicia = findViewById(R.id.switchPolicia);
        switches.put("Policia", sPolicia);

        sObras = findViewById(R.id.switchObras);
        switches.put("Obras", sObras);

        sCalleCortada = findViewById(R.id.switchCalleCortada);
        switches.put("CalleCortada", sCalleCortada);

        sRadar = findViewById(R.id.switchRadar);
        switches.put("Radar", sRadar);

        sNieve = findViewById(R.id.switchNieve);
        switches.put("Nieve", sNieve);

        sTrafico = findViewById(R.id.switchTrafico);
        switches.put("Trafico", sTrafico);

        sAnimales = findViewById(R.id.switchAnimales);
        switches.put("Animales", sAnimales);

        sLluvia = findViewById(R.id.switchLluvia);
        switches.put("Lluvia", sLluvia);

        sAsfalto = findViewById(R.id.switchAsfalto);
        switches.put("Asfalto", sAsfalto);

        /*RELLENAMOS EL HASHMAP DE LOS EDIT TEXT*/
        etAccidente = findViewById(R.id.etAccidente);
        editTextHashMap.put("etAccidente", etAccidente);

        etPolicia = findViewById(R.id.etPolicia);
        editTextHashMap.put("etPolicia", etPolicia);

        etObras = findViewById(R.id.etObras);
        editTextHashMap.put("etObras", etObras);

        etCalleCortada = findViewById(R.id.etCalleCortada);
        editTextHashMap.put("etCalleCortada", etCalleCortada);

        etRadar = findViewById(R.id.etRadar);
        editTextHashMap.put("etRadar", etRadar);

        etNieve = findViewById(R.id.etNieve);
        editTextHashMap.put("etNieve", etNieve);

        etTrafico = findViewById(R.id.etTrafico);
        editTextHashMap.put("etTrafico", etTrafico);

        etAnimales = findViewById(R.id.etAnimales);
        editTextHashMap.put("etAnimales", etAnimales);

        etLluvia = findViewById(R.id.etLluvia);
        editTextHashMap.put("etLluvia", etLluvia);

        etAsfalto = findViewById(R.id.etAsfatlo);
        editTextHashMap.put("etAsfalto", etAsfalto);

        File f = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
        if (f.exists()) {
            Boolean valorLeido;
            SharedPreferences prefs = getSharedPreferences("fichero_configuracion", Context.MODE_PRIVATE);
            for (HashMap.Entry<String,Switch> entry : switches.entrySet()) {
                valorLeido = prefs.getBoolean(entry.getKey(), true); //Hay que poner un valor por defecto por si no encuentra el dato en el fichero
                entry.getValue().setChecked(valorLeido);
            }

            int numeroLeido = 0;
            for (HashMap.Entry<String,EditText> entry : editTextHashMap.entrySet()) {
                numeroLeido = prefs.getInt(entry.getKey(), 200); //Hay que poner un valor por defecto por si no encuentra el dato en el fichero
                entry.getValue().setText(String.valueOf(numeroLeido));
            }
        }
    }

    public void saveConfiguration (View view){
        StorageReference mStorageRef;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        SharedPreferences prefs = getSharedPreferences("fichero_configuracion", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("user", user.getUid());
        for (HashMap.Entry<String,Switch> entry : switches.entrySet()) {
            if (entry.getValue().isChecked()){
                editor.putBoolean(entry.getKey(), true);
            } else {
                editor.putBoolean(entry.getKey(), false);
            }
        }

        for (HashMap.Entry<String, EditText> entry : editTextHashMap.entrySet()){
            editor.putInt(entry.getKey(), Integer.parseInt(entry.getValue().getText().toString()));
        }

        editor.commit();

        File f = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
        if (f.exists()) {
            Uri uri = Uri.fromFile(f);
            mStorageRef = FirebaseStorage.getInstance().getReference().child(user.getUid()).child("fichero_configuracion.xml");
            UploadTask uploadTask = mStorageRef.putFile(uri);
            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    int currentprogress = (int) progress;
                    Log.d("PORCENTAJE", "Nivel: "+currentprogress);
                    //progressBar.setProgress(currentprogress);
                }
            });
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    Log.d("PORCENTAJE", "Carga completada");
                }
            });

        } else {
            Log.d("PORCENTAJE", "El fichero no existe");
        }



        finish();
    }

    //Para que la vista retorne cuando se pulse el botón de atrás
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }
}
