package com.example.proxy_core;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ProxyApplication extends Application {

    //定义好解密后的文件的存放路径
    private String app_name;
    private String app_version;

    private String TAG = this.getClass().getSimpleName();

    /**
     * ActivityThread创建Application之后调用的第一个方法
     * 可以在这个方法中进行解密，同时把dex交给android去加载
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        CrashHandler.getInstance().init(base);
        //获取用户填入的metadata
        getMetaData();

        //得到当前加密了的APK文件
//        File apkFile=new File(Environment.getExternalStorageDirectory()+"/app-signed-aligned.apk");
        File apkFile=new File(getApplicationInfo().sourceDir);

        //把apk解压   app_name+"_"+app_version目录中的内容需要boot权限才能用
        File versionDir =getDir("yangkun", Context.MODE_PRIVATE);
        File appDir=new File(versionDir,"app");
        File dexDir=new File(appDir,"dexDir");

        //得到我们需要加载的Dex文件
        List<File> dexFiles=new ArrayList<>();
        //进行解密（最好做MD5文件校验）
        if(!dexDir.exists() || dexDir.list().length==0){
            //把apk解压到appDir
            Zip.unZip(apkFile,appDir);
            //获取目录下所有的文件
            File[] files=appDir.listFiles();
            for (File file : files) {
                String name=file.getName();
                if(name.endsWith(".dex") && !TextUtils.equals(name,"classes.dex")){
                    try{
                        //读取文件内容
                        byte[] bytes= ProxyUtils.getBytes(file);
                        //解密
                        byte[] decrypt = EncryptUtil.decrypt(bytes,EncryptUtil.ivBytes);
                        //写到指定的目录
                        FileOutputStream fos=new FileOutputStream(file);
                        fos.write(decrypt);
                        fos.flush();
                        fos.close();
                        dexFiles.add(file);

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }else{
            for (File file : dexDir.listFiles()) {
                dexFiles.add(file);
            }
        }

        try{
            //2.把解密后的文件加载到系统
            loadDex(dexFiles,versionDir);
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private void loadDex(List<File> dexFiles, File versionDir) throws Exception{
        //1.获取pathlist
        Field pathListField = ProxyUtils.findField(getClassLoader(), "pathList");
        Object pathList = pathListField.get(getClassLoader());
        //2.获取数组dexElements
        Field dexElementsField= ProxyUtils.findField(pathList,"dexElements");
        Object[] dexElements=(Object[])dexElementsField.get(pathList);
        //3.反射到初始化dexElements的方法
        Method makeDexElements= ProxyUtils.findMethod(pathList,"makePathElements",List.class,File.class,List.class);

        ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
        Object[] addElements=(Object[])makeDexElements.invoke(pathList,dexFiles,versionDir,suppressedExceptions);

        //合并数组
        Object[] newElements= (Object[])Array.newInstance(dexElements.getClass().getComponentType(),dexElements.length+addElements.length);
        System.arraycopy(dexElements,0,newElements,0,dexElements.length);
        System.arraycopy(addElements,0,newElements,dexElements.length,addElements.length);

        //替换classloader中的element数组
        dexElementsField.set(pathList,newElements);
    }

    private void getMetaData() {
        try{
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData=applicationInfo.metaData;
            if(null!=metaData){
                if(metaData.containsKey("app_name")){
                    app_name=metaData.getString("app_name");
                }
                if(metaData.containsKey("app_version")){
                    app_version=metaData.getString("app_version");
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}








