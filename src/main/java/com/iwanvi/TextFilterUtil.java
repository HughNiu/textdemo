package com.iwanvi;

import net.sf.json.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Niu Qianghong on 2018-01-05 0005.
 */
public class TextFilterUtil {

    private static final String ACCESSKEY = "YAhVfJRjuezX7xOiIDp1"; // 商户的 accessKey
    private static final String REQUEST_URL = "http://api.fengkongcloud.com/v2/saas/anti_fraud/text";
    private static final int INTERCEPT_SCORE = 700; // 需要拦截的最低指数
    private static final Logger LOGGER = Logger.getLogger(TextFilterUtil.class);

    /**
     * 单纯文本过滤, 返回原始处理结果
     * @param type
     * @param userId
     * @param text
     * @return
     */
    public static Map<String, Object> filter(String type, Integer userId, String text) {
        try {
            HashMap<String, Object> userData = new HashMap<String, Object>();
            userData.put("accessKey", ACCESSKEY);
            userData.put("type", type);

            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("tokenId", userId.toString());
            data.put("text", text);

            userData.put("data", data);

            JSONObject json = JSONObject.fromObject(userData);
            JSONObject result = HttpRequestUtils.httpPost(REQUEST_URL, json);
            System.out.println(result);

            json = JSONObject.fromObject(result);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 返回屏蔽敏感词之后的文本
     * @param type
     * @param userId
     * @param text
     * @return
     */
    public static String getRetouchedText(String type, Integer userId, String text) throws TextFilterException {
        Map<String, Object> filterResult = filter(type, userId, text);
        int score = MapUtils.getIntValue(filterResult, "score", 0); // 拦截指数
        if (filterResult != null
                && MapUtils.getIntValue(filterResult, "code", 0) == 1100) {

            Map detail = MapUtils.getMap(filterResult, "detail");
            LOGGER.info("数美垃圾文本识别: uid = " + userId + "text = " + text + ", detail = " + detail);

            if (score > INTERCEPT_SCORE) { // 需要拦截
                if (detail.containsKey("filteredText")) { // 数美返回修饰过的文本
                    return (String) detail.get("filteredText");
                } else { // 文本被拦截, 但未命中任何名单, 无修饰过的文本
                    throw new TextFilterException("文本被拦截, 但未命中任何名单");
                }
            } else {
                return text; // 直接放行
            }
        }
        return null; // 请求失败返回null
    }

}
