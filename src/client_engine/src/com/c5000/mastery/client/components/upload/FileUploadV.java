package com.c5000.mastery.client.components.upload;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class FileUploadV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, FileUploadV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;

    private FileUploader uploader;
    private FileUploadStatus status;

    public FileUploadV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        uploader = new FileUploader(this, "Upload");
        status = new FileUploadStatus();
        uploader.setStatusWidget(status);
        outer.add(uploader);
        setSize(80, 22);
    }

    public void setQuery(String query) {
        uploader.setQuery(query);
    }

    public void setTitle(String title) {
        uploader.setTitle(title);
    }

    public void setSize(int width, int height) {
        uploader.getFileInput().setSize(width + "px", height + "px");
        status.setProgressBarWidth(width - 2);
    }

    public void setEnabled(boolean enabled) {
        uploader.setEnabled(enabled);
    }

}