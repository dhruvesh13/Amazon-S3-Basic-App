package the.pixel.factory.ccproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    AmazonS3 s3Client;
    String bucket = "temporarysongs";

    File uploadToS3 = new File("/storage/emulated/0/Download/IMG_20180405_162122.jpg");
    File downloadFromS3 = new File("/storage/emulated/0/Download/random.jpg");
    File file;
    TransferUtility transferUtility;
    List<String> listing;
    TextView list,t;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // callback method to call credentialsProvider method.
        s3credentialsProvider();
        list=(TextView)findViewById(R.id.listofitems);
        Bitmap bm= BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.pixel_2);
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File myDir = new File(root + "/Download");
        myDir.mkdirs();

        String fname = "random.jpg";
        file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);

            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // callback method to call the setTransferUtility method
        setTransferUtility();
    }


    public void s3credentialsProvider(){

        // Initialize the AWS Credential
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:6118feda-b419-4f4d-918b-03de30718d5d", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        createAmazonS3Client(cognitoCachingCredentialsProvider);
    }

    public void createAmazonS3Client(CognitoCachingCredentialsProvider credentialsProvider){

        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider);

        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
    }

    public void setTransferUtility(){

        transferUtility = new TransferUtility(s3Client, getApplicationContext());
    }

    public void uploadFileToS3(View view){

        TransferObserver transferObserver = transferUtility.upload(
                bucket,     /* The bucket to upload to */
                "pixel.png",    /* The key for the uploaded object */
                file       /* The file where the data to upload exists */
        );

        transferObserverListener(transferObserver);
    }

    /**
     *  This method is used to Download the file to S3 by using transferUtility class
     * @param view
     **/
    public void downloadFileFromS3(View view){

        TransferObserver transferObserver = transferUtility.download(
                bucket,     /* The bucket to download from */
                "pixel.png",    /* The key for the object to download */
                downloadFromS3        /* The file to download the object to */
        );
        transferObserverListener(transferObserver);
    }

    public void fetchFileFromS3(View view){

        // Get List of files from S3 Bucket

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {

                try {
                    Looper.prepare();
                    listing = getObjectNamesForBucket(bucket, s3Client);

                    for (int i=0; i< listing.size(); i++){

                      /* t.append(listing.get(i));
                       t.append("\n");*/
                        Toast.makeText(MainActivity.this, listing.get(i),Toast.LENGTH_LONG).show();
                    }
                    Looper.loop();
                    // Log.e("tag", "listing "+ listing);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e("tag", "Exception found while listing "+ e);
                }

            }
        });
        thread.start();
    }

    /**
     * @desc This method is used to return list of files name from S3 Bucket
     * @param bucket
     * @param s3Client
     * @return object with list of files
     */
    private List<String> getObjectNamesForBucket(String bucket, AmazonS3 s3Client) {
        ObjectListing objects=s3Client.listObjects(bucket);
        List<String> objectNames=new ArrayList<String>(objects.getObjectSummaries().size());
        Iterator<S3ObjectSummary> iterator=objects.getObjectSummaries().iterator();
        while (iterator.hasNext()) {
            objectNames.add(iterator.next().getKey());
        }
        while (objects.isTruncated()) {
            objects=s3Client.listNextBatchOfObjects(objects);
            iterator=objects.getObjectSummaries().iterator();
            while (iterator.hasNext()) {
                objectNames.add(iterator.next().getKey());
            }
        }
        return objectNames;
    }

    /**
     * This is listener method of the TransferObserver
     * Within this listener method, we get status of uploading and downloading file,
     * to display percentage of the part of file to be uploaded or downloaded to S3
     * It displays an error, when there is a problem in  uploading or downloading file to or from S3.
     * @param transferObserver
     */

    public void transferObserverListener(TransferObserver transferObserver){

        transferObserver.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                Toast.makeText(getApplicationContext(), "State Change"
                        + state, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                Toast.makeText(getApplicationContext(), "Progress in %"
                        + percentage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("error","error");
            }

        });
    }
}
