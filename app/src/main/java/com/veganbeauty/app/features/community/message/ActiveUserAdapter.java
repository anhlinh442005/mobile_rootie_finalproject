package com.veganbeauty.app.features.community.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.util.List;
import java.util.Map;

public class ActiveUserAdapter extends RecyclerView.Adapter<ActiveUserAdapter.ActiveUserViewHolder> {

    private List<ConversationEntity> items;

    public ActiveUserAdapter(List<ConversationEntity> items) {
        this.items = items;
    }

    public static class ActiveUserViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivAvatar;
        public final ImageView ivAddStory;
        public final View vActiveDot;
        public final TextView tvName;

        public ActiveUserViewHolder(View view) {
            super(view);
            ivAvatar = view.findViewById(R.id.ivAvatar);
            ivAddStory = view.findViewById(R.id.ivAddStory);
            vActiveDot = view.findViewById(R.id.vActiveDot);
            tvName = view.findViewById(R.id.tvName);
        }
    }

    @NonNull
    @Override
    public ActiveUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_active_user, parent, false);
        return new ActiveUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveUserViewHolder holder, int position) {
        if (position == 0) {
            holder.tvName.setText("Tin của bạn");
            holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.gray_dark, null));
            holder.ivAddStory.setVisibility(View.VISIBLE);
            holder.vActiveDot.setVisibility(View.GONE);

            String currentUserAvatar = ProfileSession.getAvatar(holder.itemView.getContext());
            if (currentUserAvatar != null && !currentUserAvatar.isEmpty()) {
                ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                        .data(currentUserAvatar)
                        .crossfade(true)
                        .placeholder(R.color.gray_light)
                        .error(R.drawable.mascot_message)
                        .transformations(new CircleCropTransformation())
                        .target(holder.ivAvatar)
                        .build();
                Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
            } else {
                ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                        .data(R.drawable.mascot_message)
                        .transformations(new CircleCropTransformation())
                        .target(holder.ivAvatar)
                        .build();
                Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
            }
            return;
        }

        ConversationEntity item = items.get(position - 1);

        String currentUserId = ProfileSession.getCurrentUserId(holder.itemView.getContext());
        String partnerId = "";
        if (item.getMembers() != null) {
            for (String member : item.getMembers()) {
                if (!member.equals(currentUserId)) {
                    partnerId = member;
                    break;
                }
            }
        }

        ConversationEntity.MemberInfo partnerInfo = null;
        if (item.getMemberInfo() != null) {
            partnerInfo = item.getMemberInfo().get(partnerId);
        }

        String partnerName = partnerInfo != null ? partnerInfo.getName() : "Unknown";
        String partnerAvatar = partnerInfo != null ? partnerInfo.getAvatar() : "";

        if ("You".equals(partnerName) || "Unknown".equals(partnerName) || partnerAvatar.isEmpty()) {
            List<UserEntity> users = new LocalJsonReader(holder.itemView.getContext()).getUsers();
            UserEntity user = null;
            if (users != null) {
                for (UserEntity u : users) {
                    if (u.getUser_id() != null && u.getUser_id().equals(partnerId)) {
                        user = u;
                        break;
                    }
                }
            }
            if (user != null) {
                if ("You".equals(partnerName) || "Unknown".equals(partnerName)) {
                    partnerName = user.getFull_name() != null ? user.getFull_name() : partnerName;
                }
                if (partnerAvatar.isEmpty() && user.getAvatar() != null) {
                    partnerAvatar = user.getAvatar();
                }
            } else if ("test_001".equals(partnerId)) {
                if ("You".equals(partnerName) || "Unknown".equals(partnerName)) {
                    partnerName = "Test User";
                }
            }
        }

        boolean isActive = false;
        if (item.getActiveBy() != null) {
            isActive = item.getActiveBy().contains(partnerId);
        }

        String shortName = partnerName.toLowerCase()
                .replace(" ", "_")
                .replaceAll("[áàảãạăắằẳẵặâấầẩẫậ]", "a")
                .replace("đ", "d")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y");

        holder.tvName.setText(shortName);
        holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.gray_dark, null));
        holder.ivAddStory.setVisibility(View.GONE);
        holder.vActiveDot.setVisibility(isActive ? View.VISIBLE : View.GONE);

        if (!partnerAvatar.isEmpty()) {
            int errorRes = "rootie_vn".equals(partnerId) ? R.drawable.ic_logo_rootie : R.drawable.mascot_message;
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(partnerAvatar)
                    .crossfade(true)
                    .placeholder(R.color.gray_light)
                    .error(errorRes)
                    .transformations(new CircleCropTransformation())
                    .target(holder.ivAvatar)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            if ("rootie_vn".equals(partnerId)) {
                holder.ivAvatar.setImageResource(R.drawable.ic_logo_rootie);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.mascot_message);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    public void updateData(List<ConversationEntity> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }
}
