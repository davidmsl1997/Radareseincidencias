package com.example.david.radareseincidencias.Controladores;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.david.radareseincidencias.R;
import com.example.david.radareseincidencias.sampledata.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class RegistroActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseDatabase dataBase;
    private static String email, password, userName, name, surname1, surname2, password2;
    private TextView tvUser, tvName, tvSurname1, tvSurname2, tvCorreo, tvPwd, tvPwd2;

    public static boolean flag_user_repetido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        tvUser = (TextView)findViewById(R.id.IDUser);
        tvName = (TextView)findViewById(R.id.IDName);
        tvName.requestFocus();
        tvSurname1 = (TextView)findViewById(R.id.IDSurname1);
        tvSurname2 = (TextView)findViewById(R.id.IDSurname2);
        tvCorreo = (TextView)findViewById(R.id.IDEmail);
        tvPwd = (TextView)findViewById(R.id.IDPwd);
        tvPwd2 = (TextView)findViewById(R.id.IDPwd2);

        mAuth = FirebaseAuth.getInstance();
        dataBase = FirebaseDatabase.getInstance();
    }

    public void registro (View view){
        int flag_salida = 0;

        flag_user_repetido = false;

        userName = tvUser.getText().toString();
        name = tvName.getText().toString();
        surname1 = tvSurname1.getText().toString();
        surname2 = tvSurname2.getText().toString();
        email = tvCorreo.getText().toString();
        password = tvPwd.getText().toString();
        password2 = tvPwd2.getText().toString();

        /*Hacer un metodo que sea validar contraseña*/
        if (password.length() < 6){
            Toast.makeText(RegistroActivity.this, R.string.longitud_pwd, Toast.LENGTH_SHORT).show();
            tvPwd.setBackgroundColor(Color.RED);
            flag_salida = 1;
        }

        if (!password.equals(password2)) {
            Toast.makeText(RegistroActivity.this, R.string.comparacion_pwd, Toast.LENGTH_SHORT).show();
            tvPwd2.setBackgroundColor(Color.RED);
            flag_salida = 1;
        }
        /*Llamar al método validar email*/
        if (!email.contains("@")){
            Toast.makeText(RegistroActivity.this, R.string.formato_email, Toast.LENGTH_SHORT).show();
            tvCorreo.setBackgroundColor(Color.RED);
            flag_salida = 1;
        }

        if (flag_salida == 1) {
            return;
        }
        /*VALIDAR TODOS LOS CAMPOS OBLIGATORIOS DEL FORMULARIO*/
        DatabaseReference ref = dataBase.getReference("/database/userData");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()) { //ds es una lista con todos los hijos de userData
                    if(ds.child("userName").getValue().toString().equals(userName)){
                        flag_user_repetido = true;
                        Toast toast1 = Toast.makeText(getApplicationContext(), R.string.usuario_repetido1 + " " + userName + " " + R.string.usuario_repetido2, Toast.LENGTH_LONG);
                        toast1.show();
                        break; //No tiene sentido seguir buscando más
                    }
                }

                if (flag_user_repetido == false){
                    mAuth.createUserWithEmailAndPassword(email, password).
                            addOnCompleteListener(RegistroActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        FirebaseUser user = mAuth.getCurrentUser();

                                        DatabaseReference refDB = dataBase.getReference("/database/userData");
                                        User userDB = new User(name, surname1, surname2, userName, email);
                                        DatabaseReference refChild = refDB.child(user.getUid());
                                        refChild.setValue(userDB);

                                        //Creamos el fichero de configuración y lo guardamos en FireBase
                                        String [] nombresIncidencias = {"Accidente", "Policia", "Obras", "CalleCortada", "Radar", "Nieve",
                                        "Trafico", "Animales", "Lluvia", "Asfalto"};
                                        String [] editTextDistancias = {"etAccidente", "etPolicia", "etObras", "etCalleCortada", "etRadar", "etNieve",
                                                "etTrafico", "etAnimales", "etLluvia", "etAsfalto"};
                                        SharedPreferences prefs = getSharedPreferences("fichero_configuracion", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();

                                        editor.putString("user", user.getUid());
                                        for (int i = 0; i<nombresIncidencias.length; i++){
                                            editor.putBoolean(nombresIncidencias[i], true);
                                            editor.putInt(editTextDistancias[i], 200);
                                        }
                                        editor.commit();

                                        File f = new File("/data/data/" + getPackageName() + "/shared_prefs/fichero_configuracion.xml");
                                        if (f.exists()) {
                                            Uri uri = Uri.fromFile(f);
                                            StorageReference mStorageRef = FirebaseStorage.getInstance().getReference().child(user.getUid()).child("fichero_configuracion.xml");
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
                                                    mAuth.signInWithEmailAndPassword(email, password);
                                                    final FirebaseUser currentUser = mAuth.getCurrentUser();
                                                    currentUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            Log.d("VERIFICADO", "El correo ya ha sido enviado");
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d("VERIFICADO", "El correo de verificación no ha podido ser enviado " + e.getMessage());
                                                        }
                                                    });
                                                    mAuth.signOut();
                                                    finishActivity();
                                                }
                                            });

                                        } else {
                                            Log.d("PORCENTAJE", "El fichero no existe");
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(RegistroActivity.this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Here you get the error type
                                    Toast.makeText(RegistroActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void finishActivity() {
        this.finish();
    }
}
