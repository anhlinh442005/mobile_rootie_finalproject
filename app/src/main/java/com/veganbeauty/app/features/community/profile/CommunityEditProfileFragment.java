package com.veganbeauty.app.features.community.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentEditProfileBinding;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.utils.CloudinaryConfig;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.SyncDataHelper;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class CommunityEditProfileFragment extends Fragment {

    private ComFragmentEditProfileBinding binding;
    private Uri selectedAvatarUri = null;
    private boolean isSaving = false;

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        selectedAvatarUri = resultUri;
                        com.bumptech.glide.Glide.with(binding.ivAvatar.getContext()).load(resultUri).circleCrop().into(binding.ivAvatar);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        Uri destUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));
                        UCrop.Options options = new UCrop.Options();
                        options.setCircleDimmedLayer(true);
                        options.setShowCropGrid(false);
                        options.setToolbarTitle("Chỉnh sửa ảnh");

                        Intent uCropIntent = UCrop.of(uri, destUri)
                                .withAspectRatio(1f, 1f)
                                .withMaxResultSize(800, 800)
                                .withOptions(options)
                                .getIntent(requireContext());

                        cropImageLauncher.launch(uCropIntent);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context ctx = requireContext();
        String ownUserId = ProfileSessionHelper.getEffectiveUserId(ctx);
        if (ownUserId == null || ownUserId.isEmpty()) {
            ownUserId = ProfileSession.getCurrentUserId(ctx);
        }
        final String finalOwnUserId = ownUserId;

        binding.etDisplayName.setText(ProfileSession.getFullName(ctx));
        String uname = ProfileSession.getUsername(ctx);
        binding.etUsername.setText(uname.startsWith("@") ? uname : "@" + uname);
        binding.etBio.setText(ProfileSession.getBio(ctx));

        SyncDataHelper.syncUserProfileFromFirestore(ctx, () -> loadProfileFields(ctx));

        String coverUrl = ProfileSession.getPrimaryImage(ctx);
        if (!coverUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(binding.ivCover.getContext()).load(coverUrl).placeholder(android.R.color.darker_gray).into(binding.ivCover);
        }

        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.getAssets().open("User_com_friend.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONArray friendJsonArray = new JSONArray(sb.toString());
            int followersCount = 0;
            int followingCount = 0;
            for (int i = 0; i < friendJsonArray.length(); i++) {
                JSONObject obj = friendJsonArray.getJSONObject(i);
                if (obj.optString("user_id").equals(finalOwnUserId)) {
                    followersCount = obj.optJSONArray("followers") != null ? obj.optJSONArray("followers").length() : 0;
                    followingCount = obj.optJSONArray("following") != null ? obj.optJSONArray("following").length() : 0;
                    break;
                }
            }
            binding.tvFollowersCount.setText(String.valueOf(followersCount));
            binding.tvFollowingCount.setText(String.valueOf(followingCount));
        } catch (Exception e) {
            e.printStackTrace();
            binding.tvFollowersCount.setText("0");
            binding.tvFollowingCount.setText("0");
        }

        try {
            RootieDatabase db = RootieDatabase.getDatabase(requireContext());
            CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(requireContext()), new FirestoreService());
            CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
            CommunityViewModel viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);
            viewModel.getPosts().observe(getViewLifecycleOwner(), allPosts -> {
                if (allPosts != null) {
                    int myPostCount = 0;
                    for (CommunityPostEntity post : allPosts) {
                        if (finalOwnUserId.equals(post.getAuthorId())) {
                            myPostCount++;
                        }
                    }
                    binding.tvPostCount.setText(String.valueOf(myPostCount));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        View.OnClickListener saveAction = v -> performSave(ctx, finalOwnUserId);

        binding.tvSaveTop.setOnClickListener(saveAction);
        binding.btnSaveBottom.setOnClickListener(saveAction);

        View.OnClickListener pickAvatarAction = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        };

        binding.ivAvatar.setOnClickListener(pickAvatarAction);
        binding.ivCameraIcon.setOnClickListener(pickAvatarAction);
        binding.tvChangeAvatarHint.setOnClickListener(pickAvatarAction);

        binding.btnChangeCover.setOnClickListener(v -> {
            Toast.makeText(ctx, "Tính năng đổi ảnh bìa đang được cập nhật", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadProfileFields(Context ctx) {
        if (!isAdded() || binding == null) {
            return;
        }

        bindAvatarImage(ctx, ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx));

        new Thread(() -> {
            UserEntity savedUser = ProfileSessionHelper.findCurrentUser(ctx);
            if (savedUser == null || getActivity() == null) {
                return;
            }
            String resolvedAvatar = ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx, savedUser);
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }
                if (savedUser.getFull_name() != null && !savedUser.getFull_name().trim().isEmpty()) {
                    binding.etDisplayName.setText(savedUser.getFull_name().trim());
                }
                if (savedUser.getUsername() != null && !savedUser.getUsername().trim().isEmpty()) {
                    String savedUname = savedUser.getUsername().trim();
                    binding.etUsername.setText(savedUname.startsWith("@") ? savedUname : "@" + savedUname);
                }
                if (savedUser.getBio() != null && !savedUser.getBio().trim().isEmpty()) {
                    binding.etBio.setText(savedUser.getBio().trim());
                }
                bindAvatarImage(ctx, resolvedAvatar);
            });
        }).start();
    }

    private void performSave(Context ctx, String userId) {
        if (isSaving) {
            return;
        }

        String newName = binding.etDisplayName.getText().toString().trim();
        String newUname = binding.etUsername.getText().toString().trim();
        String newBio = binding.etBio.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(ctx, "Tên hiển thị không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newUname.isEmpty()) {
            Toast.makeText(ctx, "Tên người dùng không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newUname.startsWith("@")) {
            newUname = "@" + newUname;
        }

        ProfileSession.setFullName(ctx, newName);
        ProfileSession.setUsername(ctx, newUname);
        ProfileSession.setBio(ctx, newBio);

        if (selectedAvatarUri != null) {
            if (!CloudinaryConfig.isConfigured()) {
                Toast.makeText(ctx, "Chưa cấu hình Cloudinary. Sửa CloudinaryConfig.java.", Toast.LENGTH_LONG).show();
                return;
            }

            isSaving = true;
            setSaveEnabled(false);
            Toast.makeText(ctx, "Đang tải ảnh đại diện lên Cloudinary...", Toast.LENGTH_LONG).show();

            final String finalUname = newUname;
            SyncDataHelper.uploadAndSyncAvatar(ctx, selectedAvatarUri, (success, secureUrl, errorMessage) -> {
                isSaving = false;
                if (isAdded()) {
                    setSaveEnabled(true);
                }

                Context toastCtx = ctx.getApplicationContext();
                if (success && secureUrl != null) {
                    persistProfileChanges(ctx, userId, newName, finalUname, newBio, secureUrl);
                    Toast.makeText(toastCtx, "Cập nhật avatar thành công!", Toast.LENGTH_LONG).show();
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        Toast.makeText(toastCtx, errorMessage, Toast.LENGTH_LONG).show();
                    }
                    if (isAdded()) {
                        getParentFragmentManager().popBackStack();
                    }
                } else {
                    String message = errorMessage != null ? errorMessage : "Không thể cập nhật avatar. Vui lòng thử lại.";
                    Toast.makeText(toastCtx, message, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        String avatarToSave = resolveRemoteAvatarForSave(ctx);
        finishSave(ctx, userId, newName, newUname, newBio, avatarToSave);
    }

    private void finishSave(Context ctx, String userId, String fullName, String username, String bio, String avatar) {
        persistProfileChanges(ctx, userId, fullName, username, bio, avatar);
        Toast.makeText(ctx, "Lưu thông tin thành công", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    private String resolveRemoteAvatarForSave(Context ctx) {
        String avatar = ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx);
        if (SyncDataHelper.isRemoteAvatarUrl(avatar)) {
            return avatar.trim();
        }
        UserEntity user = ProfileSessionHelper.findCurrentUser(ctx);
        if (user != null && SyncDataHelper.isRemoteAvatarUrl(user.getAvatar())) {
            return user.getAvatar().trim();
        }
        return "";
    }

    private void setSaveEnabled(boolean enabled) {
        if (binding == null) {
            return;
        }
        binding.tvSaveTop.setEnabled(enabled);
        binding.btnSaveBottom.setEnabled(enabled);
    }

    private void bindAvatarImage(Context ctx, String avatarUrl) {
        if (binding == null || binding.ivAvatar == null) {
            return;
        }
        com.bumptech.glide.Glide.with(binding.ivAvatar.getContext())
                .load(avatarUrl)
                .placeholder(R.drawable.img_avatar)
                .error(R.drawable.img_avatar)
                .circleCrop()
                .into(binding.ivAvatar);
    }

    private void persistProfileChanges(Context ctx, String userId, String fullName, String username, String bio, String avatar) {
        if (!SyncDataHelper.isRemoteAvatarUrl(avatar)) {
            avatar = resolveRemoteAvatarForSave(ctx);
        }

        final String avatarToPersist = avatar;
        new Thread(() -> {
            try {
                UserEntity user = ProfileSessionHelper.findCurrentUser(ctx);
                if (user == null) {
                    user = new UserEntity(
                            userId,
                            username.replace("@", "").trim(),
                            fullName,
                            ProfileSession.getEmail(ctx) != null ? ProfileSession.getEmail(ctx) : "",
                            ProfileSession.getPhone(ctx) != null ? ProfileSession.getPhone(ctx) : "",
                            "",
                            avatarToPersist,
                            ProfileSession.getPrimaryImage(ctx)
                    );
                    user.setBio(bio);
                } else {
                    user.setFull_name(fullName);
                    user.setUsername(username.replace("@", "").trim());
                    if (SyncDataHelper.isRemoteAvatarUrl(avatarToPersist)) {
                        user.setAvatar(avatarToPersist);
                    }
                    user.setBio(bio);
                    String primaryImage = ProfileSession.getPrimaryImage(ctx);
                    if (primaryImage != null && !primaryImage.trim().isEmpty()) {
                        user.setPrimary_image(primaryImage);
                    }
                }

                ProfileSessionHelper.syncSessionFromUser(ctx, user);
                RootieDatabase.getDatabase(ctx).userDao().insertUserSync(user);
                new FirestoreService().saveUser(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
