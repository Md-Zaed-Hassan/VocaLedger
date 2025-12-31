package com.example.voicefinance;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voicefinance.databinding.ItemTransactionBinding;

import java.text.NumberFormat;
import java.util.List;

public class TransactionAdapter
        extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
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
        holder.binding.label.setText(t.label);
        holder.binding.amount.setText(
                NumberFormat.getCurrencyInstance().format(t.amount)
        );
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
}
