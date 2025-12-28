package com.example.voicefinance;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.NumberFormat;

public class CustomMarkerView extends MarkerView {

    private TextView tvContent;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    // This method is called every time the MarkerView is redrawn
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof PieEntry) {
            PieEntry pieEntry = (PieEntry) e;
            String category = pieEntry.getLabel();
            String amount = NumberFormat.getCurrencyInstance().format(pieEntry.getValue());
            tvContent.setText(category + ": " + amount);
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // This positions the marker so it doesn't overlap the touch point
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
