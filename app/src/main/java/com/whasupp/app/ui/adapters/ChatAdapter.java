package com.whasupp.app.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.whasupp.app.R;
import com.whasupp.app.model.Message;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private final List<Message> messages = new ArrayList<>();
    private final String myName;

    public ChatAdapter(String myName) {
        this.myName = (myName != null) ? myName : "Anonimo";
    }

    // Para mensajes nuevos en tiempo real (MQTT)
    public void addMessage(Message msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    // --- NUEVO: Para cargar todo el historial de golpe (Firebase) ---
    public void setMessages(List<Message> history) {
        this.messages.clear(); // Limpiamos para no repetir
        this.messages.addAll(history);
        notifyDataSetChanged(); // Pintamos todo de nuevo
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.getSenderName() != null && msg.getSenderName().equalsIgnoreCase(myName)) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        if (holder instanceof SentHolder) {
            ((SentHolder) holder).bind(msg);
        } else {
            ((ReceivedHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentHolder extends RecyclerView.ViewHolder {
        TextView content;
        SentHolder(View v) {
            super(v);
            content = v.findViewById(R.id.tvContent);
        }
        void bind(Message m) {
            content.setText(m.getContent());
            content.setTextColor(Color.BLACK);
        }
    }

    static class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView content, sender;
        ReceivedHolder(View v) {
            super(v);
            content = v.findViewById(R.id.tvContent);
            sender = v.findViewById(R.id.tvSender);
        }
        void bind(Message m) {
            content.setText(m.getContent());
            sender.setText(m.getSenderName());
            content.setTextColor(Color.BLACK);
        }
    }
}