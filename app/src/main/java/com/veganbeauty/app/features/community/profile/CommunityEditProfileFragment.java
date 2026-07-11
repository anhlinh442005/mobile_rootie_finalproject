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
import androidx.core.content.ContextCompat;
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
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.ProfileUpdateNotifier;
import com.veganbeauty.app.utils.SyncDataHelper;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class CommunityEditProfileFragment extends Fragment {

    private static final String LOCAL_COVER_FILENAME = "user_cover.jpg";

    private ComFragmentEditProfileBinding binding;
    private Uri selectedAvatarUri = null;
    private Uri selectedCoverUri = null;
    private boolean pickingCover = false;
    private boolean isSaving = false;

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || binding == null) {
                    pickingCover = false;
                    return;
                }
                Uri resultUri = UCrop.getOutput(result.getData());
                if (resultUri == null) {
                    pickingCover = false;
                    return;
                }
                if (pickingCover) {
                    selectedCoverUri = resultUri;
                    bindCoverImage(resultUri.toString());
                } else {
                    selectedAvatarUri = resultUri;
                    com.bumptech.glide.Glide.with(binding.ivAvatar.getContext())
                            .load(resultUri)
                            .circleCrop()
                            .into(binding.ivAvatar);
                }
                pickingCover = false;
            }
    );

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    pickingCover = false;
                    return;
                }
                Uri uri = result.getData().getData();
                if (uri == null) {
                    pickingCover = false;
                    return;
                }

                String prefix = pickingCover ? "cropped_cover_" : "cropped_avatar_";
                Uri destUri = Uri.fromFile(new File(requireContext().getCacheDir(),
                        prefix + System.currentTimeMillis() + ".jpg"));

                UCrop.Options options = new UCrop.Options();
                options.setShowCropGrid(true);
                options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.primary));
                options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.primary));
                options.setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.secondary));

                UCrop uCrop;
                if (pickingCover) {
                    options.setCircleDimmedLayer(false);
                    options.setToolbarTitle("Chỉnh sửa ảnh bìa");
                    uCrop = UCrop.of(uri, destUri)
                            .withAspectRatio(16f, 9f)
                            .withMaxResultSize(1600, 900)
                            .withOptions(options);
                } else {
                    options.setCircleDimmedLayer(true);
                    options.setShowCropGrid(false);
                    options.setToolbarTitle("Chỉnh sửa ảnh đại diện");
                    uCrop = UCrop.of(uri, destUri)
                            .withAspectRatio(1f, 1f)
                            .withMaxResultSize(800, 800)
                            .withOptions(options);
                }
                try {
                    Intent uCropIntent = uCrop.getIntent(requireContext());
                    uCropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    cropImageLauncher.launch(uCropIntent);
                } catch (Exception e) {
                    pickingCover = false;
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Không mở được trình cắt ảnh", Toast.LENGTH_SHORT).show();
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

        bindCoverImage(resolveCoverUrl(ctx));

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
            pickingCover = false;
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        };

        binding.ivAvatar.setOnClickListener(pickAvatarAction);
        binding.ivCameraIcon.setOnClickListener(pickAvatarAction);
        binding.tvChangeAvatarHint.setOnClickListener(pickAvatarAction);

        View.OnClickListener pickCoverAction = v -> {
            pickingCover = true;
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        };
        binding.btnChangeCover.setOnClickListener(pickCoverAction);
        binding.ivCover.setOnClickListener(pickCoverAction);
    }

    private void loadProfileFields(Context ctx) {
        if (!isAdded() || binding == null) {
            return;
        }

        bindAvatarImage(ctx, ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx));
        if (selectedCoverUri == null) {
            bindCoverImage(resolveCoverUrl(ctx));
        }

        new Thread(() -> {
            UserEntity savedUser = ProfileSessionHelper.findCurrentUser(ctx);
            if (savedUser == null || getActivity() == null) {
                return;
            }
            String resolvedAvatar = ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx, savedUser);
            String coverFromUser = savedUser.getPrimary_image();
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
                if (selectedCoverUri == null
                        && coverFromUser != null
                        && !coverFromUser.trim().isEmpty()) {
                    bindCoverImage(coverFromUser.trim());
                }
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
        ProfileSession.markLocalProfileEdited(ctx);

        String avatarToSave = selectedAvatarUri != null
                ? selectedAvatarUri.toString()
                : resolveRemoteAvatarForSave(ctx);
        String coverToSave = selectedCoverUri != null
                ? persistCoverLocally(ctx, selectedCoverUri)
                : resolveCoverUrl(ctx);

        if (coverToSave != null && !coverToSave.trim().isEmpty()) {
            ProfileSession.setPrimaryImage(ctx, coverToSave.trim());
        }

        finishSave(ctx, userId, newName, newUname, newBio, avatarToSave, coverToSave);
    }

    private void finishSave(Context ctx, String userId, String fullName, String username, String bio,
                            String avatar, String cover) {
        persistProfileChanges(ctx, userId, fullName, username, bio, avatar, cover);
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

    @NonNull
    private String resolveCoverUrl(Context ctx) {
        String localCover = ProfileSessionHelper.getLocalCoverFileUri(ctx);
        if (localCover != null) {
            return localCover;
        }
        String cover = ProfileSession.getPrimaryImage(ctx);
        return cover != null ? cover.trim() : "";
    }

    private void bindCoverImage(@Nullable String coverUrl) {
        if (binding == null || binding.ivCover == null) {
            return;
        }
        if (coverUrl == null || coverUrl.trim().isEmpty()) {
            binding.ivCover.setImageResource(R.color.primary);
            return;
        }
        com.bumptech.glide.Glide.with(binding.ivCover.getContext())
                .load(coverUrl)
                .placeholder(R.color.primary)
                .error(R.color.primary)
                .centerCrop()
                .into(binding.ivCover);
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

    @Nullable
    private String persistCoverLocally(Context ctx, @NonNull Uri sourceUri) {
        try {
            File outFile = new File(ctx.getFilesDir(), LOCAL_COVER_FILENAME);
            try (InputStream in = ctx.getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(outFile)) {
                if (in == null) {
                    return sourceUri.toString();
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            if (outFile.exists() && outFile.length() > 0) {
                return "file://" + outFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sourceUri.toString();
    }

    private void persistProfileChanges(Context ctx, String userId, String fullName, String username,
                                       String bio, String avatar, String cover) {
        final String avatarToPersist = avatar;
        final String coverToPersist = cover;
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
                            coverToPersist
                    );
                    user.setBio(bio);
                } else {
                    user.setFull_name(fullName);
                    user.setUsername(username.replace("@", "").trim());
                    if (avatarToPersist != null && !avatarToPersist.trim().isEmpty()) {
                        user.setAvatar(avatarToPersist);
                    }
                    user.setBio(bio);
                    if (coverToPersist != null && !coverToPersist.trim().isEmpty()) {
                        user.setPrimary_image(coverToPersist.trim());
                    }
                }

                ProfileSessionHelper.syncSessionFromUser(ctx, user);
                if (coverToPersist != null && !coverToPersist.trim().isEmpty()) {
                    ProfileSession.setPrimaryImage(ctx, coverToPersist.trim());
                }
                RootieDatabase.getDatabase(ctx).userDao().insertUserSync(user);
                RootieDatabase.getDatabase(ctx).communityDao().insertUsers(
                        java.util.Collections.singletonList(user)
                );
                ProfileUpdateNotifier.notifyUpdated();
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
