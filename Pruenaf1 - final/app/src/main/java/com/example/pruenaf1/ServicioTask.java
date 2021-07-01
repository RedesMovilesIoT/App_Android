package com.example.pruenaf1;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ServicioTask extends AsyncTask<Void, Void, String> {
    //variables del hilo
    private Context httpContext;//contexto
    ProgressDialog progressDialog;//dialogo cargando
    public String resultadoapi="";
    public String linkrequestAPI="";//link  para consumir el servicio rest
    public String va;
    public String es;
    public String lati;
    public String longi;
    //constructor del hilo (Asynctask)
    public ServicioTask(Context ctx, String linkAPI, String  va,String es,String lati,String longi ){
        this.httpContext=ctx;
        this.linkrequestAPI=linkAPI;
        this.va=va;
        this.es=es;
        this.lati=lati;
        this.longi=longi;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(httpContext, "Procesando Solicitud", "por favor, espere");
    }
    @Override
    protected String doInBackground(Void... params) {
        String result= null;
        String wsURL = linkrequestAPI;
        URL url = null;
        try {
            // se crea la conexion al api: http://localhost:15009/WEBAPIREST/api/persona
            url = new URL(wsURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            //crear el objeto json para enviar por POST
            JSONObject parametrosPost= new JSONObject();
            parametrosPost.put("Luminosidad", va);
            parametrosPost.put("Proximidad", es);
            parametrosPost.put("latitude",lati);
            parametrosPost.put("longitude",longi);
            //DEFINIR PARAMETROS DE CONEXION
            urlConnection.setReadTimeout(15000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("POST");// se puede cambiar por delete ,put ,etc
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Bearer");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            //OBTENER EL RESULTADO DEL REQUEST
            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(parametrosPost.toString());
            writer.flush();
            writer.close();
            os.close();
            int responseCode=urlConnection.getResponseCode();// conexion OK?
            if(responseCode== HttpURLConnection.HTTP_OK){
                BufferedReader in= new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuffer sb= new StringBuffer("");
                String linea="";
                while ((linea=in.readLine())!= null){
                    sb.append(linea);
                    break;
                }
                in.close();
                result= sb.toString();
            }
            else{
                result= new String("Error: "+ responseCode);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  result;
    }
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        progressDialog.dismiss();
        resultadoapi=s;
      // Toast.makeText(httpContext,resultadoapi,Toast.LENGTH_LONG).show();//mostrara una notificacion con el resultado del request
    }
}
