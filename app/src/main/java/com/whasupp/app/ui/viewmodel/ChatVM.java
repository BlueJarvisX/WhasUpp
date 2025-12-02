package com.whasupp.app.ui.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.database.DataSnapshot;
import com.whasupp.app.data.FirebaseRepository;
import com.whasupp.app.data.MqttRepository;
import com.whasupp.app.model.Message;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatVM extends ViewModel {
    private final MqttRepository mqttRepo;
    private final FirebaseRepository firebaseRepo;

    // Para mensajes individuales (En vivo)
    private final MutableLiveData<Message> incomingMessage = new MutableLiveData<>();
    // Para la lista completa (Historial)
    private final MutableLiveData<List<Message>> historyMessages = new MutableLiveData<>();

    private static final String TAG = "CHAT_VM";
    private String currentChatId = null;

    public ChatVM() {
        mqttRepo = MqttRepository.getInstance();
        firebaseRepo = FirebaseRepository.getInstance();
    }

    public void setupChat(String myUid, String otherUid) {
        if (currentChatId != null) return;

        // Generar ID de sala privada
        String[] ids = {myUid, otherUid};
        Arrays.sort(ids);
        currentChatId = "chat_" + ids[0] + "_" + ids[1];

        Log.d(TAG, "Sala Privada Iniciada: " + currentChatId);

        // Cargar historial y conectar
        loadHistoryFromFirebase();
        connectAndSubscribe();
    }

    private void loadHistoryFromFirebase() {
        // Obtenemos la referencia de la sala privada
        firebaseRepo.getMessagesRef(currentChatId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Message> fullHistory = new ArrayList<>();

                // Recorremos todos los mensajes guardados
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    Message msg = snapshot.getValue(Message.class);
                    if (msg != null) {
                        fullHistory.add(msg);
                    }
                }

                // Enviamos la lista completa a la UI
                historyMessages.postValue(fullHistory);
                Log.d(TAG, "Historial cargado: " + fullHistory.size() + " mensajes.");
            }
        });
    }

    private void connectAndSubscribe() {
        mqttRepo.connect(() -> {
            mqttRepo.subscribe(currentChatId, (topic, msg) -> {
                try {
                    String payload = new String(msg.getPayload());
                    JSONObject json = new JSONObject(payload);

                    // Lectura robusta (Español/Inglés)
                    String sender;
                    if (json.has("remitente")) sender = json.optString("remitente");
                    else sender = json.optString("sender", "Anónimo");

                    String content;
                    if (json.has("contenido")) content = json.optString("contenido");
                    else content = json.optString("content", "");

                    Message message = new Message(sender, content, System.currentTimeMillis());
                    incomingMessage.postValue(message);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, () -> Log.e(TAG, "Error conexión MQTT"));
    }

    public void sendMessage(String sender, String content) {
        if (currentChatId == null) return;

        try {
            Message msgObject = new Message(sender, content, System.currentTimeMillis());

            JSONObject json = new JSONObject();
            json.put("remitente", sender);
            json.put("contenido", content);

            // Publicar en sala privada
            mqttRepo.publish(currentChatId, json.toString());
            // Guardar en sala privada
            firebaseRepo.saveMessage(currentChatId, msgObject);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiveData<Message> getIncomingMessage() { return incomingMessage; }
    public LiveData<List<Message>> getHistoryMessages() { return historyMessages; }
}