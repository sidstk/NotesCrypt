package com.example.sid.NotesCrypt.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.sid.NotesCrypt.utils.CipherEngine;
import com.example.sid.NotesCrypt.R;
import com.example.sid.NotesCrypt.database.model.Note;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.MyViewHolder> {

    private Context context;
    private List<Note> notesList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView noteTitle;

        public TextView timestamp;

        public MyViewHolder(View view) {
            super(view);
            noteTitle = view.findViewById(R.id.note);
            timestamp = view.findViewById(R.id.timestamp);
        }
    }


    public NotesAdapter(Context context, List<Note> notesList) {
        this.context = context;
        this.notesList = notesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Note note = notesList.get(position);



        try {
            holder.noteTitle.setText(CipherEngine.decrypt(note.getTitle()));
        }

        catch(AEADBadTagException e){
            new MaterialDialog.Builder(context)
                    .title("Warning")
                    .iconRes(R.drawable.round_warning_24)
                    .content("Looks like empty field(s) has been modified outside app's scope. Make sure that no 3rd party app has root access.")
                    .show();
        }

        catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IOException | IllegalBlockSizeException | BadPaddingException | KeyStoreException | CertificateException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }


        // Formatting and displaying timestamp
        holder.timestamp.setText(formatDate(note.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }


    /**
     * Formatting timestamp to `MMM d, yyyy` format
     * Input: 2018-07-21 00:15:42
     * Output: Jul 21 2018
     */
    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //fmt.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getDisplayName(false,TimeZone.SHORT)));
            Date date = fmt.parse(dateStr);
            SimpleDateFormat fmtOut = new SimpleDateFormat("MMM d, yyyy");
            return fmtOut.format(date);
        } catch (ParseException e) {

        }

        return "";
    }
}