package dianagio.wheelchair;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Giovanni on 17/06/2015.
 * this class allows to Download a file from a Folder in dropbox
 * once properly configured the Oath2 autentication
 * and acquired the relative token. to call it, please call
 * UploadData(theactualContext,theDBApiconfigured,Pathofthefiletoupload,destinationfoldername,destinationfilename, asyncresponse)
 * it runs in background while uploading except from the showtoast
 */
public class DownloadData extends AsyncTask<Void, Boolean, Boolean> {
    private String mstring;                      // variabile che contiene il path del file scaricato
    private String mBaseFolder;
    private String dbfolder;                     //target dropbox folder
    private String dbfilename;                   //name of the file to download
    Context mcontext;                            //actual context
    DropboxAPI<?> mApi;
    public AsyncResponse delegate = null;        //Call back interface da implementare nella classe chiamante
    DropboxAPI.DropboxFileInfo response = null;  // parametro che permette di estrarre metadati dal file uploadato

    public DownloadData(Context context,DropboxAPI<?> Api,String path,String basefolder,String folder,String filename,AsyncResponse asyncResponse) {
        // We set the context this way so we don't accidentally leak activities
        mstring=(path);
        mcontext=context;
        mApi=Api;
        dbfolder= folder;
        dbfilename=filename;
        mBaseFolder= basefolder;
        delegate = asyncResponse;


    }

    @Override                                          //esegue nel thread principale prima dello start del thread in backgroung
    protected void onPreExecute() {
        super.onPreExecute();
        showToast("Downloading...");
    }
    @Override
    protected Boolean doInBackground(Void... params) { //routine executed in a background thread
    if (isNetworkOnline())                             //if there is a network connection
    {
    File mfile = new File(mstring);                    //create new file
    FileOutputStream outputStream = null;
    try {
        outputStream = new FileOutputStream(mfile);    //apre stream in uscita verso il file
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }
    try {
        response=mApi.getFile(dbfolder + dbfilename, null, outputStream, null);//scarica il file dalla cartella db e copia in response i metadati
    } catch (DropboxException e) {
        e.printStackTrace();
        return false;                                                          //se non riesce ritorna falso, che viene letto dal metodo chiamato alla chiusura del thread
    }

        return true;
    }
        return false;
    }

    @Override                                               //metodo chiamato alla chiusura del thread invia tramite interfaccia un output a seconda del risultato
    protected void onPostExecute(Boolean result) {

    if (result) {
        delegate.processFinish(response.getMetadata().rev); //se e andato tutto bene restituisce il rev del file wich is a code to useful to understand if the file is different from a previously downloaded one
    }
        else {
        delegate.processFinish("failed");                  //comunica il fallimento through the delegate
    }
    return;
    }


    private void showToast(String msg) {
        Toast error = Toast.makeText(mcontext, msg, Toast.LENGTH_LONG);
        error.show();
    }
    public boolean isNetworkOnline() { // checks if there is network connection, returns true for network connected, false for no network detected
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
