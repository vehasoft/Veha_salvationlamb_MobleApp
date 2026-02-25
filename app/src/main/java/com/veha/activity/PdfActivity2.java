package com.veha.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.listener.OnErrorListener;
import com.alamin5g.pdf.listener.OnLoadCompleteListener;
import com.alamin5g.pdf.listener.OnPageChangeListener;
import com.alamin5g.pdf.listener.OnDownloadProgressListener;


public class PdfActivity2 extends Activity implements OnPageChangeListener, OnLoadCompleteListener {

    PDFView pdfView = null;
    TextView tv_header = null;
    String pdfFileName;
    int pageNumber = 0;
    String url;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_pdf2);
        pdfView = findViewById(R.id.pdfView);
        tv_header = findViewById(R.id.pdf_header);
        tv_header.setOnClickListener(v -> {});
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        pdfFileName = intent.getStringExtra("fileName");
        tv_header.setText(pdfFileName);

        // Show progress dialog while downloading
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Fetching PDF from server...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        loadPdfFromUrl(url);
    }

    private void loadPdfFromUrl(String pdfUrl) {
        pdfView.fromUrl(pdfUrl)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .continuousScroll(true)
                .enableDoubletap(true)
                .defaultPage(pageNumber)
                .enableAntialiasing(true)
                .spacing(10)
                .autoSpacing(true)
                .pageFitPolicy(PDFView.FitPolicy.WIDTH)
                .pageFling(false)
                .pageSnap(false)
                .onDownloadProgress(new OnDownloadProgressListener() {
                    @Override
                    public void onDownloadProgress(long bytesDownloaded, long totalBytes, int progress) {
                        if (progress >= 0) {
                            progressDialog.setMessage("Downloading PDF... " + progress + "%");
                        } else {
                            progressDialog.setMessage("Downloaded: " + (bytesDownloaded / 1024) + " KB");
                        }
                    }
                })
                .onPageChange(PdfActivity2.this)
                .onLoad(PdfActivity2.this)
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        dismissProgressDialog();
                        Log.e("PDF", "PDFView error", t);
                        Toast.makeText(PdfActivity2.this, "Error loading PDF", Toast.LENGTH_SHORT).show();
                        PdfActivity2.this.finish();
                    }
                })
                .load();
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        dismissProgressDialog();
        Log.d("PDF", "Loaded " + nbPages + " pages");

        // Workaround: The library's loadFromFile() only renders a single page
        // and never calls initializePageOffsets() for continuous scroll mode.
        // Use reflection to call the private methods that set up multi-page scrolling.
        pdfView.post(() -> {
            try {
                java.lang.reflect.Method initOffsets =
                        pdfView.getClass().getDeclaredMethod("initializePageOffsets");
                initOffsets.setAccessible(true);
                initOffsets.invoke(pdfView);

                java.lang.reflect.Method renderVisible =
                        pdfView.getClass().getDeclaredMethod("renderVisiblePages");
                renderVisible.setAccessible(true);
                renderVisible.invoke(pdfView);

                Log.d("PDF", "Continuous scroll mode initialized via reflection");
            } catch (Exception e) {
                Log.e("PDF", "Reflection workaround failed, using layout toggle fallback", e);
                // Fallback: toggle view height to force onSizeChanged() re-fire
                int h = pdfView.getHeight();
                android.view.ViewGroup.LayoutParams lp = pdfView.getLayoutParams();
                lp.height = h - 1;
                pdfView.setLayoutParams(lp);
                pdfView.post(() -> {
                    lp.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
                    pdfView.setLayoutParams(lp);
                });
            }
        });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        if (pdfView != null) {
            pdfView.recycle();
        }
    }

}