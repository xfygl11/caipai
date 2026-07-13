package webapp.newcloud.lottery.movie.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import webapp.newcloud.lottery.movie.MainActivity;
import webapp.newcloud.lottery.movie.R;
import webapp.newcloud.lottery.movie.adapter.ChannelAdapter;
import webapp.newcloud.lottery.movie.util.DbHelper;
import webapp.newcloud.lottery.movie.db.LiveChannelDao;
import webapp.newcloud.lottery.movie.model.LiveChannel;
import webapp.newcloud.lottery.movie.util.HttpClient;
import webapp.newcloud.lottery.movie.util.M3UParser;

public class LiveFragment extends Fragment implements ChannelAdapter.OnChannelClickListener {

    private HorizontalScrollView liveCatScroll;
    private RecyclerView liveChannelList;
    private LinearLayout emptyState;
    private TextView btn_add;
    private TextView btn_refresh;

    private ChannelAdapter channelAdapter;
    private List<LiveChannel> channels = new ArrayList<>();
    private List<String> groups = new ArrayList<>();
    private String currentGroup = "";

    private final HttpClient httpClient = new HttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);
        
        liveCatScroll = view.findViewById(R.id.liveCatScroll);
        liveChannelList = view.findViewById(R.id.liveChannelList);
        emptyState = view.findViewById(R.id.emptyState);
        btn_add = view.findViewById(R.id.btn_add);
        btn_refresh = view.findViewById(R.id.btn_refresh);

        liveChannelList.setLayoutManager(new LinearLayoutManager(getContext()));
        channelAdapter = new ChannelAdapter(channels, this);
        liveChannelList.setAdapter(channelAdapter);

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddSourceDialog();
            }
        });

        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshChannels();
            }
        });

        refreshChannels();
        
        return view;
    }

    private void showAddSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("添加直播源");
        final EditText input = new EditText(getContext());
        input.setHint("请输入直播源地址 (m3u/txt)");
        input.setHintTextColor(0xff667788);
        input.setTextColor(0xffe0e0e0);
        builder.setView(input);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = input.getText().toString().trim();
                if (url.isEmpty()) {
                    Toast.makeText(getContext(), "请输入地址", Toast.LENGTH_SHORT).show();
                    return;
                }
                fetchLiveSource(url);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void fetchLiveSource(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String response = httpClient.httpGet(url);
                    final List<M3UParser.ChannelInfo> parsedChannels = M3UParser.parseLiveSource(response);
                    
                    if (parsedChannels.isEmpty()) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "未解析到频道", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    final List<LiveChannel> liveChannels = new ArrayList<>();
                    for (M3UParser.ChannelInfo info : parsedChannels) {
                        LiveChannel ch = new LiveChannel();
                        ch.id = "lv_" + java.util.UUID.randomUUID().toString().substring(0, 8);
                        ch.name = info.name;
                        ch.url = info.url;
                        ch.group = info.group;
                        ch.source = url;
                        ch.createdAt = System.currentTimeMillis();
                        liveChannels.add(ch);
                    }

                    final List<String> groupSet = new ArrayList<>();
                    Set<String> seen = new HashSet<>();
                    for (LiveChannel ch : liveChannels) {
                        if (ch.group != null && !seen.contains(ch.group)) {
                            seen.add(ch.group);
                            groupSet.add(ch.group);
                        }
                    }

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveChannels(liveChannels);
                            groups = groupSet;
                            renderGroups();
                            renderChannels();
                            Toast.makeText(getContext(), "已添加 " + liveChannels.size() + " 个频道", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Exception e) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void saveChannels(List<LiveChannel> newChannels) {
        if (getContext() == null || newChannels == null || newChannels.isEmpty()) return;
        LiveChannelDao dao = DbHelper.getInstance(getContext()).liveChannelDao();
        // Delete existing channels from same source
        dao.deleteBySource(newChannels.get(0).source);
        // Insert new channels
        for (LiveChannel ch : newChannels) {
            dao.insert(ch);
        }
    }

    private void refreshChannels() {
        if (getContext() == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<LiveChannel> allChannels = DbHelper.getInstance(getContext()).liveChannelDao().getAll();
                    final List<String> groupList = DbHelper.getInstance(getContext()).liveChannelDao().getGroups();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            channels.clear();
                            channels.addAll(allChannels);
                            groups.clear();
                            groups.addAll(groupList);
                            renderGroups();
                            renderChannels();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void renderGroups() {
        LinearLayout groupContainer = new LinearLayout(getContext());
        groupContainer.setOrientation(LinearLayout.HORIZONTAL);
        groupContainer.setGravity(Gravity.CENTER_VERTICAL);

        TextView allBtn = new TextView(getContext());
        allBtn.setText("全部");
        allBtn.setTextSize(13);
        allBtn.setPadding(12, 8, 12, 8);
        allBtn.setTextColor(0xffffffff);
        allBtn.setBackgroundResource(R.color.primary);
        allBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        allBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentGroup = "";
                renderGroups();
                renderChannels();
            }
        });
        groupContainer.addView(allBtn);

        for (final String group : groups) {
            TextView btn = new TextView(getContext());
            btn.setText(group);
            btn.setTextSize(13);
            btn.setPadding(12, 8, 12, 8);
            btn.setTextColor(0xff667788);
            btn.setBackgroundResource(R.drawable.bg_card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(8);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentGroup = group;
                    renderGroups();
                    renderChannels();
                }
            });
            groupContainer.addView(btn);
        }

        // Replace children in HorizontalScrollView
        liveCatScroll.removeAllViews();
        liveCatScroll.addView(groupContainer);
    }

    private void renderChannels() {
        if (currentGroup.isEmpty()) {
            channelAdapter.update(channels);
        } else {
            List<LiveChannel> filtered = new ArrayList<>();
            for (LiveChannel ch : channels) {
                if (ch.group != null && ch.group.equals(currentGroup)) {
                    filtered.add(ch);
                }
            }
            channelAdapter.update(filtered);
        }

        if (channels.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            liveChannelList.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            liveChannelList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onChannelClick(LiveChannel channel) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.playLiveChannel(channel);
        }
    }
}
