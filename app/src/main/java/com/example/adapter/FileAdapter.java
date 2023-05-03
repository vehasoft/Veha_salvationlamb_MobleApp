package com.example.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;
import com.example.fbproject.FileListActivity;
import com.example.fbproject.MainActivity;
import com.example.fbproject.PdfActivity2;
import com.example.fbproject.R;
import com.example.util.FilesAndFolders;

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
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileAdapter.ViewHolder holder, int position) {

        FilesAndFolders selectedFile = filesAndFolders.get(position);
        holder.textView.setText(selectedFile.getName());

        if (selectedFile.getType().equals("folder")) {
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
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

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
            Log.e("txtcolor", String.valueOf(textView.getCurrentTextColor()));
            Log.e("txtcolor1", String.valueOf(context.getColor(R.color.black)));
        }
    }
}