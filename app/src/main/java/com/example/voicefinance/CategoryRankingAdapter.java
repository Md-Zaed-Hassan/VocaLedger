package com.example.voicefinance;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRankingAdapter extends RecyclerView.Adapter<CategoryRankingAdapter.ViewHolder> {

    private List<TransactionDao.CategoryTotal> categories;
    private double totalExpense;
    private Map<String, Integer> categoryColors;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category, String year, String month);
    }

    public CategoryRankingAdapter(List<TransactionDao.CategoryTotal> categories, double totalExpense, OnCategoryClickListener listener) {
        this.categories = categories;
        this.totalExpense = totalExpense;
        this.categoryColors = new HashMap<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_ranking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionDao.CategoryTotal category = categories.get(position);

        holder.rankNumber.setText(String.valueOf(position + 1));
        holder.categoryName.setText(category.category);

        double amount = Math.abs(category.total);
        holder.categoryAmount.setText(
                CurrencyUtils.getCurrencyInstance().format(amount)
        );

        double percentage = totalExpense != 0 ? (amount / Math.abs(totalExpense)) * 100 : 0;
        holder.categoryPercentage.setText(String.format("%.1f%%", percentage));

        // Set category color from pie chart
        if (categoryColors.containsKey(category.category)) {
            int color = categoryColors.get(category.category);
            holder.colorIndicator.setBackgroundColor(color);
            holder.colorIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.colorIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category.category, "", "");
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public void updateData(List<TransactionDao.CategoryTotal> newCategories, double newTotalExpense) {
        this.categories = newCategories;
        this.totalExpense = newTotalExpense;
        notifyDataSetChanged();
    }

    public void setCategoryColors(Map<String, Integer> colors) {
        this.categoryColors = colors != null ? colors : new HashMap<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankNumber;
        TextView categoryName;
        TextView categoryAmount;
        TextView categoryPercentage;
        View colorIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankNumber = itemView.findViewById(R.id.rankNumber);
            categoryName = itemView.findViewById(R.id.categoryName);
            categoryAmount = itemView.findViewById(R.id.categoryAmount);
            categoryPercentage = itemView.findViewById(R.id.categoryPercentage);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
        }
    }
}

