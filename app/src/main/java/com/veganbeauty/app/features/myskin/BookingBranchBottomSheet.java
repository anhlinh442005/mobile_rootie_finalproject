package com.veganbeauty.app.features.myskin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.StoreEntity;

import java.util.ArrayList;
import java.util.List;

public class BookingBranchBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BookingBranchBottomSheet";

    public interface OnBranchSelectedListener {
        void onBranchSelected(StoreEntity store);
    }

    private OnBranchSelectedListener listener;
    private List<StoreEntity> branchList = new ArrayList<>();

    public void setBranchList(List<StoreEntity> branches) {
        this.branchList = branches != null ? branches : new ArrayList<>();
    }

    public void setOnBranchSelectedListener(OnBranchSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_bottom_sheet_booking_branch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_close_branch_sheet).setOnClickListener(v -> dismiss());

        RecyclerView rvBranchList = view.findViewById(R.id.rv_branch_list);
        rvBranchList.setLayoutManager(new LinearLayoutManager(requireContext()));
        BranchAdapter adapter = new BranchAdapter(branchList, store -> {
            if (listener != null) {
                listener.onBranchSelected(store);
            }
            dismiss();
        });
        rvBranchList.setAdapter(adapter);
    }
}
