package com.example.imagedescriber;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PDFUtils {

    public static File generatePDF(Context context, ImageEntry entry) {
        File pdfFile = new File(context.getExternalFilesDir(null), "Report_" + System.currentTimeMillis() + ".pdf");

        try {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            PdfContentByte canvas = writer.getDirectContent();

            // ✅ Draw page border
            Rectangle rect = new Rectangle(document.left(), document.bottom(), document.right(), document.top());
            rect.setBorder(Rectangle.BOX);
            rect.setBorderWidth(1);
            rect.setBorderColor(BaseColor.GRAY);
            canvas.rectangle(rect);

            // ✅ Add logo (optional)
            try {
                InputStream logoStream = context.getAssets().open("logo_app.png");
                Bitmap logoBitmap = BitmapFactory.decodeStream(logoStream);
                ByteArrayOutputStream logoBytes = new ByteArrayOutputStream();
                logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, logoBytes);
                Image logo = Image.getInstance(logoBytes.toByteArray());
                logo.scaleToFit(80, 80);
                logo.setAlignment(Image.ALIGN_LEFT);
                document.add(logo);
            } catch (Exception e) {
                Log.w("PDFUtils", "Logo not found, skipping.");
            }

            // ✅ Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Paragraph title = new Paragraph("ರೈತಮಿತ್ರ - Field Report", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // ✅ Load image (remote/local)
            try {
                Bitmap bitmap = null;

                if (entry.getImagePath() != null && entry.getImagePath().startsWith("http")) {
                    bitmap = Glide.with(context)
                            .asBitmap()
                            .load(entry.getImagePath())
                            .submit()
                            .get();
                } else if (entry.getImagePath() != null) {
                    Uri uri = Uri.parse(entry.getImagePath());
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }

                if (bitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                    Image img = Image.getInstance(stream.toByteArray());
                    img.scaleToFit(400, 400);
                    img.setAlignment(Image.ALIGN_CENTER);
                    document.add(img);
                    document.add(new Paragraph("\n"));
                }

            } catch (Exception e) {
                Log.e("PDFUtils", "Image load failed: " + e.getMessage());
            }

            // ✅ Format timestamp
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(entry.getTimestamp()));

            // ✅ Metadata Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 12);

            addTableRow(table, "Plant Name:", entry.getTitle(), labelFont, valueFont);
            addTableRow(table, "Timestamp:", formattedDate, labelFont, valueFont);
            addTableRow(table, "Farmer Name:", entry.getFarmerName(), labelFont, valueFont);
            addTableRow(table, "Disease:", entry.getPlantDisease(), labelFont, valueFont);
            addTableRow(table, "Location:", entry.getLocation(), labelFont, valueFont);
            addTableRow(table, "Description:", entry.getDescription(), labelFont, valueFont);
            addTableRow(table, "Details:", entry.getAdditionalDetails(), labelFont, valueFont);

            document.add(table);

            document.close();
            return pdfFile;

        } catch (Exception e) {
            Log.e("PDFUtils", "PDF generation failed", e);
            return null;
        }
    }

    private static void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Paragraph(label, labelFont));
        PdfPCell cell2 = new PdfPCell(new Paragraph(value != null ? value : "N/A", valueFont));

        cell1.setBorder(Rectangle.BOX);
        cell2.setBorder(Rectangle.BOX);

        cell1.setBackgroundColor(BaseColor.LIGHT_GRAY);

        table.addCell(cell1);
        table.addCell(cell2);
    }
}
