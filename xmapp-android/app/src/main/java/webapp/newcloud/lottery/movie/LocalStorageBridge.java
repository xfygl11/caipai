package webapp.newcloud.lottery.movie;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalStorageBridge {
    private static final String TAG = "LocalStorageBridge";
    private static final String PREFS_NAME = "webapp_storage";
    private static final String KEY_PREFIX = "ls_";

    private final SharedPreferences prefs;

    public LocalStorageBridge(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Object getItem(String key) {
        String value = prefs.getString(KEY_PREFIX + key, null);
        if (value == null) return null;
        try {
            return new JSONObject(value);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse stored value for key: " + key, e);
            return value;
        }
    }

    public void setItem(String key, Object value) {
        try {
            String json;
            if (value instanceof JSONObject) {
                json = value.toString();
            } else if (value instanceof JSONArray) {
                json = value.toString();
            } else if (value instanceof List) {
                json = new JSONArray((List<?>) value).toString();
            } else {
                json = new JSONObject().put("value", value).toString();
            }
            prefs.edit().putString(KEY_PREFIX + key, json).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize value for key: " + key, e);
        }
    }

    public void removeItem(String key) {
        prefs.edit().remove(KEY_PREFIX + key).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public String key(int index) {
        Map<String, ?> all = prefs.getAll();
        Set<String> keys = all.keySet();
        List<String> sortedKeys = new ArrayList<>(keys);
        if (index < 0 || index >= sortedKeys.size()) return null;
        String k = sortedKeys.get(index);
        return k.startsWith(KEY_PREFIX) ? k.substring(KEY_PREFIX.length()) : k;
    }

    public int size() {
        return prefs.getAll().size();
    }

    public String exportAll() {
        try {
            JSONObject result = new JSONObject();
            Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(KEY_PREFIX)) {
                    String storeKey = key.substring(KEY_PREFIX.length());
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    result.put(storeKey, value);
                }
            }
            return result.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to export storage", e);
            return "{}";
        }
    }

    public void importAll(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            SharedPreferences.Editor editor = prefs.edit();
            java.util.Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next();
                editor.putString(KEY_PREFIX + key, obj.getString(key));
            }
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to import storage", e);
        }
    }
}
