package com.example.voicefinance;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voicefinance.databinding.ItemTransactionBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter
        extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<Transaction> transactions;

    public interface OnTransactionLongClickListener {
        void onLongClick(Transaction transaction);
    }

    private final OnTransactionLongClickListener longClickListener;

    // ✅ Date formatter (simple, readable)
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public TransactionAdapter(
            List<Transaction> transactions,
            OnTransactionLongClickListener listener
    ) {
        this.transactions = transactions;
        this.longClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemTransactionBinding binding;

        ViewHolder(ItemTransactionBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        ItemTransactionBinding binding =
                ItemTransactionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        Transaction t = transactions.get(position);

        // ✅ SHOW DATE
        holder.binding.date.setText(
                dateFormat.format(new Date(t.timestamp))
        );

        holder.binding.label.setText(t.label);
        holder.binding.amount.setText(
                NumberFormat.getCurrencyInstance().format(t.amount)
        );

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(t);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
}
