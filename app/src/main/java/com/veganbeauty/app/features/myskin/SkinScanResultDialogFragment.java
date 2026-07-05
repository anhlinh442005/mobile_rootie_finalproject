package com.veganbeauty.app.features.myskin;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.SkinDialogScanResultBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SkinScanResultDialogFragment extends DialogFragment {

    private SkinDialogScanResultBinding _binding;
    private String currentImageUrl = "";

    private static final String ARG_DATA = "arg_data";

    public static SkinScanResultDialogFragment newInstance(String dataString) {
        SkinScanResultDialogFragment fragment = new SkinScanResultDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATA, dataString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinDialogScanResultBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String dataString = getArguments() != null ? getArguments().getString(ARG_DATA) : null;
        if (dataString != null) {
            try {
                JSONObject data = new JSONObject(dataString);
                bindData(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        _binding.dialogSkinBtnClose.setOnClickListener(v -> dismiss());
        _binding.dialogSkinImage.setOnClickListener(v -> showImageZoomDialog());
    }

    private void bindData(JSONObject data) throws Exception {
        int score = data.getInt("score");
        _binding.dialogSkinScoreVal.setText(String.valueOf(score));
        _binding.dialogSkinScoreLabel.setText(data.getString("overallCondition"));
        _binding.dialogSkinSummaryText.setText(data.getString("summaryText"));
        _binding.dialogSkinDate.setText(data.getString("date"));
        _binding.dialogSkinTime.setText(data.getString("time"));
        _binding.dialogSkinType.setText(data.optString("scanType", "Quét AI"));

        currentImageUrl = data.optString("imageUrl", "");
        if (!currentImageUrl.isEmpty()) {
            try {
                if (currentImageUrl.startsWith("/")) {
                    _binding.dialogSkinImage.setImageURI(android.net.Uri.fromFile(new java.io.File(currentImageUrl)));
                } else {
                    _binding.dialogSkinImage.setImageURI(android.net.Uri.parse(currentImageUrl));
                }
            } catch (Exception e) {
                _binding.dialogSkinImage.setImageResource(R.drawable.about_us_pd);
            }
        } else {
            _binding.dialogSkinImage.setImageResource(R.drawable.about_us_pd);
        }

        LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) _binding.dialogSkinProgressFill.getLayoutParams();
        fillParams.weight = score / 100f;
        _binding.dialogSkinProgressFill.setLayoutParams(fillParams);

        LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) _binding.dialogSkinProgressEmpty.getLayoutParams();
        emptyParams.weight = 1f - (score / 100f);
        _binding.dialogSkinProgressEmpty.setLayoutParams(emptyParams);

        JSONObject detailedEval = data.getJSONObject("detailedEvaluation");
        populateScoreGrid(detailedEval);
        setupRadarChart(detailedEval);
        populateMetrics(detailedEval);

        JSONArray suggestions = data.getJSONArray("suggestions");
        populateSuggestions(suggestions);
    }

    private void populateScoreGrid(JSONObject eval) throws Exception {
        LinearLayout container = _binding.dialogSkinScoreGrid;
        container.removeAllViews();

        String[][] gridDef = {
                {"sensitivity", "Mụn viêm"},
                {"oil", "Mụn không viêm"},
                {"oil", "Sợi bã nhờn"},
                {"pigmentation", "Sẹo"},
                {"pigmentation", "Sắc tố da"},
                {"pores", "Lỗ chân lông"}
        };

        List<Integer> scores = new ArrayList<>();
        for (String[] def : gridDef) {
            int raw = eval.getJSONObject(def[0]).getInt("score");
            scores.add(Math.max(1, Math.round(raw / 10f)));
        }

        LayoutInflater inflater = getLayoutInflater();

        for (int row = 0; row < 3; row++) {
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int col = 0; col < 2; col++) {
                int idx = row * 2 + col;
                View card = inflater.inflate(R.layout.item_skin_score_grid, rowLayout, false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(col == 0 ? 0 : 4, 0, col == 1 ? 0 : 4, 8);
                card.setLayoutParams(lp);

                TextView label = card.findViewById(R.id.grid_label);
                TextView scoreTv = card.findViewById(R.id.grid_score);
                View dot = card.findViewById(R.id.grid_dot);

                label.setText(gridDef[idx][1]);
                scoreTv.setText(scores.get(idx) + "/10");

                int scoreVal = scores.get(idx);
                String dotColor;
                if (scoreVal >= 7) dotColor = "#27AE60";
                else if (scoreVal >= 5) dotColor = "#F1C40F";
                else dotColor = "#E74C3C";

                android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
                dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotBg.setColor(Color.parseColor(dotColor));
                dot.setBackground(dotBg);

                rowLayout.addView(card);
            }
            container.addView(rowLayout);
        }
    }

    private void setupRadarChart(JSONObject eval) throws Exception {
        List<String> labels = new ArrayList<>();
        labels.add("Độ ẩm");
        labels.add("Dầu");
        labels.add("Lỗ chân lông");
        labels.add("Sắc tố");
        labels.add("Nhạy cảm");

        float moisture = eval.getJSONObject("moisture").getInt("score");
        float oil = eval.getJSONObject("oil").getInt("score");
        float pores = eval.getJSONObject("pores").getInt("score");
        float pigmentation = eval.getJSONObject("pigmentation").getInt("score");
        float sensitivity = eval.getJSONObject("sensitivity").getInt("score");

        List<Float> scores = new ArrayList<>();
        scores.add(moisture);
        scores.add(oil);
        scores.add(pores);
        scores.add(pigmentation);
        scores.add(sensitivity);

        ArrayList<RadarEntry> entries = new ArrayList<>();
        for (Float score : scores) {
            entries.add(new RadarEntry(score));
        }

        RadarDataSet dataSet = new RadarDataSet(entries, "Skin Metrics");
        dataSet.setColor(Color.parseColor("#677559"));
        dataSet.setFillColor(Color.parseColor("#E6EBE6"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawHighlightCircleEnabled(true);
        dataSet.setDrawHighlightIndicators(false);

        RadarData radarData = new RadarData(dataSet);
        radarData.setDrawValues(false);
        radarData.setValueTextColor(Color.parseColor("#333333"));

        _binding.dialogSkinRadarChart.setData(radarData);
        _binding.dialogSkinRadarChart.getDescription().setEnabled(false);
        _binding.dialogSkinRadarChart.setWebLineWidth(1f);
        _binding.dialogSkinRadarChart.setWebColor(Color.parseColor("#E6EBE6"));
        _binding.dialogSkinRadarChart.setWebLineWidthInner(1f);
        _binding.dialogSkinRadarChart.setWebColorInner(Color.parseColor("#E6EBE6"));
        _binding.dialogSkinRadarChart.setWebAlpha(255);

        XAxis xAxis = _binding.dialogSkinRadarChart.getXAxis();
        xAxis.setTextSize(10f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.parseColor("#4A5D3E"));

        YAxis yAxis = _binding.dialogSkinRadarChart.getYAxis();
        yAxis.setLabelCount(5, false);
        yAxis.setTextSize(11f);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setDrawLabels(false);

        _binding.dialogSkinRadarChart.setRotationEnabled(false);
        _binding.dialogSkinRadarChart.getLegend().setEnabled(false);
        _binding.dialogSkinRadarChart.animateXY(1000, 1000);
        _binding.dialogSkinRadarChart.invalidate();

        setupChartPinchZoom(_binding.dialogSkinRadarChart);
    }

    private void setupChartPinchZoom(View chartView) {
        final float[] scaleFactor = {1.0f};
        final float minScale = 0.8f;
        final float maxScale = 2.5f;

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(requireContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scaleFactor[0] *= detector.getScaleFactor();
                        scaleFactor[0] = Math.max(minScale, Math.min(scaleFactor[0], maxScale));
                        chartView.setScaleX(scaleFactor[0]);
                        chartView.setScaleY(scaleFactor[0]);
                        return true;
                    }
                });

        chartView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return true;
        });
    }

    private void populateMetrics(JSONObject eval) throws Exception {
        LinearLayout container = _binding.dialogSkinMetricsContainer;
        container.removeAllViews();

        String[] keys = {"moisture", "oil", "pores", "pigmentation", "sensitivity"};
        String[] titles = {"Độ ẩm", "Lượng dầu", "Lỗ chân lông", "Sắc tố", "Độ nhạy cảm"};
        String[] colors = {"#1D82CD", "#3CA754", "#D88B2A", "#8D62A6", "#E35B5B"};

        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < keys.length; i++) {
            JSONObject obj = eval.getJSONObject(keys[i]);
            View view = inflater.inflate(R.layout.item_skin_metric, container, false);

            View dot = view.findViewById(R.id.metric_dot);
            TextView title = view.findViewById(R.id.metric_title);
            TextView badge = view.findViewById(R.id.metric_badge);
            TextView desc = view.findViewById(R.id.metric_desc);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(colors[i]));
            dot.setBackground(gd);

            title.setText(titles[i]);
            badge.setText(obj.getString("level"));
            desc.setText(obj.getString("description"));

            container.addView(view);
        }
    }

    private void populateSuggestions(JSONArray sugg) throws Exception {
        LinearLayout container = _binding.dialogSkinSuggestionsContainer;
        container.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int itemGap = (int) (10 * density);

        for (int i = 0; i < sugg.length(); i++) {
            String text = sugg.getString(i);
            TextView tv = new TextView(requireContext());
            tv.setText("• " + text);
            tv.setTextSize(16f);
            tv.setLineSpacing(4f * density, 1f);
            tv.setTextColor(requireContext().getColor(R.color.content));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, itemGap);
            tv.setLayoutParams(params);
            try {
                tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro));
            } catch (Exception e) {
                e.printStackTrace();
            }
            container.addView(tv);
        }
    }

    private void showImageZoomDialog() {
        Dialog zoomDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        zoomDialog.setContentView(R.layout.skin_dialog_image_zoom);

        ImageView zoomImageView = zoomDialog.findViewById(R.id.zoom_image_view);
        View btnClose = zoomDialog.findViewById(R.id.zoom_btn_close);

        if (!currentImageUrl.isEmpty()) {
            try {
                if (currentImageUrl.startsWith("/")) {
                    zoomImageView.setImageURI(android.net.Uri.fromFile(new java.io.File(currentImageUrl)));
                } else {
                    zoomImageView.setImageURI(android.net.Uri.parse(currentImageUrl));
                }
            } catch (Exception e) {
                zoomImageView.setImageResource(R.drawable.about_us_pd);
            }
        } else {
            Drawable currentDrawable = _binding.dialogSkinImage.getDrawable();
            if (currentDrawable != null) {
                zoomImageView.setImageDrawable(currentDrawable);
            } else {
                zoomImageView.setImageResource(R.drawable.about_us_pd);
            }
        }

        setupZoomableImageView(zoomImageView);
        btnClose.setOnClickListener(v -> zoomDialog.dismiss());

        zoomDialog.show();
    }

    private void setupZoomableImageView(ImageView imageView) {
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        final Matrix matrix = new Matrix();
        final Matrix savedMatrix = new Matrix();
        final PointF startPoint = new PointF();
        final PointF midPoint = new PointF();
        final float[] matrixValues = new float[9];

        final int NONE = 0;
        final int DRAG = 1;
        final int ZOOM = 2;
        final int[] mode = {NONE};
        final float[] oldDist = {1f};
        final float minScale = 1.0f;
        final float maxScale = 5.0f;
        final boolean[] initialized = {false};

        imageView.post(() -> {
            Drawable drawable = imageView.getDrawable();
            if (drawable == null) return;

            int dWidth = drawable.getIntrinsicWidth();
            int dHeight = drawable.getIntrinsicHeight();
            int vWidth = imageView.getWidth();
            int vHeight = imageView.getHeight();

            float scale = Math.min((float) vWidth / dWidth, (float) vHeight / dHeight);
            float dx = (vWidth - dWidth * scale) / 2f;
            float dy = (vHeight - dHeight * scale) / 2f;

            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            imageView.setImageMatrix(matrix);
            initialized[0] = true;
        });

        imageView.setOnTouchListener((v, event) -> {
            if (!initialized[0]) return true;

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startPoint.set(event.getX(), event.getY());
                    mode[0] = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist[0] = spacing(event);
                    if (oldDist[0] > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(midPoint, event);
                        mode[0] = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode[0] == DRAG) {
                        matrix.set(savedMatrix);
                        float dx = event.getX() - startPoint.x;
                        float dy = event.getY() - startPoint.y;
                        matrix.postTranslate(dx, dy);
                    } else if (mode[0] == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist[0];

                            matrix.getValues(matrixValues);
                            float currentScale = matrixValues[Matrix.MSCALE_X];
                            float targetScale = currentScale * scale;

                            if (targetScale < minScale) {
                                scale = minScale / currentScale;
                            } else if (targetScale > maxScale) {
                                scale = maxScale / currentScale;
                            }

                            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode[0] = NONE;
                    v.performClick();
                    break;
            }

            imageView.setImageMatrix(matrix);
            return true;
        });
    }

    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        if (event.getPointerCount() < 2) return;
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
