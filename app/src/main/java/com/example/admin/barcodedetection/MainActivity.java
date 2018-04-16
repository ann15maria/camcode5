package com.example.admin.barcodedetection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    TextToSpeech t1;
    private static final int REQUEST_CAMERA = 200;
    Bitmap bp,grayBmp,bpGrad,bpBlurr,bpTreshold,bpClosed,bpCot;
    ImageView img1_iv,gray_iv,grad_iv,blurr_iv,treshold_iv,closed_iv,contour_iv;
    TextView data_tv,tv_scandetails2;
    Button camera_bt,findProduct_bt;
    Mat gradX,gradY,grad,blurred,tresh,kernal,closed,cropped,stencil;
    String r;
    TextView totalprice_tv,itempurchased_tv,totalpriceDisplay_tv,itempurchasedDisplay_tv;
    private ListView listView1;
    ArrayList<String> productList1=new ArrayList<String>();
    ArrayList<String> storeList1=new ArrayList<String>();
    int totalPrice=0;
    int itemPurchased=0;


    //Context context;


   /* static {

        if (!OpenCVLoader.initDebug()){
            Log.e("Yes","Not Loaded");
        }
        else {
            Log.e("Yes","Loaded");
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        OpenCVLoader.initDebug();

        img1_iv=(ImageView)findViewById(R.id.iv_img1);
        gray_iv=(ImageView)findViewById(R.id.iv_imgGray);
        grad_iv=(ImageView)findViewById(R.id.iv_imgGrad);
        blurr_iv=(ImageView)findViewById(R.id.iv_imgBlurr);
        treshold_iv=(ImageView)findViewById(R.id.iv_imgTreshold);
        closed_iv=(ImageView)findViewById(R.id.iv_imgClosed);
        contour_iv=(ImageView)findViewById(R.id.iv_imgContour);

        data_tv=(TextView) findViewById(R.id.tv_data);
        //tv_scandetails2=(TextView) findViewById(R.id.tv_scandetails2);


        camera_bt=(Button)findViewById(R.id.bt_selectcamera);
        //findProduct_bt=(Button)findViewById(R.id.bt_findProduct);

        totalprice_tv=findViewById(R.id.tv_totalPrice1);
        itempurchased_tv=findViewById(R.id.tv_itemPurchased1);
        totalpriceDisplay_tv=findViewById(R.id.tv_totalpriceDisplay1);
        itempurchasedDisplay_tv=findViewById(R.id.tv_itemPurchasedDisplay1);
        listView1 = (ListView) findViewById(R.id.lst1);

    }

    public void cameraHandler(View view) {
        Intent in = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(in, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK){

            if (requestCode == REQUEST_CAMERA) {
                bp = (Bitmap) data.getExtras().get("data");

                try {

                    Mat mat=new Mat();
                    Mat grayMat=new Mat();

                    BitmapFactory.Options o=new BitmapFactory.Options();
                    o.inDither=false;
                    o.inSampleSize=4;

                    img1_iv.setImageBitmap(bp);


                    int width=bp.getWidth();
                    int height=bp.getHeight();



                    Utils.bitmapToMat(bp, mat);
                    Imgproc.cvtColor(mat,grayMat,Imgproc.COLOR_BGR2GRAY);

                    grayBmp=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    Utils.matToBitmap(grayMat,grayBmp);
                    gray_iv.setImageBitmap(grayBmp);


                    int ddepth= CvType.CV_32F;
                    gradX=new Mat();
                    gradY=new Mat();
                    grad=new Mat();
                    blurred=new Mat();
                    tresh=new Mat();
                    kernal=new Mat();
                    closed=new Mat();
                    //cropped=new Mat();
                    //stencil=Mat.zeros(mat.size(),mat.type());


                    Imgproc.Sobel(grayMat,gradX,ddepth,1,0,-1,1,0,Core.BORDER_DEFAULT);
                    Imgproc.Sobel(grayMat,gradY,ddepth,0,1,-1,1,0,Core.BORDER_DEFAULT);

                    Core.subtract(gradX,gradY,grad);
                    Core.convertScaleAbs(grad,grad);


                    bpGrad=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    Utils.matToBitmap(grad,bpGrad);
                    grad_iv.setImageBitmap(bpGrad);


                    Imgproc.blur(grad,blurred,new org.opencv.core.Size(9,9));

                    bpBlurr=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    Utils.matToBitmap(blurred,bpBlurr);
                    blurr_iv.setImageBitmap(bpBlurr);

                    Imgproc.threshold(blurred,tresh,255,255,Imgproc.THRESH_OTSU);

                    bpTreshold=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    Utils.matToBitmap(tresh,bpTreshold);
                    treshold_iv.setImageBitmap(bpTreshold);


                    kernal=Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new org.opencv.core.Size(4,1));
                    Imgproc.morphologyEx(tresh,closed,Imgproc.MORPH_CLOSE,kernal);

                    Imgproc.erode(closed,closed,new org.opencv.core.Mat(),new org.opencv.core.Point(),30);
                    Imgproc.dilate(closed,closed,new org.opencv.core.Mat(),new org.opencv.core.Point(),30);


                    bpClosed=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    Utils.matToBitmap(closed,bpClosed);
                    closed_iv.setImageBitmap(bpClosed);


                    List<MatOfPoint> contours = new ArrayList<>();
                    //List<MatOfPoint> jj=new ArrayList<>();
                    Mat hierarchy = new Mat();
                    //Mat kk=new Mat();
                    Imgproc.findContours(closed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                    Point p,q;


                    // now iterate over all top level contours
                    for(int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                        MatOfPoint matOfPoint = contours.get(idx);
                        Rect rect = Imgproc.boundingRect(matOfPoint);
                        p=new Point(rect.x-12,rect.y-12);
                        q=new Point(12+(rect.x+rect.width),12+(rect.y+rect.height));
                        Imgproc.rectangle(mat,p,q, new Scalar(0, 0, 255),1);


                        // Imgproc.fillPoly(stencil,contours,new Scalar(255, 255, 255));
                        //Core.bitwise_and(mat,stencil,cropped);

                        //Imgproc.rectangle(cropped,p,q, new Scalar(0, 0, 255),1);




                    }

                    bpCot=Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
                    Utils.matToBitmap(mat,bpCot);
                    contour_iv.setImageBitmap(bpCot);


                    TextRecognizer textRecognizer=new TextRecognizer.Builder(getApplicationContext()).build();
                    if (!textRecognizer.isOperational()){
                        Toast.makeText(getApplicationContext(),"No Text",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Frame frame=new Frame.Builder().setBitmap(grayBmp).build();
                        SparseArray<TextBlock> items=textRecognizer.detect(frame);
                        StringBuilder sb=new StringBuilder();

                        for (int y=0;y<items.size();++y){
                            TextBlock myItem=items.valueAt(y);
                            sb.append(myItem.getValue());

                        }
                        r=sb.toString().replace(" ","");

                        data_tv.setText("Barcode : "+r);

                        ProductTask2 productTask=new ProductTask2(MainActivity.this);
                        productTask.execute(r);

                   /* findProduct_bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    });*/
                    }



                }
                catch (Exception e){
                    Toast.makeText(getApplicationContext(),"Error In Image !!",Toast.LENGTH_SHORT).show();
                }


            }

        }
    }

    public class ProductTask2 extends AsyncTask<String,Void,String> {

        Activity activity;
        Context ctx;
        String data_url="https://camcode444.000webhostapp.com/product_data/productdata.php";
        private final String TAG = getClass().getSimpleName();
        AlertDialog alertDialog;
        ProgressDialog progressDialog;
        AlertDialog.Builder builder;



        String barcode;

        ProductTask2(Context ctx) {
            this.ctx=ctx;
            activity = (Activity)ctx;

        }

        @Override
        protected void onPreExecute() {
            builder=new AlertDialog.Builder(activity);
            progressDialog=new ProgressDialog(activity);
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Connecting to server...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            barcode=params[0];
            try {
                URL url=new URL(data_url);
                HttpURLConnection httpURLConnection= (HttpURLConnection)url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                OutputStream OS=httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(OS,"UTF-8"));
                String data= URLEncoder.encode("barcode","UTF-8")+"="+URLEncoder.encode(barcode,"UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                OS.close();

                InputStream IS=httpURLConnection.getInputStream();
                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(IS));
                StringBuilder sb=new StringBuilder();
                String line="";
                while((line=bufferedReader.readLine())!=null)
                {
                    sb.append(line+" ");
                }
                Thread.sleep(1500);
                httpURLConnection.disconnect();
                IS.close();
                Log.e(TAG,""+sb);

                return sb.toString().trim();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {

            progressDialog.dismiss();

            //String jerror="<br /> <b>Notice</b>:  Undefined variable: output in <b>/storage/ssd1/658/3030658/public_html/Product/productdata.php</b> on line <b>17</b><br /> null";
            if (result==null){
                Toast.makeText(MainActivity.this,"No Internet Connection",Toast.LENGTH_SHORT).show();
            }
            else if (result.contains("Notice")){
                Toast.makeText(MainActivity.this,"Product Not Available",Toast.LENGTH_SHORT).show();
            }
            else {


                try {


                    String productName,productId,productMfd,productExp;
                    int productPrice;
                    JSONArray jArray = new JSONArray(result);
                    for(int i=0; i<jArray.length();i++){
                        JSONObject json = jArray.getJSONObject(i);
                        productId=json.getString("barcode");
                        productName=json.getString("product_name");
                        productPrice=json.getInt("product_price");
                        productMfd=json.getString("mfd");
                        productExp=json.getString("exp");

                        Log.v(TAG," "+productName+"~~~~~~"+productPrice);

                        showMessage(productName,productPrice,productId,productMfd,productExp);

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

        }
    }

    public void showMessage(String pn,int pp,String pi,String pmfd,String pexp){

        //scandetails_tv.setText("Product Name : "+pn+"\nProduct Price : â‚¹"+pp);

        Boolean r=false;
        int searchListLength = storeList1.size();
        for (int i = 0; i < searchListLength; i++) {
            if (storeList1.get(i).contains(pi)) {
                productList1.remove(i);
                listView1.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_list_item_1, productList1));
                storeList1.remove(i);
                totalPrice=totalPrice-pp;
                itemPurchased--;
                totalprice_tv.setText(" ₹"+totalPrice);
                itempurchased_tv.setText(" "+itemPurchased);
                String jis="Barcode : "+pi+"\nProduct Name : "+pn+"\nPrice : ₹"+pp+"\nManufactured Date : "+pmfd+"\nExpiry Date : "+pexp+"\nTotal Price ₹: "+totalPrice+"\nItems Purchased : "+itemPurchased;
                t1.speak(jis, TextToSpeech.QUEUE_FLUSH, null);
                r=true;
                break;
            }
        }

        if (!r){

            productList1.add("Barcode : "+pi+"\nProduct Name : "+pn+"\nPrice : ₹"+pp+"\nManufactured Date : "+pmfd+"\nExpiry Date : "+pexp);
            storeList1.add(pi);
            String jis="Barcode : "+pi+"\nProduct Name : "+pn+"\nPrice : ₹"+pp+"\nManufactured Date : "+pmfd+"\nExpiry Date : "+pexp+"\nTotal Price ₹: "+totalPrice+"\nItems Purchased : "+itemPurchased;
            t1.speak(jis, TextToSpeech.QUEUE_FLUSH, null);

            listView1.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_list_item_1, productList1));
            totalPrice=totalPrice+pp;
            itemPurchased++;
            totalprice_tv.setText(" ₹"+totalPrice);
            itempurchased_tv.setText(" "+itemPurchased);
        }

    }


}
