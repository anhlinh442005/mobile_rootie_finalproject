package com.veganbeauty.app.features.myskin;

import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookingHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Object> items;
    private final OnItemClickListener onViewDetailClick;
    private final OnItemClickListener onCancelClick;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnItemClickListener {
        void onClick(BookingHistoryEntity item);
    }

    public BookingHistoryAdapter(List<Object> items, OnItemClickListener onViewDetailClick, OnItemClickListener onCancelClick) {
        this.items = items;
        this.onViewDetailClick = onViewDetailClick;
        this.onCancelClick = onCancelClick;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.skin_item_booking_history_header, parent, false));
        } else {
            return new ItemViewHolder(inflater.inflate(R.layout.skin_item_booking_history, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof HeaderViewHolder && item instanceof String) {
            ((HeaderViewHolder) holder).tvHeaderTitle.setText((String) item);
        } else if (holder instanceof ItemViewHolder && item instanceof BookingHistoryEntity) {
            BookingHistoryEntity entity = (BookingHistoryEntity) item;
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            String[] parsedDate = BookingDateParser.parseDateDisplay(entity.getDateDisplay(), entity.getMonthDisplay(), entity.getDayOfWeek());
            itemHolder.tvDateNum.setText(parsedDate[0]);
            itemHolder.tvMonthDay.setText(parsedDate[1]);
            itemHolder.tvServiceName.setText(entity.getServiceName());
            itemHolder.tvTime.setText(entity.getTime());

            String storeName = entity.getStoreName() != null ? entity.getStoreName() : "";
            String[] parts = storeName.split("\n");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                String boldName = "<b>" + parts[0] + "</b>";
                String rest = entity.getStoreAddress();
                itemHolder.tvStore.setText(Html.fromHtml(boldName + "<br/>" + rest, Html.FROM_HTML_MODE_COMPACT));
            } else {
                itemHolder.tvStore.setText(storeName + "\n" + entity.getStoreAddress());
            }

            itemHolder.tvStatusTag.setText(entity.getStatus());

            String status = entity.getStatus() != null ? entity.getStatus() : "";
            if (status.equalsIgnoreCase("Sắp diễn ra") || status.equalsIgnoreCase("Chờ xác nhận") || status.equalsIgnoreCase("pending")) {
                if (status.equalsIgnoreCase("Chờ xác nhận") || status.equalsIgnoreCase("pending")) {
                    itemHolder.tvStatusTag.setText("Chờ xác nhận");
                    itemHolder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_pending);
                    itemHolder.tvStatusTag.setTextColor(Color.parseColor("#E65100"));
                } else {
                    itemHolder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_upcoming);
                    itemHolder.tvStatusTag.setTextColor(Color.parseColor("#1976D2"));
                }
                itemHolder.llActions.setVisibility(View.VISIBLE);
            } else if (status.equalsIgnoreCase("Đã hoàn thành")) {
                itemHolder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_completed);
                itemHolder.tvStatusTag.setTextColor(Color.parseColor("#2E7D32"));
                itemHolder.llActions.setVisibility(View.GONE);
            } else if (status.equalsIgnoreCase("Đã huỷ") || status.equalsIgnoreCase("Đã hủy")) {
                itemHolder.tvStatusTag.setText("Đã huỷ");
                itemHolder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_cancelled);
                itemHolder.tvStatusTag.setTextColor(Color.parseColor("#C62828"));
                itemHolder.llActions.setVisibility(View.GONE);
            } else {
                itemHolder.tvStatusTag.setBackgroundColor(Color.GRAY);
                itemHolder.tvStatusTag.setTextColor(Color.WHITE);
                itemHolder.llActions.setVisibility(View.GONE);
            }

            itemHolder.btnViewDetail.setOnClickListener(v -> {
                if (onViewDetailClick != null) onViewDetailClick.onClick(entity);
            });

            itemHolder.btnCancel.setOnClickListener(v -> {
                if (onCancelClick != null) onCancelClick.onClick(entity);
            });

            itemHolder.itemView.setOnClickListener(v -> {
                if (onViewDetailClick != null) onViewDetailClick.onClick(entity);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<Object> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvHeaderTitle;

        public HeaderViewHolder(View view) {
            super(view);
            tvHeaderTitle = view.findViewById(R.id.tv_header_title);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvDateNum;
        public final TextView tvMonthDay;
        public final TextView tvServiceName;
        public final TextView tvStatusTag;
        public final TextView tvTime;
        public final TextView tvStore;
        public final View llActions;
        public final TextView btnViewDetail;
        public final TextView btnCancel;

        public ItemViewHolder(View view) {
            super(view);
            tvDateNum = view.findViewById(R.id.tv_history_date_num);
            tvMonthDay = view.findViewById(R.id.tv_history_month_day);
            tvServiceName = view.findViewById(R.id.tv_history_service_name);
            tvStatusTag = view.findViewById(R.id.tv_history_status_tag);
            tvTime = view.findViewById(R.id.tv_history_time);
            tvStore = view.findViewById(R.id.tv_history_store);
            llActions = view.findViewById(R.id.ll_history_actions);
            btnViewDetail = view.findViewById(R.id.btn_view_detail);
            btnCancel = view.findViewById(R.id.btn_cancel_booking);
        }
    }

    public static class BookingDateParser {
        public static String[] parseDateDisplay(String dateDisplay, String monthDisplay, String dayOfWeek) {
            String dateStr = dateDisplay != null ? dateDisplay.trim() : "";

            if (dateStr.contains("/")) {
                String[] parts = dateStr.split("/");
                if (parts.length > 0) {
                    String day = parts[0];
                    int monthNum = 0;
                    if (parts.length > 1) {
                        try {
                            monthNum = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ignored) {}
                    }
                    String monthStr = monthNum > 0 ? "Tháng " + monthNum : "Tháng --";
                    String dayOfWeekShort = getShortDayOfWeek(dayOfWeek);
                    return new String[]{day, monthStr + "\n" + dayOfWeekShort};
                }
            }

            Pattern pattern = Pattern.compile("(\\d+)\\s+tháng\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(dateStr);
            if (matcher.find()) {
                String day = matcher.group(1);
                String month = matcher.group(2);
                String dayOfWeekShort = getShortDayOfWeek(dayOfWeek);
                return new String[]{day, "Tháng " + month + "\n" + dayOfWeekShort};
            }

            return new String[]{dateStr, (monthDisplay != null && !monthDisplay.isEmpty()) ? monthDisplay : dayOfWeek};
        }

        private static String getShortDayOfWeek(String dayOfWeek) {
            if (dayOfWeek == null) return "";
            String trimmed = dayOfWeek.trim();
            switch (trimmed) {
                case "Thứ 2": return "T.2";
                case "Thứ 3": return "T.3";
                case "Thứ 4": return "T.4";
                case "Thứ 5": return "T.5";
                case "Thứ 6": return "T.6";
                case "Thứ 7": return "T.7";
                case "Chủ Nhật":
                case "Chủ nhật":
                case "CN": return "CN";
                default: return trimmed;
            }
        }
    }
}
