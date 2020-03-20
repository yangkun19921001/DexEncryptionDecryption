package com.example.proxy_tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

public class Main {

    /**
     * 制作 Dex 命令
     */
    private static String DX_PATH = "/Users/devyk/Data/Android/SDK/build-tools/26.0.1/dx --dex --output ";

    /**
     * 制作 对齐 命令
     */
    private static String ZIPALIGN = "/Users/devyk/Data/Android/SDK/build-tools/26.0.1/zipalign -v -p  4 ";

    /**
     * 制作 签名打包 命令
     */
    private static String APKSIGNER = "/Users/devyk/Data/Android/SDK/build-tools/26.0.1/apksigner sign --ks ";

    /**
     * 记录执行制作 dex 的次数，第一次执行成功但是没有生成
     */
    private static int count = 0;

    public static void main(String[] args) throws Exception {


        /**
         * 1.制作只包含解密代码的dex文件
         */
        makeDecodeDex();

        /**
         * 2.加密APK中所有的dex文件
         */
        encryptApkAllDex();

        /**
         * 3.把dex放入apk解压目录，重新压成apk文件
         */
        makeApk();


        /**
         * 4.对齐
         */
        zipalign();

        /**
         * 5. 签名打包
         */
        jksToApk();
    }


    /**
     * 1.制作只包含解密代码的dex文件
     */
    public static void makeDecodeDex() throws IOException, InterruptedException {
        if (count >= 2) return;
        File aarFile = new File("proxy_core/build/outputs/aar/proxy_core-debug.aar");
        File aarTemp = new File("proxy_tools/temp");
        Zip.unZip(aarFile, aarTemp);
        File classesJar = new File(aarTemp, "classes.jar");
        File classesDex = new File(aarTemp, "classes.dex");
        //dx --dex --output out.dex in.jar
        //dx --dex --output D:\Downloads\android_space\DexDEApplication\proxy_tools\temp\classes.dex D:\Downloads\android_space\DexDEApplication\proxy_tools\temp\classes.jar
//       Windows 执行
//       Process process = Runtime.getRuntime().exec("cmd /c dx --dex --output " + classesDex.getAbsolutePath()
        //MAC 执行
        Process process = Runtime.getRuntime().exec(DX_PATH + classesDex.getAbsolutePath()
                + " " + classesJar.getAbsolutePath());
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error");
        }

        if (!classesDex.exists()) makeDecodeDex();
        System.out.println("makeDecodeDex--ok");
        count++;
    }

    /**
     * 2.加密APK中所有的dex文件
     */
    public static void encryptApkAllDex() throws Exception {
        File apkFile = new File("app/build/outputs/apk/debug/app-debug.apk");
        File apkTemp = new File("app/build/outputs/apk/debug/temp");
        Zip.unZip(apkFile, apkTemp);
        //只要dex文件拿出来加密
        File[] dexFiles = apkTemp.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".dex");
            }
        });
        //AES加密了
//        AES.init(AES.DEFAULT_PWD);
        for (File dexFile : dexFiles) {
            byte[] bytes = DexUtils.getBytes(dexFile);
            byte[] encrypt = EncryptUtil.encrypt(bytes, EncryptUtil.ivBytes);
            FileOutputStream fos = new FileOutputStream(new File(apkTemp,
                    "secret-" + dexFile.getName()));
            fos.write(encrypt);
            fos.flush();
            fos.close();
            dexFile.delete();

        }
        System.out.println("encryptApkAllDex--ok");
    }

    /**
     * 3.把dex放入apk解压目录，重新压成apk文件
     */
    private static void makeApk() throws Exception {
        File apkTemp = new File("app/build/outputs/apk/debug/temp");
        File aarTemp = new File("proxy_tools/temp");
        File classesDex = new File(aarTemp, "classes.dex");
        classesDex.renameTo(new File(apkTemp, "classes.dex"));
        File unSignedApk = new File("app/build/outputs/apk/debug/app-unsigned.apk");
        Zip.zip(apkTemp, unSignedApk);
        System.out.println("makeApk--ok");
    }

    /**
     * 4. 对齐
     */
    private static void zipalign() throws IOException, InterruptedException {
        File unSignedApk = new File("app/build/outputs/apk/debug/app-unsigned.apk");
        // zipalign -v -p 4 my-app-unsigned.apk my-app-unsigned-aligned.apk
        File alignedApk = new File("app/build/outputs/apk/debug/app-unsigned-aligned.apk");
        //Windows 执行
//        Process process = Runtime.getRuntime().exec("cmd /c zipalign -v -p  4 " + unSignedApk.getAbsolutePath()
        //MAC 执行
        Process process = Runtime.getRuntime().exec(ZIPALIGN + unSignedApk.getAbsolutePath()
                + " " + alignedApk.getAbsolutePath());
        process.waitFor();

        //zipalign -v -p 4 D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-unsigned.apk D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-unsigned-aligned.apk
//        System.out.println(process.waitFor() == 0 ? "zipalign成功" : "zipalign失败");

        System.out.println("zipalign---ok");
    }

    /**
     * 签名 打包
     *
     * @throws IOException
     */
    public static void jksToApk() throws IOException, InterruptedException {
        // apksigner sign --ks my-release-key.jks --out my-app-release.apk my-app-unsigned-aligned.apk
        //apksigner sign  --ks jks文件地址 --ks-key-alias 别名 --ks-pass pass:jsk密码 --key-pass pass:别名密码 --out  out.apk in.apk
        File signedApk = new File("app/build/outputs/apk/debug/app-signed-aligned.apk");
        File jks = new File("proxy_tools/dexjks.jks");
        File alignedApk = new File("app/build/outputs/apk/debug/app-unsigned-aligned.apk");
        //apksigner sign --ks D:\Downloads\android_space\DexDEApplication\proxy_tools\dexjks.jks --ks-key-alias yangkun --ks-pass pass:123123 --key-pass pass:123123 --out D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-signed-aligned.apk D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-unsigned-aligned.apk
        //apksigner sign --ks my-release-key.jks --out my-app-release.apk my-app-unsigned-aligned.apk

        //Windows 执行
//        Process process = Runtime.getRuntime().exec("cmd /c  apksigner sign --ks " + jks.getAbsolutePath()
        //MAC 执行
        Process process = Runtime.getRuntime().exec(APKSIGNER + jks.getAbsolutePath()
                + " --ks-key-alias yangkun --ks-pass pass:123123 --key-pass pass:123123 --out "
                + signedApk.getAbsolutePath() + " " + alignedApk.getAbsolutePath());
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error");
        }
        System.out.println("打包成功->" + signedApk.getAbsolutePath());
    }
}
