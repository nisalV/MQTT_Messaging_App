package com.exercises.mqttconnect;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Button connect;
    private Button disconnect;
    private Button subscribe;
    private Button unsubscribe;
    private EditText host, port, newTopic, newTopicData, subscribeTopic, qos;
    private IMqttToken token;
    private MqttAndroidClient client;
    private String clientId, uri;
    private boolean connectSuccess;
    private ArrayList<String> listItems;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect = findViewById(R.id.connect);
        disconnect = findViewById(R.id.disconnect);
        Button publish = findViewById(R.id.publish);
        subscribe = findViewById(R.id.subscribe);
        unsubscribe = findViewById(R.id.unsubscribe);
        ListView list = findViewById(R.id.list);
        host = findViewById(R.id.host);
        port = findViewById(R.id.port);
        newTopic = findViewById(R.id.newTopic);
        newTopicData = findViewById(R.id.newTopicData);
        subscribeTopic = findViewById(R.id.subscribeTopic);
        qos = findViewById(R.id.qos);

        connectSuccess = false;

        clientId = MqttClient.generateClientId();

        listItems = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        list.setAdapter(adapter);

        connect.setOnClickListener(v -> {
                if (internet_connection()){
                    try {
                        uri = host.getText().toString() + ":"+ port.getText().toString(); // tcp://broker.mqttdashboard.com:1883
                        client = new MqttAndroidClient(MainActivity.this, uri, clientId);
                        token = client.connect();
                        token.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                connectSuccess = true;
                                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                                connect.setTextColor(Color.parseColor("#58F15E"));
                                disconnect.setTextColor(Color.parseColor("#FFEC5757"));
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Toast.makeText(MainActivity.this, "Connection failure",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Connection failed, check host & port",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                if (!internet_connection()) {
                    Toast.makeText(MainActivity.this, "No network connection!", Toast.LENGTH_SHORT).show();
                }
        });
        disconnect.setOnClickListener(v -> {

            if(internet_connection() && (connectSuccess)) {
                try {
                    token = client.unsubscribe(String.valueOf(subscribeTopic.getText()));
                    token = client.disconnect();
                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            connectSuccess = false;
                            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                            connect.setTextColor(Color.parseColor("#FFFFFFFF"));
                            disconnect.setTextColor(Color.parseColor("#FFFFFFFF"));
                            subscribe.setTextColor(Color.parseColor("#FFFFFFFF"));
                            unsubscribe.setTextColor(Color.parseColor("#FFFFFFFF"));
                            subscribeTopic.setText("");
                            qos.setText("");
                            listItems.clear();
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Toast.makeText(MainActivity.this, "Problem occurred! probably disconnected", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "Connect first!", Toast.LENGTH_SHORT).show();
            }
        });

        publish.setOnClickListener(v -> {
            try {
                if (internet_connection() && (connectSuccess)) {
                    if (!(newTopic.getText().length() == 0)) {
                        client.publish(String.valueOf(newTopic.getText()), newTopicData.getText().toString().getBytes(),
                                0,false);
                    } else {
                        Toast.makeText(MainActivity.this, "Add a topic", Toast.LENGTH_SHORT).show();
                    }
                } else{
                    Toast.makeText(MainActivity.this, "Connect to a broker", Toast.LENGTH_SHORT).show();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });

        subscribe.setOnClickListener(v -> {
            try {
                if (internet_connection() && (connectSuccess) && (subscribeTopic.getText().length() > 0) && qos.getText().length() > 0) {
                    int i = Integer.parseInt(String.valueOf(qos.getText()));
                    IMqttToken subToken = client.subscribe(String.valueOf(subscribeTopic.getText()), i);
                    subToken.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            subscribe.setTextColor(Color.parseColor("#58F15E"));
                            unsubscribe.setTextColor(Color.parseColor("#FFEC5757"));
                            client.setCallback(new MqttCallback() {
                                @Override
                                public void connectionLost(Throwable cause) {
                                    Toast.makeText(MainActivity.this, "Connection lost", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void messageArrived(String topic, MqttMessage message) throws Exception {
                                    if ((!new String(message.getPayload()).equals("the payload"))) {
                                        String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
                                        adapter.insert(currentDateTimeString + "\n" + topic + ": " + new String(message.getPayload()), 0);
                                    }
                                }

                                @Override
                                public void deliveryComplete(IMqttDeliveryToken token) {
                                }
                            });
                        }
                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Toast.makeText(MainActivity.this, "Subscription failed: " + asyncActionToken, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Subscription problem. Check again", Toast.LENGTH_SHORT).show();
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });

        unsubscribe.setOnClickListener(v -> {
            try {
                if (internet_connection() && (connectSuccess)) {
                    token = client.unsubscribe(String.valueOf(subscribeTopic.getText()));
                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Toast.makeText(MainActivity.this, "Unsubscribed", Toast.LENGTH_SHORT).show();
                            subscribe.setTextColor(Color.parseColor("#FFFFFFFF"));
                            unsubscribe.setTextColor(Color.parseColor("#FFFFFFFF"));
                            subscribeTopic.setText("");
                            qos.setText("");
                            listItems.clear();
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        }
                    });
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
    }

    boolean internet_connection(){
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}