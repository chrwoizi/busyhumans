package com.c5000.mastery.client.components.picture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class PictureV extends BasePictureV {
    interface ThisUiBinder extends UiBinder<Widget, PictureV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField FocusPanel outer;
    @UiField HTMLPanel inner;
    @UiField Anchor imageLink;
    @UiField Image image;
    @UiField Button licenseButton;
    @UiField SimplePanel licensePos;

    public PictureV() {
        initWidget(uiBinder.createAndBindUi(this));
        setDefault();
        setSize(110, 110, true, true);
    }

    @Override
    protected FocusPanel getOuter() {
        return outer;
    }

    @Override
    protected HTMLPanel getInner() {
        return inner;
    }

    @Override
    protected Anchor getImageLink() {
        return imageLink;
    }

    @Override
    protected Image getImage() {
        return image;
    }

    @Override
    protected Button getLicenseButton() {
        return licenseButton;
    }

    @Override
    protected SimplePanel getLicensePos() {
        return licensePos;
    }

    @UiHandler("outer")
    void pictureLargeOuterMouseOver(MouseOverEvent event) {
        super.onPictureLargeOuterMouseOver(event);
    }

    @UiHandler("outer")
    void pictureLargeOuterMouseOut(MouseOutEvent event) {
        super.onPictureLargeOuterMouseOut(event);
    }

    @UiHandler("licenseButton")
    void pictureLicenseClick(ClickEvent event) {
        super.onPictureLicenseClick(event);
    }

    @UiHandler("image")
    void imageLoaded(LoadEvent event) {
        super.onImageLoaded(event);
    }

    @Override
    protected int getBorderWidth() {
        return 1;
    }
}