package com.example.david.radareseincidencias.Controladores;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.david.radareseincidencias.R;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class RankingActivity extends AppCompatActivity {
    private DatabaseReference dbUsers;
    private Map<String, Integer> reputations;
    private ImageView [] imageViews;
    private TextView [] textViewsUserNames;
    private TextView [] textViewsPuntos;
    private String [] userNames;
    private int [] userReputations;

    private int [] nombresImageViews;
    private int [] nombresTextViewsUserNames;
    private int [] nombresTextViewsPuntos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_ranking);
        //Establece la barra de herramientas como la barra de aplicación de la App
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.ranking_title);
        }

        this.imageViews = new ImageView[5];
        this.textViewsPuntos = new TextView[5];
        this.textViewsUserNames = new TextView[5];

        this.imageViews[0] = findViewById(R.id.ivPrimero);
        this.imageViews[1] = findViewById(R.id.ivSegundo);
        this.imageViews[2] = findViewById(R.id.ivTercero);
        this.imageViews[3] = findViewById(R.id.ivCuarto);
        this.imageViews[4] = findViewById(R.id.ivQuinto);

        this.textViewsPuntos[0] = findViewById(R.id.tvPuntoUser1);
        this.textViewsPuntos[1] = findViewById(R.id.tvPuntoUser2);
        this.textViewsPuntos[2] = findViewById(R.id.tvPuntoUser3);
        this.textViewsPuntos[3] = findViewById(R.id.tvPuntoUser4);
        this.textViewsPuntos[4] = findViewById(R.id.tvPuntoUser5);

        this.textViewsUserNames[0] = findViewById(R.id.tvUserName1);
        this.textViewsUserNames[1] = findViewById(R.id.tvUserName2);
        this.textViewsUserNames[2] = findViewById(R.id.tvUserName3);
        this.textViewsUserNames[3] = findViewById(R.id.tvUserName4);
        this.textViewsUserNames[4] = findViewById(R.id.tvUserName5);

        this.reputations = new TreeMap<String, Integer>();
        //this.userNames = new ArrayList<String>();
        //this.userReputations = new ArrayList<Integer>();
        this.dbUsers = FirebaseDatabase.getInstance().getReference().child("database").child("userData");
        dbUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> imgDefecto = new ArrayList<>();
                HashMap<String, String> profilePhoto = new HashMap<>();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Log.d("MAPAREP", "REPUTATIONS: Clave : " + ds.child("reputation").getValue().toString() + " Valor :" + ds.child("userName").getValue().toString());
                    reputations.put(ds.child("userName").getValue().toString(), Integer.valueOf(ds.child("reputation").getValue().toString()));

                    if(ds.child("profilePhoto").getValue().toString().equals(" ")){
                       imgDefecto.add(ds.child("userName").getValue().toString());
                    } else {
                        profilePhoto.put(ds.child("userName").getValue().toString(), ds.getKey());
                    }
                }

                //Del mapa de reputations se cogen los 5 mayores
                int reputation_max = 0;
                String max = null;
                boolean flag_image = false;
                StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                for(int i = 0; i<5; i++){
                    Iterator iterator = reputations.keySet().iterator();
                    while (iterator.hasNext()) {
                        flag_image = false;
                        Object key = iterator.next();
                        Log.d("MAPAREP", "Voy a ver los puntos de: " + key.toString());
                        if (reputations.get(key) >= reputation_max){
                            max = key.toString(); //Usuario con más puntos
                            reputation_max = reputations.get(key);
                        }
                    }
                    reputation_max = 0;
                    Log.d("MAPAREP", "El usuario con mas puntuacion es: " + max);
                    textViewsUserNames[i].setText(max);
                    textViewsPuntos[i].setText(reputations.get(max) + " Pts.");
                    reputations.remove(max); //Se elimina para que no vuelva a salir el mismo

                    for(String s : imgDefecto){
                        if (s.equals(max)){
                            StorageReference imgRef = mStorageRef.child("general/user.png");
                            Glide.with(RankingActivity.this).using(new FirebaseImageLoader()).load(imgRef).into(imageViews[i]);
                            flag_image = true;
                        }
                    }
                    if (flag_image == false){
                        Log.d("IMAGEN_RANKING", "El usuario " + max + " tiene otra foto.");
                        File f = new File("/data/data/" + getPackageName() + "/profilePhoto" + i + ".jpg");
                        if (f.exists()){
                            Log.d("IMAGEN_RANKING", "El fichero existía y se va a borrar");
                            f.delete(); //Lo borramos porque puede ser antiguo
                        }

                        StorageReference pathReference = FirebaseStorage.getInstance().getReference().child(profilePhoto.get(max)).child("profilePhoto");
                        File localFile = null;
                        try {
                            localFile = new File("/data/data/" + getPackageName() + "/profilePhoto" + i + ".jpg");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        final int cont_img = i;
                        pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                //YA HA DESCARGADO LA IMAGEN
                                Log.d("IMAGEN_RANKING", "Se actualiza la imagen.");
                                imageViews[cont_img].setImageURI(Uri.fromFile(new File("/data/data/" + getPackageName() + "/profilePhoto" + cont_img + ".jpg")));
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                                Log.d("IMAGEN_RANKING", "Ha habido un error al descargar la imagen " + exception.getMessage());
                            }
                        });
                    }

                }

                //HACEMOS EL RANKING
                /*Map<Integer, String> rankingMap = sortByValueDesc(reputations); //Ordenamos los elementos de forma descendente para obtener el ranking
                Iterator iterator = rankingMap.keySet().iterator();

                while (iterator.hasNext()) {
                    flag_image = false;
                    Object key = iterator.next();
                    Log.d("MAPAREP", "Clave : " + key + " Valor :" + rankingMap.get(key));
                    if (contador < 5) { //Solo se ponen en el ranking los 5 primeros usuarios con más puntos
                        textViewsUserNames[contador].setText(reputations.get(key));
                        textViewsPuntos[contador].setText(key.toString() + " Pts.");

                        for(String s : imgDefecto){
                            if (s.equals(reputations.get(key))) {
                                StorageReference imgRef = mStorageRef.child("general/user.png");
                                Glide.with(RankingActivity.this).using(new FirebaseImageLoader()).load(imgRef).into(imageViews[contador]);
                                flag_image = true;
                            }
                        }
                        if (flag_image == false){
                            Log.d("IMAGEN_RANKING", "El usuario " + reputations.get(key) + " tiene otra foto.");
                            File f = new File("/data/data/" + getPackageName() + "/profilePhoto" + contador + ".jpg");
                            if (f.exists()){
                                Log.d("IMAGEN_RANKING", "El fichero existía y se va a borrar");
                                f.delete(); //Lo borramos porque puede ser antiguo
                            }

                            StorageReference pathReference = FirebaseStorage.getInstance().getReference().child(profilePhoto.get(reputations.get(key))).child("profilePhoto");
                            File localFile = null;
                            try {
                                localFile = new File("/data/data/" + getPackageName() + "/profilePhoto" + contador + ".jpg");

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            final int cont_img = contador;
                            pathReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    //YA HA DESCARGADO LA IMAGEN
                                    Log.d("IMAGEN_RANKING", "Se actualiza la imagen.");
                                    imageViews[cont_img].setImageURI(Uri.fromFile(new File("/data/data/" + getPackageName() + "/profilePhoto" + cont_img + ".jpg")));
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                    Log.d("IMAGEN_RANKING", "Ha habido un error al descargar la imagen " + exception.getMessage());
                                }
                            });
                        }
                    } else {
                        break;
                    }
                    contador++;
                }*/

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static Map<Integer, String> sortByValueDesc(Map<Integer, String> map) {
        List<Map.Entry<Integer, String>> list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, String>>() {
            @Override
            public int compare(Map.Entry<Integer, String> o1, Map.Entry<Integer, String> o2) {
                if(o1.getKey() == o2.getKey()){
                    Log.d("MAPAREP", "Las claves son iguales");
                    return o1.getKey();
                }
                Log.d("MAPAREP", "Las claves NO son iguales");
                return o2.getKey().compareTo(o1.getKey());
            }
        });

        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
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
