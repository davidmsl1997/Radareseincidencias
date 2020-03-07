package com.example.david.radareseincidencias.Controladores;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.david.radareseincidencias.R;
import com.firebase.ui.storage.images.FirebaseImageLoader;
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
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

public class PerfilActivity extends AppCompatActivity {
    private TextView tvName, tvSurname, tvUser, tvEmail, tvAccidentes, tvTrafico, tvRadares, tvObras, tvCalleCortada,
    tvPolicia, tvAnimales, tvLluvia, tvNieve, tvAsfalto, tvReputation, tvRanking;
    private ImageView ivProfile;
    private static final int SELECT_FILE = 1; //Para cuando se abra la galería para elegir una imagen de perfil

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private StorageReference mStorageRef;
    private DatabaseReference dbUsers;
    private InputStream imageStream;

    private TreeMap<Integer, String> reputations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        /*TV DE LOS DATOS PERSONALES*/
        tvName = (TextView)findViewById(R.id.tvPerfilName);
        tvSurname = (TextView)findViewById(R.id.tvPerfilSurname);
        tvUser = (TextView)findViewById(R.id.tvPerfilUserName);
        tvEmail = (TextView)findViewById(R.id.tvPerfilEmail);

        /*TV PUNTUACIONES*/
        tvReputation = (TextView)findViewById(R.id.tvReputation);
        tvRanking = (TextView)findViewById(R.id.tvRanking);
        reputations = new TreeMap<Integer, String>();

        /*TV DE LAS INCIDENCIAS*/
        tvAccidentes = (TextView)findViewById(R.id.tvCuentaAccidentes);
        tvTrafico = (TextView)findViewById(R.id.tvCuentaTrafico);
        tvRadares = (TextView)findViewById(R.id.tvCuentaRadares);
        tvObras = (TextView)findViewById(R.id.tvCuentaObras);
        tvCalleCortada = (TextView)findViewById(R.id.tvCuentaCalleCortada);
        tvPolicia = (TextView)findViewById(R.id.tvCuentaPolicia);
        tvAnimales = (TextView)findViewById(R.id.tvCuentaAnimales);
        tvLluvia = (TextView)findViewById(R.id.tvCuentaLluvia);
        tvNieve = (TextView)findViewById(R.id.tvCuentaNieve);
        tvAsfalto = (TextView)findViewById(R.id.tvCuentaAsfalto);
        ivProfile = (ImageView) findViewById(R.id.ivProfile);

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_perfil);
        //Establece la barra de herramientas como la barra de aplicación de la App
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.profile);
        }

        this.mAuth = FirebaseAuth.getInstance();
        this.user = this.mAuth.getCurrentUser();

        this.dbUsers = FirebaseDatabase.getInstance().getReference().child("database").child("userData");
        this.dbUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String currentUser = null;
                StorageReference imgRef = null;


                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    if (ds.getKey().equals(user.getUid())){
                        /*SE RELLENAN LOS DATOS PERSONALES*/
                        currentUser = ds.child("userName").getValue().toString();
                        tvName.setText(ds.child("name").getValue().toString());
                        tvSurname.setText(ds.child("surname1").getValue().toString() + " " + ds.child("surname2").getValue().toString());
                        tvUser.setText(ds.child("userName").getValue().toString());
                        tvEmail.setText(ds.child("email").getValue().toString());

                        /*SE RELLENA LA FOTO*/
                        mStorageRef = FirebaseStorage.getInstance().getReference();
                        if (ds.child("profilePhoto").getValue().toString().equals(" ")) { //Si no hay nada, se muestra la imagen por defecto
                            imgRef = mStorageRef.child("general/user.png");
                            Glide.with(PerfilActivity.this).using(new FirebaseImageLoader()).load(imgRef).into(ivProfile);
                        } else {
                            File f = new File("/data/data/" + getPackageName() + "/profilePhoto.jpg");
                            if (f.exists()){
                                f.delete(); //Lo borramos porque puede ser antiguo
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
                                    Log.d("IMAGEN", "Voy a actualizar la imagen en el iv PERFIL");
                                    ivProfile.setImageURI(Uri.fromFile(new File("/data/data/" + getPackageName() + "/profilePhoto.jpg")));
                                    Log.d("IMAGEN", "He actualizado la imagen en el iv PERFIL");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });
                        }

                        /*SE RELLENAN LOS DATOS DE LAS PUNTUACIONES*/
                        tvReputation.setText(ds.child("reputation").getValue().toString() + " pts.");

                        /*SE RELLENAN LOS DATOS DE LAS INCIDENCIAS*/
                        tvAccidentes.setText(ds.child("accidente").getValue().toString());
                        tvTrafico.setText(ds.child("trafico").getValue().toString());
                        tvRadares.setText(ds.child("radar").getValue().toString());
                        tvObras.setText(ds.child("obras").getValue().toString());
                        tvCalleCortada.setText(ds.child("calleCortada").getValue().toString());
                        tvPolicia.setText(ds.child("policia").getValue().toString());
                        tvAnimales.setText(ds.child("animales").getValue().toString());
                        tvNieve.setText(ds.child("nieve").getValue().toString());
                        tvLluvia.setText(ds.child("lluvia").getValue().toString());
                        tvAsfalto.setText(ds.child("asfalto").getValue().toString());
                    }
                    reputations.put(Integer.valueOf(ds.child("reputation").getValue().toString()), ds.child("userName").getValue().toString());
                }
                /*HACEMOS EL RANKING PARA VER EN QUE PUESTO ESTA EL USUARIO EN CUESTION*/
                NavigableMap rankingMap = reputations.descendingMap(); //Ordenamos los elementos de forma descendente para obtener el ranking
                Iterator iterator = rankingMap.keySet().iterator();
                int puesto = 0;
                while (iterator.hasNext()) {
                    puesto ++;
                    Object key = iterator.next();
                    if (currentUser.equals(rankingMap.get(key))){
                        tvRanking.setText(String.valueOf(puesto));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ERRORDATO", "Error!", databaseError.toException());
            }
        });
    }

    public void editImage(View view){
        Intent intent = new Intent();
        intent.setType("image/*"); //Permite escoger una imagen de todos los tipos de archivo
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccione una imagen"), SELECT_FILE);
    }

    //Procesamos la respuesta del intent que abre la galería
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Uri selectedImage;

        String filePath = null;
        switch (requestCode) {
            case SELECT_FILE: //Solo nos interesa la de la galería
                if (resultCode == Activity.RESULT_OK) {
                    selectedImage = imageReturnedIntent.getData();
                    String selectedPath=selectedImage.getPath();
                    if (requestCode == SELECT_FILE) {

                        if (selectedPath != null) {
                            imageStream = null;
                            try {
                                imageStream = getContentResolver().openInputStream(
                                        selectedImage);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }

                            //Almacenamos la imagen en FirebaseStorage
                            final StorageReference imgRef = mStorageRef.child(user.getUid()+"/"+"profilePhoto"); //Accedemos a la carpeta del usuario
                            UploadTask uploadTask = imgRef.putFile(selectedImage);
                            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                    int currentprogress = (int) progress;
                                    Log.d("PORCENTAJE", "Nivel: "+currentprogress);
                                }
                            });
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    Log.d("PORCENTAJE", "Carga completada");
                                    //Actualiza el campo de la base de datos a la ruta de la imagen actual => nueva URL de descarga
                                    dbUsers.child(user.getUid()).child("profilePhoto").setValue(imgRef.getDownloadUrl().toString());
                                    Bitmap bmp = BitmapFactory.decodeStream(imageStream);
                                    Log.d("PORCENTAJE", "Voy a mostrar la imagen en el circulo");
                                    ivProfile.setImageBitmap(bmp);//La muestra en el círculo
                                }
                            });
                        }
                    }
                }
                break;
        }
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
