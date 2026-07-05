package com.veganbeauty.app.features.myskin;



import android.graphics.Typeface;

import android.text.SpannableString;

import android.text.Spanned;

import android.text.style.ForegroundColorSpan;

import android.text.style.StyleSpan;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.FrameLayout;

import android.widget.ImageView;

import android.widget.TextView;



import androidx.annotation.NonNull;

import androidx.cardview.widget.CardView;

import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.RecyclerView;



import com.veganbeauty.app.R;



import java.util.List;
import java.util.function.BooleanSupplier;

import java.util.regex.Matcher;

import java.util.regex.Pattern;



public class BookingServiceAdapter extends RecyclerView.Adapter<BookingServiceAdapter.ViewHolder> {

    public interface OnItemClickListener {

        void onClick(BookingService service);

    }



    private static final Pattern PRICE_PATTERN = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+\\s*đ");



    private final List<BookingService> list;

    private final OnItemClickListener listener;

    private final BooleanSupplier loginGate;

    private int selectedIndex = -1;



    public BookingServiceAdapter(List<BookingService> list, OnItemClickListener listener) {

        this(list, listener, () -> true);

    }



    public BookingServiceAdapter(List<BookingService> list, OnItemClickListener listener, BooleanSupplier loginGate) {

        this.list = list;

        this.listener = listener;

        this.loginGate = loginGate != null ? loginGate : () -> true;

    }



    static class ViewHolder extends RecyclerView.ViewHolder {

        final CardView cardView;

        final View container;

        final FrameLayout iconWrap;

        final ImageView ivIcon;

        final TextView tvName;

        final TextView tvDuration;

        final TextView tvDesc;

        final ImageView ivCheck;



        ViewHolder(View itemView) {

            super(itemView);

            cardView = itemView.findViewById(R.id.card_service);

            container = itemView.findViewById(R.id.container_service);

            iconWrap = itemView.findViewById(R.id.icon_service_wrap);

            ivIcon = itemView.findViewById(R.id.iv_service_icon);

            tvName = itemView.findViewById(R.id.tv_service_name);

            tvDuration = itemView.findViewById(R.id.tv_service_duration);

            tvDesc = itemView.findViewById(R.id.tv_service_desc);

            ivCheck = itemView.findViewById(R.id.iv_service_check);

        }

    }



    @NonNull

    @Override

    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())

                .inflate(R.layout.skin_item_booking_service, parent, false);

        return new ViewHolder(view);

    }



    @Override

    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        BookingService item = list.get(position);

        holder.tvName.setText(item.getName());

        holder.tvDuration.setText(item.getDuration());

        setHighlightedPriceText(holder.tvDesc, extractPriceLabel(item.getPriceInfo()));



        boolean isSelected = position == selectedIndex;

        holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.container.setBackgroundResource(resolveBackground(item.getId(), isSelected));

        holder.iconWrap.setBackgroundResource(resolveIconBackground(item.getId()));

        holder.ivIcon.setImageResource(resolveIcon(item.getId()));

        holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), resolveAccentColor(item.getId())));

        holder.ivCheck.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), resolveAccentColor(item.getId())));

        holder.cardView.setCardElevation(isSelected ? 10f : 4f);



        holder.cardView.setOnClickListener(v -> {

            if (!loginGate.getAsBoolean()) {

                return;

            }

            int oldIndex = selectedIndex;

            selectedIndex = holder.getBindingAdapterPosition();

            if (oldIndex >= 0) notifyItemChanged(oldIndex);

            if (selectedIndex >= 0) notifyItemChanged(selectedIndex);

            if (listener != null && selectedIndex >= 0 && selectedIndex < list.size()) {

                listener.onClick(list.get(selectedIndex));

            }

        });

    }



    private String extractPriceLabel(String priceInfo) {

        if (priceInfo == null) return "";

        int separator = priceInfo.indexOf(" * ");

        return separator >= 0 ? priceInfo.substring(separator + 3).trim() : priceInfo;

    }



    private int resolveBackground(String serviceId, boolean isSelected) {

        if ("2".equals(serviceId)) {

            return isSelected

                    ? R.drawable.skin_bg_booking_service_yellow_selected

                    : R.drawable.skin_bg_booking_service_yellow;

        }

        if ("3".equals(serviceId)) {

            return isSelected

                    ? R.drawable.skin_bg_booking_service_blue_selected

                    : R.drawable.skin_bg_booking_service_blue;

        }

        return isSelected

                ? R.drawable.skin_bg_booking_service_green_selected

                : R.drawable.skin_bg_booking_service_green;

    }



    private int resolveIconBackground(String serviceId) {

        if ("2".equals(serviceId)) {

            return R.drawable.skin_bg_booking_service_icon_yellow;

        }

        if ("3".equals(serviceId)) {

            return R.drawable.skin_bg_booking_service_icon_blue;

        }

        return R.drawable.skin_bg_booking_service_icon_green;

    }



    private int resolveIcon(String serviceId) {

        if ("2".equals(serviceId)) {

            return R.drawable.ic_advanced;

        }

        if ("3".equals(serviceId)) {

            return R.drawable.ic_checklist_pen;

        }

        return R.drawable.ic_skin_sparkle;

    }



    private int resolveAccentColor(String serviceId) {

        if ("2".equals(serviceId)) {

            return R.color.status_level_yellow;

        }

        if ("3".equals(serviceId)) {

            return R.color.status_level_blue;

        }

        return R.color.status_level_green;

    }



    private void setHighlightedPriceText(TextView textView, String priceInfo) {

        SpannableString spannable = new SpannableString(priceInfo);

        int redColor = ContextCompat.getColor(textView.getContext(), R.color.status_level_red);



        int freeIdx = priceInfo.indexOf("Miễn phí");

        if (freeIdx >= 0) {

            applyHighlight(spannable, freeIdx, freeIdx + "Miễn phí".length(), redColor);

        }



        Matcher matcher = PRICE_PATTERN.matcher(priceInfo);

        if (matcher.find()) {

            applyHighlight(spannable, matcher.start(), matcher.end(), redColor);

        }



        textView.setText(spannable);

    }



    private void applyHighlight(SpannableString spannable, int start, int end, int color) {

        spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    }



    @Override

    public int getItemCount() {

        return list != null ? list.size() : 0;

    }

}
