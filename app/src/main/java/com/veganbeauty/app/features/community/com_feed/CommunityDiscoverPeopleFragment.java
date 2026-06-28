package com.veganbeauty.app.features.community.com_feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.dao.CommunityDao;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.UserMemoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentDiscoverPeopleBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityDiscoverPeopleFragment extends RootieFragment {

    private ComFragmentDiscoverPeopleBinding binding;
    private CommunityViewModel viewModel;
    private CommunityDao communityDao;
    private final FirestoreService firestoreService = new FirestoreService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private void handleUserAction(UserEntity user, String action) {
        String content = "FOLLOW".equals(action) ? "Bạn đã bắt đầu theo dõi " + user.getUsername() : "Bạn đã chấp nhận yêu cầu theo dõi của " + user.getUsername();
        UserMemoryEntity memory = new UserMemoryEntity(
                java.util.UUID.randomUUID().toString(),
                action,
                user.getUser_id(),
                user.getUsername(),
                user.getAvatar() != null ? user.getAvatar() : "",
                content,
                System.currentTimeMillis()
        );
        executor.execute(() -> {
            try {
                communityDao.insertUserMemory(memory);
                firestoreService.uploadUserMemory(memory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private final DiscoverPeopleAdapter suggestAdapter = new DiscoverPeopleAdapter(new ArrayList<>(), DiscoverPeopleAdapter.ActionType.SUGGEST, this::handleUserAction);
    private final DiscoverPeopleAdapter requestAdapter = new DiscoverPeopleAdapter(new ArrayList<>(), DiscoverPeopleAdapter.ActionType.REQUEST, this::handleUserAction);
    private final DiscoverPeopleAdapter followBackAdapter = new DiscoverPeopleAdapter(new ArrayList<>(), DiscoverPeopleAdapter.ActionType.FOLLOW_BACK, this::handleUserAction);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentDiscoverPeopleBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        communityDao = db.communityDao();
        CommunityRepository repository = new CommunityRepository(communityDao, new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(CommunityViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.rvSuggestions.setAdapter(suggestAdapter);
        binding.rvRequests.setAdapter(requestAdapter);
        binding.rvFollowBacks.setAdapter(followBackAdapter);
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null && !users.isEmpty()) {
                executor.execute(() -> {
                    try {
                        LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
                        Map<String, List<String>> mySocialData = jsonReader.getSocialDataForUser("test_001");
                        
                        Set<String> myFriends = new HashSet<>();
                        if (mySocialData.containsKey("friends")) myFriends.addAll(mySocialData.get("friends"));
                        
                        List<String> suggestIds = mySocialData.containsKey("suggested") ? mySocialData.get("suggested") : new ArrayList<>();
                        List<String> requestIds = mySocialData.containsKey("friend_requests") ? mySocialData.get("friend_requests") : new ArrayList<>();
                        
                        Set<String> followers = new HashSet<>();
                        if (mySocialData.containsKey("followers")) followers.addAll(mySocialData.get("followers"));
                        
                        Set<String> following = new HashSet<>();
                        if (mySocialData.containsKey("following")) following.addAll(mySocialData.get("following"));
                        
                        Set<String> followBackIds = new HashSet<>(followers);
                        followBackIds.removeAll(following);
                        followBackIds.removeAll(myFriends);
                        
                        List<UserEntity> suggestions = new ArrayList<>();
                        List<UserEntity> requests = new ArrayList<>();
                        List<UserEntity> followBacks = new ArrayList<>();
                        
                        for (UserEntity user : users) {
                            if (!"test_001".equals(user.getUser_id())) {
                                if (suggestIds.contains(user.getUser_id())) suggestions.add(user);
                                if (requestIds.contains(user.getUser_id())) requests.add(user);
                                if (followBackIds.contains(user.getUser_id())) followBacks.add(user);
                            }
                        }
                        
                        List<String> allTargetIds = new ArrayList<>();
                        for (UserEntity u : suggestions) allTargetIds.add(u.getUser_id());
                        for (UserEntity u : requests) allTargetIds.add(u.getUser_id());
                        for (UserEntity u : followBacks) allTargetIds.add(u.getUser_id());
                        
                        Map<String, List<String>> mutualMap = jsonReader.getMutualFriendsForUsers(new ArrayList<>(myFriends), allTargetIds);
                        
                        applyMutualData(suggestions, mutualMap, users);
                        applyMutualData(requests, mutualMap, users);
                        applyMutualData(followBacks, mutualMap, users);
                        
                        requireActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                suggestAdapter.updateData(suggestions);
                                requestAdapter.updateData(requests);
                                followBackAdapter.updateData(followBacks);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
    
    private void applyMutualData(List<UserEntity> list, Map<String, List<String>> mutualMap, List<UserEntity> allUsers) {
        for (UserEntity user : list) {
            List<String> mutualIds = mutualMap.get(user.getUser_id());
            if (mutualIds == null) mutualIds = new ArrayList<>();
            
            user.setMutualCount(mutualIds.size());
            if (!mutualIds.isEmpty()) {
                List<UserEntity> mutualUsers = new ArrayList<>();
                for (UserEntity u : allUsers) {
                    if (mutualIds.contains(u.getUser_id())) {
                        mutualUsers.add(u);
                    }
                }
                
                if (!mutualUsers.isEmpty()) {
                    String name = mutualUsers.get(0).getFull_name();
                    if (name == null || name.trim().isEmpty()) {
                        name = mutualUsers.get(0).getUsername();
                    }
                    user.setFirstMutualFriendName(name);
                    
                    List<String> avatars = new ArrayList<>();
                    for (int i = 0; i < Math.min(3, mutualUsers.size()); i++) {
                        String avatar = mutualUsers.get(i).getAvatar();
                        if (avatar != null) avatars.add(avatar);
                    }
                    user.setMutualFriendAvatars(avatars);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
