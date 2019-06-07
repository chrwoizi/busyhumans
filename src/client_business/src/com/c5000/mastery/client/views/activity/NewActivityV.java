package com.c5000.mastery.client.views.activity;

import com.c5000.mastery.client.Loggers;
import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.api.google.GoogleApi;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.components.upload.FileUploadV;
import com.c5000.mastery.client.components.upload.VideoRecorder;
import com.c5000.mastery.client.components.upload.VideoSelect;
import com.c5000.mastery.client.components.upload.VideoUpload;
import com.c5000.mastery.client.events.*;
import com.c5000.mastery.client.events.auth.AuthProviderCredentialValidChangedE;
import com.c5000.mastery.shared.FileParts;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.Sanitizer;
import com.c5000.mastery.shared.data.base.ContentBlockD;
import com.c5000.mastery.shared.data.base.PersonD;
import com.c5000.mastery.shared.data.base.ResourceD;
import com.c5000.mastery.shared.data.base.TokenizedResourceD;
import com.c5000.mastery.shared.data.combined.ActivityVD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

public class NewActivityV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, NewActivityV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Hyperlink authorName;
    @UiField PictureV authorPicture;
    @UiField HTMLPanel contentPanel;
    @UiField Button create;
    @UiField FileUploadV uploadPicture;
    @UiField VideoUpload uploadVideo;
    @UiField Button recordVideoButton;
    @UiField VideoRecorder recordVideo;
    @UiField Button selectVideoButton;
    @UiField VideoSelect selectVideo;
    @UiField TextArea textEdit;
    @UiField HTMLPanel contentInner;
    @UiField HTMLPanel needsGoogleAuth;
    @UiField HTMLPanel hasGoogleAuth;
    @UiField HTMLPanel videoButtons;
    @UiField Button googleAuth;

    private String assignmentId;
    private PersonD author;

    public NewActivityV(String assignmentId, PersonD author) {
        this.assignmentId = assignmentId;
        this.author = author;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        authorPicture.setSize(50, 50, true, true);
        uploadPicture.setTitle("Upload");
        uploadPicture.setSize(80, 22);
        uploadVideo.setAssignment(assignmentId);
        bind();
    }

    private void bind() {
        authorPicture.setLink("#person=" + author.id, null);
        authorPicture.set(author.picture, ImageHelper.Size.SMALL);
        authorName.setText(author.name);
        authorName.setTargetHistoryToken("person=" + author.id);
        onGoogleAuthChanged();
    }

    private void onGoogleAuthChanged() {
        if (Auth.google.credentialValidatedByServer()) {
            needsGoogleAuth.setVisible(false);
            hasGoogleAuth.setVisible(true);
        } else {
            needsGoogleAuth.setVisible(true);
            hasGoogleAuth.setVisible(false);
        }
    }

    @UiHandler("textEdit")
    void textEditChanged(ChangeEvent event) {
        String text = extractVideos(textEdit.getText());
        text = Sanitizer.activityText(text);
        if (!textEdit.getText().equals(text)) {
            textEdit.setText(text != null ? text : "");
        }
    }

    private static final String YOUTUBE_REGEX = "(^|\\s)((http(s)?://)?(www\\.)?youtu(be.\\w+|.be)/(watch\\?v=|embed/)?(([\\w-])+)([&#][&#=\\-%\\w]*)*)($|\\s|\\.)";

    private String extractVideos(String text) {
        String result = text;

        RegExp regExp = RegExp.compile(YOUTUBE_REGEX, "gm");
        for (MatchResult matcher = regExp.exec(text); matcher != null; matcher = regExp.exec(text)) {
            String videoId = matcher.getGroup(8);
            String videoUrl = matcher.getGroup(2);
            addVideo(videoId, false);
            result = result.replace(videoUrl, "");
        }

        return result;
    }

    @UiHandler("textEdit")
    void textEditKeyDown(KeyDownEvent event) {
        if (event != null && event.getNativeKeyCode() == 13) {
            event.preventDefault();
        }
    }

    @UiHandler("textEdit")
    void textEditKeyUp(KeyUpEvent event) {
        String text = textEdit.getText();
        if (text.length() > Sanitizer.MAX_ACTIVITY_TEXT_LENGTH) {
            textEdit.setText(text.substring(0, Sanitizer.MAX_ACTIVITY_TEXT_LENGTH));
        }
    }

    @UiHandler("googleAuth")
    void googleAuthClicked(ClickEvent event) {
        GoogleApi.login();
    }

    @UiHandler("selectVideoButton")
    void selectVideoButtonClicked(ClickEvent event) {
        selectVideo.start(
                new SimpleAsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        selectVideo.setVisible(true);
                        videoButtons.setVisible(false);
                    }
                },
                new SimpleAsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        selectVideo.setVisible(false);
                        videoButtons.setVisible(true);
                    }
                }
        );
    }

    @UiHandler("recordVideoButton")
    void recordVideoButtonClicked(ClickEvent event) {
        recordVideo.start(
                new SimpleAsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        recordVideo.setVisible(true);
                        videoButtons.setVisible(false);
                    }
                },
                new SimpleAsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        recordVideo.setVisible(false);
                        videoButtons.setVisible(true);
                    }
                }
        );
    }

    @UiHandler("create")
    void createClicked(ClickEvent event) {

        ArrayList<ContentBlockD> contentBlocks = new ArrayList<ContentBlockD>();

        ContentBlockD textBlock = new ContentBlockD();
        textBlock.typ = ContentBlockD.TYPE_TEXT;
        textBlock.value = createResource(Sanitizer.activityText(textEdit.getText()), null);
        contentBlocks.add(textBlock);

        for (int i = 0; i < contentPanel.getWidgetCount(); ++i) {
            ContentBlockBaseV view = (ContentBlockBaseV) contentPanel.getWidget(i);
            contentBlocks.add(view.block);
        }

        if (contentBlocks.size() == 1 && textBlock.value.resource.resource == null) {
            Window.alert("Please describe your activity with text, pictures or videos.");
        } else {
            IMasteryS.Instance.get().createActivity(assignmentId, contentBlocks, new SimpleAsyncCallback<ActivityVD>() {
                @Override
                public void onSuccess(ActivityVD result) {
                    if (result != null)
                        MasteryEvents.dispatch(new ActivityCreatedE(result.activity, result.author));
                }
            });

            uploadPicture.setEnabled(false);
            uploadVideo.setEnabled(false);
            recordVideoButton.setEnabled(false);
            recordVideo.setEnabled(false);
            selectVideoButton.setEnabled(false);
            selectVideo.setEnabled(false);
            textEdit.setEnabled(false);
            googleAuth.setEnabled(false);
            create.setEnabled(false);
        }
    }

    private TokenizedResourceD createResource(String value, String token) {
        ResourceD resource = new ResourceD();
        resource.resource = value;
        TokenizedResourceD result = new TokenizedResourceD();
        result.token = token;
        result.resource = resource;
        return result;
    }

    private TokenizedResourceD createDbImageResource(String value, String token) {
        ResourceD image = new ResourceD();
        image.resource = value;
        image.small = value + "/" + FileParts.SMALL;
        image.medium = value + "/" + FileParts.MEDIUM;
        image.large = value + "/" + FileParts.LARGE;
        image.hires = value + "/" + FileParts.HIRES;
        TokenizedResourceD result = new TokenizedResourceD();
        result.token = token;
        result.resource = image;
        return result;
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof NewContentBlockDeletedE) {
            ContentBlockBaseV deletedView = ((NewContentBlockDeletedE) event).view;
            for (int i = 0; i < contentPanel.getWidgetCount(); ++i) {
                ContentBlockBaseV view = (ContentBlockBaseV) contentPanel.getWidget(i);
                if (deletedView == view) {
                    contentPanel.remove(i);
                    break;
                }
            }
        } else if (event instanceof FileUploadCompleteE) {
            FileUploadCompleteE e = (FileUploadCompleteE) event;
            if (e.sender == uploadPicture) {
                if (e.fileId != null) {
                    ContentBlockD block = new ContentBlockD();
                    block.typ = ContentBlockD.TYPE_IMAGE;
                    block.value = createDbImageResource("mastery/res/dynamic/" + e.fileId, e.fileId);
                    block.ready = true;
                    block.error = null;
                    block.authentic = true;
                    addContentBlock(block);
                }
            }
        } else if (event instanceof VideoUploadCompleteE) {
            VideoUploadCompleteE e = (VideoUploadCompleteE) event;
            if (e.sender == uploadVideo) {
                if (e.fileId != null) {
                    addVideo(e.fileId, true);
                }
            }

        } else if (event instanceof VideoRecordingCompleteE) {
            VideoRecordingCompleteE e = (VideoRecordingCompleteE) event;
            if (e.sender == recordVideo) {
                if (e.video != null) {
                    ContentBlockD block = new ContentBlockD();
                    block.typ = ContentBlockD.TYPE_VIDEO;
                    block.value = createResource(e.video.id, e.video.id);
                    block.ready = e.video.ready;
                    block.error = e.video.error;
                    block.authentic = true;
                    addContentBlock(block);
                }
            }
        } else if (event instanceof VideoSelectedE) {
            VideoSelectedE e = (VideoSelectedE) event;
            if (e.sender == selectVideo) {
                if (e.video != null) {
                    ContentBlockD block = new ContentBlockD();
                    block.typ = ContentBlockD.TYPE_VIDEO;
                    block.value = createResource(e.video.id, e.video.id);
                    block.ready = e.video.ready;
                    block.error = e.video.error;
                    block.authentic = true;
                    addContentBlock(block);
                }
            }
        } else if (event instanceof AuthProviderCredentialValidChangedE) {
            if (((AuthProviderCredentialValidChangedE) event).provider == Auth.google)
                onGoogleAuthChanged();
        }
    }

    private void addVideo(String videoId, boolean authentic) {
        ContentBlockD block = new ContentBlockD();
        block.typ = ContentBlockD.TYPE_VIDEO;
        block.value = createResource(videoId, videoId);
        block.ready = false;
        block.error = null;
        block.authentic = authentic;
        addContentBlock(block);
    }

    private void addContentBlock(ContentBlockD block) {
        switch (block.typ) {
            case ContentBlockD.TYPE_IMAGE:
                contentPanel.add(new ImageContentBlockV(block, true));
                break;
            case ContentBlockD.TYPE_VIDEO:
                contentPanel.add(new VideoContentBlockV(block, true));
                break;
        }
    }

}