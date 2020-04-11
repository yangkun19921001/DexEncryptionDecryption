package com.example.proxy_tools;

import com.gmail.yang1001yk.utils.DexUtils;
import com.gmail.yang1001yk.utils.EncryptUtil;
import com.gmail.yang1001yk.utils.Zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

public class Main {
    //Ref : https://stackoverflow.com/questions/228477/how-do-i-programmatically-determine-operating-system-in-java
    //FIXME  !!!  fix path as your local PATH
    //TODO
    /**
     * 制作 Dex 命令
     */
    private static String DX_PATH = "/Users/devyk/Data/Android/SDK/build-tools/29.0.2/dx --dex --output ";

    /**
     * 制作 对齐 命令
     */
    private static String ZIPALIGN = "/Users/devyk/Data/Android/SDK/build-tools/29.0.2/zipalign -v -p  4 ";

    /**
     * 制作 签名打包 命令
     */
    private static String APKSIGNER = "/Users/devyk/Data/Android/SDK/build-tools/29.0.2/apksigner sign --ks ";

    /**
     * JAVA_HOME
     */
    private static String JAVA_HOME = "/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home/bin/";

    /**
     * 记录执行制作 dex 的次数，第一次执行成功但是没有生成
     */
    private static int count = 0;
    private static boolean isWindwos = false;

    public static void main(String[] args) throws Exception {
//        System.getProperties().list(System.out);
        final String osname = System.getProperty("os.name").toLowerCase();
        if (osname.contains("windows")) {
            isWindwos = true;
            DX_PATH = "D:\\SDK\\build-tools\\29.0.3\\dx --dex --output ";
            ZIPALIGN = "D:\\SDK\\build-tools\\29.0.3\\zipalign -v -p  4 ";
            APKSIGNER = "D:\\SDK\\build-tools\\29.0.3\\apksigner sign --ks ";
        }

        /**
         * 1. 多个 jar 合并在一起，目的是制作 dex 文件
         */
        mergeJar();

        /**
         * 2.制作只包含解密代码的dex文件
         */
        makeDecodeDex("proxy_tools/temp/classes.jar", "classes.jar", "proxy_tools/temp/", "classes.dex");

        /**
         * 3.加密APK中所有的dex文件
         */
        encryptApkAllDex();

        /**
         * 4.把dex放入apk解压目录，重新压成apk文件
         */
        makeApk();
        /**
         * 5.对齐
         */
        zipalign();
        /**
         * 6. 签名打包
         */
        jksToApk();
    }

    /**
     * 这一步的目的是因为打包 utils 的时候没有将 utils.jar makeDex ,
     * 所以我们不能直接加载 class 的 jar ，而是需要将 class 转化成 dex 字节码。
     * 不然 utils 包下的 class 都会报 ClassNotFoundException
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private static void mergeJar() throws IOException, InterruptedException {
        System.out.println("start merge jar");
        File file = new File("proxy_tools/temp/");
        if (file.exists())
            file.delete();
        file.mkdir();
        /**
         * 1.1 解压 utils
         */
        unZipFile("utils/build/libs/utils-0.1.0.jar", "proxy_tools/temp/");
        /**
         * 1.2. 解压 core 库
         */
        unZipFile("proxy_core/build/outputs/aar/proxy_core-debug.aar", "proxy_tools/temp/");


        /**
         * 1.3 解压核心 jar 目的是为了将 core jar 和 工具包 jar 合并一个 jar
         */
        unZipFile("proxy_tools/temp/classes.jar", "proxy_tools/temp/");

        /**
         * 1.4 解压完成，需要删除 jar ,目的为了合并
         */
        Zip.deleteFile(new File("proxy_tools/temp/classes.jar"));

        /**
         * 1.5 开始合并
         * 第三个参数指定在 temp 下执行打包
         *
         */
        File curLocation = new File("proxy_tools/temp");
        final Process process = Runtime.getRuntime().exec(JAVA_HOME + "jar -cvfM classes.jar .", null, curLocation.getAbsoluteFile());
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("make jar error");
        }
        System.out.println("merge jar successful!");
    }

    private static void unZipFile(String jarPath, String tempFilePath) throws FileNotFoundException {
        File unZipFile = new File(jarPath);
        File tempFile = new File(tempFilePath);
        checkFile(unZipFile, tempFile);
        Zip.unZip(unZipFile, tempFile, false);
    }

    private static void checkFile(File unZipFile, File tempFile) throws FileNotFoundException {
        if (!unZipFile.exists() || !tempFile.exists())
            throw new FileNotFoundException(unZipFile.getAbsolutePath() + " or " + tempFile.getAbsolutePath());
    }


    /**
     * 1.制作只包含解密代码的dex文件
     */
    public static void makeDecodeDex(String jarPath, String jarName, String dexPath, String dexName) throws IOException, InterruptedException {
        if (count >= 2) return;
        System.out.println("makeDecodeDex start");
        File classesJar = new File(jarPath);
        File classesDex = new File(dexPath);
        checkFile(classesJar, classesDex);
        //dx --dex --output out.dex in.jar
        //dx --dex --output D:\Downloads\android_space\DexDEApplication\proxy_tools\temp\classes.dex D:\Downloads\android_space\DexDEApplication\proxy_tools\temp\classes.jar
        //       Windows 执行
//         Process process = Runtime.getRuntime().exec("cmd /c dx --dex --output " + classesDex.getAbsolutePath()+ " " + classesJar.getAbsolutePath());
        //MAC 执行
//        Process process = Runtime.getRuntime().exec(DX_PATH + classesDex.getAbsolutePath() + " " + classesJar.getAbsolutePath());
        String args = classesDex.getAbsolutePath() + File.separator + dexName + " " + classesJar.getAbsolutePath();
        String command = DX_PATH + args;
        if (isWindwos) {
            command = "cmd /c " + DX_PATH + args;
        }
        final Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error");
        }
        if (!classesDex.exists()) makeDecodeDex(jarPath, jarName, dexPath, dexPath);
        System.out.println("makeDecodeDex--ok");
        count++;
    }

    /**
     * 2.加密APK中所有的dex文件
     */
    public static void encryptApkAllDex() throws Exception {
        System.out.println("encryptApkAllDex start");
        File apkFile = new File("app/build/outputs/apk/debug/app-debug.apk");
        File apkTemp = new File("app/build/outputs/apk/debug/temp");
        Zip.unZip(apkFile, apkTemp, true);
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
        System.out.println("makeApk start");
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
        System.out.println("zipalign start");
        File unSignedApk = new File("app/build/outputs/apk/debug/app-unsigned.apk");
        // zipalign -v -p 4 my-app-unsigned.apk my-app-unsigned-aligned.apk
        File alignedApk = new File("app/build/outputs/apk/debug/app-unsigned-aligned.apk");
//zipalign -v -p 4 D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-unsigned.apk D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-unsigned-aligned.apk
        //Windows 执行
//        Process process = Runtime.getRuntime().exec("cmd /c zipalign -v -p  4 " + unSignedApk.getAbsolutePath()+ " " + alignedApk.getAbsolutePath());
        //MAC 执行
//        Process process = Runtime.getRuntime().exec(ZIPALIGN + unSignedApk.getAbsolutePath() + " " + alignedApk.getAbsolutePath());
        final String args = unSignedApk.getAbsolutePath() + " " + alignedApk.getAbsolutePath();
        String command = ZIPALIGN + args;
        if (isWindwos) {
            command = "cmd /c " + ZIPALIGN + args;
        }
        final Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
//        System.out.println(process.waitFor() == 0 ? "zipalign成功" : "zipalign失败");
        System.out.println("zipalign---ok");
    }

    /**
     * 签名 打包
     *
     * @throws IOException
     */
    public static void jksToApk() throws IOException, InterruptedException {
        System.out.println("jksToApk start");
        // apksigner sign --ks my-release-key.jks --out my-app-release.apk my-app-unsigned-aligned.apk
        //apksigner sign  --ks jks文件地址 --ks-key-alias 别名 --ks-pass pass:jsk密码 --key-pass pass:别名密码 --out  out.apk in.apk
        File signedApk = new File("app/release/app-signed-aligned.apk");
        if (!signedApk.getParentFile().exists()) {
            signedApk.getParentFile().mkdir();
        }
        File jks = new File("proxy_tools/dexjks.jks");
        File alignedApk = new File("app/build/outputs/apk/debug/app-unsigned-aligned.apk");
        //apksigner sign --ks D:\Downloads\android_space\DexDEApplication\proxy_tools\dexjks.jks --ks-key-alias yangkun --ks-pass pass:123123 --key-pass pass:123123 --out D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-signed-aligned.apk D:\Downloads\android_space\DexDEApplication\app\build\outputs\apk\debug\app-unsigned-aligned.apk
        //apksigner sign --ks my-release-key.jks --out my-app-release.apk my-app-unsigned-aligned.apk
        //Windows 执行
//        Process process = Runtime.getRuntime().exec("cmd /c  apksigner sign --ks " + jks.getAbsolutePath()
        //MAC 执行
//        Process process = Runtime.getRuntime().exec(APKSIGNER + jks.getAbsolutePath() + " --ks-key-alias yangkun --ks-pass pass:123123 --key-pass pass:123123 --out " + signedApk.getAbsolutePath() + " " + alignedApk.getAbsolutePath());
        final String args = jks.getAbsolutePath() + " --ks-key-alias yangkun --ks-pass pass:123123 --key-pass pass:123123 --out " + signedApk.getAbsolutePath() + " " + alignedApk.getAbsolutePath();
        String command = APKSIGNER + args;
        if (isWindwos) {
            command = "cmd /c  " + APKSIGNER + args;
        }
        final Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error");
        }
        System.out.println("打包成功->" + signedApk.getAbsolutePath());
    }
}
