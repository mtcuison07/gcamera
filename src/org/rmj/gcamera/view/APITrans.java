package org.rmj.gcamera.view;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.WebClient;

public class APITrans {
    public static String getUserPanalo(String fsValue){
        String sURL = "https://restgk.guanzongroup.com.ph/gconnect/upload/getMyPanalo.php";
        
        JSONObject param = new JSONObject();
        param.put("payload", fsValue);
        
        return getResponse(sURL, param.toJSONString());
    }
    
    private static String getResponse(String sURL, String sJSon){
        try {
            String response = WebClient.sendHTTP(sURL, sJSon, (HashMap<String, String>) getHeaders());
            if(response == null){
                JSONObject err_detl = new JSONObject();
                err_detl.put("code", "250");
                err_detl.put("message", "No response from server.");

                JSONObject err_mstr = new JSONObject();
                err_mstr.put("result", "error");
                err_mstr.put("error", err_detl);
                return err_mstr.toJSONString();
            } 
            
            return response;
        } catch (IOException ex) {
            JSONObject err_detl = new JSONObject();
            err_detl.put("code", "250");
            err_detl.put("message", "IOException: " + ex.getMessage());

            JSONObject err_mstr = new JSONObject();
            err_mstr.put("result", "error");
            err_mstr.put("error", err_detl);
            return err_mstr.toJSONString();
        }
    }
    
    private static HashMap getHeaders(){
        String clientid = System.getProperty("user.client.id");
        String user = System.getProperty("user.id");
        
        String productid = "IntegSys";
        String imei = MiscUtil.getPCName();
        
        Calendar calendar = Calendar.getInstance();
        Map<String, String> headers = 
                        new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("g-api-id", productid);
        headers.put("g-api-imei", imei);
        
        headers.put("g-api-key", SQLUtil.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));        
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));
        headers.put("g-api-client", clientid);    
        headers.put("g-api-user", user);    
        headers.put("g-api-log", "");    
        headers.put("g-char-request", "UTF-8");
        headers.put("g-api-token", "");    
        
        return (HashMap) headers;
    }
}
