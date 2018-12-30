package com.project.sky31radio.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import com.project.sky31radio.interfaces.DownloadFileListener;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created   on 2018/12/24.
 */
public class HttpUtil {

    private static final String SSL_PROTOCOL = "TLS";

    public static final int MAX_READ_TIME = 30000;
    public static final int MAX_CONNECT_TIME = 30000;

    public static final String UTF8_ENCODE = "UTF-8";

    public static final String PARAM_API_KEY = "appid";

    public static byte[] get(String urlStr) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        if (isHttps(urlStr)) {
            return getUrlBytesWithHttps(urlStr);
        } else {
            return getUrlBytes(urlStr);
        }
    }

    public static byte[] get(String urlStr, Map<String, String> params, Context context) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        if (params == null) {
            params = new HashMap<String, String>();
        }

        setCommonParams(params, context, null);

        Iterator<String> iterator = params.keySet().iterator();
        StringBuilder builder = new StringBuilder(urlStr);
        if (!params.isEmpty() && !urlStr.contains("?")) {
            builder.append("?");
        }
        while (iterator.hasNext()) {
               String key = iterator.next();
               builder.append(key);
               builder.append("=");
               builder.append(URLEncoder.encode(params.get(key), UTF8_ENCODE));
               builder.append("&");
        }

        urlStr = builder.substring(0 , builder.length() - 1);

        if (isHttps(urlStr)) {
            return getUrlBytesWithHttps(urlStr);
        } else {
            return getUrlBytes(urlStr);
        }
    }

    public static byte[] post(String urlStr, Map<String, String> params, Context context) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if (params == null) {
            params = new HashMap<String, String>();
        }

        setCommonParams(params, context, null);

        if (isHttps(urlStr)) {
            return postUrlBytesWithHttps(urlStr, params, UTF8_ENCODE);
        } else {
            return postUrlBytes(urlStr, params, UTF8_ENCODE);
        }
    }

    public static byte[] post(String urlStr, Map<String, String> params, Context context, String iccid) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if (params == null) {
            params = new HashMap<String, String>();
        }

        setCommonParams(params, context, iccid);

        if (isHttps(urlStr)) {
            return postUrlBytesWithHttps(urlStr, params, UTF8_ENCODE);
        } else {
            return postUrlBytes(urlStr, params, UTF8_ENCODE);
        }
    }

    public static byte[] postFile(String urlStr, Map<String, File> fileParams, Map<String, String> textParams, Context context) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if (textParams == null) {
            textParams = new HashMap<String, String>();
        }

        setCommonParams(textParams, context, null);

        if (isHttps(urlStr)) {
            return postFileBytesWithHttps(urlStr, fileParams, textParams, UTF8_ENCODE);
        } else {
            return postFileBytes(urlStr, fileParams, textParams, UTF8_ENCODE);
        }
    }

    public static byte[] getUrlBytes(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setReadTimeout(MAX_READ_TIME);
        conn.setConnectTimeout(MAX_CONNECT_TIME);

        try {
            InputStream in = conn.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    public static byte[] postUrlBytes(String urlStr, Map<String, String> params, String encode) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setReadTimeout(MAX_READ_TIME);
        conn.setConnectTimeout(MAX_CONNECT_TIME);

        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        try {
            if (params != null) {
                OutputStream outputStream = conn.getOutputStream();
                Uri.Builder builder = new Uri.Builder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && value != null) {
                        builder.appendQueryParameter(key, value);
                    }
                }
                String query = builder.build().getQuery();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, encode));
                writer.write(query);
                writer.flush();
                writer.close();
                outputStream.close();
            }

            InputStream in = conn.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }

    }

    public static byte[] postFileBytes(String urlStr, Map<String, File> fileParams, Map<String, String> textParams, String encode) throws IOException {
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setReadTimeout(MAX_READ_TIME);
        conn.setConnectTimeout(MAX_CONNECT_TIME);

        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Connection", "Keep-Alive");
//        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        try {
            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());

            // 文件参数
            for (String key : fileParams.keySet()) {
                File file = fileParams.get(key);
                ds.writeBytes(twoHyphens + boundary + crlf);
                ds.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + URLEncoder.encode(file.getName(), encode) + "\"" + crlf);
                ds.writeBytes(crlf);

                FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                in.close();
                ds.write(out.toByteArray());

                ds.writeBytes(crlf);
            }

            // 文本参数
            for (String key : textParams.keySet()) {
                String value = textParams.get(key);
                ds.writeBytes(twoHyphens + boundary + crlf);
                ds.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + crlf);
                ds.writeBytes(crlf);
                ds.writeBytes(URLEncoder.encode(value, encode));
                ds.writeBytes(crlf);
            }

            ds.writeBytes(twoHyphens + boundary + crlf);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

            ds.flush();
            ds.close();

            InputStream in = conn.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    public static byte[] getUrlBytesWithHttps(String urlStr) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setReadTimeout(MAX_READ_TIME);
        conn.setConnectTimeout(MAX_CONNECT_TIME);

        SSLContext ssl = SSLContext.getInstance(SSL_PROTOCOL);
        ssl.init(null, new TrustManager[]{new AllTrustManager()}, new SecureRandom());
        conn.setSSLSocketFactory(ssl.getSocketFactory());
        conn.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        });

        try {
            InputStream in = conn.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    public static byte[] postUrlBytesWithHttps(String urlStr, Map<String, String> params, String encode) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setReadTimeout(MAX_READ_TIME);
        conn.setConnectTimeout(MAX_CONNECT_TIME);

        SSLContext ssl = SSLContext.getInstance(SSL_PROTOCOL);
        ssl.init(null, new TrustManager[]{new AllTrustManager()}, new SecureRandom());
        conn.setSSLSocketFactory(ssl.getSocketFactory());
        conn.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        });

        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        try {
            if (params != null) {
                Uri.Builder builder = new Uri.Builder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && value != null) {
                        builder.appendQueryParameter(key, value);
                    }
                }
                String query = builder.build().getQuery();
                OutputStream outputStream = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, encode));
                writer.write(query);
                writer.flush();
                writer.close();
                outputStream.close();
            }

            InputStream in = conn.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }

    }

    public static byte[] postFileBytesWithHttps(String urlStr, Map<String, File> fileParams, Map<String, String> textParams, String encode) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setReadTimeout(MAX_READ_TIME);
        conn.setConnectTimeout(MAX_CONNECT_TIME);

        SSLContext ssl = SSLContext.getInstance(SSL_PROTOCOL);
        ssl.init(null, new TrustManager[]{new AllTrustManager()}, new SecureRandom());
        conn.setSSLSocketFactory(ssl.getSocketFactory());
        conn.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        });

        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Connection", "Keep-Alive");
//        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        try {
            DataOutputStream ds = new DataOutputStream(conn.getOutputStream());

            // 文件参数
            for (String key : fileParams.keySet()) {
                File file = fileParams.get(key);
                ds.writeBytes(twoHyphens + boundary + crlf);
                ds.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + URLEncoder.encode(file.getName(), encode) + "\"" + crlf);
                ds.writeBytes(crlf);

                FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                in.close();
                ds.write(out.toByteArray());

                ds.writeBytes(crlf);
            }

            // 文本参数
            for (String key : textParams.keySet()) {
                String value = textParams.get(key);
                ds.writeBytes(twoHyphens + boundary + crlf);
                ds.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + crlf);
                ds.writeBytes(crlf);
                ds.writeBytes(URLEncoder.encode(value, encode));
                ds.writeBytes(crlf);
            }

            ds.writeBytes(twoHyphens + boundary + crlf);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

            ds.flush();
            ds.close();

            InputStream in = conn.getInputStream();
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * GET请求方式下载文件，若文件已存在则进行覆盖。
     * @param urlStr 下载的url，该方法使用的GET请求
     * @param targetFile 目标文件
     * @return true表示下载成功，false则为失败
     */
    public static boolean getDownloadFile(String urlStr, File targetFile) throws Exception {
        if (urlStr == null || targetFile == null) {
            throw new NullPointerException("urlStr or targetFile is null!");
        } else if (urlStr.isEmpty()) {
            throw new Exception("urlStr is empty!");
        }

        if (targetFile.exists()) {
            targetFile.delete();
        }
        try {
            targetFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean downloadSucceed = false;
        HttpURLConnection conn = null;
        FileOutputStream fos = null;
        try {
            URL url = new URL(urlStr);
            if (isHttps(urlStr)) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
                SSLContext ssl = SSLContext.getInstance(SSL_PROTOCOL);
                ssl.init(null, new TrustManager[]{new AllTrustManager()}, new SecureRandom());
                httpsConn.setSSLSocketFactory(ssl.getSocketFactory());
                httpsConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(final String hostname, final SSLSession session) {
                        return true;
                    }
                });
                conn = httpsConn;
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setReadTimeout(MAX_READ_TIME);
            conn.setConnectTimeout(MAX_CONNECT_TIME);

            InputStream is = conn.getInputStream();
            fos = new FileOutputStream(targetFile);

            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            is.close();
            downloadSucceed = true;
        } catch (Exception e) {
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
            }
        }
        return downloadSucceed;
    }

    public static void downloadFile(String url, String filePath, DownloadFileListener listener) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(filePath)) {
            if (listener != null) {
                listener.onFinish(false, null);
            }
            return;
        }
        HttpURLConnection conn = null;
        FileOutputStream fos = null;
        InputStream is = null;
        File targetFile = null;
        long totalSize = 0;
        long downloadSize = 0;
        long lastProgressSize = 0;
        try {
            targetFile = new File(filePath);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            targetFile.createNewFile();

            URL httpUrl = new URL(url);
            if (isHttps(url)) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) httpUrl.openConnection();
                SSLContext ssl = SSLContext.getInstance(SSL_PROTOCOL);
                ssl.init(null, new TrustManager[]{new AllTrustManager()}, new SecureRandom());
                httpsConn.setSSLSocketFactory(ssl.getSocketFactory());
                httpsConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(final String hostname, final SSLSession session) {
                        return true;
                    }
                });
                conn = httpsConn;
            } else {
                conn = (HttpURLConnection) httpUrl.openConnection();
            }
            if (200 != conn.getResponseCode()) {
                if (listener != null) {
                    listener.onFinish(false, null);
                }
                return;
            }
            conn.setReadTimeout(MAX_READ_TIME);
            conn.setConnectTimeout(MAX_CONNECT_TIME);
            totalSize = conn.getContentLength();

            is = conn.getInputStream();
            fos = new FileOutputStream(targetFile);
            byte buffer[] = new byte[1024];
            int readSize = 0;
            while ((readSize = is.read(buffer)) > 0) {
                fos.write(buffer, 0, readSize);
                downloadSize += readSize;
                if (lastProgressSize == 0 || (downloadSize - lastProgressSize) > (totalSize / 10)) {
                    if (listener != null) {
                        int progress = (int) (downloadSize * 100L / totalSize);
                        listener.onProgress(progress);
                    }
                    lastProgressSize = downloadSize;
                }
            }
        } catch (Exception e) {
            if (targetFile != null && targetFile.exists()) {
                targetFile.delete();
            }
            if (listener != null) {
                listener.onFinish(false, null);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (targetFile != null
                    && targetFile.exists()
                    && listener != null
                    && totalSize > 0
                    && totalSize == downloadSize) {
                listener.onFinish(true, targetFile);
            }
        }
    }

    private static void setCommonParams(Map<String, String> params, Context context, String iccid) {
        params.put(PARAM_API_KEY, "bbin001");

    }

    private static boolean isHttps(String url) {
        return url.contains("https://");
    }

    private static class AllTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}
