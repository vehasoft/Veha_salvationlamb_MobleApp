package com.veha.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import com.veha.activity.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.io.*;


import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class PdfActivity2 extends Activity implements OnPageChangeListener, OnLoadCompleteListener {
    private static final int MEGABYTE = 1024 * 1024;
    PDFView pdfView = null;
    TextView tv_header = null;
    String pdfFileName;
    String SAMPLE_FILE = "sample.pdf";
    int pageNumber = 0;
    String url ;//="https://salvationlamb-images.s3.ap-south-1.amazonaws.com/files/1679558065527.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_pdf2);
        pdfView = (PDFView)findViewById(R.id.pdfView1);
        tv_header = (TextView) findViewById(R.id.pdf_header);
        tv_header.setOnClickListener(v -> {

        });
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        pdfFileName = intent.getStringExtra("fileName");
        tv_header.setText(pdfFileName);
        LOADURL loadurl = new LOADURL(PdfActivity2.this);
        loadurl.execute(url);

    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }
    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e("TAG", String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public class LOADURL extends AsyncTask<String,Void,InputStream> {
        private ProgressDialog progressDialog;

        public LOADURL(PdfActivity2 loadPdf) {

            progressDialog = new ProgressDialog(loadPdf);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Fetching PDF from server...");
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected InputStream doInBackground(String... strings) {
            InputStream inputStream = null;
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection)
                        url.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = new
                            BufferedInputStream(urlConnection.getInputStream());

                }
            } catch (IOException e) {
                Log.e("Exception",e.toString());
                return null;
            }
            Log.e("inputstream",inputStream.toString());
            return inputStream;
        }

        @Override
        protected void onPostExecute(InputStream inputStream) {
            Log.e("inputstream",inputStream.toString());
            Log.e("pdfView",pdfView.toString());
            pdfView.fromStream(inputStream)
                    .defaultPage(pageNumber)
                    .onPageChange(PdfActivity2.this)
                    .enableAnnotationRendering(true)
                    .onLoad(PdfActivity2.this)
                    .scrollHandle(new DefaultScrollHandle(PdfActivity2.this))
                    .spacing(10) // in dp
                    .enableAnnotationRendering(false)
                    .enableAntialiasing(true)
                    .load();
            progressDialog.dismiss();
        }
    }
    private class DownloadFile extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... strings) {
            String fileUrl = strings[0];   // -> http://maven.apache.org/maven-1.x/maven.pdf
            String fileName = strings[1];  // -> maven.pdf
            String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
            File folder = new File(extStorageDirectory, "testthreepdf");
            folder.mkdir();

            File pdfFile = new File(folder, fileName);

            try{
                pdfFile.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
            downloadFile(fileUrl, pdfFile);
            return null;
        }
        public void downloadFile(String fileUrl, File directory){
            try {

                URL url = new URL(fileUrl);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                //urlConnection.setRequestMethod("GET");
                //urlConnection.setDoOutput(true);
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                FileOutputStream fileOutputStream = new FileOutputStream(directory);
                int totalSize = urlConnection.getContentLength();

                byte[] buffer = new byte[MEGABYTE];
                int bufferLength = 0;
                while((bufferLength = inputStream.read(buffer))>0 ){
                    fileOutputStream.write(buffer, 0, bufferLength);
                }
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}