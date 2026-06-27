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

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

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

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        selectedAvatarUri = resultUri;
                        ImageRequest request = new ImageRequest.Builder(requireContext())
                                .data(resultUri)
                                .crossfade(true)
                                .transformations(new CircleCropTransformation())
                                .target(binding.ivAvatar)
                                .build();
                        Coil.imageLoader(requireContext()).enqueue(request);
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
        String ownUserId = "test_001";

        binding.etDisplayName.setText(ProfileSession.getFullName(ctx));
        String uname = ProfileSession.getUsername(ctx);
        binding.etUsername.setText(uname.startsWith("@") ? uname : "@" + uname);
        binding.etBio.setText(ProfileSession.getBio(ctx));

        String avatarUrl = ProfileSession.getAvatar(ctx);
        if (!avatarUrl.isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(ctx)
                    .data(avatarUrl)
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .placeholder(android.R.color.darker_gray)
                    .error(R.drawable.img_avatar)
                    .target(binding.ivAvatar)
                    .build();
            Coil.imageLoader(ctx).enqueue(request);
        } else {
            binding.ivAvatar.setImageResource(R.drawable.img_avatar);
        }

        String coverUrl = ProfileSession.getPrimaryImage(ctx);
        if (!coverUrl.isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(ctx)
                    .data(coverUrl)
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .target(binding.ivCover)
                    .build();
            Coil.imageLoader(ctx).enqueue(request);
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
                if (ownUserId.equals(obj.optString("user_id"))) {
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
                        if (ownUserId.equals(post.getAuthorId())) {
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

        View.OnClickListener saveAction = v -> {
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
                ProfileSession.setAvatar(ctx, selectedAvatarUri.toString());
            }

            Toast.makeText(ctx, "Lưu thông tin thành công", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        };

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
