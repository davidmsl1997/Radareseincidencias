package com.example.david.radareseincidencias.Validador;

import android.content.res.Resources;
import android.support.design.widget.TextInputLayout;

import com.bumptech.glide.load.engine.Resource;
import com.example.david.radareseincidencias.Controladores.MainActivity;
import com.example.david.radareseincidencias.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Validador {
    public static void validarFormatoEmail (TextInputLayout tilEmail, Map<String, TextInputLayout> errors) {
        String email = tilEmail.getEditText().getText().toString();

        if (null == email || email.equals("")){
            errors.put(MainActivity.getCustomResources().getString(R.string.fallo_email_vacio), tilEmail);
            return;
        }
        Pattern pattern = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        Matcher matcher = pattern.matcher(email);
        if(!matcher.find()) {
            String string = MainActivity.getCustomResources().getString(R.string.fallo_email_incorrecto);
            errors.put(string, tilEmail);
            return;
        }
    }

    public static void validarCampoObligatorio(TextInputLayout tilCampo, Map<String , TextInputLayout> errors) {
        String pwd = tilCampo.getEditText().getText().toString();
        if(null == pwd || pwd.equals("")) {
            errors.put(MainActivity.getCustomResources().getString(R.string.fallo_passw_vacia), tilCampo);
        }
    }
}
