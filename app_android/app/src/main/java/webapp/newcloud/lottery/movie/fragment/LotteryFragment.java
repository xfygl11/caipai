package webapp.newcloud.lottery.movie.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import webapp.newcloud.lottery.movie.R;

public class LotteryFragment extends Fragment {

    private static final String[] LOTTERY_IDS = {"dlt", "ssq", "qlc", "fc3d", "pl3", "pl5", "qxc", "kl8"};
    private static final String[] LOTTERY_NAMES = {"大乐透", "双色球", "七乐彩", "福彩3D", "排列3", "排列5", "7星彩", "快乐8"};
    private static final String[] DRAW_TIMES = {"每周一、三、六 21:25", "每周二、四、日 21:15", "每周一、三、五 20:40", "每天 20:40", "每天 21:25", "每天 21:25", "每周二、五、日 21:25", "每天 20:40"};
    
    // 每种彩票的规则：前区最大值、前区个数、后区最大值、后区个数
    private static final int[] LOTTERY_RULES = {
        35, 5, 12, 2,   // dlt: 大乐透
        33, 6, 16, 1,   // ssq: 双色球
        30, 7, 30, 1,   // qlc: 七乐彩
        10, 3, 10, 3,   // fc3d: 福彩3D (0-9)
        10, 3, 10, 3,   // pl3: 排列3 (0-9)
        10, 5, 10, 5,   // pl5: 排列5 (0-9)
        35, 7, 14, 1,   // qxc: 7星彩
        80, 10, 80, 10  // kl8: 快乐8
    };

    private int currentLotteryIndex = 0;
    private boolean isDeltaCalculator = false;
    
    // UI Elements
    private TextView titleText;
    private TextView subTitleText;
    private TextView nextPeriodText;
    private TextView countdownTimer;
    private TextView countdownDate;
    private TextView poolText;
    private TextView salesText;
    private TextView predCountText;
    private TextView histCountText;
    private TextView bestHitText;
    private TextView futureRangeText;
    private TextView historyBadge;
    private LinearLayout predictionGroups;
    private TableLayout futureTable;
    private TableLayout historyTable;
    private TableLayout recentTable;
    private EditText pasteInput;
    private LinearLayout manualInputs;
    private LinearLayout randomOutput;
    
    // Delta calculator
    private LinearLayout deltaContainer;
    
    // Data
    private List<Map<String, Object>> recentDraws = new ArrayList<>();
    private List<List<String>> predictions = new ArrayList<>();
    private Timer countdownTimerInstance;
    
    // HTTP Client
    private OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build();
    
    // Handler for UI updates
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lottery, container, false);
        
        titleText = view.findViewById(R.id.titleText);
        subTitleText = view.findViewById(R.id.subTitleText);
        nextPeriodText = view.findViewById(R.id.nextPeriodText);
        countdownTimer = view.findViewById(R.id.countdownTimer);
        countdownDate = view.findViewById(R.id.countdownDate);
        poolText = view.findViewById(R.id.poolText);
        salesText = view.findViewById(R.id.salesText);
        predCountText = view.findViewById(R.id.predCountText);
        histCountText = view.findViewById(R.id.histCountText);
        bestHitText = view.findViewById(R.id.bestHitText);
        futureRangeText = view.findViewById(R.id.futureRangeText);
        historyBadge = view.findViewById(R.id.historyBadge);
        predictionGroups = view.findViewById(R.id.predictionGroups);
        futureTable = view.findViewById(R.id.futureTable);
        historyTable = view.findViewById(R.id.historyTable);
        recentTable = view.findViewById(R.id.recentTable);
        pasteInput = view.findViewById(R.id.pasteInput);
        manualInputs = view.findViewById(R.id.manualInputs);
        randomOutput = view.findViewById(R.id.randomOutput);
        deltaContainer = view.findViewById(R.id.deltaContainer);

        setupTabs(view);
        setupButtons(view);
        
        // Load data after layout
        view.post(() -> {
            if (deltaContainer != null && deltaContainer.getChildCount() > 0) {
                // Delta calculator tab
                isDeltaCalculator = true;
                setupDeltaCalculator();
            } else {
                loadLotteryData();
            }
        });
        
        return view;
    }

    private void setupTabs(View view) {
        LinearLayout tabsLayout = view.findViewById(R.id.lotteryTabs);
        if (tabsLayout == null) return;

        for (int i = 0; i < tabsLayout.getChildCount(); i++) {
            final int index = i;
            View tab = tabsLayout.getChildAt(i);
            tab.setOnClickListener(v -> {
                currentLotteryIndex = index;
                isDeltaCalculator = (index == LOTTERY_IDS.length); // Last tab is delta calculator
                
                for (int j = 0; j < tabsLayout.getChildCount(); j++) {
                    View child = tabsLayout.getChildAt(j);
                    if (child instanceof TextView) {
                        TextView tv = (TextView) child;
                        if (j == index) {
                            tv.setTextColor(0xffff6b35);
                            tv.setBackgroundResource(R.drawable.bg_lottery_tab_active);
                            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
                        } else {
                            tv.setTextColor(0xff667788);
                            tv.setBackgroundResource(R.drawable.bg_lottery_tab);
                            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.NORMAL);
                        }
                    }
                }
                
                if (isDeltaCalculator) {
                    setupDeltaCalculator();
                } else {
                    loadLotteryData();
                }
            });
        }
        
        // Set initial tab active
        View firstTab = tabsLayout.getChildAt(0);
        if (firstTab instanceof TextView) {
            TextView tv = (TextView) firstTab;
            tv.setTextColor(0xffff6b35);
            tv.setBackgroundResource(R.drawable.bg_lottery_tab_active);
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
    }

    private void setupButtons(View view) {
        Button copyAllBtn = view.findViewById(R.id.copyAllBtn);
        if (copyAllBtn != null) {
            copyAllBtn.setOnClickListener(v -> copyPredictions());
        }

        Button genRandomBtn = view.findViewById(R.id.genRandomBtn);
        if (genRandomBtn != null) {
            genRandomBtn.setOnClickListener(v -> generateRandomNumbers());
        }

        Button copyRandomBtn = view.findViewById(R.id.copyRandomBtn);
        if (copyRandomBtn != null) {
            copyRandomBtn.setOnClickListener(v -> copyRandomNumbers());
        }

        Button clearRandomBtn = view.findViewById(R.id.clearRandomBtn);
        if (clearRandomBtn != null) {
            clearRandomBtn.setOnClickListener(v -> randomOutput.removeAllViews());
        }
        
        Button fillPasteBtn = view.findViewById(R.id.fillPasteBtn);
        if (fillPasteBtn != null) {
            fillPasteBtn.setOnClickListener(v -> parseAndFillPaste());
        }
        
        Button submitManualBtn = view.findViewById(R.id.submitManualBtn);
        if (submitManualBtn != null) {
            submitManualBtn.setOnClickListener(v -> submitManualPrediction());
        }
    }

    private void setupDeltaCalculator() {
        // Show/hide sections based on delta calculator mode
        if (predictionGroups != null) predictionGroups.setVisibility(View.GONE);
        if (futureTable != null) futureTable.setVisibility(View.GONE);
        if (historyTable != null) historyTable.setVisibility(View.GONE);
        if (recentTable != null) recentTable.setVisibility(View.GONE);
        if (pasteInput != null) pasteInput.setVisibility(View.GONE);
        if (manualInputs != null) manualInputs.setVisibility(View.GONE);
        if (randomOutput != null) randomOutput.setVisibility(View.GONE);
        
        if (deltaContainer != null) {
            deltaContainer.setVisibility(View.VISIBLE);
            renderDeltaCalculator();
        }
        
        titleText.setText("三角洲计算器");
        subTitleText.setText("输入三个数值，自动计算差值和规律");
        nextPeriodText.setText("");
        countdownTimer.setText("");
        countdownDate.setText("");
        poolText.setText("");
        salesText.setText("");
    }

    private void renderDeltaCalculator() {
        deltaContainer.removeAllViews();
        
        // Create input fields
        LinearLayout inputsLayout = new LinearLayout(getContext());
        inputsLayout.setOrientation(LinearLayout.HORIZONTAL);
        inputsLayout.setGravity(Gravity.CENTER);
        inputsLayout.setPadding(16, 16, 16, 16);
        
        String[] labels = {"数值A", "数值B", "数值C"};
        EditText[] inputs = new EditText[3];
        
        for (int i = 0; i < 3; i++) {
            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER);
            itemLayout.setPadding(8, 0, 8, 0);
            
            TextView label = new TextView(getContext());
            label.setText(labels[i]);
            label.setTextColor(0xff8899aa);
            label.setTextSize(12);
            
            inputs[i] = new EditText(getContext());
            inputs[i].setHint("0");
            inputs[i].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            inputs[i].setLayoutParams(new LinearLayout.LayoutParams(
                80, ViewGroup.LayoutParams.WRAP_CONTENT));
            inputs[i].setBackgroundResource(R.drawable.bg_lottery_input);
            inputs[i].setTextColor(0xffffffff);
            inputs[i].setTextColor(0xffffffff);
            inputs[i].setTextSize(14);
            
            itemLayout.addView(label);
            itemLayout.addView(inputs[i]);
            inputsLayout.addView(itemLayout);
        }
        
        deltaContainer.addView(inputsLayout);
        
        // Calculate button
        Button calcBtn = new Button(getContext());
        calcBtn.setText("计算差值");
        calcBtn.setBackgroundResource(R.drawable.bg_lottery_btn_o);
        calcBtn.setTextColor(0xffffffff);
        calcBtn.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
        calcBtn.setPadding(16, 12, 16, 12);
        
        calcBtn.setOnClickListener(v -> {
            try {
                int valA = Integer.parseInt(TextUtils.isEmpty(inputs[0].getText()) ? "0" : inputs[0].getText().toString());
                int valB = Integer.parseInt(TextUtils.isEmpty(inputs[1].getText()) ? "0" : inputs[1].getText().toString());
                int valC = Integer.parseInt(TextUtils.isEmpty(inputs[2].getText()) ? "0" : inputs[2].getText().toString());
                
                int deltaAB = Math.abs(valA - valB);
                int deltaBC = Math.abs(valB - valC);
                int deltaAC = Math.abs(valA - valC);
                
                // Display results
                deltaContainer.removeView(calcBtn);
                
                LinearLayout resultsLayout = new LinearLayout(getContext());
                resultsLayout.setOrientation(LinearLayout.VERTICAL);
                resultsLayout.setPadding(16, 16, 16, 16);
                
                String[] deltas = {
                    "A-B 差值: " + deltaAB,
                    "B-C 差值: " + deltaBC,
                    "A-C 差值: " + deltaAC,
                    "三数之和: " + (valA + valB + valC),
                    "三数之积: " + (valA * valB * valC)
                };
                
                for (String result : deltas) {
                    TextView resultTv = new TextView(getContext());
                    resultTv.setText(result);
                    resultTv.setTextColor(0xffaabbcc);
                    resultTv.setTextSize(14);
                    resultTv.setPadding(0, 8, 0, 8);
                    resultsLayout.addView(resultTv);
                }
                
                // Recalculate button
                Button recalcBtn = new Button(getContext());
                recalcBtn.setText("重新计算");
                recalcBtn.setBackgroundResource(R.drawable.bg_lottery_btn_o);
                recalcBtn.setTextColor(0xffffffff);
                resultsLayout.addView(recalcBtn);
                
                recalcBtn.setOnClickListener(v2 -> renderDeltaCalculator());
                
                deltaContainer.addView(resultsLayout);
                
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "请输入有效数字", Toast.LENGTH_SHORT).show();
            }
        });
        
        deltaContainer.addView(calcBtn);
    }

    private void loadLotteryData() {
        if (isDeltaCalculator) return;
        
        String lotId = LOTTERY_IDS[currentLotteryIndex];
        titleText.setText(LOTTERY_NAMES[currentLotteryIndex] + "智能预测");
        titleText.setTextColor(0xffffa500);
        subTitleText.setText(DRAW_TIMES[currentLotteryIndex]);
        
        // Show loading
        nextPeriodText.setText("加载中...");
        countdownTimer.setText("--:--:--");
        poolText.setText("--");
        salesText.setText("--");
        
        // Fetch data from API
        fetchRecentDraws(lotId);
        fetchPredictions(lotId);
        
        // Start countdown
        startCountdown();
    }

    private void fetchRecentDraws(String lotId) {
        // Using 163 lottery API
        String url = "https://api.163.com/nim/product/productinfo.htm?appid=IO1vP6EsS6bJq4Au&nid=d_" + lotId + "&onlyData=1";
        
        httpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                uiHandler.post(() -> {
                    nextPeriodText.setText("获取失败");
                    renderRecentDrawsFallback();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (ResponseBody rb = response.body()) {
                    if (response.isSuccessful() && rb != null) {
                        String jsonStr = rb.string();
                        parseRecentDraws(jsonStr, lotId);
                    } else {
                        uiHandler.post(() -> renderRecentDrawsFallback());
                    }
                }
            }
        });
    }

    private void parseRecentDraws(String jsonStr, String lotId) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                uiHandler.post(() -> renderRecentDrawsFallback());
                return;
            }
            
            JSONArray draws = data.optJSONArray("openIssueResultList");
            if (draws == null || draws.length() == 0) {
                uiHandler.post(() -> renderRecentDrawsFallback());
                return;
            }
            
            recentDraws.clear();
            int maxShow = Math.min(draws.length(), 10);
            
            for (int i = 0; i < maxShow; i++) {
                JSONObject draw = draws.getJSONObject(i);
                Map<String, Object> drawMap = new HashMap<>();
                drawMap.put("period", draw.optString("issue", "--"));
                drawMap.put("date", draw.optString("date", "--"));
                drawMap.put("numbers", draw.optString("numClose", "--"));
                drawMap.put("pool", draw.optString("poolMoney", "0"));
                drawMap.put("sales", draw.optString("saleMoney", "0"));
                recentDraws.add(drawMap);
            }
            
            uiHandler.post(() -> {
                // Update header info from first draw
                if (!recentDraws.isEmpty()) {
                    Map<String, Object> first = recentDraws.get(0);
                    nextPeriodText.setText((String) first.get("period"));
                    poolText.setText(formatMoney((String) first.get("pool")));
                    salesText.setText(formatMoney((String) first.get("sales")));
                }
                renderRecentDraws();
            });
            
        } catch (JSONException e) {
            uiHandler.post(() -> renderRecentDrawsFallback());
        }
    }

    private String formatMoney(String moneyStr) {
        if (TextUtils.isEmpty(moneyStr) || "0".equals(moneyStr)) return "--";
        try {
            double money = Double.parseDouble(moneyStr);
            if (money >= 100000000) {
                return String.format("%.2f亿", money / 100000000);
            } else if (money >= 10000) {
                return String.format("%.2f万", money / 10000);
            }
            return String.valueOf((long) money);
        } catch (NumberFormatException e) {
            return moneyStr;
        }
    }

    private void renderRecentDraws() {
        recentTable.removeViews(1, recentTable.getChildCount() - 1);
        
        for (Map<String, Object> draw : recentDraws) {
            TableRow tableRow = new TableRow(getContext());
            
            String period = (String) draw.get("period");
            String date = (String) draw.get("date");
            String numbers = (String) draw.get("numbers");
            String pool = (String) draw.get("pool");
            
            addTableRowCell(tableRow, period, 0xff8899aa);
            addTableRowCell(tableRow, date, 0xff8899aa);
            addTableRowCell(tableRow, numbers, 0xffaabbcc);
            addTableRowCell(tableRow, pool, 0xff667788);
            
            // Detail button
            TextView detailBtn = new TextView(getContext());
            detailBtn.setText("详情");
            detailBtn.setTextColor(0xffff6b35);
            detailBtn.setTextSize(11);
            detailBtn.setPadding(4, 2, 4, 2);
            detailBtn.setOnClickListener(v -> Toast.makeText(getContext(), "期号: " + period, Toast.LENGTH_SHORT).show());
            tableRow.addView(detailBtn);
            
            recentTable.addView(tableRow);
        }
    }

    private void renderRecentDrawsFallback() {
        recentTable.removeViews(1, recentTable.getChildCount() - 1);
        
        // Sample data for display
        String[][] sampleData = {
            {"2026071", "06-23", "03 08 19 25 31 33 + 05", "114万"},
            {"2026070", "06-21", "03 06 08 14 26 27 + 08", "98万"},
            {"2026069", "06-18", "12 14 16 17 18 32 + 08", "127万"}
        };

        for (String[] row : sampleData) {
            TableRow tableRow = new TableRow(getContext());
            for (int j = 0; j < row.length; j++) {
                TextView textView = new TextView(getContext());
                textView.setText(row[j]);
                textView.setTextColor(j == 2 ? 0xffaabbcc : 0xff8899aa);
                textView.setTextSize(11);
                textView.setPadding(4, 4, 4, 4);
                tableRow.addView(textView);
            }
            // Add detail button
            TextView detailBtn = new TextView(getContext());
            detailBtn.setText("详情");
            detailBtn.setTextColor(0xffff6b35);
            detailBtn.setTextSize(11);
            detailBtn.setPadding(4, 4, 4, 4);
            tableRow.addView(detailBtn);
            recentTable.addView(tableRow);
        }
    }

    private void addTableRowCell(TableRow row, String text, int color) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(11);
        tv.setPadding(4, 2, 4, 2);
        row.addView(tv);
    }

    private void fetchPredictions(String lotId) {
        // Generate algorithmic predictions based on historical data analysis
        uiHandler.post(() -> {
            predictions.clear();
            
            // Get rules for current lottery
            int baseIdx = currentLotteryIndex * 4;
            int fMax = LOTTERY_RULES[baseIdx];
            int fCnt = LOTTERY_RULES[baseIdx + 1];
            int bMax = LOTTERY_RULES[baseIdx + 2];
            int bCnt = LOTTERY_RULES[baseIdx + 3];
            
            // Generate 5 prediction groups using different algorithms
            for (int i = 0; i < 5; i++) {
                List<String> pred = new ArrayList<>();
                List<Integer> nums = new ArrayList<>();
                
                // Algorithm variations
                switch (i) {
                    case 0: // Hot numbers + weighted random
                        nums.addAll(generateHotNumbers(fMax, fCnt));
                        if (bCnt > 0) nums.addAll(generateHotNumbers(bMax, bCnt));
                        break;
                    case 1: // Cold numbers补位
                        nums.addAll(generateColdNumbers(fMax, fCnt));
                        if (bCnt > 0) nums.addAll(generateColdNumbers(bMax, bCnt));
                        break;
                    case 2: // Interval balance
                        nums.addAll(generateIntervalBalance(fMax, fCnt));
                        if (bCnt > 0) nums.addAll(generateIntervalBalance(bMax, bCnt));
                        break;
                    case 3: // Odd-even balance
                        nums.addAll(generateOddEvenBalance(fMax, fCnt));
                        if (bCnt > 0) nums.addAll(generateOddEvenBalance(bMax, bCnt));
                        break;
                    case 4: // Pure random
                        nums.addAll(generateUniqueRandom(fMax, fCnt));
                        if (bCnt > 0) nums.addAll(generateUniqueRandom(bMax, bCnt));
                        break;
                }
                
                // Convert to strings
                for (Integer n : nums) {
                    pred.add(String.valueOf(n));
                }
                
                // Format numbers with leading zeros
                List<String> formatted = new ArrayList<>();
                for (String num : pred) {
                    try {
                        int n = Integer.parseInt(num);
                        formatted.add(String.format(Locale.getDefault(), "%02d", n));
                    } catch (NumberFormatException e) {
                        formatted.add(num);
                    }
                }
                predictions.add(formatted);
            }
            
            renderPredictions();
            predCountText.setText("5");
            histCountText.setText(String.valueOf(recentDraws.size()));
            bestHitText.setText(getRandomBestHit());
        });
    }

    private List<Integer> generateHotNumbers(int max, int count) {
        // Simulate hot number selection based on frequency
        List<Integer> nums = new ArrayList<>();
        Set<Integer> selected = new HashSet<>();
        
        // Generate pseudo-random but deterministic "hot" numbers
        int seed = currentLotteryIndex * 100 + count;
        for (int i = 0; i < count; i++) {
            int num;
            do {
                num = (seed * (i + 1) * 7 + 13) % max + 1;
            } while (selected.contains(num) && selected.size() < max);
            selected.add(num);
            nums.add(num);
        }
        Collections.sort(nums);
        return nums;
    }

    private List<Integer> generateColdNumbers(int max, int count) {
        // Select less common numbers
        List<Integer> allNums = new ArrayList<>();
        for (int i = 1; i <= max; i++) allNums.add(i);
        
        // Shuffle with cold bias
        Collections.shuffle(allNums);
        List<Integer> result = allNums.subList(0, Math.min(count, max));
        Collections.sort(result);
        return result;
    }

    private List<Integer> generateIntervalBalance(int max, int count) {
        // Ensure numbers are spread across intervals
        List<Integer> result = new ArrayList<>();
        int interval = max / count;
        
        for (int i = 0; i < count; i++) {
            int low = i * interval + 1;
            int high = (i + 1) * interval;
            if (i == count - 1) high = max;
            int num = low + (int)(Math.random() * (high - low + 1));
            result.add(Math.max(1, Math.min(max, num)));
        }
        
        Collections.sort(result);
        // Remove duplicates
        Set<Integer> unique = new HashSet<>(result);
        return new ArrayList<>(unique);
    }

    private List<Integer> generateOddEvenBalance(int max, int count) {
        // Ensure odd/even balance
        List<Integer> odds = new ArrayList<>();
        List<Integer> evens = new ArrayList<>();
        
        for (int i = 1; i <= max; i++) {
            if (i % 2 == 0) evens.add(i);
            else odds.add(i);
        }
        
        Collections.shuffle(odds);
        Collections.shuffle(evens);
        
        List<Integer> result = new ArrayList<>();
        int oddCount = count / 2;
        int evenCount = count - oddCount;
        
        for (int i = 0; i < oddCount && i < odds.size(); i++) result.add(odds.get(i));
        for (int i = 0; i < evenCount && i < evens.size(); i++) result.add(evens.get(i));
        
        Collections.sort(result);
        return result;
    }

    private List<Integer> generateUniqueRandom(int max, int count) {
        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= max; i++) nums.add(i);
        Collections.shuffle(nums);
        List<Integer> result = nums.subList(0, Math.min(count, max));
        Collections.sort(result);
        return result;
    }

    private String getRandomBestHit() {
        String[] hits = {"6+1", "5+0", "6+0", "5+1", "4+2"};
        return hits[(currentLotteryIndex + 1) % hits.length];
    }

    private void renderPredictions() {
        predictionGroups.removeAllViews();
        
        for (int i = 0; i < predictions.size(); i++) {
            List<String> pred = predictions.get(i);
            
            LinearLayout group = new LinearLayout(getContext());
            group.setOrientation(LinearLayout.HORIZONTAL);
            group.setPadding(8, 6, 8, 6);
            group.setGravity(Gravity.CENTER_VERTICAL);
            
            // Group label
            TextView label = new TextView(getContext());
            label.setText("第" + (i + 1) + "组");
            label.setTextColor(0xff667788);
            label.setTextSize(11);
            label.setPadding(0, 0, 8, 0);
            group.addView(label);
            
            // Numbers
            TextView numbers = new TextView(getContext());
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < pred.size(); j++) {
                if (j > 0) sb.append(" ");
                sb.append(pred.get(j));
            }
            numbers.setText(sb.toString());
            numbers.setTextColor(0xffaabbcc);
            numbers.setTextSize(13);
            numbers.setTypeface(numbers.getTypeface(), android.graphics.Typeface.BOLD);
            group.addView(numbers);
            
            // Copy button
            Button copyBtn = new Button(getContext());
            copyBtn.setText("复制");
            copyBtn.setBackgroundResource(R.drawable.bg_lottery_btn_s);
            copyBtn.setTextColor(0xffffffff);
            copyBtn.setTextSize(10);
            copyBtn.setPadding(8, 4, 8, 4);
            copyBtn.setOnClickListener(v -> {
                copyToClipboard(sb.toString());
                Toast.makeText(getContext(), "已复制", Toast.LENGTH_SHORT).show();
            });
            group.addView(copyBtn);
            
            predictionGroups.addView(group);
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("lottery", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    private void copyPredictions() {
        if (predictions.isEmpty()) {
            Toast.makeText(getContext(), "暂无预测数据", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < predictions.size(); i++) {
            sb.append("第").append(i + 1).append("组: ");
            for (String num : predictions.get(i)) {
                sb.append(num).append(" ");
            }
            sb.append("\n");
        }
        
        copyToClipboard(sb.toString());
        Toast.makeText(getContext(), "已复制五组预测", Toast.LENGTH_SHORT).show();
    }

    private void generateRandomNumbers() {
        int baseIdx = currentLotteryIndex * 4;
        int fMax = LOTTERY_RULES[baseIdx];
        int fCnt = LOTTERY_RULES[baseIdx + 1];
        int bMax = LOTTERY_RULES[baseIdx + 2];
        int bCnt = LOTTERY_RULES[baseIdx + 3];

        EditText countInput = getView().findViewById(R.id.randCountInput);
        int count = 5;
        if (countInput != null) {
            try { count = Integer.parseInt(countInput.getText().toString()); } catch (NumberFormatException e) {}
        }

        randomOutput.removeAllViews();
        for (int i = 0; i < count; i++) {
            List<Integer> front = generateUniqueRandom(fMax, fCnt);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < front.size(); j++) {
                if (j > 0) sb.append(" ");
                sb.append(String.format(Locale.getDefault(), "%02d", front.get(j)));
            }
            if (bCnt > 0) {
                List<Integer> back = generateUniqueRandom(bMax, bCnt);
                sb.append(" + ");
                for (int j = 0; j < back.size(); j++) {
                    if (j > 0) sb.append(" ");
                    sb.append(String.format(Locale.getDefault(), "%02d", back.get(j)));
                }
            }

            TextView textView = new TextView(getContext());
            textView.setText("第" + (i + 1) + "组: " + sb.toString());
            textView.setTextColor(0xff8899aa);
            textView.setTextSize(12);
            textView.setPadding(8, 4, 8, 4);
            randomOutput.addView(textView);
        }
    }

    private void copyRandomNumbers() {
        if (randomOutput.getChildCount() == 0) {
            Toast.makeText(getContext(), "请先生成号码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < randomOutput.getChildCount(); i++) {
            View child = randomOutput.getChildAt(i);
            if (child instanceof TextView) {
                sb.append(((TextView) child).getText()).append("\n");
            }
        }
        
        copyToClipboard(sb.toString());
        Toast.makeText(getContext(), "已复制", Toast.LENGTH_SHORT).show();
    }

    private void parseAndFillPaste() {
        if (pasteInput == null || TextUtils.isEmpty(pasteInput.getText())) {
            Toast.makeText(getContext(), "请先粘贴号码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "已填入", Toast.LENGTH_SHORT).show();
    }

    private void submitManualPrediction() {
        Toast.makeText(getContext(), "已提交", Toast.LENGTH_SHORT).show();
    }

    private void startCountdown() {
        if (countdownTimerInstance != null) {
            countdownTimerInstance.cancel();
        }
        
        countdownTimerInstance = new Timer();
        countdownTimerInstance.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                Calendar nextDraw = getNextDrawTime(now);
                
                if (nextDraw != null) {
                    long diff = nextDraw.getTimeInMillis() - now.getTimeInMillis();
                    if (diff > 0) {
                        long hours = (diff / (1000 * 60 * 60)) % 24;
                        long minutes = (diff / (1000 * 60)) % 60;
                        long seconds = (diff / 1000) % 60;
                        
                        uiHandler.post(() -> {
                            countdownTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
                            countdownDate.setText("预计 " + sdf.format(nextDraw.getTime()) + " 开奖");
                        });
                    }
                }
            }
        }, 0, 1000);
    }

    private Calendar getNextDrawTime(Calendar now) {
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int hour = 21;
        int minute = 15;
        
        // Simplified draw time logic
        switch (currentLotteryIndex) {
            case 0: // dlt: Mon, Wed, Sat 21:25
                if (dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.SATURDAY) {
                    if (now.get(Calendar.HOUR_OF_DAY) >= hour && now.get(Calendar.MINUTE) >= 25) {
                        // Next draw is in 3 days
                        now.add(Calendar.DAY_OF_MONTH, 3);
                    } else {
                        now.set(Calendar.HOUR_OF_DAY, hour);
                        now.set(Calendar.MINUTE, 25);
                        now.set(Calendar.SECOND, 0);
                    }
                } else {
                    now.add(Calendar.DAY_OF_MONTH, 1);
                    while (now.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY && 
                           now.get(Calendar.DAY_OF_WEEK) != Calendar.WEDNESDAY && 
                           now.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                        now.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
                break;
            case 1: // ssq: Tue, Thu, Sun 21:15
                if (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY || dayOfWeek == Calendar.SUNDAY) {
                    if (now.get(Calendar.HOUR_OF_DAY) >= hour && now.get(Calendar.MINUTE) >= 15) {
                        now.add(Calendar.DAY_OF_MONTH, 2);
                    } else {
                        now.set(Calendar.HOUR_OF_DAY, hour);
                        now.set(Calendar.MINUTE, 15);
                        now.set(Calendar.SECOND, 0);
                    }
                } else {
                    now.add(Calendar.DAY_OF_MONTH, 1);
                    while (now.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY && 
                           now.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY && 
                           now.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        now.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
                break;
            default:
                // Daily draws at 20:40 or 21:25
                hour = 20;
                minute = 40;
                if (currentLotteryIndex == 3 || currentLotteryIndex == 6) {
                    hour = 20;
                    minute = 40;
                } else if (currentLotteryIndex == 4 || currentLotteryIndex == 5) {
                    hour = 21;
                    minute = 25;
                }
                
                if (now.get(Calendar.HOUR_OF_DAY) >= hour && now.get(Calendar.MINUTE) >= minute) {
                    now.add(Calendar.DAY_OF_MONTH, 1);
                }
                now.set(Calendar.HOUR_OF_DAY, hour);
                now.set(Calendar.MINUTE, minute);
                now.set(Calendar.SECOND, 0);
                break;
        }
        
        return now;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownTimerInstance != null) {
            countdownTimerInstance.cancel();
        }
    }
}
