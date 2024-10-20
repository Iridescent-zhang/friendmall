package com.aeterna.friendmall.order.config;

import com.aeterna.friendmall.order.vo.PayVo;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "9021000140627129";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCYRRy9R7X2dbK9uddWXSuKfM8yScnmYg47JO2qQXieXZUAV539pHXlo8GoLPa3q7C2j6tpOMwGJgkisfuf6Rr5JHnGbP6onCCB11/OOm1+teBWc2kJoDASQ4pzR4fh7fNiWmafjdGcRaf3qo4KIUGNEPH6Yc9rQULUcbRnfGyW+h91WYCgWtulSfpqSRtx7/yJc1Ilr2vGfKJGDXrbykXhvxYtJPPVKZ/ua9ncmi9E5fA3ODw59G2mpac60jaHKynzKqT+RkLjeswccJv/+jA2QxejZV/Bbce0OHXtwqTiNr3yY8ghdLf9W5OQTIA08FFVloWVN6OLidbqug1KkToDAgMBAAECggEAJNVK/2pSVNzhsM08jrKY7rYENMjuouEDSkFDkFSoBb7jZSLMn+dhcXwsQy8lOwa4B2E3eIt5pt8ahJ8DHAp6MNwm7N2+uDCUGHRtw2gUxnreWmsudFqgZjqaSgp6jydNIXR2sI/QrXmYCOvj0gV7YtE/h26TiwFNVjBvED1j0x0sGqj9FtLmflsTd2e1pdBHHgCC9EvtzsM18JMWPZy3jvzfBKCt7gVd638nwPwZP760uxOYQobwBfzbsSUhk9SBV7rUNfmDFgrNrSDmo4e6SThVMmaKj2DZUKIgeB0bwWy/9AeUZz9I3LLVglBf8HlNHe5JhLBgQiwkqg2iTcWkoQKBgQDWOPOiB4+/EP3lYj9YYLAJlPZ9IKXU+X3L01lPsHIJLMkHv+/98r4t49K6xRJ7pKdqpqcDqkxvLV1CtqzTYNCzF5I785s5uCWZRkFA3JeecATwVIBJXjqqSgOHY3vkzZPBEAKAKuqZrR0kXmMkXSkXSGyGJQtb9zXsGYRkydgb0wKBgQC19y8+YDWviplWuzev5A0+in7qe+Qr75W/uxSPqlosFC5dmA2pmrt6S6BaFGIva/0lPkUbt9zKUNVCjl7tq81XSLDgRKlOlQaPg2IMqpbgf52yWZK7P1f8j7I1imVvL1JefRKdjVqoF3BYeMnhqrRKlWa5e923+rOyIh/NwCN7EQKBgQDBOWN/7cQZuhDZGzI53BRMwEFIge8yV8vA0qTHPUbOwceeJuEotRAXQlxsPJ+8SLr9ds5EfxIsYcWyDCV9D+GO+J7dVpDXkiLPys/G2nIj5bR+til0g/r0aIggUXqJ2WgBhxhKVOuAq+YSTNSuEx2iM6A3qyhAzo2jpSyG4Iz+IwKBgQCx+nyuEOJRuUmz6FhWZMODiWkLpnQNeUBKEQzSXDfoZzDWgJPUcTCwNGo50Tgb1A79L4Pe0Z5WEGwWtjQpWWH13naKVP4Yi3CoTERJyUzbdlbjC/OVzCYeNNqEvcHq1ylEnCbqDUk/ofVTw4kKfbHsRWb9Qycg/UhnkjLzRtwtUQKBgGUNWuj4rv4IeR+wgv6i1vPKlC6Iedn2K0PFcpKWn/JDvNpcxCMxWS0OBMFqwiBuIYlnsTxE/s8Kdau8ixo2Ts1KcKx8j5H/Ql7PviDAXWL2Ddc8NAMcrCXszMEvcGPnt8Nv8nz1Ih0BuWOvoHNML8a2uSD69+idjr4TyFSg5trb";

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnA3XLhEMsf6sVmCETxsj1mOg0lv/8cUe3UgY3TzTzbjLx41b4li8BqIRlRhyvEU6qfZsTxa5nLrZVrjn/SvpU0TmIu1U2f1T31dejN3j/pellFCicIEdVhH2Dod2Sc47gSAjaFqm7taNkAxfiEqRT/LDfmonZQSW9d3l2lbA+7RznOCsFgvCNnwY0hddGhNsjGT3O4YR9Nxtf/mYGbJbT/FpapUcgMRRkaNwyOt8Btz51j/1rXwEEHs8vnuTQn6H61ENGX8heTD9sOp3j+6jZ1CK2RcftSjgFE8N9ECVXBIpIBkeqm1TRLX8iDRukJdwQoKjeUFXckTAPFNzEPwwmQIDAQAB";

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息，注意要配置为内网穿透的【注意小心这个外网穿透的域名会不会改变】，我们用这个信息来改订单状态，比如从待付款改成支付成功
    private  String notify_url = "http://g.free.idcfengye.com/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = "http://member.friendmall.com/memberOrder.html";

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";  // 从沙箱到线上只要更换这个网关就能成功

    //订单超时时间
    private String timeout = "1m";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
