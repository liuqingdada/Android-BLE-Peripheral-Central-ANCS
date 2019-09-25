# Android-BLE-Peripheral-Central-ANCS
Android Bluetooth Low Energy (BLE) 快速开发框架。分为外设（Peripheral）、中心服务（Central）和ANCS三部分；另外为了方便消息传输，提供了消息拆分包的简单实现；解决外设（Peripheral）和中心服务（Central）传输丢包问题。



## 一、中心服务

这一部分，入坑接近一年了，确实有好多坑。

一般为手机来扫描周边设备，具体步骤为扫描设备，连接设备，发现服务。

有可能碰到的问题：扫不到设备？GATT 133？  GATT 19？

对于这部分，博客也好，git开源的库都相当多了，可以以他们为基础，继续优化即可。推荐使用FastBle来进行连接、读写等操作的管理。另外，扫描这一部分，单独摘出来做优化，可参考 Nordic 的 [scanner 兼容库](https://github.com/NordicSemiconductor/Android-Scanner-Compat-Library)

* **扫描**

  * 1. 基本使用

       开始扫描，推荐使用这个设置，REPORT_DELAY 为500ms

       ```kotlin
       fun startLeCompatScan() {
           if (bluetoothAdapter.isEnabled) {
               val settings = ScanSettings.Builder()
                   .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                   .setReportDelay(REPORT_DELAY)
                   .setUseHardwareBatchingIfSupported(false)
                   .build()

               val scanner = BluetoothLeScannerCompat.getScanner()
               scanner.startScan(null, settings, scanCallback)
               LogUtil.d(TAG, "startLeCompatScan: ")
           }
       }
       ```

       停止扫描更简单了

       ```kotlin
       fun stopLeCompatScan() {
           val scanner = BluetoothLeScannerCompat.getScanner()
           scanner.stopScan(scanCallback)
           LogUtil.d(TAG, "stopLeCompatScan: ")
       }
       ```

       实践经验：注册蓝牙状态广播，蓝牙Closing的时候，最好调用一次停止扫描。

       Scanner的lib不要混淆，全部Keep住。

       权限问题：扫描之前，我们要有定位权限，有些手机需要打开定位服务。

       是否要打开定位服务呢？如果返回false，让用户手动开启GPS即可

       ```java
       public static boolean isLocationServiceAllowed(Context context) {
           if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               int locationMode = Settings.Secure.LOCATION_MODE_OFF;
               try {
                   locationMode = Settings.Secure.getInt(context.getContentResolver(),
                           Settings.Secure.LOCATION_MODE);
               } catch (final Settings.SettingNotFoundException e) {
                   // do nothing
               }
               return locationMode != Settings.Secure.LOCATION_MODE_OFF;
           }
           return true;
       }
       ```

    2. GATT_Register: can't Register GATT client, MAX client

       这个是蓝牙进程的LOG，可以过滤一下，定位问题

       在我们APP扫描的回调中，会收到这个error code： SCAN_FAILED_APPLICATION_REGISTRATION_FAILED

       造成这个问题的原因有许多：

       * 多次打开App/退出App/后台被杀等

       * 其他进程注册Ble Scanner，比如我遇到的PUSH服务（个推），Google的FastPair，小米、华为等等他们的系统都会有进程来注册Scanner。

       总之，Scanner到达上限，就无法扫描设备了。那么最暴力的方法当然是重开蓝牙，但是，问题是这样会影响用户体验，万一还有其他设备连接着呢，比如听歌。

       这时候，来看一下FW源码：

       android.bluetooth.le.BluetoothLeScanner.java 有一个 inner class BleScanCallbackWrapper

       ```java
       public void startRegisteration() {
         synchronized (this) {
           // Scan stopped.
           if (mClientIf == -1) return;
           try {
             UUID uuid = UUID.randomUUID();
             mBluetoothGatt.registerClient(new ParcelUuid(uuid), this);
             wait(REGISTRATION_CALLBACK_TIMEOUT_MILLIS);
           } catch (InterruptedException | RemoteException e) {
             Log.e(TAG, "application registeration exception", e);
             postCallbackError(mScanCallback, ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
           }
           if (mClientIf > 0) {
             mLeScanClients.put(mScanCallback, this);
           } else {
             // Registration timed out or got exception, reset clientIf to -1 so no
             // subsequent operations can proceed.
             if (mClientIf == 0) mClientIf = -1;
             postCallbackError(mScanCallback,
                               ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED);
           }
         }
       }

       public void stopLeScan() {
         synchronized (this) {
           if (mClientIf <= 0) {
             Log.e(TAG, "Error state, mLeHandle: " + mClientIf);
             return;
           }
           try {
             mBluetoothGatt.stopScan(mClientIf, false);
             mBluetoothGatt.unregisterClient(mClientIf);
           } catch (RemoteException e) {
             Log.e(TAG, "Failed to stop scan and unregister", e);
           }
           mClientIf = -1;
         }
       }
       ```

       问题的切入点就在这个地方

       * 一个是registerClient，系统也很暴力，等待两秒。。。也就是这里回调的注册失败。

       * 另一个是停止扫描并且unregisterClient。
       * 那么MAX client的问题很有可能就是App注册后被杀，系统没有回收这个Client

       问题解决：我们开始扫描三秒后如果注册成功，一定能拿到这个ScannerId（ ClientIf ），保存起来，我们的扫描Service刚创建的时候反射一下unregisterClient。实际开发中，很有效果。这部分代码已上传到工程[BleCompatibility目录下](https://github.com/liuqingdada/Android-BLE-Peripheral-Central-ANCS/tree/master/BleCompatibility), 另外还有GattServer的源码，感觉这才是精髓。

       其他坑：安卓8.0之后，这个地方的方法名变了(unregisterScanner)，并且ClientIf变成了ScannerId。所以我们需要反射这个新的方法，做一下版本适配。目前安卓9.0上已测试通过。

