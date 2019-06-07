package com.c5000.mastery.client.components.upload;

import com.c5000.mastery.client.events.FileUploadCompleteE;
import com.c5000.mastery.client.events.FileUploadFailedE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import gwtupload.client.*;

public class FileUploader extends SingleUploader implements IUploader.OnFinishUploaderHandler {

    private static FileUploaderConstants constants;

    private class StatusWidget extends BaseUploadStatus {

        @Override
        public void setError(String msg) {
            setStatus(Status.ERROR);
            if(msg.startsWith("The request was rejected because its size")) {
                MasteryEvents.dispatch(new FileUploadFailedE(owner, FileUploadFailedE.Reason.SIZE));
            }
            else {
                MasteryEvents.dispatch(new FileUploadFailedE(owner, FileUploadFailedE.Reason.UNKNOWN));
            }
        }
    }

    private FileUploadV owner;
    private StatusWidget statusWidget;

    public FileUploader(FileUploadV owner, String title) {
        super(IFileInput.FileInputType.BUTTON);
        this.owner = owner;

        constants = new FileUploaderConstants(title);
        setI18Constants(constants);
        setServletPath("mastery/upload");
        setAutoSubmit(true);
        statusWidget = new StatusWidget();
        statusWidget.setI18Constants(constants);
        setStatusWidget(statusWidget);
        addOnFinishUploadHandler(this);
    }

    public void setQuery(String query) {
        setServletPath("mastery/upload" + query);
    }

    public void setTitle(String title) {
        constants.uploaderBrowseValue = title;
        setI18Constants(constants);
        statusWidget.setI18Constants(constants);
    }

    public void onFinish(IUploader uploader) {
        if (uploader.getStatus() == IUploadStatus.Status.SUCCESS) {
            String xmlResponse = uploader.getServerResponse();
            RegExp regExp = RegExp.compile("CDATA\\[\\s(.*)\\s\\]");
            MatchResult match = regExp.exec(xmlResponse);
            if (match.getGroupCount() == 2) {
                String response = match.getGroup(1);
                if (!response.isEmpty()) {
                    MasteryEvents.dispatch(new FileUploadCompleteE(owner, response));
                } else {
                    MasteryEvents.dispatch(new FileUploadFailedE(owner, FileUploadFailedE.Reason.UNKNOWN));
                }
            } else {
                MasteryEvents.dispatch(new FileUploadFailedE(owner, FileUploadFailedE.Reason.UNKNOWN));
            }
        }
        else if(uploader.getStatus() == IUploadStatus.Status.CANCELED) {
            MasteryEvents.dispatch(new FileUploadFailedE(owner, FileUploadFailedE.Reason.TYPE));
        }
        else if(uploader.getStatus() != IUploadStatus.Status.ERROR) {
            MasteryEvents.dispatch(new FileUploadFailedE(owner, FileUploadFailedE.Reason.UNKNOWN));
        }
    }
}
