package com.wzypan.utils;

import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OKHttpUtils {
    private static final int TIME_OUT_SECONDS = 8;

    private static OkHttpClient.Builder getClientBuilder() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().followRedirects(false).retryOnConnectionFailure(false);
        clientBuilder.connectTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS).readTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
        return clientBuilder;
    }

    private static Request.Builder getRequestBuilder(Map<String, String> header) {
        Request.Builder requestBuilder = new Request.Builder();
        if (header!=null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue();
                requestBuilder.addHeader(key, value);
            }
        }
        return requestBuilder;
    }

    public static String getRequest(String url) throws BusinessException {
        ResponseBody responseBody = null;
        try {
            OkHttpClient.Builder clientBuilder = getClientBuilder();
            Request.Builder requestBuilder = getRequestBuilder(null);
            OkHttpClient client = clientBuilder.build();
            Request request = requestBuilder.url(url).build();
            Response response = client.newCall(request).execute();
            responseBody = response.body();
            String responseStr = responseBody.string();
            return responseStr;
        } catch (SocketTimeoutException | ConnectException e) {
            throw new BusinessException(ResponseCodeEnum.CODE_500.getCode(), "okhttp post 请求超时");
        } catch (Exception e) {
            log.error("okhttp get 异常");
            return null;
        } finally {
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }
}
