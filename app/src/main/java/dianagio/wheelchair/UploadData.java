package dianagio.wheelchair;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by Giovanni on 17/06/2015.
 * this class allows to upload a file to a Folder in dropbox
 * once properly configured the Oath2 autentication
 * and acquired the relative token. to call it, please call
 * UploadData(theactualContext,theDBApiconfigured,Pathofthefiletoupload,destinationfoldername,destinationfilename)
 * it runs in background while uploading except from the showtoast
 */
public class UploadData extends AsyncTask<Void, Long, String> {
    private String mstring;
    private String dbfolder;
    private String dbfilename;
    private File mfile;
    Context mcontext;
    DropboxAPI<?> mApi;
    public AsyncResponse delegate = null;//Call back interface

    public void UploadData_parameters(Context context,DropboxAPI<?> Api,String path,String folder,String filename) {

        mstring=(path);
        mcontext=context;
        mApi=Api;
        dbfolder= folder;
        dbfilename=filename;



    }
    public UploadData(AsyncResponse asyncResponse){
        mstring=null;
        mcontext=null;
        mApi=null;
        dbfolder= null;
        dbfilename=null;
        delegate = asyncResponse;//Assigning call back interfacethrough constructor
    }


    @Override


    protected void onPreExecute() {
        super.onPreExecute();
        showToast("Uploading...");
    }
    @Override
    protected String doInBackground(Void... params) {

        mfile = new File(mstring);
        FileInputStream inputStream = null;
        if(isNetworkOnline()== true) {

            try {

                inputStream = new FileInputStream(mfile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            DropboxAPI.Entry response = null;
            try {
                response = mApi.putFile(dbfolder + dbfilename, inputStream,
                        mfile.length(), null, null);
            } catch (DropboxException e) {
                e.printStackTrace();
            }
            if (response.bytes== mfile.length())
            {
                return null;
            }
            else{

                return mstring;
            }

        }
        else {

            return mstring;
        }
    }
        private void showToast(String msg) {
        Toast error = Toast.makeText(mcontext, msg, Toast.LENGTH_LONG);
        error.show();
    }
    protected void onPostExecute(String res)
    {

        if (res== null)
        {
            showToast("success!");
            delegate.processFinish(null);
            return;
        }
        else{
            delegate.processFinish(res);
            return;
        }

    }
    public boolean isNetworkOnline() {
        boolean status=false;
        try{
            ConnectivityManager cm = (ConnectivityManager) mcontext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState()==NetworkInfo.State.CONNECTED) {
                status= true;
            }else {
                netInfo = cm.getNetworkInfo(1);
                if(netInfo!=null && netInfo.getState()== NetworkInfo.State.CONNECTED)
                    status= true;
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return status;

    }
}

