package com.example.pruenaf1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//Necesarios para el funcionamiento
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;
//Libreria para Mqtt
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
//Libreria para Objetos Json
import org.json.JSONException;
import org.json.JSONObject;
//Libreria para Coap
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class MainActivity extends AppCompatActivity {
    private static final long MIN_TIME = 10000; // 10 segundos
    TextView tvMensaje;
    TextView textView,textViewcolor;
    //Button btnCntMqtt;
    SensorManager mSensorManager;
    Sensor sLuminosidad;
    Sensor sensorProx;
    SensorEventListener lightEventListener;
    SensorEventListener AproxsensorEventListener;
    float maximoValor;
    float valor;
    int estado;
    String va,es;
    JSONObject content = new JSONObject();
    Double Latitud;
    Double Longitud;
    String lati,longi;
    EditText etDato;
    EditText etDato1;
    Button btnGuardar,btnGuardar1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textViewcolor = findViewById(R.id.textView7);
        //btnCntMqtt = findViewById(R.id.btnCntMqtt);

       // etDato = (EditText)findViewById(R.id.etDato);
        btnGuardar = (Button)findViewById(R.id.namehost);
        btnGuardar1 = (Button)findViewById(R.id.nametoken);

        SharedPreferences sharpref = getPreferences(Context.MODE_PRIVATE);
        String dato1 = sharpref.getString("Host","No hay dato");
        String dato2 = sharpref.getString("Token","No hay dato");
        Toast.makeText(getApplicationContext(), "Host : "+dato1,Toast.LENGTH_LONG).show();
        Toast.makeText(getApplicationContext(), "Token : "+dato2,Toast.LENGTH_LONG).show();
        // Activacion de servicio Sensor_Service asi como de los sensores de luz y proximidad
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sLuminosidad = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorProx = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Adquisicion del Host y el Token de la aplicacion

        etDato= ((EditText) findViewById(R.id.editHost));//.setText("demo.thingsboard.io");
        etDato1=((EditText) findViewById(R.id.edittoken));//.setText("Your token");

        tvMensaje = findViewById(R.id.tvMensaje);
        // Condición If para activar el GPS del celular cuando este este desactivado
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            // Inicia la clase Localizacion que toma los valores del GPS del celular
            iniciarLocalizacion();
        }
        // Condición If en caso de que no haya sensor de luminosidad en el celular
        if (sLuminosidad == null) {
            Toast.makeText(this, "El celular no tiene sensor de luminosidad", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Condición If en caso de que no haya sensor de proximidad en el celular
        if (sensorProx == null) {
            Toast.makeText(this, "El celular no tiene sensor de aproximación", Toast.LENGTH_SHORT).show();
            finish();
        }


        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharpref = getPreferences(Context.MODE_PRIVATE);
                //String valor = sharpref.getString("MiDato","No hay dato");
                SharedPreferences.Editor editor = sharpref.edit();
                String Dato1 =etDato.getText().toString();
                String Dato2 =etDato1.getText().toString();
                editor.putString("Host",Dato1 );
                editor.putString("Token",Dato2 );
                editor.commit();

            }
        });
        btnGuardar1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharpref = getPreferences(Context.MODE_PRIVATE);
                String dato1 = sharpref.getString("Host","No hay dato");
                String dato2 = sharpref.getString("Token","No hay dato");
                Toast.makeText(getApplicationContext(), "Host : "+dato1,Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(), "Token : "+dato2,Toast.LENGTH_LONG).show();
            }
        });




        // max value for light sensor
        maximoValor = sLuminosidad.getMaximumRange();
        lightEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                valor = sensorEvent.values[0];
                va = Float.toString(valor);
                textView.setText("Luminosidad: " + valor + " lux");
                // between 0 and 255
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                mSensorManager.registerListener(lightEventListener, sLuminosidad, SensorManager.SENSOR_DELAY_FASTEST);
            }
        };
        AproxsensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.values[0] < sensorProx.getMaximumRange()) {
                    textViewcolor.setText("Proximidad: Cerca");
                    estado = 1;
                    es = Integer.toString(estado);
                    textViewcolor.setBackgroundColor(Color.RED);
                } else {
                    textViewcolor.setText("Proximidad:Lejos");
                    textViewcolor.setBackgroundColor(Color.GREEN);
                    estado = 0;
                    es = Integer.toString(estado);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        start();
    }

    private void iniciarLocalizacion() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener local = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                   Latitud=location.getLatitude();
                   lati = Double.toString(Latitud);
                   Longitud=location.getLongitude();
                   longi = Double.toString(Longitud);
                String texto = "Mi ubicación es: \n"
                        + "Latitud = " + location.getLatitude() + "\n"
                        + "Longitud = " + location.getLongitude();
                tvMensaje.setText(texto);
            }
        };

        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gpsEnabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, 0, local);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, 0, local);
        tvMensaje.setText("Localizacion agregada");
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarLocalizacion();
                return;
            }
        }
    }
        public void start(){
            mSensorManager.registerListener(AproxsensorEventListener,sensorProx,2000*1000);
        }
        public void stop(){
            mSensorManager.unregisterListener(AproxsensorEventListener);
        }
        @Override
        protected void onPause() {
            stop();
            super.onPause();
        }
        @Override
        protected void onResume() {
            start();
            super.onResume();
            mSensorManager.registerListener(lightEventListener, sLuminosidad, SensorManager.SENSOR_DELAY_FASTEST);
        }
    public void sendMessageMqtt (View view) {
        // Adquisición del Host y el Token de la aplicación
        String host = ((EditText)findViewById(R.id.editHost)).getText().toString();
        String token = ((EditText)findViewById(R.id.edittoken)).getText().toString();
        // Variables tipo String de los valores de Mqtt en el servidor
        final String topic = "v1/devices/me/telemetry";
        final int qos = 2;
        final String broker = "tcp://"+host+":1883";
        final String clientId = MqttClient.generateClientId();
        final MemoryPersistence persistence = new MemoryPersistence();
        // Creación del archivo Json compatible con Thingsboard
        try {
            content.put("Luminosidad", valor);
            content.put("Proximidad", estado);
            content.put("latitude",Latitud);
            content.put("longitude",Longitud);
        } catch (JSONException error) {
        }
        // Inicio de la conexión de Mqtt y envio de datos
        try {
            // Creación del cliente Mqtt
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            //connOpts.setCleanSession(false);
            // Translado del user o Token
            connOpts.setUserName(token);
            connOpts.setPassword("".toCharArray());
            connOpts.setCleanSession(true);
           // System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: " + content);
            // Codificación del mensaje que se enviará al servidor
            MqttMessage message = new MqttMessage(content.toString().getBytes());
            // Calidad del servicio en Mqtt
            message.setQos(qos);
            // Mensaje puclicado
            sampleClient.publish(topic, message);
            System.out.println("Message published");
            // Fin de la conexión
           // sampleClient.disconnect();
            System.out.println("Disconnected");
            // System.exit(0);
        } catch (MqttException me) {
            me.printStackTrace();
        }
    }
    public void sendMessageHttp (View view) {
        // Adquisición del Host y el Token de la aplicación
        String host = ((EditText)findViewById(R.id.editHost)).getText().toString();
        String token = ((EditText)findViewById(R.id.edittoken)).getText().toString();
        // Do something in response to button
        ServicioTask servicioTask= new ServicioTask(this,"https://"+host+"/api/v1/"+token+"/telemetry",va,es,lati,longi);
        servicioTask.execute();
    }
    public void clickGet(View view) {
        // Adquisición del Host y el Token de la aplicación
        String host = ((EditText)findViewById(R.id.editHost)).getText().toString();
        String token = ((EditText)findViewById(R.id.edittoken)).getText().toString();
        String uri ="coap://"+host+"/api/v1/"+token+"/telemetry";
        new CoapGetTask().execute(uri);
    }
    class CoapGetTask extends AsyncTask<String, String, CoapResponse> {
        protected void onPreExecute() {
            // reset text fields
            ((TextView)findViewById(R.id.textCode)).setText("");
            ((TextView)findViewById(R.id.textCodeName)).setText("Loading...");
            ((TextView)findViewById(R.id.textRtt)).setText("");
        }
        protected CoapResponse doInBackground(String... args) {
            try {
                // Creación del archivo Json compatible con Thingsboard
                try {
                    content.put("Luminosidad", valor);
                    content.put("Proximidad", estado);
                    content.put("latitude",Latitud);
                    content.put("longitude",Longitud);
                } catch (JSONException error) {
                }
                // Creación del cliente Coap
                CoapClient client = new CoapClient(args[0]);
                String content1 = client.get().getResponseText();
                System.out.println("RESPONSE 1: " + content1);
                // Codificación del mensaje que se enviará al servidor y puclica el Mensaje
                CoapResponse resp2 = client.post(content.toString(), MediaTypeRegistry.TEXT_PLAIN);
                System.out.println("RESPONSE 2 CODE: " + resp2.getCode());
                return resp2;
                // Acaba la conexión
            } catch(Exception ex) {
                Log.e("coap", ex.getMessage(), ex);
                return null;
            }
        }
        protected void onPostExecute(CoapResponse response) {
            if (response!=null) {
                // Respuesta del cliente coap y tiempo del proceso
                ((TextView)findViewById(R.id.textCode)).setText(response.getCode().toString());
                ((TextView)findViewById(R.id.textCodeName)).setText(response.getCode().name());
                ((TextView)findViewById(R.id.textRtt)).setText(response.advanced().getRTT()+" ms");
            } else {
                ((TextView)findViewById(R.id.textCodeName)).setText("No response");
            }
        }
    }
}