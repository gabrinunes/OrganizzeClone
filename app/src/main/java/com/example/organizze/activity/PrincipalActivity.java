package com.example.organizze.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.organizze.R;
import com.example.organizze.adapter.AdapterMovimentacao;
import com.example.organizze.config.ConfiguracaoFireBase;
import com.example.organizze.helper.Base64Custom;
import com.example.organizze.model.Movimentacao;
import com.example.organizze.model.Usuario;
import com.facebook.login.LoginManager;
import com.github.clans.fab.FloatingActionMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PrincipalActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView textoSaudacao,textoSaldo;
    private Double despesaTotal = 0.0;
    private Double receitaTotal = 0.0;
    private Double resunmoUsuario = 0.0;

    private FirebaseAuth autenticacao = ConfiguracaoFireBase.getFireBaseAutenticacao();
    private DatabaseReference firebaseRef = ConfiguracaoFireBase.getFirebaseDatabase();
    private DatabaseReference usuarioRef;
    private ValueEventListener valueEventListenerUsuario;
    private ValueEventListener valueEventListenerMovimentacoes;

    private RecyclerView recyclerView;
    private AdapterMovimentacao adapterMovimentacao;
    private List<Movimentacao> movimentacoes = new ArrayList<>();
    private Movimentacao movimentacao;
    private DatabaseReference movimentacaoRef;
    private String mesAnoSelecionado;
    private Usuario usuario;

    private FloatingActionMenu fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Organizze");
        setSupportActionBar(toolbar);

        calendarView =  findViewById(R.id.calendarView);
        textoSaudacao = findViewById(R.id.textSaudacao);
        textoSaldo = findViewById(R.id.textSaldo);
        recyclerView = findViewById(R.id.recyclerMovimentos);
        fab = findViewById(R.id.menu);
        configuraCalendarView();
        swipe();
        usuario = new Usuario();

      

        //Configurar adpater
        adapterMovimentacao = new AdapterMovimentacao(movimentacoes,this);


        //ConfigarRecyclerView

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapterMovimentacao);




    }


    public void configuraCalendarView(){

        CalendarDay dataAtual = calendarView.getCurrentDate();
        String mesSelecionado = String.format("%02d",(dataAtual.getMonth()+1));
        mesAnoSelecionado = String.valueOf( mesSelecionado + "" + dataAtual.getYear());
        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                String mesSelecionado = String.format("%02d",(date.getMonth()+1));

                mesAnoSelecionado = String.valueOf(mesSelecionado+ "" + date.getYear());

                movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
                recuperarMovimentacoes();
            }
        });
    }


    public void swipe(){

        ItemTouchHelper.Callback itemTouch = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                int swipeFlags = ItemTouchHelper.START |ItemTouchHelper.END;
                return makeMovementFlags(dragFlags,swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
               excluirMovimentacao(viewHolder);
            }
        };

        new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);

    }

    public void excluirMovimentacao(final RecyclerView.ViewHolder viewHolder){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Excluir Movimentação da Conta");
        alertDialog.setMessage("Você tem certeza que deseja realmente excluir essa movimentação da sua conta? ");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                int position = viewHolder.getAdapterPosition();
                movimentacao = movimentacoes.get(position);

                String emailUsuario = autenticacao.getCurrentUser().getEmail();
                String idUsuario = Base64Custom.codificarBase64(emailUsuario);

                movimentacaoRef= firebaseRef.child("movimentacao")
                        .child(idUsuario)
                        .child(mesAnoSelecionado);

                movimentacaoRef.child(movimentacao.getKey()).removeValue();
                adapterMovimentacao.notifyItemRemoved(position);
                atualizarSaldo();



            }
        });

        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(PrincipalActivity.this
                ,       "Cancelado",
                        Toast.LENGTH_SHORT).show();
                adapterMovimentacao.notifyDataSetChanged();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();
    }

    public void atualizarSaldo(){

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);
        if(movimentacao.getTipo().equals("r")){
            receitaTotal = receitaTotal - movimentacao.getValor();
            usuarioRef.child("receitaTotal").setValue(receitaTotal);
        }
        if(movimentacao.getTipo().equals("d")){
            despesaTotal = despesaTotal - movimentacao.getValor();
            usuarioRef.child("despesaTotal").setValue(despesaTotal);
        }

    }


    public void recuperarMovimentacoes(){
        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);

        movimentacaoRef= firebaseRef.child("movimentacao")
                                    .child(idUsuario)
                                    .child(mesAnoSelecionado);

        valueEventListenerMovimentacoes = movimentacaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                movimentacoes.clear();

                for(DataSnapshot dados: dataSnapshot.getChildren()){

                    Movimentacao movimentacao = dados.getValue(Movimentacao.class);
                    movimentacao.setKey(dados.getKey());
                    movimentacoes.add(movimentacao);

                }

                adapterMovimentacao.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }


    public void recuperarResumo(){

        String emailUsuario = autenticacao.getCurrentUser().getEmail();
        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
        usuarioRef = firebaseRef.child("usuarios").child(idUsuario);

       valueEventListenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Usuario usuario = dataSnapshot.getValue(Usuario.class);
                //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(usuario==null){ //Verificação de usuario, feito para resolver o bug de login sem criação do usuario no firebase
                     cadastroFbUser();//metodo que faz a criação dos nos no firebase(caso o login seja facebook,gmaill,etc)
                }else {

                    despesaTotal = usuario.getDespesaTotal();
                    receitaTotal = usuario.getReceitaTotal();
                    resunmoUsuario = receitaTotal - despesaTotal;

                    DecimalFormat decimalFormat = new DecimalFormat("0.##");
                    String resultadoFormatado = decimalFormat.format(resunmoUsuario);

                    textoSaudacao.setText("Olá, " + usuario.getNome());
                    textoSaldo.setText("R$ " + resultadoFormatado);
                }

            }
           public void cadastroFbUser(){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user==null){
                    Toast.makeText(PrincipalActivity.this,"usuario excluído",Toast.LENGTH_LONG).show();
                }else {
                    String name = user.getDisplayName();
                    usuario.setEmail(user.getEmail());
                    usuario.setNome(name);
                    String IdUsuario = Base64Custom.codificarBase64(user.getEmail());
                    usuario.setIdUsuario(IdUsuario);
                    //Log.i("dados","dado: " +name);
                    usuario.salvar();
                }
           }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menuSair :
                      autenticacao.signOut();
                      LoginManager.getInstance().logOut();
                      startActivity(new Intent(this,MainActivity.class));
                      finish();
                      break;
            case R.id.menuExcluirConta:
                alertDialogExcluirUser();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void alertDialogExcluirUser(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Excluir Conta");
        alertDialog.setMessage("Você deseja realmente excluir essa conta?");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                abrirTelaLogin();
            }
        });

        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void abrirTelaLogin() {
    startActivity(new Intent(this,LoginActivity.class));
    }

    public void adicionarReceita(View view){
        startActivity(new Intent(this,ReceitasActivity.class));

    }

    public void adicionarDespesa(View view){
        startActivity(new Intent(this,DespesasActivity.class));
    }





    @Override
    protected void onStart() {
        super.onStart();
            recuperarResumo();
            recuperarMovimentacoes();
        }


    @Override
    protected void onStop() {
        super.onStop();
        usuarioRef.removeEventListener(valueEventListenerUsuario);
        movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
    }
}

