package org.clashledger.app;

import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

/** Keep large exports out of OEM text layout and keyboard extracted-text buffers. */
public final class ExportEditText extends EditText {
    private String loaded;
    private boolean replacing;
    public ExportEditText(Context context){
        super(context);
        setSaveEnabled(false);
        setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI|android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable value){
                if(replacing)return;
                loaded=null;
                if(value.length()>4096)loadExport(value.toString());
            }
        });
    }
    public String exportText(){return loaded==null?getText().toString():loaded;}
    public void loadExport(String value){
        if(value.length()>1_000_000){setError("Export exceeds 1 MB. Copy only the village JSON.");return;}
        replacing=true;
        loaded=value.length()>4096?value:null;
        setText(loaded==null?value:"Export loaded · "+value.length()+" characters\nTap Preview import to review your village.\nUse Clear to enter another export.");
        setFocusableInTouchMode(loaded==null);setFocusable(loaded==null);setCursorVisible(loaded==null);
        if(loaded!=null){clearFocus();((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);}
        replacing=false;setError(null);
    }
    public void pasteExport(boolean replaceAll) {
        try {
            ClipData clip=getContext().getSystemService(ClipboardManager.class).getPrimaryClip();
            if(clip==null||clip.getItemCount()==0){setError("Copy the village export first.");return;}
            CharSequence value=clip.getItemAt(0).getText();
            if(value==null){setError("Clipboard has no JSON text.");return;}
            int start=replaceAll||loaded!=null?0:Math.max(0,Math.min(getSelectionStart(),getSelectionEnd()));
            int end=replaceAll||loaded!=null?length():Math.max(start,Math.max(getSelectionStart(),getSelectionEnd()));
            if((long)length()-(end-start)+value.length()>1_000_000){setError("Export exceeds 1 MB. Copy only the village JSON.");return;}
            loadExport(getText().toString().substring(0,start)+value.toString()+getText().toString().substring(end));
            if(loaded==null)setSelection(Math.min(length(),start+value.length()));
        }catch(SecurityException e){setError("Android blocked clipboard access. Try Choose file instead.");}
    }
    @Override public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo info){
        android.view.inputmethod.InputConnection connection=super.onCreateInputConnection(info);
        return connection==null?null:new android.view.inputmethod.InputConnectionWrapper(connection,false){
            @Override public boolean commitText(CharSequence text,int position){
                if(text!=null&&text.length()>4096){loadExport(text.toString());return true;}
                return super.commitText(text,position);
            }
        };
    }
    @Override public boolean onTextContextMenuItem(int id) {
        if(id==android.R.id.paste||id==android.R.id.pasteAsPlainText){pasteExport(false);return true;}
        return super.onTextContextMenuItem(id);
    }
}
