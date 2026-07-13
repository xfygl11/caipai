package webapp.newcloud.lottery.movie;

import java.util.Map;

public interface SpiderBase {
    String homeContent(boolean filter) throws Exception;
    String homeVideoContent() throws Exception;
    String categoryContent(String tid, String pg, boolean filter, Map<String, String> extend) throws Exception;
    String detailContent(java.util.List<String> ids) throws Exception;
    String playerContent(String flag, String url, java.util.List<String> vipFlags) throws Exception;
    String searchContent(String key, boolean quick) throws Exception;
}
