package com.whasupp.app.ui.view;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.whasupp.app.R;
import com.whasupp.app.ui.adapters.ChatAdapter;
import com.whasupp.app.ui.viewmodel.ChatVM;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private ChatVM viewModel;
    private ChatAdapter adapter;
    private EditText etMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. Datos del Amigo
        String chatName = getIntent().getStringExtra("USER_NAME");
        String chatPhoto = getIntent().getStringExtra("USER_PHOTO");
        String chatPartnerUid = getIntent().getStringExtra("USER_UID");

        // UI Header
        TextView tvTitle = findViewById(R.id.tvChatTitle);
        CircleImageView imgAvatar = findViewById(R.id.imgChatAvatar);

        if (chatName != null) tvTitle.setText(chatName);
        if (chatPhoto != null && !chatPhoto.isEmpty()) {
            Glide.with(this).load(chatPhoto).into(imgAvatar);
        }

        // 2. Mis Datos
        String myName = "Anonimo";
        String myUid = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            myName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 3. Setup RecyclerView
        etMessage = findViewById(R.id.etMessage);
        FloatingActionButton btnSend = findViewById(R.id.btnSend);
        RecyclerView recycler = findViewById(R.id.recyclerChat);

        adapter = new ChatAdapter(myName);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // El chat empieza abajo
        recycler.setLayoutManager(layoutManager);
        recycler.setAdapter(adapter);

        // 4. ViewModel
        viewModel = new ViewModelProvider(this).get(ChatVM.class);

        if (chatPartnerUid != null && !myUid.isEmpty()) {
            viewModel.setupChat(myUid, chatPartnerUid);
        } else {
            Toast.makeText(this, "Error de usuario", Toast.LENGTH_SHORT).show();
        }

        // --- OBSERVADOR 1: HISTORIAL COMPLETO ---
        viewModel.getHistoryMessages().observe(this, historyList -> {
            if (historyList != null) {
                // Aquí cargamos todos los mensajes antiguos de una sola vez
                adapter.setMessages(historyList);
                if (!historyList.isEmpty()) {
                    recycler.scrollToPosition(historyList.size() - 1);
                }
            }
        });

        // --- OBSERVADOR 2: NUEVOS MENSAJES (En Vivo) ---
        viewModel.getIncomingMessage().observe(this, message -> {
            if(message != null) {
                adapter.addMessage(message);
                recycler.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        // 5. Enviar
        final String senderName = myName;
        btnSend.setOnClickListener(v -> {
            String txt = etMessage.getText().toString().trim();
            if(!txt.isEmpty()) {
                viewModel.sendMessage(senderName, txt);
                etMessage.setText("");
            }
        });
    }
}