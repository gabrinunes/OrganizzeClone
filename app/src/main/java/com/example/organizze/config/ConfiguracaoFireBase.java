package com.example.organizze.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfiguracaoFireBase {

    private static FirebaseAuth autenticacao;
    private static DatabaseReference firebase;

    //retorna a instancia do Firebase

    public static DatabaseReference getFirebaseDatabase(){
        if(firebase==null){
            firebase = FirebaseDatabase.getInstance().getReference();

        }
        return  firebase;

    }

    //retorna a instancia do FirebaseAuth

    public static FirebaseAuth getFireBaseAutenticacao(){
        if(autenticacao==null){
            autenticacao = FirebaseAuth.getInstance();

        }
        return autenticacao;
    }
}
