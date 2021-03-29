package com.example.proxy_core;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

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
    private long endTime;

    /**
     * ActivityThread创建Application之后调用的第一个方法
     * 可以在这个方法中进行解密，同时把dex交给android去加载
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //用于在解密过程中如果出现异常，可以进行捕获
        CrashHandler.getInstance().init(base);
        long startTime = SystemClock.currentThreadTimeMillis();

        //1. 获取用户填入的metadata
        getMetaData();

        //2. 得到当前加密了的APK文件,也就是安装包
//        File apkFile=new File(Environment.getExternalStorageDirectory()+"/app-signed-aligned.apk");
        File apkFile=new File(getApplicationInfo().sourceDir);

        //3. 把apk解压   app_name+"_"+app_version 目录中的内容需要 boot 权限才能用
        File versionDir =getDir("DevYK", Context.MODE_PRIVATE);
        File appDir=new File(versionDir,"app");
        File dexDir=new File(appDir,"dexDir");

        //4. 得到我们需要加载的Dex文件
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
                        //5. 解密 dex
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
            //6.把解密后的文件加载到系统
            loadDex(dexFiles,versionDir);

            endTime = SystemClock.currentThreadTimeMillis() - startTime;
            Log.d(TAG,"解密完成! 共耗时：" + endTime +" ms");

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private void loadDex(List<File> dexFiles, File versionDir) throws Exception{
        //1.先从 ClassLoader 中获取 pathList 的变量
        Field pathListField = ProxyUtils.findField(getClassLoader(), "pathList");
        //1.1 得到 DexPathList 类
        Object pathList = pathListField.get(getClassLoader());
        //1.2 从 DexPathList 类中拿到 dexElements 变量
        Field dexElementsField= ProxyUtils.findField(pathList,"dexElements");
        //1.3 拿到已加载的 dex 数组
        Object[] dexElements=(Object[])dexElementsField.get(pathList);

        //2. 反射到初始化 dexElements 的方法，也就是得到加载 dex 到系统的方法
        Method makeDexElements= ProxyUtils.findMethod(pathList,"makePathElements",List.class,File.class,List.class);
        //2.1 实例化一个 集合  makePathElements 需要用到
        ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
        //2.2 反射执行 makePathElements 函数，把已解码的 dex 加载到系统，不然是打不开 dex 的，会导致 crash
        Object[] addElements=(Object[])makeDexElements.invoke(pathList,dexFiles,versionDir,suppressedExceptions);

        //3. 实例化一个新数组，用于将当前加载和已加载的 dex 合并成一个新的数组
        Object[] newElements= (Object[])Array.newInstance(dexElements.getClass().getComponentType(),dexElements.length+addElements.length);
        //3.1 将系统中的已经加载的 dex 放入 newElements 中
        System.arraycopy(dexElements,0,newElements,0,dexElements.length);
        //3.2 将解密后已加载的 dex 放入新数组中
        System.arraycopy(addElements,0,newElements,dexElements.length,addElements.length);

        //4. 将合并的新数组重新设置给 DexPathList的 dexElements
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

    /**
     * 开始替换application
     */
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Toast.makeText(getApplicationContext(),"解密完成! 共耗时：" + endTime +" ms" ,Toast.LENGTH_LONG).show();
            bindRealApplicatin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    boolean isBindReal;
    Application delegate;


    private void bindRealApplicatin() throws Exception {
        if (isBindReal) {
            return;
        }
        if (TextUtils.isEmpty(app_name)) {
            return;
        }
        //1. 得到 attachBaseContext(context) 传入的上下文 ContextImpl
        Context baseContext = getBaseContext();
        //2. 拿到真实 APK APPlication 的 class
        Class<?> delegateClass = Class.forName(app_name);
        //3. 反射实例化，其实 Android 中四大组件都是这样实例化的。
        delegate = (Application) delegateClass.newInstance();

        //3.1 得到 Application attach() 方法 也就是最先初始化的
        Method attach = Application.class.getDeclaredMethod("attach", Context.class);
        attach.setAccessible(true);
        //执行 Application#attach(Context)
        //3.2 将真实的 Application 和假的 Application 进行替换。想当于自己手动控制 真实的 Application 生命周期
        attach.invoke(delegate, baseContext);


//        ContextImpl---->mOuterContext(app)   通过Application的attachBaseContext回调参数获取
        //4. 拿到 Context 的实现类
        Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
        //4.1 获取 mOuterContext Context 属性
        Field mOuterContextField = contextImplClass.getDeclaredField("mOuterContext");
        mOuterContextField.setAccessible(true);
        //4.2 将真实的 Application 交于 Context 中。这个根据源码执行，实例化 Application 下一个就行调用 setOuterContext 函数，所以需要绑定 Context
        //  app = mActivityThread.mInstrumentation.newApplication(
        //                    cl, appClass, appContext);
        //  appContext.setOuterContext(app);
        mOuterContextField.set(baseContext, delegate);

//        ActivityThread--->mAllApplications(ArrayList)       ContextImpl的mMainThread属性
        //5. 拿到 ActivityThread 变量
        Field mMainThreadField = contextImplClass.getDeclaredField("mMainThread");
        mMainThreadField.setAccessible(true);
        //5.1 拿到 ActivityThread 对象
        Object mMainThread = mMainThreadField.get(baseContext);

//        ActivityThread--->>mInitialApplication
        //6. 反射拿到 ActivityThread class
        Class<?> activityThreadClass=Class.forName("android.app.ActivityThread");
        //6.1 得到当前加载的 Application 类
        Field mInitialApplicationField = activityThreadClass.getDeclaredField("mInitialApplication");
        mInitialApplicationField.setAccessible(true);
        //6.2 将 ActivityThread 中的 Applicaiton 替换为 真实的 Application 可以用于接收相应的声明周期和一些调用等
        mInitialApplicationField.set(mMainThread,delegate);


//        ActivityThread--->mAllApplications(ArrayList)       ContextImpl的mMainThread属性
        //7. 拿到 ActivityThread 中所有的 Application 集合对象，这里是多进程的场景
        Field mAllApplicationsField = activityThreadClass.getDeclaredField("mAllApplications");
        mAllApplicationsField.setAccessible(true);
        ArrayList<Application> mAllApplications =(ArrayList<Application>) mAllApplicationsField.get(mMainThread);
        //7.1 删除 ProxyApplication
        mAllApplications.remove(this);
        //7.2 添加真实的 Application
        mAllApplications.add(delegate);

//        LoadedApk------->mApplication                      ContextImpl的mPackageInfo属性
        //8. 从 ContextImpl 拿到 mPackageInfo 变量
        Field mPackageInfoField = contextImplClass.getDeclaredField("mPackageInfo");
        mPackageInfoField.setAccessible(true);
        //8.1 拿到 LoadedApk 对象
        Object mPackageInfo=mPackageInfoField.get(baseContext);

        //9 反射得到 LoadedApk 对象
        //    @Override
        //    public Context getApplicationContext() {
        //        return (mPackageInfo != null) ?
        //                mPackageInfo.getApplication() : mMainThread.getApplication();
        //    }
        Class<?> loadedApkClass=Class.forName("android.app.LoadedApk");
        Field mApplicationField = loadedApkClass.getDeclaredField("mApplication");
        mApplicationField.setAccessible(true);
        //9.1 将 LoadedApk 中的 Application 替换为 真实的 Application
        mApplicationField.set(mPackageInfo,delegate);

        //修改ApplicationInfo className   LooadedApk

        //10. 拿到 LoadApk 中的 mApplicationInfo 变量
        Field mApplicationInfoField = loadedApkClass.getDeclaredField("mApplicationInfo");
        mApplicationInfoField.setAccessible(true);
        //10.1 根据变量反射得到 ApplicationInfo 对象
        ApplicationInfo mApplicationInfo = (ApplicationInfo)mApplicationInfoField.get(mPackageInfo);
        //10.2 将我们真实的 APPlication ClassName 名称赋值于它
        mApplicationInfo.className=app_name;

        //11. 执行 代理 Application onCreate 声明周期
        delegate.onCreate();

        //解码完成
        isBindReal = true;
    }

    /**
     * 让代码走入if中的第三段中
     * @return
     */
    @Override
    public String getPackageName() {
        if(!TextUtils.isEmpty(app_name)){
            return "";
        }
        return super.getPackageName();
    }

    /**
     * 这个函数是如果在 AndroidManifest.xml 中定义了 ContentProvider 那么就会执行此处 : installProvider，简介调用该函数
     * @param packageName
     * @param flags
     * @return
     * @throws PackageManager.NameNotFoundException
     */
    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        if(TextUtils.isEmpty(app_name)){
            return super.createPackageContext(packageName, flags);
        }
        try {
            bindRealApplicatin();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return delegate;

    }
}








