package com.example.david.radareseincidencias.Controladores;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.david.radareseincidencias.R;
import com.example.david.radareseincidencias.Validador.Validador;
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

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    private TextView user, pwd;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    protected static MainActivity instance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        /*PIDE PERMISOS DE LOCALIZACIÓN*/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            //SI NO LOS TIENE SALE DE LA APP -> MEJOR PONER UN RECUADRO PREGUNTANDO SI SE ESTÁ SEGURO Y PEDIR DE NUEVO O NO LOS PERMISOS EN FUNCIÓN DE LA RESPUESTA
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                System.exit(0);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.mainActivityBackgroundColor));
        }

        //Igual se puede hacer desde la interfaz -> icono de la app
        ImageView iv = findViewById(R.id.ivIcono);
        iv.setImageResource(R.drawable.icono_app);

        /*SI EL USUARIO ANTERIOR NO HABIA CERRADO SESION, NO TIENE QUE VOLVER A INICIAR*/
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, DisplayMapActivity.class);
            startActivity(intent);
        }

        mAuth = FirebaseAuth.getInstance();
        user = (TextView) findViewById(R.id.editText2);
        pwd = (TextView) findViewById(R.id.editText4);

        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    protected void onResume(){
        super.onResume();
        user.requestFocus();
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void login(View view) {
        final TextInputLayout tilEmail = findViewById(R.id.til_email);
        final TextInputLayout tilPwd = findViewById(R.id.til_pwd);
        Map <String, TextInputLayout> errors = new HashMap<>();
        progressBar.setVisibility(View.VISIBLE);

        String email = user.getText().toString();
        String password = pwd.getText().toString();
        tilEmail.setErrorEnabled(false);
        tilPwd.setErrorEnabled(false);

        Validador.validarFormatoEmail(tilEmail, errors);
        Validador.validarCampoObligatorio(tilPwd, errors);

        if(errors.isEmpty()) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                Intent intent = new Intent(MainActivity.this, DisplayMapActivity.class);
                                startActivity(intent);
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            /*MIRA EN BASE DE DATOS*/
                            progressBar.setVisibility(View.INVISIBLE);
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/database/userData");
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Boolean flag = false;
                                    for(DataSnapshot ds : dataSnapshot.getChildren()) { //ds es una lista con todos los hijos de userData
                                        if(ds.child("email").getValue().toString().equals(tilEmail.getEditText().getText().toString())) {
                                            tilPwd.setError(getString(R.string.fallo_passw_incorrecta));
                                            tilPwd.setErrorEnabled(true);
                                            flag = true;
                                        }
                                    }

                                    if(flag == false) {
                                        Toast.makeText(MainActivity.this,  R.string.fallo_inicio, Toast.LENGTH_SHORT).show();
                                        tilPwd.setError("Pepe");
                                        tilEmail.setError("Pepe");
                                        if (tilEmail.getChildCount() == 2) {
                                            tilEmail.getChildAt(1).setVisibility(View.GONE);
                                        }
                                        if (tilPwd.getChildCount() == 2) {
                                            tilPwd.getChildAt(1).setVisibility(View.GONE);
                                        }
                                        tilPwd.setErrorEnabled(true);
                                        tilEmail.setErrorEnabled(true);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                    }
            });
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            for(Map.Entry<String, TextInputLayout> entry : errors.entrySet()) {
                entry.getValue().setError(entry.getKey());
                entry.getValue().setErrorEnabled(true);
            }
        }
    }

    public void registro(View view){
        Intent intent = new Intent(this, RegistroActivity.class);
        startActivity(intent);
    }

    public void recuperarPwd(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.cuadro_dialogo_recuperarpwd, null); //Cogemos la vista personalizada del cuadro de diálogo

        builder.setView(dialogView);
        final AlertDialog dialog;
        Button enviarButton = dialogView.findViewById(R.id.enviarButton);
        Button cancelarButton = dialogView.findViewById(R.id.cancelarButton);
        final EditText emailET = dialogView.findViewById(R.id.emailET);

        enviarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = emailET.getText().toString();

                if (email.equals("")){
                    Toast toast1 = Toast.makeText(MainActivity.this, R.string.email_vacio, Toast.LENGTH_SHORT);
                    toast1.show();
                    return;
                }

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                final FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseDatabase dataBase = FirebaseDatabase.getInstance();

                DatabaseReference ref = dataBase.getReference("/database/userData");
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean flag = false;
                        for(DataSnapshot ds : dataSnapshot.getChildren()) { //ds es una lista con todos los hijos de userData
                            if(ds.child("email").getValue().toString().equals(email)){
                                flag = true;
                                auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast toast1 = Toast.makeText(MainActivity.this, R.string.recuperar_pwd_mail_enviado, Toast.LENGTH_SHORT);
                                            toast1.show();
                                        } else {
                                            Toast toast1 = Toast.makeText(MainActivity.this, R.string.recuperar_pwd_mail_error, Toast.LENGTH_LONG);
                                            toast1.show();
                                        }
                                    }
                                });
                                //finish();
                            }
                        }

                        if (flag == false){
                            Toast toast1 = Toast.makeText(MainActivity.this, R.string.email_error + email, Toast.LENGTH_SHORT);
                            toast1.show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
        dialog = builder.create();
        cancelarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public static Resources getCustomResources() {
        return instance.getResources();
    }
}
