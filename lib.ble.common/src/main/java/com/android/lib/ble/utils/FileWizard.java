package com.android.lib.ble.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.misc.Cleaner;
import sun.nio.ch.FileChannelImpl;

/**
 * Created by suhen
 * 18-1-16.
 * Email: 1239604859@qq.com
 */

public class FileWizard {
    /**
     * 适用于非频繁操作, 小点的文件
     */
    public static void readFile(String path, StringCallback callback) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(path), "r");
        String s;
        while ((s = raf.readLine()) != null) {
            if (callback != null) {
                callback.onReadLine(s);
            }
        }
        raf.close();
    }

    public interface StringCallback {
        void onReadLine(String line);
    }

    ///////////////////////////////////////////////////////

    /**
     * 效率有点低, 但适合大文件
     * 一行一行读取文件
     */
    public static void readBigFile(String path, BytesCallback callback) throws IOException {
        // 指定读取文件所在位置
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            System.out.println("this path is not a file, or file is not exsist");
        }
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel fileChannel = raf.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        // 使用temp字节数组用于存储不完整的行的内容
        byte[] temp = new byte[0];
        while (fileChannel.read(byteBuffer) != -1) {
            byte[] bs = new byte[byteBuffer.position()];
            byteBuffer.flip();
            byteBuffer.get(bs);
            byteBuffer.clear();
            int startNum = 0;
            // 判断是否出现了换行符，注意这要区分LF-\n,CR-\r,CRLF-\r\n,这里判断\n
            boolean isNewLine = false;
            for (int i = 0; i < bs.length; i++) {
                if (bs[i] == 10) {
                    isNewLine = true;
                    startNum = i;
                }
            }

            if (isNewLine) {
                // 如果出现了换行符，将temp中的内容与换行符之前的内容拼接
                byte[] toTemp = new byte[temp.length + startNum];
                System.arraycopy(temp, 0, toTemp, 0, temp.length);
                System.arraycopy(bs, 0, toTemp, temp.length, startNum);
                //System.out.println(new String(toTemp));
                if (callback != null) {
                    callback.onReadLine(toTemp);
                }
                // 将换行符之后的内容(去除换行符)存到temp中
                temp = new byte[bs.length - startNum - 1];
                System.arraycopy(bs, startNum + 1, temp, 0, bs.length - startNum - 1);
                // 使用return即为单行读取，不打开即为全部读取
                //return;
            } else {
                //如果没出现换行符，则将内容保存到temp中
                byte[] toTemp = new byte[temp.length + bs.length];
                System.arraycopy(temp, 0, toTemp, 0, temp.length);
                System.arraycopy(bs, 0, toTemp, temp.length, bs.length);
                temp = toTemp;
            }

        }
        if (temp.length > 0) {
            //System.out.println(new String(temp));
            if (callback != null) {
                callback.onReadLine(temp);
            }
        }
        fileChannel.close();
        raf.close();
    }

    public interface BytesCallback {
        void onReadLine(byte[] bytes);
    }

    ///////////////////////////////////////////////////////

    public static class StreamFileReader {
        private BufferedInputStream fileIn;
        private long fileLength;
        private int arraySize;
        private byte[] array;

        public StreamFileReader(String fileName, int arraySize) throws IOException {
            this.fileIn = new BufferedInputStream(new FileInputStream(fileName), arraySize);
            this.fileLength = fileIn.available();
            this.arraySize = arraySize;
        }

        public int read() throws IOException {
            byte[] tmpArray = new byte[arraySize];
            int bytes = fileIn.read(tmpArray);// 暂存到字节数组中
            if (bytes != -1) {
                array = new byte[bytes];// 字节数组长度为已读取长度
                System.arraycopy(tmpArray, 0, array, 0, bytes);// 复制已读取数据
                return bytes;
            }
            return -1;
        }

        public void close() throws IOException {
            fileIn.close();
            array = null;
        }

        public byte[] getArray() {
            return array;
        }

        public long getFileLength() {
            return fileLength;
        }
    }

    ///////////////////////////////////////////////////////

    public static class ChannelFileReader {
        private FileInputStream fileIn;
        private ByteBuffer byteBuf;
        private long fileLength;
        private byte[] array;

        public ChannelFileReader(String fileName, int arraySize) throws IOException {
            this.fileIn = new FileInputStream(fileName);
            this.fileLength = fileIn.getChannel()
                                    .size();
            this.byteBuf = ByteBuffer.allocate(arraySize);
        }

        public int read() throws IOException {
            FileChannel fileChannel = fileIn.getChannel();
            int bytes = fileChannel.read(byteBuf);// 读取到ByteBuffer中
            if (bytes != -1) {
                array = new byte[bytes];// 字节数组长度为已读取长度
                byteBuf.flip();
                byteBuf.get(array);// 从ByteBuffer中得到字节数组
                byteBuf.clear();
                return bytes;
            }
            return -1;
        }

        public void close() throws IOException {
            fileIn.close();
            array = null;
        }

        public byte[] getArray() {
            return array;
        }

        public long getFileLength() {
            return fileLength;
        }
    }

    ///////////////////////////////////////////////////////

    /**
     * 这个方法仍然不能读取虚拟内存比文件小的情况; {@link MappedBiggerFileReader}
     * MappedByteBuffer doesn't support reading lines
     */
    public static class MappedFileReader {
        private FileInputStream fileIn;
        private MappedByteBuffer mappedBuf;
        private long fileLength;
        private int arraySize;
        private byte[] array;

        public MappedFileReader(String fileName, int arraySize) throws IOException {
            this.fileIn = new FileInputStream(fileName);
            FileChannel fileChannel = fileIn.getChannel();
            this.fileLength = fileChannel.size();
            this.mappedBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
            this.arraySize = arraySize;
        }

        public int read() {
            int limit = mappedBuf.limit();
            int position = mappedBuf.position();
            if (position == limit) {
                return -1;
            }
            if (limit - position > arraySize) {
                array = new byte[arraySize];
                mappedBuf.get(array);
                return arraySize;
            } else {// 最后一次读取数据
                array = new byte[limit - position];
                mappedBuf.get(array);
                return limit - position;
            }
        }

        public void close() throws IOException {
            clean(mappedBuf);
            unmap(mappedBuf);
            fileIn.close();
            array = null;
        }

        public byte[] getArray() {
            return array;
        }

        public long getFileLength() {
            return fileLength;
        }
    }

    ///////////////////////////////////////////////////////

    /**
     * 如果文件比较大的话可以分段进行映射
     * MappedByteBuffer doesn't support reading lines
     */
    public static class MappedBiggerFileReader {
        private MappedByteBuffer[] mappedBufArray;
        private int count = 0;
        private int number;
        private FileInputStream fileIn;
        private long fileLength;
        private int arraySize;
        private byte[] array;

        public MappedBiggerFileReader(String fileName, int arraySize) throws IOException {
            this(fileName, arraySize, 0);
        }

        public MappedBiggerFileReader(String fileName, int arraySize, long position)
                throws IOException {
            if (0 > position) {
                throw new RuntimeException("file position must > 0");
            }

            this.fileIn = new FileInputStream(fileName);
            FileChannel fileChannel = fileIn.getChannel();
            this.fileLength = fileChannel.size() - position; // 读取的长度

            long map_block = 1024 * 1024 * 1024;
            //long map_block = (long) Integer.MAX_VALUE; // 自定义映射大小

            this.number = (int) Math.ceil((double) fileLength / (double) map_block);
            this.mappedBufArray = new MappedByteBuffer[number]; // 内存文件映射数组

            long preLength = 0;
            long regionSize = map_block;
            for (int i = 0; i < number; i++) { // 将文件的连续区域映射到内存文件映射数组中
                if (fileLength - preLength < map_block) {
                    regionSize = fileLength - preLength; // 最后一片区域的大小
                }
                mappedBufArray[i] = fileChannel.map(FileChannel.MapMode.READ_ONLY,
                                                    preLength + position,
                                                    regionSize);
                preLength += regionSize; // 下一片区域的开始
            }
            this.arraySize = arraySize;
        }

        public int read() {
            if (count >= number) {
                return -1;
            }
            int limit = mappedBufArray[count].limit();
            int position = mappedBufArray[count].position();
            if (limit - position > arraySize) {
                array = new byte[arraySize];
                mappedBufArray[count].get(array);
                return arraySize;
            } else { // 本内存文件映射最后一次读取数据
                array = new byte[limit - position];
                mappedBufArray[count].get(array);
                if (count < number) {
                    count++; // 转换到下一个内存文件映射
                }
                return limit - position;
            }
        }

        public void close() throws IOException {
            for (MappedByteBuffer buffer : mappedBufArray) {
                clean(buffer);
                unmap(buffer);
            }
            fileIn.close();
            array = null;
        }

        public byte[] getArray() {
            return array;
        }

        public long getFileLength() {
            return fileLength;
        }
    }

    ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////

    public static void writeFile(String filePath, String fileName, byte[] bytes)
            throws IOException {
        writeFile(filePath, fileName, bytes, true, false, 0);
    }

    public static void writeFileAppend(String filePath, String fileName, byte[] bytes)
            throws IOException {
        writeFile(filePath, fileName, bytes, false, false, 0);
    }

    /**
     * 多功能写文件处理函数
     *
     * @param iSwipe   true: wipe all existing data, and write the bytes;
     *                 false: next param is effective.
     * @param isInsert true: skip param is effective, and insert bytes to file;
     *                 false: added bytes to the end of file.
     * @param skip     the file position.
     */
    public static void writeFile(String filePath, String fileName, byte[] bytes, boolean iSwipe,
                                 boolean isInsert, long skip) throws IOException {
        File dir = new File(filePath);
        if (!dir.exists()) { // 判断文件目录是否存在
            boolean mkdirs = dir.mkdirs();
            System.out.println("创建目录" + mkdirs);
        }
        File file = new File(filePath, fileName);

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        if (iSwipe) { // 覆盖写入
            raf.setLength(bytes.length);
            raf.write(bytes);
            raf.close();
            return;
        }

        if (isInsert) { // 从指定位置插入内容
            if (skip < 0 || skip > raf.length() || bytes.length + raf.length() > Long.MAX_VALUE) {
                System.out.println("跳过字节数无效");
                return;
            }
            raf.setLength(raf.length() + bytes.length);

            for (long i = raf.length() - 1; i > bytes.length + skip - 1; i--) {
                raf.seek(i - bytes.length);
                byte temp = raf.readByte();
                raf.seek(i);
                raf.writeByte(temp);
            }
            raf.seek(skip);
            raf.write(bytes);
            raf.close();
            return;
        }

        // 追加文件内容
        if (bytes.length + raf.length() > Long.MAX_VALUE) {
            System.out.println("文件超过: " + Long.MAX_VALUE);
            return;
        }
        raf.seek(raf.length());
        raf.setLength(raf.length() + bytes.length);
        raf.write(bytes);
        raf.close();
    }

    ///////////////////////////////////////////////////////

    /**
     * create file and write bytes.
     * it will wipe existing data.
     * write big data, use this.
     */
    public static class MappedWriter {
        private FileChannel rwChannel;
        private MappedByteBuffer[] mappedBufArray;
        private int number;
        private int count;

        public MappedWriter(String filePath, String fileName, long size) throws IOException {
            this(filePath, fileName, size, 0, true);
        }

        public MappedWriter(String filePath, String fileName, long size, long position,
                            boolean cover) throws IOException {
            if (0 > position) {
                throw new RuntimeException("file position must > 0");
            }

            if (0 > size) {
                throw new RuntimeException("file size must > 0");
            }

            File dir = new File(filePath);
            if (!dir.exists()) { // 判断文件目录是否存在
                boolean mkdirs = dir.mkdirs();
                System.out.println("创建目录" + mkdirs);
            }
            long map_block = 1024 * 1024 * 1024;
            //long map_block = (long) Integer.MAX_VALUE; // 自定义映射大小

            this.number = (int) Math.ceil((double) size / (double) map_block);
            this.mappedBufArray = new MappedByteBuffer[number]; // 内存文件映射数组

            File file = new File(filePath, fileName);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            if (cover) {
                raf.setLength(0);
            }
            rwChannel = raf.getChannel();

            long preLength = 0;
            long regionSize = map_block; // 映射区域的大小
            for (int i = 0; i < number; i++) {
                if (size - preLength < regionSize) {
                    regionSize = size - preLength; // 最后一片区域的大小
                }

                mappedBufArray[i] = rwChannel.map(FileChannel.MapMode.READ_WRITE,
                                                  preLength + position,
                                                  regionSize);

                preLength += regionSize; // 下一片区域的开始
            }
        }

        public void write(byte[] bytes) {
            int limit = mappedBufArray[count].limit();
            int position = mappedBufArray[count].position();

            if (count >= mappedBufArray.length) {
                return;
            }

            // limit - position: 剩余区域大小
            int rest = limit - position;
            if (rest > bytes.length) {
                mappedBufArray[count].put(bytes);

            } else { // 映射可能不足
                mappedBufArray[count].put(bytes, 0, rest);

                count++;

                if (count >= mappedBufArray.length) {
                    return;
                }

                // 下一块区域:
                if (rest < bytes.length) {
                    mappedBufArray[count].put(bytes, rest, bytes.length - rest);
                }
            }
        }

        public void close() throws IOException {
            for (MappedByteBuffer buffer : mappedBufArray) {
                clean(buffer);
                unmap(buffer);
            }
            rwChannel.close();
        }
    }

    ///////////////////////////////////////////////////////

    /**
     * in JDK 8, I don't find this method.
     */
    @SuppressWarnings({ "unchecked", "RedundantArrayCreation" })
    private static void clean(final MappedByteBuffer buffer) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    Method getCleanerMethod = buffer.getClass()
                                                    .getMethod("cleaner", new Class[0]);
                    getCleanerMethod.setAccessible(true);
                    Cleaner cleaner = (Cleaner) getCleanerMethod.invoke(buffer,
                                                                        new Object[0]);
                    cleaner.clean();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    /**
     * in JDK 8, I don't find this method.
     */
    private static void unmap(MappedByteBuffer buffer) {
        // 加上这几行代码,手动unmap
        try {
            Method m = FileChannelImpl.class.getDeclaredMethod("unmap",
                                                               MappedByteBuffer.class);
            m.setAccessible(true);
            m.invoke(FileChannelImpl.class, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////

    public static byte[] getContent(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        long fileSize = file.length();
        FileInputStream fi = new FileInputStream(file);
        byte[] buffer = new byte[(int) fileSize];
        int offset = 0;
        int numRead;
        while (offset < buffer.length
                && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
            offset += numRead;
        }
        // 确保所有数据均被读取
        if (offset != buffer.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        fi.close();
        return buffer;
    }

    public static void createFile(byte[] bfile, String filePath, String fileName) {
        createFile(filePath, fileName, bfile, false);
    }

    /**
     * the traditional io way
     * small file use this
     * it will wipe all existing data, and write the bytes
     */
    public static void createFile(String filePath, String fileName, byte[] bfile, boolean append) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file;
        try {
            File dir = new File(filePath);
            if (!dir.exists()) { //判断文件目录是否存在
                boolean mkdirs = dir.mkdirs();
                System.out.println("创建目录" + mkdirs);
            }
            file = new File(filePath, fileName);
            fos = new FileOutputStream(file, append);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
