package com.coding.opencv;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    static{
        if(!OpenCVLoader.initDebug()){
            Log.d("TAG", "Not loaded");

        }else{
            Log.d("TAG", "Loaded");
        }
    }


    int iLowH = 0;
    int iHighH = 0;
    int iLowS = 0;
    int iHighS = 0;
    int iLowV = 0;
    int iHighV = 255;
    Mat imgHSV, imgThresholded;
    Scalar sc1, sc2;



/*int iLowH = 45;
    int iHighH = 75;
    int iLowS = 20;
    int iHighS = 255;
    int iLowV = 10;
    int iHighV = 255;
    Mat imgHSV, imgThresholded;
    Scalar sc1, sc2;*/





    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat mRGBA;
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;


            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
       // requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);


        sc1 = new Scalar(iLowH,iLowS,iLowV);
        sc2 = new Scalar(iHighH, iHighS, iHighV);

        javaCameraView=(JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setCameraIndex(0);
        javaCameraView.enableView();

    }

    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.i(TAG, "lOADED SUCCESSFULLY");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }else
        {
            Log.i(TAG, "Not Loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0,this,mLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        imgHSV = new Mat(width,height,CvType.CV_16UC4);
        imgThresholded = new Mat(width,height,CvType.CV_16UC4);

        mRGBA = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

      //  Imgproc.cvtColor(inputFrame.rgba(),imgHSV, Imgproc.COLOR_BGR2HSV);
      //  Core.inRange(imgHSV,sc1,sc2,imgThresholded);
        mRGBA = inputFrame.rgba();

        /**********HSV conversion**************/
        //convert mat rgb to mat hsv
        Imgproc.cvtColor(mRGBA, imgHSV, Imgproc.COLOR_RGB2HSV);

        //find scalar sum of hsv
        Scalar mColorHsv = Core.sumElems(imgHSV);

        int pointCount = 320*240;


        //convert each pixel
        for (int i = 0; i < mColorHsv.val.length; i++) {
            mColorHsv.val[i] /= pointCount;
        }

        //convert hsv scalar to rgb scalar
        Scalar mColorRgb = convertScalarHsv2Rgba(mColorHsv);

    /*Log.d("intensity", "Color: #" + String.format("%02X", (int)mColorHsv.val[0])
            + String.format("%02X", (int)mColorHsv.val[1])
            + String.format("%02X", (int)mColorHsv.val[2]) );*/
        //print scalar value
        Log.d("intensity", "R:"+ String.valueOf(mColorRgb.val[0])+" G:"+String.valueOf(mColorRgb.val[1])+" B:"+String.valueOf(mColorRgb.val[2]));


        /*Convert to YUV*/

        int R = (int) mColorRgb.val[0];
        int G = (int) mColorRgb.val[1];
        int B = (int) mColorRgb.val[2];

        int Y = (int) (R *  .299000 + G *  .587000 + B *  .114000);
        int U = (int) (R * -.168736 + G * -.331264 + B *  .500000 + 128);
        int V = (int) (R *  .500000 + G * -.418688 + B * -.081312 + 128);

        //int I = (R+G+B)/3;


        //Log.d("intensity", "I: "+String.valueOf(I));
        Log.d("intensity", "Y:"+ String.valueOf(Y)+" U:"+String.valueOf(U)+" V:"+String.valueOf(V));

        /*calibration*/



        return mRGBA;

    }



    //convert Mat hsv to scalar
    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public void saveFrames(Mat subimg){
        //subimg -> your frame

        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(subimg.cols(), subimg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(subimg, bmp);
        } catch (CvException e) {
            Log.d(TAG, e.getMessage());
        }

        subimg.release();


        FileOutputStream out = null;

        String filename = "frame.png";


        File sd = new File(Environment.getExternalStorageDirectory() + "/frames");
        boolean success = true;
        if (!sd.exists()) {
            success = sd.mkdir();
        }
        if (success) {
            File dest = new File(sd, filename);

            try {
                out = new FileOutputStream(dest);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored

            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
            } finally {
                try {
                    if (out != null) {
                        out.close();
                        Log.d(TAG, "OK!!");
                    }
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage() + "Error");
                    e.printStackTrace();
                }
            }
        }
    }

}