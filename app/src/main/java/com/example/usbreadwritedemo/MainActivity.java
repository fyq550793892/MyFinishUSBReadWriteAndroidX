package com.example.usbreadwritedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    FileSystem fileSystem;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);


    }
    public static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_LOGS"};


    //然后通过一个函数来申请
    public void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            } else {
                fileSystem = otgGet(MainActivity.this);
                doIt();
            }

            //检测是否网络的权限
            int permissionInternet = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.INTERNET");
            if (permissionInternet != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void doIt () throws IOException {
//        //获取根目录
        UsbFile root = fileSystem.getRootDirectory();
        root.getAbsolutePath();
//        //创建文件夹
        UsbFile newDir = root.createDirectory("record");
//        //创建文件
        UsbFile newFile = newDir.createFile("1.mp4");

//        // 写入文件
//        OutputStream os = new UsbFileOutputStream(newFile, true);
//        os.write("hello".getBytes());
//        os.close();
//        Log.d(TAG, "doIt: ddd");
        //最后关闭
        //        device.close();

        CopyFile(newFile);
    }




    //获取到OTG连接的U盘
    public static FileSystem otgGet(Context context) {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(context);
        Log.d(TAG, "otgGet: " + devices.length);
        FileSystem currentFs = null;

        for (UsbMassStorageDevice device : devices) {//一般只有一个OTG借口，所以这里只取第一个
            try {
                device.init();
                //如果设备不支持一些格式的U盘，这里会有异常
                if (device == null || device.getPartitions() == null ||
                        device.getPartitions().get(0) == null ||
                        device.getPartitions().get(0).getFileSystem() == null) {
                    return null;
                }
                currentFs = device.getPartitions().get(0).getFileSystem();
                Log.e("OTG", "容量: " + currentFs.getCapacity());
                Log.e("OTG", "已使用空间: " + currentFs.getOccupiedSpace());
                Log.e("OTG", "剩余空间: " + currentFs.getFreeSpace());
                Log.e("OTG", "block数目: " + currentFs.getChunkSize());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return currentFs;
    }


    private void CopyFile(UsbFile usbFile) {
        @SuppressLint("SdCardPath") File file=new File("/mnt/sdcard/1.mp4");
//        if (file.exists()){
//            //EventBus.getDefault().post(new MessageEvent(MyDataType.FILEDETEING));//开始删除原文件
//            //deleteFile(file);
//        }
        Log.d(TAG, "CopyFile: " + file);
        //file.mkdir();

        copy(usbFile,  file.getAbsolutePath());
    }


    //删除源文件
    private void deleteFile(File file) {

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            // Log.i("songkunjian","文件大小::"+file.length());
            file.delete();
        }
    }
    /**
     *
     * 复制文件
     *
     * @param fromFile 要复制的文件目录
     * @param toFile   要粘贴的文件目录
     * @return 是否复制成功
     */
    private  static int mIdent=0;
    public static boolean copy(UsbFile toFFile, String fromFile) {
        if (toFFile==null)return false;

        CopySdcardFile(toFFile, fromFile);
        return true;
    }

    //文件拷贝
    //要复制的目录下的所有非子目录(文件夹)文件拷贝
    public static boolean CopySdcardFile( UsbFile tocFolder, String fromFile) {
        //EventBus.getDefault().post(new MessageEvent(MyDataType.COPYING,cFolder.getLength()));//正在拷贝文件
        try {

            InputStream inusb = new FileInputStream(fromFile);
            UsbFileOutputStream fosto = new UsbFileOutputStream(tocFolder, true);
//            UsbFileInputStream inusb = new UsbFileInputStream(tocFolder);
//            OutputStream fosto = new FileOutputStream(fromFile);

            byte bt[] = new byte[1024 * 10];
            int c;
            while ((c = inusb.read(bt)) != -1) {
                fosto.write(bt, 0, c);
            }
            mIdent--;
            Log.i(" songkunjian", " 完成拷贝==:" + mIdent);
            if (mIdent <= 0) {
                Log.i(" songkunjian", " 最终完成拷贝==:");
                //EventBus.getDefault().post(new MessageEvent(MyDataType.COPYSTOP));
            }
            fosto.flush();
            inusb.close();
            fosto.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}