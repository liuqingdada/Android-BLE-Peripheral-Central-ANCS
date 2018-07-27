package com.suhen.android.libble;

import com.suhen.android.libble.utils.ByteZipUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private String mString = "/**\n" +
            " * Example local unit test, which will execute on the development machine (host).\n" +
            " *\n" +
            " * @see <a href=\"http://d.android.com/tools/testing\">Testing documentation</a>\n" +
            " */";


    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void bzip2Test() {
        mString +=mString;
        byte[] origin = mString.getBytes();
        System.out.println(origin.length);

        byte[] bZip2 = ByteZipUtil.bZip2(origin);
        System.out.println(bZip2.length);

        //////
        String str = new String(ByteZipUtil.unBZip2(bZip2));
        System.out.println(str);
    }

    @Test
    public void gzipTest() {
        mString +=mString;
        byte[] origin = mString.getBytes();
        System.out.println(origin.length);

        byte[] bZip2 = ByteZipUtil.gZip(origin);
        System.out.println(bZip2.length);

        //////
        String str = new String(ByteZipUtil.unGZip(bZip2));
        System.out.println(str);
    }

    @Test
    public void uuid(){
        // 4622c045-1cd2-4211-adc5-89df72c789ec
        String u = "4622c045-1cd2-4211-adc5-89df72c789ec";
    }
}