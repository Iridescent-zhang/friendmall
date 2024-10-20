package com.aeterna.friendmall.thirdparty;

import com.aeterna.friendmall.thirdparty.component.SmsComponent;
import com.aeterna.friendmall.thirdparty.util.HttpUtils;
import com.aliyun.oss.OSSClient;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class FriendmallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Autowired
    SmsComponent smsComponent;

    @Test
    void testSendCode() {
        String code = UUID.randomUUID().toString().substring(0, 6);
        smsComponent.sendSmsCode("15160431594", code);
    }

    @Test
    public void testUpload() throws FileNotFoundException {

        // 填写Bucket名称，例如examplebucket。
        String bucketName = "friendmall";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "dream.jpg";
        InputStream inputStream = new FileInputStream("D:\\LifePictures\\Gears\\dream.jpg");

        // 创建PutObject请求上传文件流
        ossClient.putObject(bucketName, objectName, inputStream);

        ossClient.shutdown();

        System.out.println("上传完成");
    }

    @Test
    public void sendSms(){
        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "27e23e0e988541e99f9c02d8349fd838";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", "15160431594");
        querys.put("param", "**code**:12345,**minute**:5");

//smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html

        querys.put("smsSignId", "2e65b1bb3d054466b82f0c9d125465e2");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
        Map<String, String> bodys = new HashMap<String, String>();


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
