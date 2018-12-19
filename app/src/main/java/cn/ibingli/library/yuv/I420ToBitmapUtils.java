package cn.ibingli.library.yuv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import cn.ibingli.library.BuildConfig;

/**
 * <pre>
 *     i420数据转换bitmap工具类
 * </pre>
 * Author：hyj
 * Date：2018/9/11　15:49
 */
public class I420ToBitmapUtils {
    public final String TAG = getClass().getSimpleName();
    private boolean showLog = BuildConfig.DEBUG;    //是否打印日志

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    private int imageCount = 1;
    private long preTime = System.currentTimeMillis();

    public I420ToBitmapUtils(Context cxt) {
        rs = RenderScript.create(cxt);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    /**
     * 打印日志
     *
     * @param msg
     */
    private void showLog(String msg) {
        if (!showLog) {
            return;
        }
        Log.e(TAG, msg);
    }

    /**
     * 显示当前帧率
     */
    public void showFrameRate() {
        imageCount++;
        if (System.currentTimeMillis() - preTime > 1000) {
            showLog("被叫 当前帧率：" + imageCount);
            preTime = System.currentTimeMillis();
            imageCount = 1;
        }
    }

    /**
     * 使用java代码方式将I420数据转换成NV21数据，转换后，颜色、格式都是OK的
     * 但是转换过程时间太长，平均时间再80ms左右，无法满足视频的要求
     *
     * @param data   i420类型的byte[]数据
     * @param width  图片宽度
     * @param height 图片高度
     * @return
     */
    public Bitmap I420ToNv21(byte[] data, int width, int height) {
        long preTim = System.currentTimeMillis();
        long tagTime = System.currentTimeMillis();

        final int frameSize = width * height;   //bufferY
        final int qFrameSize = frameSize / 4;   //bufferV
        final int tempFrameSize = frameSize * 5 / 4;    //bufferU

        byte[] ret = new byte[data.length];
        System.arraycopy(data, 0, ret, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            ret[frameSize + i * 2] = data[tempFrameSize + i]; // Cb (U)
            ret[frameSize + i * 2 + 1] = data[frameSize + i]; // Cr (V)
        }

        long i420ToNV21 = System.currentTimeMillis() - tagTime;

        Bitmap bitmap = null;
        try {
            tagTime = System.currentTimeMillis();

            //调用这两个方法，生成bitmap的帧率都在10帧左右，无法满足要求
            bitmap = nv21ToBitmapByArgb(ret, width, height); //使用数组创建bitmap
//            bitmap = nv21ToBitmapByYuvImage(ret, width, height);//使用yuvImage方式

            long bitmapTime = System.currentTimeMillis() - tagTime;
            showLog("I420转Nv21时间：" + i420ToNV21 + " bitmap时间：" + bitmapTime);
        } catch (Exception e) {
            e.printStackTrace();
        }

        showLog("生成 bimtp时间：" + (System.currentTimeMillis() - preTim) + "  文件大小：" + bitmap.getRowBytes());
        return bitmap;
    }

    /**
     * 将nv21数据通过数组变换的方式转换成bitMap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    private Bitmap nv21ToBitmapByArgb(byte[] nv21, int width, int height) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);

        return bmpout;
    }

    /**
     * 将nv21数据通过YuvImage的方式转换成bitmap
     *
     * @param nv21
     * @param width
     * @param height
     * @return
     */
    private Bitmap nv21ToBitmapByYuvImage(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * 会用so包的方式把i420数据转换成nv21的数据
     * 然后通过nv21生成bitmap数据
     * 通过这种思路，生成的bitmap在耗时、颜色上都达到了要求
     *
     * @param i420Data
     * @param width
     * @param height
     * @return
     */
    public Bitmap soI420ToNv21(byte[] i420Data, int width, int height) {
        long tagTime = System.currentTimeMillis();
        byte[] nv21Data = new byte[i420Data.length];
        YuvUtils.yuvI420ToNV21(i420Data, nv21Data, width, height);
        Bitmap bitmap = nv21ToBitmapByArgb(nv21Data, width, height);
        showLog("soI420ToNv21时间：" + (System.currentTimeMillis() - tagTime));
        return bitmap;
    }

    /**
     * 将I420数据转换成argb的bitmap数据，
     * 想通过这种方式直接将其转换成一个可以显示的bitmap
     * 通过测试发现转换耗时上是OK的，
     * 但是在颜色上通过工具发现将其转换成了YV12格式的数据了，在颜色转换上存在问题
     * 这种方法还可以优化
     *
     * @param data
     * @param width
     * @param height
     * @return
     */
    public Bitmap soI420ToArgb(byte[] data, int width, int height) {
        long tagTime = System.currentTimeMillis();

        //用于保存将yuv数据转成argb数据
        byte[] rgbbuffer = new byte[width * height * 4];
        //将上面的yuv I420 还原成argb数据
        YuvUtils.convertToArgb(data, width * height * 3 / 2, rgbbuffer, width * 4, 0, 0, width, height, width, height, 0, 0);

//        writeToFile("tenI420ToArgb", ByteBuffer.wrap(rgbbuffer));

        //还原成位图
        Bitmap stitchBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        stitchBmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgbbuffer));

        //显示还原的位图
//            imgShow.setImageBitmap(stitchBmp);
        showLog("I420转Nv21时间：" + (System.currentTimeMillis() - tagTime));
        return stitchBmp;
    }

    /**
     * 将帧数据写成yuv文件，
     * 我们可以使用这个播放器观看到格式、分辨率颜色等数据
     * https://download.csdn.net/download/zxccxzzxz/9508288
     *
     * @param name
     * @param buffer
     */
    private void writeToFile(String name, ByteBuffer buffer) {
        try {
            String path = "/sdcard/" + name + ".yuv";
            FileOutputStream fos = new FileOutputStream(path, true);
            byte[] in = new byte[buffer.capacity()];
            buffer.clear();
            buffer.get(in);
            fos.write(in);
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * SO测试代码
     * 将一张jpeg图片转换成i420的byte数据，
     * 让后再将这个i420的数据还原成argb的数据生成一个bitMap
     * 资料：https://blog.csdn.net/XIAIBIANCHENG/article/details/73065646
     *
     * @param cxt 上下文对象
     * @return
     */
    public Bitmap soTestJpeg(Context cxt) {
        try {
            InputStream is = cxt.getResources().getAssets().open("test.jpg");//读取assert 的文图
            Bitmap image = BitmapFactory.decodeStream(is);
            int w = image.getWidth(), h = image.getHeight();

            //将位图资源转为二进制数据，数据大小为w*h*4
            int bytes = image.getByteCount();
            ByteBuffer buf = ByteBuffer.allocate(bytes);
            image.copyPixelsToBuffer(buf);
            byte[] byteArray = buf.array();

            byte[] ybuffer = new byte[w * h];//用于保存y分量数据
            byte[] ubuffer = new byte[w * h * 1 / 4];//用于保存u分量数据
            byte[] vbuffer = new byte[w * h * 1 / 4];//用于保存v分量数据
            //将位图数据argb转换为yuv I420 转换后的数据分别保存在 ybuffer、ubuffer和vbuffer里面
            YuvUtils.argbtoi420(byteArray, w * 4, ybuffer, w, ubuffer, (w + 1) / 2, vbuffer, (w + 1) / 2, w, h);

            //将上面的yuv数据保存到一个数组里面组成一帧yuv I420 数据 分辨率为w*h
            byte[] frameBuffer = new byte[w * h * 3 / 2];
            System.arraycopy(ybuffer, 0, frameBuffer, 0, w * h);
            System.arraycopy(ubuffer, 0, frameBuffer, w * h, w * h * 1 / 4);
            System.arraycopy(vbuffer, 0, frameBuffer, w * h * 5 / 4, w * h * 1 / 4);

            //用于保存将yuv数据转成argb数据
            byte[] rgbbuffer = new byte[w * h * 4];
            //将上面的yuv I420 还原成argb数据
            YuvUtils.convertToArgb(frameBuffer, w * h * 3 / 2, rgbbuffer, w * 4, 0, 0, w, h, w, h, 0, 0);

            //还原成位图
            Bitmap stitchBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            stitchBmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgbbuffer));

            //显示还原的位图
//            imgShow.setImageBitmap(stitchBmp);

            return stitchBmp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}