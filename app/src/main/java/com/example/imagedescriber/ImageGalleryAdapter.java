package com.example.imagedescriber;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.ViewHolder> {

    private Context context;
    private List<ImageEntry> imageList;

    public ImageGalleryAdapter(Context context, List<ImageEntry> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    public void setFilteredList(List<ImageEntry> filteredList) {
        this.imageList = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ImageEntry entry = imageList.get(position);
        holder.title.setText(entry.getTitle());
        holder.description.setText(entry.getDescription());  // New: set description

        String imagePath = entry.getImagePath();

        Glide.with(context)
                .load(imagePath)
                .placeholder(R.drawable.image_placeholder_background)
                .error(R.drawable.error_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewImageActivity.class);
            intent.putExtra("IMAGE_ID", entry.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title;
        TextView description; // New field for disease/description

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            title = itemView.findViewById(R.id.title_text);
            description = itemView.findViewById(R.id.description_text); // bind new view
        }
    }
}
