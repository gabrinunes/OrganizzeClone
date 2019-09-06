package com.example.organizze.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.organizze.R;
import com.example.organizze.config.ConfiguracaoFireBase;
import com.example.organizze.helper.Base64Custom;
import com.example.organizze.model.Usuario;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText campoEmail,campoSenha;
    private Button buttonEntrar;
    private LoginButton loginButton;
    private Usuario usuario;
    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef = ConfiguracaoFireBase.getFirebaseDatabase();
    private CallbackManager mcallbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private DatabaseReference usuarioRef,movimentacaoRef;
    private DatabaseReference firebase = ConfiguracaoFireBase.getFirebaseDatabase();
    private ValueEventListener valueEventListenerUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        buttonEntrar = findViewById(R.id.buttonEntrar);
        loginButton = findViewById(R.id.login_button);

        mcallbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions(Arrays.asList("email"));

        autenticacao = FirebaseAuth.getInstance();

        //Login com Google

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);









        usuario = new Usuario();



        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().registerCallback(mcallbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Handle(loginResult.getAccessToken());



                    }

                    @Override
                    public void onCancel() {

                    }

                    @Override
                    public void onError(FacebookException error) {

                    }
                });
            }
        });

        buttonEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String textoEmail = campoEmail.getText().toString();
                String textoSenha = campoSenha.getText().toString();

                if(!textoEmail.isEmpty()){
                    if(!textoSenha.isEmpty()){

                       usuario = new Usuario();
                       usuario.setEmail(textoEmail);
                       usuario.setSenha(textoSenha);
                       validarLogin();

                    }else{
                        Toast.makeText(LoginActivity.this,"Preencha a Senha!",
                                Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(LoginActivity.this,"Preencha o E-mail!",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
    }



    private void Handle(AccessToken token){
        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());


        autenticacao.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                abrirTelaPrincipal();

                }else{
                    Toast.makeText(LoginActivity.this,"email ja cadastrado",Toast.LENGTH_LONG).show();
                    Log.i("task","task: "+task);
                }
            }
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct){
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(),null);
        autenticacao.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    abrirTelaPrincipal();
                }else{
                    Toast.makeText(LoginActivity.this,"email ja cadastrado",Toast.LENGTH_LONG).show();

                }
            }
        });
    }



    public void validarLogin(){

        autenticacao = ConfiguracaoFireBase.getFireBaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                   abrirTelaPrincipal();
                }else{

                    String excecao="";
                    try {
                        throw task.getException();
                    }catch(FirebaseAuthInvalidUserException e) {
                        excecao = "Usuário não está cadastrado.";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excecao = "E-mail e senha não correspondem a um usuário cadastrado";
                    }catch (Exception e){
                        excecao ="Erro ao cadastrar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }


                    Toast.makeText(LoginActivity.this,
                            excecao,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void excluirUser() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        usuarioRef = firebaseRef.child("usuarios");
        movimentacaoRef = firebaseRef.child("movimentacao");
        usuarioRef.child(idUsuario).removeValue();
        movimentacaoRef.child(idUsuario).removeValue();
        //Usuario usuario = null;
        user.delete();
        autenticacao.signOut();
        LoginManager.getInstance().logOut();
        Log.i("keyss","keyss: "+ usuarioRef);
    }



    @Override
    protected void onStart() {
        autenticacao = ConfiguracaoFireBase.getFireBaseAutenticacao();
        super.onStart();
        if(autenticacao.getCurrentUser()!=null){//Solução encontrada pra resolver o bug de apos deletar a conta o usuario logar e tentar sair o app crasha
            Log.i("usexx","usexx: "+ autenticacao);
            excluirUser();
            startActivity(new Intent(this,MainActivity.class));
        }
    }

    private void signIn(){
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent,1);
    }

    @Override
    protected  void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        mcallbackManager.onActivityResult(requestCode,resultCode,data);
        //login google Try-Catch verificando request code

        if(requestCode== 1){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

            }catch (ApiException e){
                Log.i("erro","erro de login", e);
            }

        }
    }




    public void abrirTelaPrincipal(){
       startActivity(new Intent(this,PrincipalActivity.class));
       finish();
    }
    public void cadastroFbUser(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = user.getDisplayName();
        usuario.setEmail(user.getEmail());
        usuario.setNome(name);
        String IdUsuario = Base64Custom.codificarBase64(user.getEmail());
        usuario.setIdUsuario(IdUsuario);
        //Log.i("dados","dado: " +name);
        usuario.salvar();
        abrirTelaPrincipal();
    }

    private void enviarEmailRedifinirSenha() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String email = campoEmail.getText().toString();
        if (email.equals("")) {
            Toast.makeText(LoginActivity.this, "campo email em branco,por favor digite um email válido", Toast.LENGTH_LONG).show();
        } else {
            auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "email enviado", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "email não enviado", Toast.LENGTH_LONG).show();
                    }
                }
            });
            Log.i("email", "email: " + email);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cadastro,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menuLogin:
                enviarEmailRedifinirSenha();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        signIn();
    }
}
