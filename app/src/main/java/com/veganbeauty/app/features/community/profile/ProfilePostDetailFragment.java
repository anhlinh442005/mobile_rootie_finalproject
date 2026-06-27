package com.veganbeauty.app.features.community.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.PostEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.features.community.UserMemoryHelper;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory;
import com.veganbeauty.app.features.community.com_feed.PostAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfilePostDetailFragment extends Fragment {

    private RecyclerView rvPosts;
    private String userId = "test_001";
    private int initialPosition = 0;
    private int currentTab = 0;
    private String targetPostId = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static ProfilePostDetailFragment newInstance(String userId, int initialPosition, int currentTab, String targetPostId) {
        ProfilePostDetailFragment fragment = new ProfilePostDetailFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        args.putInt("INITIAL_POSITION", initialPosition);
        args.putInt("CURRENT_TAB", currentTab);
        if (targetPostId != null) {
            args.putString("TARGET_POST_ID", targetPostId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public static ProfilePostDetailFragment newInstance(String userId, int initialPosition) {
        return newInstance(userId, initialPosition, 0, null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID", "test_001");
            initialPosition = getArguments().getInt("INITIAL_POSITION", 0);
            currentTab = getArguments().getInt("CURRENT_TAB", 0);
            targetPostId = getArguments().getString("TARGET_POST_ID");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_profile_post_detail, container, false);
        rvPosts = view.findViewById(R.id.rvPosts);
        
        View ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        CommunityViewModel viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);

        PostAdapter adapter = new PostAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvPosts.setLayoutManager(layoutManager);
        rvPosts.setAdapter(adapter);

        LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
        List<ProductEntity> productsList = jsonReader.getProducts();

        viewModel.getPosts().observe(getViewLifecycleOwner(), dbPosts -> {
            executor.execute(() -> {
                try {
                    List<PostEntity> newsList = jsonReader.getCommunityNews();
                    List<PostEntity> allPostsCombined = new ArrayList<>();
                    if (dbPosts != null) allPostsCombined.addAll(dbPosts);
                    if (newsList != null) allPostsCombined.addAll(newsList);

                    String ownUserId = "test_001";
                    try {
                        String loggedInEmail = ProfileSession.getEmail(requireContext());
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("users.json"), StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            JSONArray usersJsonArray = new JSONArray(sb.toString());
                            for (int i = 0; i < usersJsonArray.length(); i++) {
                                JSONObject obj = usersJsonArray.getJSONObject(i);
                                if (loggedInEmail != null && loggedInEmail.equals(obj.optString("email"))) {
                                    ownUserId = obj.optString("user_id", "test_001");
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {}

                    String finalUserId = userId;
                    if (targetPostId != null && !targetPostId.isEmpty()) {
                        for (PostEntity p : allPostsCombined) {
                            if (targetPostId.equals(p.getPostId())) {
                                finalUserId = p.getAuthorId();
                                break;
                            }
                        }
                    }

                    List<PostEntity> myPosts = new ArrayList<>();
                    Set<String> addedPostIds = new HashSet<>();
                    for (PostEntity post : allPostsCombined) {
                        boolean shouldInclude = false;
                        switch (currentTab) {
                            case 0:
                            case 1:
                                shouldInclude = finalUserId.equals(post.getAuthorId());
                                break;
                            case 2:
                                shouldInclude = UserMemoryHelper.isPostReposted(requireContext(), ownUserId, post.getPostId());
                                break;
                            case 3:
                                shouldInclude = UserMemoryHelper.isPostSaved(requireContext(), ownUserId, post.getPostId());
                                break;
                            default:
                                shouldInclude = finalUserId.equals(post.getAuthorId());
                                break;
                        }

                        if (shouldInclude && !addedPostIds.contains(post.getPostId())) {
                            myPosts.add(post);
                            addedPostIds.add(post.getPostId());
                        }
                    }

                    Collections.sort(myPosts, (p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));

                    int scrollPos = initialPosition;
                    if (targetPostId != null && !targetPostId.isEmpty()) {
                        for (int i = 0; i < myPosts.size(); i++) {
                            if (targetPostId.equals(myPosts.get(i).getPostId())) {
                                scrollPos = i;
                                break;
                            }
                        }
                    }
                    
                    final int finalScrollPos = scrollPos;
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            adapter.updateData(myPosts, new ArrayList<>(), new ArrayList<>(), productsList);
                            layoutManager.scrollToPositionWithOffset(finalScrollPos, 0);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
