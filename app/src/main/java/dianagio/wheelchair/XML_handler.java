package dianagio.wheelchair;

import android.os.Environment;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Created by Giovanni and Diana on 06/07/2015.
 */
public class XML_handler {

    //==========================================================================
    public User read() {
        //==========================================================================

        String TagName;
        String Name_Xml = "";
        String Surname_Xml = "";
        String ApkVersion_Xml="";

        User user;          // instantiates a user object

        if(isExternalStorageWritable()) {
            File folder = new File(getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair");

            if (!folder.exists()) {
                folder.mkdir();
                User user_default = new User("defaultname", "defaultsurname", null);
                user_default.setcurrentSWVersion("defaultSWversion");
                write(user_default);
                return user_default;
            } else {
                FileInputStream MyXml = null;
                String pathToUserMetadataXML = folder.getAbsolutePath().toString() + "/load_config.xml";
                File path = new File(pathToUserMetadataXML);


                // search for the file
                try {
                    MyXml = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // initializes parser and sets MyXml as input file
                XmlPullParser MyParser = Xml.newPullParser();
                if (MyXml != null) {
                    try {
                        MyParser.setInput(MyXml, null);
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }

                    try {
                        MyParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }

                    int eventType = 0;
                    try {
                        eventType = MyParser.getEventType();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }

                    while (eventType != XmlPullParser.END_DOCUMENT) {   //browse document
                        switch (eventType) {
                            case XmlPullParser.START_DOCUMENT:
                                break;

                            case XmlPullParser.START_TAG:                   // if there is a start tag

                                TagName = MyParser.getName();               // read it
                                if (TagName.equals("version")) {
                                    try {
                                        ApkVersion_Xml = MyParser.nextText();
                                    } catch (XmlPullParserException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else if (TagName.equals("child")) {
                                    Name_Xml = "c";
                                } else if (TagName.equals("name")) {
                                    Name_Xml = "n";
                                    try {
                                        Name_Xml = MyParser.nextText();
                                    } catch (XmlPullParserException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else if (TagName.equals("surname")) {
                                    try {
                                        Surname_Xml = MyParser.nextText();
                                    } catch (XmlPullParserException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                        } // end switch

                        try {
                            eventType = MyParser.next();
                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }   // end while


                }

                user = new User(Name_Xml, Surname_Xml, null);
                user.setcurrentSWVersion(ApkVersion_Xml);
                return user;
            }

        } // end control myxml != null
        return null;
    }


    //==========================================================================
    public void write(User user) {
        //==========================================================================
        String Name=user.tellName();
        String Surname=user.tellSurname();
        String apk_version=user.tellcurrentSWVersion();
        String XmlString;
        FileOutputStream outputStream;
        String pathToUserMetadataXML = getExternalStorageDirectory().getAbsolutePath() + "/Wheelchair/load_config.xml";

        // create file
        File MyXml = new File(pathToUserMetadataXML);


        // write file with data
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);

            serializer.startTag("", "configurations");

            serializer.startTag("", "apk_version");
            serializer.text(apk_version);
            serializer.endTag("", "apk_version");

            serializer.startTag("", "child");

            serializer.startTag("", "name");
            serializer.text(Name);
            serializer.endTag("", "name");

            serializer.startTag("", "surname");
            serializer.text(Surname);
            serializer.endTag("", "surname");

            serializer.endTag("", "child");

            serializer.endTag("", "configurations");
            serializer.endDocument();

            XmlString = writer.toString();

            try {
                outputStream = new FileOutputStream(MyXml);
                outputStream.write(XmlString.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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

}