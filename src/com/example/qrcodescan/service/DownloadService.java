package com.example.qrcodescan.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;


public class DownloadService extends IntentService {
    private static final int BUFFER_SIZE = 10 * 1024; // 8k ~ 32K
    private static final int TIME_OUT = 10 * 1000;
    private static final int RETRY_TIMES = 2;// ���ԵĴ���
    private static final String TAG = "DownloadService";
    private static final String DOWNLOAD_SUCCESS = "";
    private static final String DOWNLOAD_PROGRESS = "";
    private static final String DOWNLOAD_FAILURE = "";
    
    private NotificationManager mNotifyManager;
    private Builder mBuilder;
    public static final String APK_DOWNLOAD_URL = "url";
    public static final String APK_DOWNLOAD_APKNAME = "apkName";
    public static final String APK_DOWNLOAD_ICON = "icon";
    public static final String APK_DOWNLOAD_APPNAME = "appName";
    public static final String DOWNLOAD_DIR = "/quanql/"; // ����·��
    private int oldProgress = 0;
    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);

        String appName = getString(getApplicationInfo().labelRes);
        int icon = getApplicationInfo().icon;
        String apkName = "Lofter.apk";
        String urlStr = intent.getStringExtra(APK_DOWNLOAD_URL);
        if(intent.hasExtra(APK_DOWNLOAD_APKNAME)){
            apkName = intent.getStringExtra(APK_DOWNLOAD_APKNAME);
        }
        if(intent.hasExtra(APK_DOWNLOAD_APPNAME)){
            appName = intent.getStringExtra(APK_DOWNLOAD_APPNAME);
        }
        if(intent.hasExtra(APK_DOWNLOAD_ICON)){
            icon = intent.getIntExtra(APK_DOWNLOAD_ICON, getApplicationInfo().icon);
        }
        mBuilder.setContentTitle(appName).setSmallIcon(icon);
        InputStream in = null;
        FileOutputStream out = null;
        HttpURLConnection urlConnection = null;

        boolean isRetry = true;
        int timesOfRetry = 0;
        while (isRetry) {
            try {
                URL url = new URL(urlStr);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(false);
                urlConnection.setConnectTimeout(TIME_OUT);
                urlConnection.setReadTimeout(TIME_OUT);
                urlConnection.setRequestProperty("Connection", "Keep-Alive");
                urlConnection.setRequestProperty("Charset", "UTF-8");
                // urlConnection.setRequestProperty("Accept-Encoding",
                // "gzip, deflate");
                urlConnection.setRequestProperty("Accept-Encoding", "identity");

                urlConnection.connect();
                long bytetotal = urlConnection.getContentLength();
                long bytesum = 0;
                int byteread = 0;
                in = urlConnection.getInputStream();
                // File dir = StorageUtils.getCacheDirectory(this);
                File dir = new File(getCustomDirectory(DOWNLOAD_DIR));
                // String apkName=urlStr.substring(urlStr.lastIndexOf("/")+1, urlStr.length());
//              String apkName = "Lofter.apk";
                File apkFile = new File(dir, apkName);
                out = new FileOutputStream(apkFile);
                byte[] buffer = new byte[BUFFER_SIZE];

                while ((byteread = in.read(buffer)) != -1) {
                    bytesum += byteread;
                    out.write(buffer, 0, byteread);

                    int progress = (int) (bytesum * 100L / bytetotal);
                    // ���ȴ���ԭ���ȲŸ��£�����������ui���¿�ʼ
                    if (progress > oldProgress) {
                        updateProgress(progress);
                        oldProgress = progress;
                    }
                }
                // �������
                mBuilder.setContentText(DOWNLOAD_SUCCESS).setProgress(0, 0, false);

                Intent installAPKIntent = new Intent(Intent.ACTION_VIEW);
                // ���û������SDCardдȨ�ޣ�����û��sdcard,apk�ļ��������ڴ��У���Ҫ����Ȩ�޲��ܰ�װ
                String[] command = { "chmod", "777", apkFile.toString() };
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.start();

                installAPKIntent.setDataAndType(Uri.fromFile(apkFile),
                        "application/vnd.android.package-archive");
                // installAPKIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // installAPKIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // installAPKIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        0, installAPKIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                mBuilder.setContentIntent(pendingIntent);
                Notification noti = mBuilder.build();
                noti.flags = android.app.Notification.FLAG_AUTO_CANCEL;
                mNotifyManager.notify(0, noti);

            } catch (Exception e) {
                Log.e(TAG, "download apk file error timesOfRetry = " + timesOfRetry, e);

                if (timesOfRetry == RETRY_TIMES) {
                    isRetry = false;

                    mBuilder.setContentText(DOWNLOAD_FAILURE).setProgress(0, 0, false);
                    Notification noti = mBuilder.build();
                    noti.flags = android.app.Notification.FLAG_AUTO_CANCEL;
                    mNotifyManager.notify(0, noti);
                }
                ++timesOfRetry;
                try {
                    // ֹͣ2s������
                    Thread.sleep(2 * 1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                continue;

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            isRetry = false;

        }

    }

    private void updateProgress(int progress) {
        // "��������:" + progress + "%"
//        mBuilder.setContentText(this.getString(R.string.download_progress, progress)).setProgress(100, progress, false);
        mBuilder.setContentText(String.format(DOWNLOAD_PROGRESS, progress)).setProgress(100, progress, false);
        // setContentInent�����������4.0+��û�����⣬��4.0���»ᱨ�쳣
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0,
                new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setContentIntent(pendingintent);
        mNotifyManager.notify(0, mBuilder.build());
    }

    /** ���Ŀ¼,��������ڣ��򴴽� **/
    public static String getCustomDirectory(String custDir) {
        String dir = getSDPath() + custDir;
        String substr = dir.substring(0, 4);
        if (substr.equals("/mnt")) {
            dir = dir.replace("/mnt", "");
        }
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        return dir;
    }
    
    /**** ȡSD��·������/ ****/
    private static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); // �ж�sd���Ƿ����
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// ��ȡ��Ŀ¼
        }
        if (sdDir != null) {
            return sdDir.toString();
        } else {
            return "";
        }
    }
    
}
