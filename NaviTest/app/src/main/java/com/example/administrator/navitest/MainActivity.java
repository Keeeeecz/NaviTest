package com.example.administrator.navitest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {
    private SurfaceView camera_sf;
    private Button camera_btn;
    //安卓硬件相机
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private int mPictureCount=9;//连拍次数
    int pic_count=0;//记录当前已拍张数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null == mCamera) {
            mCamera = getCustomCamera();
            if (mHolder != null) {
                //开户相机预览
                previceCamera(mCamera, mHolder);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放相机资源
            mCamera.release();
            mCamera = null;
        }
    }

    private void initViews() {
        this.camera_sf = (SurfaceView) findViewById(R.id.camera_sf);
        this.camera_btn = (Button) findViewById(R.id.camera_btn);
        this.camera_btn.setOnClickListener(this);
        camera_sf.setOnClickListener(this);
        //获取SurfaceView的SurfaceHolder对象
        mHolder = camera_sf.getHolder();
        //实现SurfaceHolder.Callback接口
        mHolder.addCallback(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_sf://点击可以对焦
                if (null != mCamera)
                    mCamera.autoFocus(null);
                break;
            case R.id.camera_btn://点击进行拍照
                startTakephoto();
                break;
        }
    }

    private void startTakephoto() {
        //获取到相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewFpsRange(10, 20); // 每秒显示20~30帧
        parameters.setPictureFormat(ImageFormat.NV21); // 设置图片格式
        //设置图片大小
        parameters.setPreviewSize(480, 720);
        //设置对焦
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //设置自动对焦
        Log.e("Count",Integer.toString( pic_count ));
        //拍照
        /*mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mCamera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            new dealWithCameraData().execute( data );
                            if(++pic_count<mPictureCount){
                                previceCamera(mCamera, mHolder);
                                startTakephoto();
                            }
                        }
                    });
                }
            }
        });*/

        //录像
        mCamera.setPreviewCallback(new Camera.PreviewCallback(){
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                try{
                    YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                    if(image!=null){
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        //因为图片会放生旋转，因此要对图片进行旋转到和手机在一个方向上
                        rotateMyBitmap(bmp);
                        saveBitmap( bmp );
                        stream.close();
                    }
                }catch(Exception ex){
                    Log.e("Sys","Error:"+ex.getMessage());
                }
            }
        });
    }
    public void rotateMyBitmap(Bitmap bmp){
        //*****旋转一下
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap nbmp2 = Bitmap.createBitmap(bmp, 0,0, bmp.getWidth(),  bmp.getHeight(), matrix, true);
    };
    public void saveBitmap(Bitmap bm) {
        //保存录像图片
        Log.e("TAG", "保存图片");
        String fileName = "/sdcard/1/" + System.currentTimeMillis() + ".jpg"; //保存路径
        File f = new File( fileName );
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, out);//bitmap的压缩方法
            out.flush();
            out.close();
            Log.i("TAG", "已经保存");
        } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

/*    //保存拍照数据
   class dealWithCameraData extends AsyncTask<byte[], String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(byte[]... params){
            FileOutputStream fos = null;
            String tempStr = "/sdcard/1/";
            //图片临时保存位置
            String fileName = tempStr + System.currentTimeMillis() + ".jpg";
            File tempFile = new File( fileName );
            try {
                fos = new FileOutputStream( fileName );
                fos.write( params[0] );
                Log.d( "show", "图片保存成功" + pic_count + "张");
                new CompressPic().getSmallBitmap( tempFile,400,300 );
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }*/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        previceCamera(mCamera, holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        previceCamera(mCamera, holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放相机资源
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera getCustomCamera() {
        if (null == mCamera) {
            //使用Camera的Open函数开机摄像头硬件
            mCamera = Camera.open();
            //Camera.open()方法说明：2.3以后支持多摄像头，所以开启前可以通过getNumberOfCameras先获取摄像头数目，
            // 再通过 getCameraInfo得到需要开启的摄像头id，然后传入Open函数开启摄像头，
            // 假如摄像头开启成功则返回一个Camera对象
        }
        return mCamera;
    }

    private void previceCamera(Camera camera, SurfaceHolder holder) {
        try {
            //摄像头设置SurfaceHolder对象，把摄像头与SurfaceHolder进行绑定
            camera.setPreviewDisplay(holder);
            //调整系统相机拍照角度
            camera.setDisplayOrientation(90);
            //调用相机预览功能
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
