package com.veganbeauty.app.features.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.features.shop.product.CartHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kotlinx.coroutines.CoroutineScope;

public class RootieChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static class RootieChatItem {
        public enum Sender { USER, AI }
        public enum ItemType { TEXT, DIAGNOSTIC }

        public static class DiagnosticData {
            public final String assessment;
            public final String detailExplanation;
            public final String moistureVal;
            public final String sensitivityVal;
            public final String barrierVal;
            public final String whyExplanation;
            public final List<String> recommendedProductIds;
            public final List<String> productPhases;
            public final List<String> productSubcategories;
            public final List<String> productExpertReasons;

            public DiagnosticData(String assessment, String detailExplanation, String moistureVal, String sensitivityVal, String barrierVal, String whyExplanation, List<String> recommendedProductIds, List<String> productPhases, List<String> productSubcategories, List<String> productExpertReasons) {
                this.assessment = assessment;
                this.detailExplanation = detailExplanation;
                this.moistureVal = moistureVal;
                this.sensitivityVal = sensitivityVal;
                this.barrierVal = barrierVal;
                this.whyExplanation = whyExplanation;
                this.recommendedProductIds = recommendedProductIds;
                this.productPhases = productPhases;
                this.productSubcategories = productSubcategories;
                this.productExpertReasons = productExpertReasons;
            }
        }

        private final String id;
        private final Sender sender;
        private final String messageText;
        private final String timeStr;
        private final ItemType type;
        private final DiagnosticData diagnosticData;

        public RootieChatItem(String id, Sender sender, String messageText, String timeStr, ItemType type, DiagnosticData diagnosticData) {
            this.id = id != null ? id : UUID.randomUUID().toString();
            this.sender = sender;
            this.messageText = messageText != null ? messageText : "";
            this.timeStr = timeStr != null ? timeStr : "";
            this.type = type != null ? type : ItemType.TEXT;
            this.diagnosticData = diagnosticData;
        }

        public Sender getSender() { return sender; }
        public String getMessageText() { return messageText; }
        public String getTimeStr() { return timeStr; }
        public ItemType getType() { return type; }
        public DiagnosticData getDiagnosticData() { return diagnosticData; }
    }

    private final Context context;
    private final CoroutineScope coroutineScope;
    private final List<RootieChatItem> items = new ArrayList<>();
    private List<ProductEntity> allProducts;

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI_TEXT = 1;
    private static final int TYPE_AI_DIAGNOSTIC = 2;

    public RootieChatAdapter(Context context, CoroutineScope coroutineScope) {
        this.context = context;
        this.coroutineScope = coroutineScope;
    }

    private List<ProductEntity> getAllProducts() {
        if (allProducts == null) {
            allProducts = new LocalJsonReader(context).getAllProducts();
        }
        return allProducts;
    }

    public List<RootieChatItem> getItems() {
        return items;
    }

    public void submitList(List<RootieChatItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addMessage(RootieChatItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeMessageAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        RootieChatItem item = items.get(position);
        if (item.getSender() == RootieChatItem.Sender.USER) {
            return TYPE_USER;
        } else {
            return item.getType() == RootieChatItem.ItemType.DIAGNOSTIC ? TYPE_AI_DIAGNOSTIC : TYPE_AI_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == TYPE_AI_TEXT) {
            View view = inflater.inflate(R.layout.item_chat_ai_text, parent, false);
            return new AiTextViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_ai_diagnostic, parent, false);
            return new AiDiagnosticViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RootieChatItem item = items.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(item);
        } else if (holder instanceof AiTextViewHolder) {
            ((AiTextViewHolder) holder).bind(item);
        } else if (holder instanceof AiDiagnosticViewHolder) {
            ((AiDiagnosticViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;

        public UserViewHolder(View view) {
            super(view);
            tvMessage = view.findViewById(R.id.tvUserMessage);
            tvTime = view.findViewById(R.id.tvUserTime);
        }

        public void bind(RootieChatItem item) {
            tvMessage.setText(item.getMessageText());
            tvTime.setText(item.getTimeStr());
        }
    }

    class AiTextViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;
        private final ImageView ivAvatar;

        public AiTextViewHolder(View view) {
            super(view);
            tvMessage = view.findViewById(R.id.tvAiMessage);
            tvTime = view.findViewById(R.id.tvAiTime);
            ivAvatar = view.findViewById(R.id.ivAiAvatar);
        }

        public void bind(RootieChatItem item) {
            tvMessage.setText(item.getMessageText());
            tvTime.setText(item.getTimeStr());
            ivAvatar.setImageResource(R.drawable.mascot_message);
        }
    }

    class AiDiagnosticViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAssessment;
        private final TextView tvDetail;
        private final TextView tvMoistureVal;
        private final TextView tvSensitivityVal;
        private final TextView tvBarrierVal;
        private final TextView tvWhyContent;
        private final LinearLayout layoutProducts;
        private final View btnAddToCartAll;
        private final ImageView ivAvatar;

        public AiDiagnosticViewHolder(View view) {
            super(view);
            tvAssessment = view.findViewById(R.id.tvDiagnosticAssessment);
            tvDetail = view.findViewById(R.id.tvDiagnosticDetail);
            tvMoistureVal = view.findViewById(R.id.tvMetricMoistureVal);
            tvSensitivityVal = view.findViewById(R.id.tvMetricSensitivityVal);
            tvBarrierVal = view.findViewById(R.id.tvMetricBarrierVal);
            tvWhyContent = view.findViewById(R.id.tvWhyContent);
            layoutProducts = view.findViewById(R.id.layoutDiagnosticProducts);
            btnAddToCartAll = view.findViewById(R.id.btnAddToCartAll);
            ivAvatar = view.findViewById(R.id.ivAiAvatar);
        }

        public void bind(RootieChatItem item) {
            ivAvatar.setImageResource(R.drawable.mascot_message);
            RootieChatItem.DiagnosticData data = item.getDiagnosticData();
            if (data == null) return;

            tvAssessment.setText(data.assessment);
            tvDetail.setText(data.detailExplanation);
            tvMoistureVal.setText(data.moistureVal);
            tvSensitivityVal.setText(data.sensitivityVal);
            tvBarrierVal.setText(data.barrierVal);
            tvWhyContent.setText(data.whyExplanation);

            layoutProducts.removeAllViews();

            List<ProductEntity> productsToRecommend = new ArrayList<>();

            for (int idx = 0; idx < data.recommendedProductIds.size(); idx++) {
                String prodId = data.recommendedProductIds.get(idx);
                ProductEntity product = null;
                for (ProductEntity p : getAllProducts()) {
                    if (p.getId().equals(prodId)) {
                        product = p;
                        break;
                    }
                }

                if (product != null) {
                    productsToRecommend.add(product);

                    View pView = LayoutInflater.from(context).inflate(R.layout.item_chat_diagnostic_product, layoutProducts, false);
                    ImageView ivProdImage = pView.findViewById(R.id.ivProductImage);
                    TextView tvPhase = pView.findViewById(R.id.tvProductPhase);
                    TextView tvPrice = pView.findViewById(R.id.tvProductPrice);
                    TextView tvSubcat = pView.findViewById(R.id.tvProductSubcategory);
                    TextView tvName = pView.findViewById(R.id.tvProductName);
                    TextView tvReason = pView.findViewById(R.id.tvExpertReason);

                    ImageRequest request = new ImageRequest.Builder(context)
                            .data(product.getMainImage())
                            .crossfade(true)
                            .placeholder(android.R.color.darker_gray)
                            .target(ivProdImage)
                            .build();
                    Coil.imageLoader(context).enqueue(request);

                    tvPhase.setText(idx < data.productPhases.size() ? data.productPhases.get(idx) : "GIAI ĐOẠN " + (idx + 1));

                    String formattedPrice;
                    if (product.getPrice() >= 1000) {
                        formattedPrice = (product.getPrice() / 1000) + "K";
                    } else {
                        formattedPrice = product.getPrice() + "đ";
                    }
                    tvPrice.setText(formattedPrice);

                    tvSubcat.setText(idx < data.productSubcategories.size() ? data.productSubcategories.get(idx) : product.getCategory().toUpperCase());
                    tvName.setText(product.getName());
                    tvReason.setText(idx < data.productExpertReasons.size() ? data.productExpertReasons.get(idx) : "Lý do khuyên dùng bởi chuyên gia Rootie.");

                    layoutProducts.addView(pView);
                }
            }

            btnAddToCartAll.setOnClickListener(v -> {
                if (!productsToRecommend.isEmpty()) {
                    for (ProductEntity prod : productsToRecommend) {
                        CartHelper.addToCart(context, coroutineScope, prod, 1);
                    }
                }
            });
        }
    }
}
