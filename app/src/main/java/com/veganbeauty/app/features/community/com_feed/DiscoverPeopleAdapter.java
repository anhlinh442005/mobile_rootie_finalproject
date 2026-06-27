package com.veganbeauty.app.features.community.com_feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.decode.SvgDecoder;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.databinding.ComItemDiscoverPersonBinding;

import java.util.List;

public class DiscoverPeopleAdapter extends RecyclerView.Adapter<DiscoverPeopleAdapter.PersonViewHolder> {

    public enum ActionType {
        SUGGEST, REQUEST, FOLLOW_BACK
    }

    public interface OnActionClickListener {
        void onActionClick(UserEntity user, String action);
    }

    private List<UserEntity> users;
    private final ActionType actionType;
    private final OnActionClickListener onActionClick;

    public DiscoverPeopleAdapter(List<UserEntity> users, ActionType actionType, OnActionClickListener onActionClick) {
        this.users = users;
        this.actionType = actionType;
        this.onActionClick = onActionClick;
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        public final ComItemDiscoverPersonBinding binding;

        public PersonViewHolder(ComItemDiscoverPersonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ComItemDiscoverPersonBinding binding = ComItemDiscoverPersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PersonViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        UserEntity user = users.get(position);
        holder.binding.tvUsername.setText(user.getUsername());

        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(user.getAvatar())
                    .decoderFactory(new SvgDecoder.Factory())
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .error(R.drawable.img_avatar)
                    .target(holder.binding.ivAvatar)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray);
        }

        if (user.getMutualCount() > 0) {
            String friendName = user.getFirstMutualFriendName() != null ? user.getFirstMutualFriendName() : "Ai đó";
            if (user.getMutualCount() == 1) {
                holder.binding.tvSubtitle.setText("Có " + friendName + " đang theo dõi");
            } else {
                holder.binding.tvSubtitle.setText("Có " + friendName + " và " + (user.getMutualCount() - 1) + " người khác đang theo dõi");
            }
            holder.binding.tvSubtitle.setVisibility(View.VISIBLE);

            List<String> avatars = user.getMutualFriendAvatars();
            if (avatars != null && !avatars.isEmpty()) {
                holder.binding.flMutualAvatars.setVisibility(View.VISIBLE);

                if (avatars.size() >= 1) {
                    holder.binding.cvMutual1.setVisibility(View.VISIBLE);
                    loadImage(holder.itemView.getContext(), avatars.get(0), holder.binding.ivMutual1);
                } else holder.binding.cvMutual1.setVisibility(View.GONE);

                if (avatars.size() >= 2) {
                    holder.binding.cvMutual2.setVisibility(View.VISIBLE);
                    loadImage(holder.itemView.getContext(), avatars.get(1), holder.binding.ivMutual2);
                } else holder.binding.cvMutual2.setVisibility(View.GONE);

                if (avatars.size() >= 3) {
                    holder.binding.cvMutual3.setVisibility(View.VISIBLE);
                    loadImage(holder.itemView.getContext(), avatars.get(2), holder.binding.ivMutual3);
                } else holder.binding.cvMutual3.setVisibility(View.GONE);

            } else {
                holder.binding.flMutualAvatars.setVisibility(View.GONE);
            }
        } else {
            holder.binding.tvSubtitle.setVisibility(View.GONE);
            holder.binding.flMutualAvatars.setVisibility(View.GONE);
        }

        switch (actionType) {
            case SUGGEST:
                holder.binding.btnPrimary.setText("Theo dõi");
                holder.binding.btnSecondary.setVisibility(View.GONE);
                holder.binding.ivClose.setVisibility(View.VISIBLE);
                break;
            case REQUEST:
                holder.binding.btnPrimary.setText("Xác nhận");
                holder.binding.btnSecondary.setVisibility(View.VISIBLE);
                holder.binding.ivClose.setVisibility(View.GONE);
                break;
            case FOLLOW_BACK:
                holder.binding.btnPrimary.setText("Theo dõi lại");
                holder.binding.btnSecondary.setVisibility(View.GONE);
                holder.binding.ivClose.setVisibility(View.VISIBLE);
                break;
        }

        holder.binding.btnPrimary.setOnClickListener(v -> {
            String btnText = holder.binding.btnPrimary.getText().toString();
            if (btnText.equals("Theo dõi") || btnText.equals("Theo dõi lại")) {
                if (onActionClick != null) onActionClick.onActionClick(user, "FOLLOW");
                holder.binding.btnPrimary.setText("Đang theo dõi");
                holder.binding.btnPrimary.setBackgroundResource(R.drawable.com_bg_filter_normal);
                holder.binding.btnPrimary.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary, null));
            } else if (btnText.equals("Xác nhận")) {
                if (onActionClick != null) onActionClick.onActionClick(user, "ACCEPT_REQUEST");
                holder.binding.btnPrimary.setText("Đã xác nhận");
                holder.binding.btnSecondary.setVisibility(View.GONE);
            }
        });
    }

    private void loadImage(android.content.Context context, String url, android.widget.ImageView target) {
        ImageRequest request = new ImageRequest.Builder(context)
                .data(url)
                .decoderFactory(new SvgDecoder.Factory())
                .transformations(new CircleCropTransformation())
                .crossfade(true)
                .error(R.drawable.img_avatar)
                .target(target)
                .build();
        Coil.imageLoader(context).enqueue(request);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateData(List<UserEntity> newUsers) {
        users = newUsers;
        notifyDataSetChanged();
    }
}
