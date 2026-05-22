package com.veganbeauty.app.features.home.welcome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.List;

public class HomeWelcomePagerAdapter extends RecyclerView.Adapter<HomeWelcomePagerAdapter.PageViewHolder> {

    public static class WelcomePage {
        public final int imageRes;

        public WelcomePage(int imageRes) {
            this.imageRes = imageRes;
        }
    }

    private final List<WelcomePage> pages;

    public HomeWelcomePagerAdapter(List<WelcomePage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_welcome_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(pages.get(position));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.home_welcome_image);
        }

        void bind(WelcomePage page) {
            image.setImageResource(page.imageRes);
        }
    }
}
