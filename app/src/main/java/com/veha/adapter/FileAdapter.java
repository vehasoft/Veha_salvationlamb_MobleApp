package com.veha.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;
import com.veha.activity.FileListActivity;
import com.veha.activity.PdfActivity2;
import com.veha.activity.R;
import com.veha.util.FilesAndFolders;
import com.veha.util.Util;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    Context context;
    ArrayList<FilesAndFolders> filesAndFolders = new ArrayList<>();

    public FileAdapter(Context context, ArrayList<FilesAndFolders> filesAndFolders) {
        this.context = context;
        this.filesAndFolders = filesAndFolders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.child_folders, parent, false);
        ViewHolder views =  new ViewHolder(view);
        if (!Util.listview){
            views.fileLinear.setOrientation(LinearLayout.VERTICAL);
            views.fileLinear.setGravity(Gravity.CENTER);
            views.textView.setTextColor(context.getColor(R.color.black));
        } else {
            views.fileLinear.setOrientation(LinearLayout.HORIZONTAL);
            views.textView.setTextColor(context.getColor(R.color.black));
        }
        return views;
    }

    @Override
    public void onBindViewHolder(FileAdapter.ViewHolder holder, int position) {
        FilesAndFolders selectedFile = filesAndFolders.get(position);
        holder.textView.setText(selectedFile.getName());
        if (selectedFile.getType().equals("folder")) {
            holder.imageView.setImageResource(R.drawable.folder_icon);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
        }
        holder.itemView.setOnClickListener(v -> {
            Log.e("type", selectedFile.getType());
            if (selectedFile.getType().equals("folder")) {
                Intent intent = new Intent(context, FileListActivity.class);
                intent.putExtra("folderId", selectedFile.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Intent intent = new Intent(context, PdfActivity2.class);
                intent.putExtra("url", selectedFile.getUrl());
                intent.putExtra("fileName", selectedFile.getName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filesAndFolders.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;
        LinearLayout fileLinear;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
            fileLinear = itemView.findViewById(R.id.file_linear);
        }
    }
}