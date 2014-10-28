package com.example.qrcodescan.activity;

import com.example.qr_codescan.R;
import com.example.qrcodescan.service.DownloadService;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final static int SCANNIN_GREQUEST_CODE = 1;
    /**
     * ��ʾɨ����
     */
    private TextView mTextView;
    /**
     * ��ʾɨ���ĵ�ͼƬ
     */
    private ImageView mImageView;
    private Button mScanButton;
    private Button mOpenButton;
    private Button mDownloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mScanButton = (Button) findViewById(R.id.button1);
        mTextView = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.qrcode_bitmap);
        mOpenButton = (Button) findViewById(R.id.btn_open);
        mDownloadButton = (Button) findViewById(R.id.btn_download);
        
        // �����ť��ת����ά��ɨ����棬�����õ���startActivityForResult��ת
        // ɨ������֮������ý���
        mScanButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MipcaActivityCapture.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
            }
        });
        
        
        mOpenButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(mTextView.getText().toString());
                Intent it = new Intent(Intent.ACTION_VIEW, uri);
                MainActivity.this.startActivity(it);
            }
        });
        mDownloadButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DownloadService.class);
                intent.putExtra(DownloadService.APK_DOWNLOAD_URL, Uri.parse(mTextView.getText().toString()));
                intent.putExtra(DownloadService.APK_DOWNLOAD_APKNAME, "lofter.apk");
                intent.putExtra(DownloadService.APK_DOWNLOAD_APPNAME, "����LOFTER");
                intent.putExtra(DownloadService.APK_DOWNLOAD_ICON, android.R.drawable.stat_sys_download_done);
                MainActivity.this.startService(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case SCANNIN_GREQUEST_CODE:
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                // ��ʾɨ�赽������
                mTextView.setText(bundle.getString("result"));
                mTextView.setVisibility(View.VISIBLE);
                // ��ʾ
                mImageView.setImageBitmap((Bitmap) data.getParcelableExtra("bitmap"));
                mImageView.setVisibility(View.VISIBLE);
                mOpenButton.setVisibility(View.VISIBLE);
                mDownloadButton.setVisibility(View.VISIBLE);
            }
            break;
        }
    }

}
