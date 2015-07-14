package dianagio.wheelchair;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.jar.Attributes;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by Giovanni on 18/06/2015.
 * Class created to provide a complete configuration of the user-relate storage file
 * locally. it also provides the name of the folders for the online storage
 *
 * call User(name,surname,dateofbirth) to initialize
 * tellPathToUserMetadata(void) to retrieve a string with the complete path to user metadata document
 * tellUserFolder(void) to retrieve a string  composed by name+surname
 */
public class User implements Serializable{
    public static String Acquisitions;// string used to name properly the acquisition folder
    public static String Metadata;// string used to name properly metadata file
    private String name="ciccio"; //user name
    private String surname="bello"; //user surname
    private String UserFolder; //string that names the folder in dropbox and also part of the name of the local userfolder
    private File userFile; //local base file
    private String pathToUserMetadata;
    private String currentSWVersion = "1.0";
    private File userAcquisitionFile;//local acquisitions file
    private Date birthday;//to implement
    private Integer age;//to implement
    private Integer dayOfUse;//to implement
    public String tellcurrentSWVersion()
    {

        return currentSWVersion;
    }
    public void setcurrentSWVersion(String crntSW)
    {

        currentSWVersion=crntSW;
    }
    public String tellName()
    {

        return name;
    }
    public String tellSurname()
    {

        return surname;
    }
    public String tellAcquisitionsFolder()
    {
        return userAcquisitionFile.getAbsolutePath().toString();
    }
    public String tellBaseFolder()
    {
        return userFile.getAbsolutePath().toString();
    }
    public String tellPathToUserMetadata()
    {
        String path=pathToUserMetadata;
        return path;
    }
    public String tellUserFolder()
    {
        String name=UserFolder;
        return name;
    }

    public User(String n,String s,Date b)
    {
        name=n;
        surname=s;
        birthday=b;
       // pathToUserMetadata= new String();
        Acquisitions= "/Acquisitions";
        Metadata="/metadata.txt";
        CreateUserFile();
        CreateUserDocument();

    }

    //==========================================================================
    protected boolean CreateUserFile ()
    //==========================================================================
    {
        if (isExternalStorageWritable()==true)
        {
            create_folder(name+"_"+surname);
            return true;
        }
         else return false;
    }
    //==========================================================================
    protected void CreateUserDocument()
    //==========================================================================
    {
         UserFolder="/"+name+surname;
        try {
            String path=userFile.getPath().toString()+"/"+UserFolder+"_userdata.txt";
            pathToUserMetadata=path;
            FileOutputStream fOut = new FileOutputStream(path);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            // Write the string to the file
            osw.write("USER DATA:\t"+"NAME:"+name+"\t"+"SURNAME:"+surname+"\t"
                  //  + "AGE:"+age.toString()+"\t"
            );

            osw.flush();
            osw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //==========================================================================
    private boolean isExternalStorageWritable() {
        //==========================================================================
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    //==========================================================================
    private boolean create_folder(String name)
    //==========================================================================
    {
        if(isExternalStorageWritable()) {
            userFile = new File(getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/");
            //userFile = new File(Environment.getExternalStorageDirectory().toString() + "/"+name+"_WheelChair");
            boolean success = false;
            if (!userFile.exists()) {
                success =userFile.mkdir();
            }

            userAcquisitionFile = new File(userFile.getPath() + "/Acquisitions/");//Acquisitions);

            if (!userAcquisitionFile.exists()) {
                success = userAcquisitionFile.mkdir(); //success && userAcquisitionFile.mkdir();
            }
            if (!success) {
                //CODE HERE
            } else {
                //CODE HERE
            }
            return true;
        }
        return false;
    }
   /* private void write_stuff_in_file(String stuff,String filepath,String document)
    {
        try {
            FileOutputStream fOut = new FileOutputStream(filepath+document);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            // Write the string to the file
            osw.write(stuff);

            osw.flush();
            osw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
   private static final long serialVersionUID = 1L;
    @Override
    public String toString() {
        return "Person [name=" + name + ", surname=" + surname + "pathtouseracquisitionfolder= "+ tellAcquisitionsFolder()+"]";
    }



}
