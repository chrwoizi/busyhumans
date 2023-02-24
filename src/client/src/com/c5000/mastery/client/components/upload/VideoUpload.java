package com.c5000.mastery.client.components.upload;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.VideoUploadCompleteE;
import com.c5000.mastery.client.events.VideoUploadFailedE;
import com.c5000.mastery.shared.PublicGoogleConfig;
import com.c5000.mastery.shared.data.base.VideoUploadFormD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import gwtupload.client.DecoratedFileUpload;

public class VideoUpload extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, VideoUpload> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField FormPanel form;
    @UiField HTMLPanel formInner;
    @UiField Hidden token;
    @UiField HTMLPanel outer;
    @UiField HTMLPanel uploadPanel;
    @UiField HTMLPanel uploadingPanel;
    @UiField HTMLPanel authPanel;

    private DecoratedFileUpload upload;
    private NamedFrame target;
    private String assignmentId;

    public VideoUpload() {
        target = new NamedFrame("ytuploadresult");
        target.setVisible(false);
        initWidget(uiBinder.createAndBindUi(this));
        outer.add(target);

        upload = new DecoratedFileUpload("Upload to YouTube");
        upload.setName("file");
        upload.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                if (upload.getFilename() != null && !upload.getFilename().isEmpty()) {
                    if (Auth.google.credentialValidatedByServer()) {
                        showUploadingPanel();
                        upload();
                    } else {
                        throw new RuntimeException("Must be connected to google before clicking " + VideoUpload.class.getName() + ".");
                    }
                }
            }
        });
        formInner.add(upload);
    }

    public void setAssignment(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        upload.setSize("110px", "22px");
    }

    @UiFactory
    FormPanel makeFormPanel() {
        return new FormPanel(target);
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof VideoUploadCompleteE) {
            VideoUploadCompleteE e = (VideoUploadCompleteE) event;
            if (e.sender == this) {
                showUploadPanel();
            }
        } else if (event instanceof VideoUploadFailedE) {
            VideoUploadFailedE e = (VideoUploadFailedE) event;
            if (e.sender == this) {
                showUploadPanel();
            }
        }
    }

    private void upload() {
        if(upload.getFilename() == null)
            return;

        IMasteryS.Instance.get().getVideoUploadForm(assignmentId, new SimpleAsyncCallback<VideoUploadFormD>() {
            @Override
            public void onSuccess(VideoUploadFormD result) {
                if (result != null) {
                    initResultFrame();
                    token.setValue(result.token);
                    form.setAction(result.url + "?nexturl=" + PublicGoogleConfig.YOUTUBE_REDIRECT);
                    form.submit();
                    showUploadingPanel();
                } else {
                    showUploadPanel();
                }
            }
        });
    }

    private void showUploadingPanel() {
        uploadPanel.setVisible(false);
        authPanel.setVisible(false);
        uploadingPanel.setVisible(true);
    }

    private void showUploadPanel() {
        uploadingPanel.setVisible(false);
        authPanel.setVisible(false);
        uploadPanel.setVisible(true);
    }

    private void showAuthPanel() {
        uploadingPanel.setVisible(false);
        uploadPanel.setVisible(false);
        authPanel.setVisible(true);
    }

    private native void initResultFrame() /*-{
        var _this = this;
        window.top.ytuploadcallback = function (status, id, code) {
            _this.@com.c5000.mastery.client.components.upload.VideoUpload::youtubeUploadCallback(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(status, id, code);
        }
    }-*/;

    void youtubeUploadCallback(String status, String id, String code) {
        if (status.equals("200")) {
            MasteryEvents.dispatch(new VideoUploadCompleteE(this, id));
        } else {
            MasteryEvents.dispatch(new VideoUploadFailedE(this, VideoUploadFailedE.Reason.UNKNOWN));
            Loggers.root.error("Video upload failed: " + status + " - " + code);
        }
    }

    public void setEnabled(boolean enabled) {
        upload.setEnabled(enabled);
    }
}