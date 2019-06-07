package com.c5000.mastery.client.components.upload;

import gwtupload.client.IUploader;

public class FileUploaderConstants implements IUploader.UploaderConstants {

    public String uploaderBrowseValue;

    public FileUploaderConstants(String browse) {
        this.uploaderBrowseValue = browse;
    }

    @Override
    public String uploaderActiveUpload() {
        return "Error 5000";
    }

    @Override
    public String uploaderAlreadyDone() {
        return "Done";
    }

    @Override
    public String uploaderBlobstoreError() {
        return "Error 5001";
    }

    @Override
    public String uploaderBrowse() {
        return uploaderBrowseValue;
    }

    @Override
    public String uploaderInvalidExtension() {
        return "Invalid file.\nOnly these types are allowed:\n";
    }

    @Override
    public String uploaderSend() {
        return "Upload";
    }

    @Override
    public String uploaderServerError() {
        return "Error 5002";
    }

    @Override
    public String submitError() {
        return "Error 5003";
    }

    @Override
    public String uploaderServerUnavailable() {
        return "Error 5004: ";
    }

    @Override
    public String uploaderTimeout() {
        return "Error 5005";
    }

    @Override
    public String uploadLabelCancel() {
        return "Cancel";
    }

    @Override
    public String uploadStatusCanceled() {
        return "Canceled";
    }

    @Override
    public String uploadStatusCanceling() {
        return "Please wait...";
    }

    @Override
    public String uploadStatusDeleted() {
        return "Deleted";
    }

    @Override
    public String uploadStatusError() {
        return "Error 5006";
    }

    @Override
    public String uploadStatusInProgress() {
        return "Uploading...";
    }

    @Override
    public String uploadStatusQueued() {
        return "Please wait...";
    }

    @Override
    public String uploadStatusSubmitting() {
        return "Uploading...";
    }

    @Override
    public String uploadStatusSuccess() {
        return "Done";
    }
}
