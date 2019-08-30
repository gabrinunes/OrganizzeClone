package com.example.organizze.helper;

import java.text.SimpleDateFormat;

public class DateCustom {

 public static String dataAtual(){

    long date = System.currentTimeMillis();
     SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
     String dataString = simpleDateFormat.format(date);
     return dataString;
 }

 public static String mesAnoDataEscolhida(String data){

     //28/08/2019
     String retornoData[] = data.split("/");
     String dia = retornoData[0]; //dia 28
     String mes = retornoData[1]; //mes 08
     String ano = retornoData[2]; //ano 2019

     String mesAno = mes + ano;
     return mesAno;
 }



}
