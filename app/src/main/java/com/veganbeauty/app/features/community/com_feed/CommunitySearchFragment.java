package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentSearchBinding;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunitySearchFragment extends RootieFragment {

    private ComFragmentSearchBinding binding;

    private CommunityViewModel viewModel;
    private UserSearchAdapter userAdapter;
    private PostAdapter postAdapter;
    private ConcatAdapter concatAdapter;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentSearchBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.ivClear.setOnClickListener(v -> binding.etSearch.setText(""));

        userAdapter = new UserSearchAdapter(new ArrayList<>(), user -> {
            CommunityProfileFragment fragment = CommunityProfileFragment.newInstance(user.getUser_id());
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        postAdapter = new PostAdapter();

        concatAdapter = new ConcatAdapter(userAdapter, postAdapter);
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchResults.setAdapter(concatAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                binding.ivClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                binding.llSuggestions.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);
                binding.rvSearchResults.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                performSearch(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        for (int i = 0; i < binding.llSuggestionItems.getChildCount(); i++) {
            View child = binding.llSuggestionItems.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setOnClickListener(v -> {
                    String text = textView.getText().toString().replace("• ", "").trim();
                    binding.etSearch.setText(text);
                    binding.etSearch.setSelection(text.length());

                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                });
            }
        }

        binding.llRefresh.setOnClickListener(v -> {
            List<View> children = new ArrayList<>();
            for (int i = 0; i < binding.llSuggestionItems.getChildCount(); i++) {
                children.add(binding.llSuggestionItems.getChildAt(i));
            }
            Collections.shuffle(children);
            binding.llSuggestionItems.removeAllViews();
            for (View child : children) {
                binding.llSuggestionItems.addView(child);
            }
        });

        binding.tvSearchAction.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        binding.etSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            userAdapter.updateData(new ArrayList<>());
            postAdapter.updateData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            binding.tvEmpty.setVisibility(View.GONE);
            return;
        }

        String lowerQuery = query.toLowerCase();

        List<UserEntity> allUsers = viewModel.getUsers().getValue() != null ? viewModel.getUsers().getValue() : new ArrayList<>();
        
        List<CommunityPostEntity> allPosts = viewModel.getPosts().getValue() != null ? viewModel.getPosts().getValue() : new ArrayList<>();
        
        final List<ReelEntity> allReels = viewModel.getReels().getValue() != null ? viewModel.getReels().getValue() : new ArrayList<>();

        List<UserEntity> matchedUsers = new ArrayList<>();
        for (UserEntity user : allUsers) {
            if ((user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerQuery)) ||
                (user.getFull_name() != null && user.getFull_name().toLowerCase().contains(lowerQuery))) {
                matchedUsers.add(user);
            }
        }

        List<CommunityPostEntity> matchedPosts = new ArrayList<>();
        for (CommunityPostEntity post : allPosts) {
            boolean matchesContent = post.getContent() != null && post.getContent().toLowerCase().contains(lowerQuery);
            boolean matchesType = post.getType() != null && post.getType().toLowerCase().contains(lowerQuery);
            boolean matchesSkinType = post.getSkinType() != null && post.getSkinType().toLowerCase().contains(lowerQuery);
            boolean matchesConcern = post.getConcern() != null && post.getConcern().toLowerCase().contains(lowerQuery);
            
            boolean matchesAuthor = false;
            for (UserEntity u : matchedUsers) {
                if (u.getUser_id() != null && u.getUser_id().equals(post.getAuthorId())) {
                    matchesAuthor = true;
                    break;
                }
            }
            
            if (matchesContent || matchesType || matchesSkinType || matchesConcern || matchesAuthor) {
                matchedPosts.add(post);
            }
        }

        executor.execute(() -> {
            Context ctx = getContext();
            if (ctx == null) return;
            
            final List<CommunityProduct> finalProductsList = new LocalJsonReader(ctx).getProducts();
            
            requireActivity().runOnUiThread(() -> {
                userAdapter.updateData(matchedUsers);
                postAdapter.updateData(matchedPosts, new ArrayList<>(), allReels, finalProductsList);

                if (matchedUsers.isEmpty() && matchedPosts.isEmpty()) {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmpty.setVisibility(View.GONE);
                }
            });
        });
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        binding = null;
    }
}
