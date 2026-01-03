package com.example.voicefinance;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voicefinance.databinding.ItemHistoryDateBinding;
import com.example.voicefinance.databinding.ItemTransactionBinding;

import java.text.NumberFormat;
import java.util.List;

public class HistoryAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE = 0;
    private static final int TYPE_TRANSACTION = 1;

    private final List<HistoryListItem> items;
    private final TransactionAdapter.OnTransactionLongClickListener listener;

    public HistoryAdapter(
            List<HistoryListItem> items,
            TransactionAdapter.OnTransactionLongClickListener listener
    ) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HistoryListItem.DateHeader
                ? TYPE_DATE
                : TYPE_TRANSACTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_DATE) {
            return new DateViewHolder(
                    ItemHistoryDateBinding.inflate(
                            inflater, parent, false
                    )
            );
        } else {
            return new TransactionViewHolder(
                    ItemTransactionBinding.inflate(
                            inflater, parent, false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        HistoryListItem item = items.get(position);

        if (holder instanceof DateViewHolder) {

            HistoryListItem.DateHeader header =
                    (HistoryListItem.DateHeader) item;

            ((DateViewHolder) holder).binding.dateText
                    .setText(header.dateText);

            ((DateViewHolder) holder).binding.totalText
                    .setText(
                            "Expenses: " +
                                    NumberFormat.getCurrencyInstance()
                                            .format(header.total)
                    );

        } else {

            Transaction t =
                    ((HistoryListItem.TransactionItem) item).transaction;

            TransactionViewHolder vh =
                    (TransactionViewHolder) holder;

            vh.binding.label.setText(t.label);
            vh.binding.amount.setText(
                    NumberFormat.getCurrencyInstance()
                            .format(t.amount)
            );

            vh.itemView.setOnLongClickListener(v -> {
                listener.onLongClick(t);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        ItemHistoryDateBinding binding;
        DateViewHolder(ItemHistoryDateBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ItemTransactionBinding binding;
        TransactionViewHolder(ItemTransactionBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }
}
