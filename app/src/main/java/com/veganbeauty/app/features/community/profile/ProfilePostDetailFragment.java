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
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.features.community.UserMemoryHelper;
import com.veganbeauty.app.features.community.com_feed.CommunityPostsDiff;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory;
import com.veganbeauty.app.features.community.com_feed.PostAdapter;
import com.veganbeauty.app.utils.TimeFormatter;

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
    private List<CommunityPostEntity> lastObservedDbPosts = null;

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
        List<CommunityProduct> productsList = jsonReader.getProducts();

        viewModel.getPosts().observe(getViewLifecycleOwner(), dbPosts -> {
            List<CommunityPostEntity> safeDbPosts = dbPosts != null ? dbPosts : Collections.emptyList();
            if (!CommunityPostsDiff.hasStructuralChange(lastObservedDbPosts, safeDbPosts)) {
                lastObservedDbPosts = new ArrayList<>(safeDbPosts);
                return;
            }
            lastObservedDbPosts = new ArrayList<>(safeDbPosts);

            executor.execute(() -> {
                try {
                    List<CommunityPostEntity> newsList = jsonReader.getCommunityNews();
                    List<CommunityPostEntity> allPostsCombined =
                            UserMemoryHelper.mergePostSources(dbPosts, newsList);

                    String ownUserId = com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(requireContext());
                    if (ownUserId == null || ownUserId.isEmpty()) {
                        ownUserId = ProfileSession.getUserId(requireContext());
                    }
                    if (ownUserId == null || ownUserId.isEmpty()) ownUserId = "test_001";

                    // For saved/reposted tabs, keep the profile owner id (whose list we are viewing).
                    // Only remap to post author when browsing that author's posts (tab 0).
                    String profileUserId = userId;
                    if (currentTab == 0 && targetPostId != null && !targetPostId.isEmpty()) {
                        for (CommunityPostEntity p : allPostsCombined) {
                            if (targetPostId.equals(p.getPostId())) {
                                profileUserId = p.getAuthorId();
                                break;
                            }
                        }
                    }

                    // Also try snapshots when target is a Rootie news post not yet in combined list
                    if (currentTab == 3 && targetPostId != null && !targetPostId.isEmpty()) {
                        boolean found = false;
                        for (CommunityPostEntity p : allPostsCombined) {
                            if (targetPostId.equals(p.getPostId())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            for (CommunityPostEntity snap : UserMemoryHelper.getSavedPosts(requireContext(), userId)) {
                                if (targetPostId.equals(snap.getPostId())) {
                                    allPostsCombined.add(snap);
                                    break;
                                }
                            }
                        }
                    }

                    List<CommunityPostEntity> myPosts = new ArrayList<>();
                    Set<String> addedPostIds = new HashSet<>();

                    if (currentTab == 3) {
                        myPosts.addAll(UserMemoryHelper.resolveSavedPosts(
                                requireContext(), profileUserId, allPostsCombined));
                        for (CommunityPostEntity post : myPosts) {
                            addedPostIds.add(post.getPostId());
                        }
                    } else if (currentTab == 2) {
                        myPosts.addAll(UserMemoryHelper.resolveRepostedPosts(
                                requireContext(), profileUserId, allPostsCombined));
                        for (CommunityPostEntity post : myPosts) {
                            addedPostIds.add(post.getPostId());
                        }
                    } else {
                        for (CommunityPostEntity post : allPostsCombined) {
                            boolean shouldInclude = currentTab == 0
                                    && profileUserId.equals(post.getAuthorId());

                            if (shouldInclude && !addedPostIds.contains(post.getPostId())) {
                                myPosts.add(post);
                                addedPostIds.add(post.getPostId());
                            }
                        }
                    }

                    Collections.sort(myPosts, (p1, p2) -> TimeFormatter.compareCreatedAtDesc(p1.getCreatedAt(), p2.getCreatedAt()));

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
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                adapter.updateData(myPosts, new ArrayList<>(), new ArrayList<>(), productsList);
                                layoutManager.scrollToPositionWithOffset(finalScrollPos, 0);
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
