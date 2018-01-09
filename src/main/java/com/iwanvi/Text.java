package com.iwanvi;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Niu Qianghong on 2018-01-03 0003.
 */
public class Text {
    private static final String ACCESSKEY = "YAhVfJRjuezX7xOiIDp1"; // 商户的 accessKey
    private static final String REQUEST_URL = "http://api.fengkongcloud.com/v2/saas/anti_fraud/text";
    private static final Logger logger = Logger.getLogger(Text.class);

    private static int checkText(String type, Integer userId, String text) {
        HashMap<String, Object> userData = new HashMap<String, Object>();
        userData.put("accessKey", ACCESSKEY);
        userData.put("type", type);

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("tokenId", userId.toString());
        data.put("text", text);

        userData.put("data", data);

        JSONObject json = JSONObject.fromObject(userData);
        JSONObject result = HttpRequestUtils.httpPost(REQUEST_URL, json);

        System.out.println(result.toString());
        json = JSONObject.fromObject(result);

        /**
         * 接口会返回code， code=1100 时说明请求成功，根据不同的 riskLevel 风险级别进行业务处理
         * 当 code!=1100 时，如果是 1902 错误，需要检查参数配置
         * 其余情况需要根据错误码进行重试或者其它异常处理
         */
        if (json.getInt("code") == 1100) {
            String riskLevel = json.getString("riskLevel");
            int score = json.getInt("score");
            /*if ("PASS".equals(riskLevel)) {
                // 放行
                return 1;
            } else if ("REVIEW".equals(riskLevel)) {
                // 人工审核，如果没有审核，就放行
                return 1;
            } else if ("REJECT".equals(riskLevel)) {
                // 拒绝
                return -1;
            } else {
                // 异常
                return -1;
            }*/
            return score;
        } else {
            // 接口请求失败，需要参照返回码进行不同的处理
            return -1;
        }
    }

    private static void testExcel(String filePath) {
        int num = 0;
        try {
            InputStream file = new FileInputStream(new File(filePath));
            HSSFWorkbook excel = new HSSFWorkbook(file);
            HSSFSheet sheet = excel.getSheetAt(0);
            long start = System.currentTimeMillis();
            for (Iterator ite = sheet.rowIterator(); ite.hasNext(); ) {
                HSSFRow row = (HSSFRow) ite.next();
                HSSFCell cell = row.getCell(0);
                CellType cellType = cell.getCellTypeEnum();
                String text = cellType.compareTo(CellType.NUMERIC) == 0 ? cell.getNumericCellValue() + "" : cell.getStringCellValue();
                int score = checkText("GAME", num, text);
                /*if (score == 1) {
                    int type = (int)row.getCell(1).getNumericCellValue();
                    logger.error(text + "-->" + type);
                } else {*/
                HSSFCell scoreCell = row.createCell(2);
                scoreCell.setCellValue(score);
                //}
                num++;
                logger.info("num = " + num);
            }
            logger.info("总用时: " + (System.currentTimeMillis() - start) / 1000.0 + "s");


            FileOutputStream excelFileOutPutStream = new FileOutputStream("G:/敏感词_score.xls");
            excel.write(excelFileOutPutStream);
            // 执行 flush 操作， 将缓存区内的信息更新到文件上
            excelFileOutPutStream.flush();
            excelFileOutPutStream.close();

            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws TextFilterException {
        //testExcel("g:\\敏感词.xls");
        //System.out.println(checkText("GAME", 1, "肛门拳交有需要的吗"));
        System.out.println(TextFilterUtil.getRetouchedText("ZHIBO", 1, "肛门拳交有需要的吗"));
    }
}
