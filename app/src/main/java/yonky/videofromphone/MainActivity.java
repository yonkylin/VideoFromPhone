package yonky.videofromphone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class MainActivity extends AppCompatActivity {
    private AsyncHttpServer mServer= new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},10086);
        }else {

            Log.e("yonky,", "has permission");
        }
        mServer.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    response.send(getIndexContent());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(500).end();
                }

            }
        });
        mServer.get("/jquery-1.7.2.min.js", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    String fullPath = request.getPath();
                    fullPath = fullPath.replace("%20", " ");
                    String resourceName = fullPath;
                    if (resourceName.startsWith("/")) {
                        resourceName = resourceName.substring(1);
                    }
                    if (resourceName.indexOf("?") > 0) {
                        resourceName = resourceName.substring(0, resourceName.indexOf("?"));
                    }
                    response.setContentType("application/javascript");
                    BufferedInputStream bInputStream = new BufferedInputStream(getAssets().open(resourceName));
                    response.sendStream(bInputStream, bInputStream.available());
                } catch (IOException e) {
                    e.printStackTrace();
                    response.code(404).end();
                    return;
                }
            }
        });

        mServer.get("/files/.*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath().replace("/files/", "");
                try {
                    path = URLDecoder.decode(path, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        response.sendStream(fis, fis.available());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                response.code(404).send("Not found!");
            }
        });
        mServer.get("/files", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONArray array = new JSONArray();
                File dir = new File(Environment.getExternalStorageDirectory().getPath());
                String[] fileNames  = dir.list();
//                Log.e("yonky",fileNames.toString());
                if(fileNames !=null){
                    for(String fileName:fileNames){
                        File file = new File(dir,fileName);
                        if(file.exists() &&file.isFile()&&file.getName().endsWith(".mp4") ){
                            try{
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("name",fileName);
                                jsonObject.put("path",file.getAbsolutePath());
                                array.put(jsonObject);
                            }catch (JSONException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }else {
                    Log.e("yonky","the path of: "+Environment.getExternalStorageDirectory().getPath()+" is null");
                }
                response.send(array.toString());
            }
        });
        mServer.listen(mAsyncServer,54321);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mServer !=null){
            mServer.stop();
        }
        if(mAsyncServer !=null){
            mAsyncServer.stop();
        }
    }

    private String getIndexContent() throws IOException{
        BufferedInputStream bInputStream = null;
        try{
            bInputStream = new BufferedInputStream(getAssets().open("index.html"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            byte[] tmp=new byte[10240];
            while((len = bInputStream.read(tmp))>0){
                baos.write(tmp,0,len);
            }
            return new String(baos.toByteArray(),"utf-8");
        }catch (IOException e){
            e.printStackTrace();
            throw e;
        }finally {
            if(bInputStream!=null){
                try{
                    bInputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }


}