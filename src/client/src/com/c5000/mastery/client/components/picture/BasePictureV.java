package com.c5000.mastery.client.components.picture;

import com.c5000.mastery.client.views.skill.LicenseV;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.data.base.LicenseTypes;
import com.c5000.mastery.shared.data.base.ResourceD;
import com.c5000.mastery.shared.data.base.TokenizedResourceD;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.user.client.ui.*;

abstract class BasePictureV extends Composite {

    private boolean isDefault = true;
    private TokenizedResourceD picture;
    private ImageHelper.Size size;
    private int width;
    private int height;
    private int maxWidth = 0;
    private boolean horizontalCenter = false;
    private boolean verticalCenter = false;
    private boolean licenseButtonAllowed = true;

    public void setDefault() {
        picture = new TokenizedResourceD();
        ResourceD image = new ResourceD();
        image.resource = "static/default-skill.png";
        picture.resource = image;
        picture.resource.authorName = "busyhumans.com";
        picture.resource.authorUrl = "http://busyhumans.com";
        picture.resource.license = LicenseTypes.INTERNAL_PICTURE;
        set(picture, null);
        isDefault = true;
    }

    protected abstract FocusPanel getOuter();

    protected abstract HTMLPanel getInner();

    protected abstract Anchor getImageLink();

    protected abstract Image getImage();

    protected abstract Button getLicenseButton();

    protected abstract SimplePanel getLicensePos();

    protected abstract int getBorderWidth();

    protected void onAttach() {
        super.onAttach();
        updateSize();
    }

    protected void onPictureLargeOuterMouseOver(MouseOverEvent event) {
        switch (picture.resource.license) {
            case LicenseTypes.NONE:
            case LicenseTypes.INTERNAL_PICTURE:
            case LicenseTypes.YOUTUBE:
            case LicenseTypes.FACEBOOK:
            case LicenseTypes.TWITTER:
            case LicenseTypes.UNKNOWN:
                getLicenseButton().setVisible(false);
                break;
            default:
                getLicenseButton().setVisible(licenseButtonAllowed);
                break;
        }
    }

    protected void onPictureLargeOuterMouseOut(MouseOutEvent event) {
        getLicenseButton().setVisible(false);
    }

    protected void onPictureLicenseClick(ClickEvent event) {
        getLicenseButton().setVisible(false);
        PopupPanel popup = new PopupPanel(true);
        popup.setWidget(new LicenseV(picture.resource));
        popup.showRelativeTo(getLicensePos());
    }

    protected void onImageLoaded(LoadEvent event) {
        updateSize();
    }

    public void set(TokenizedResourceD picture, ImageHelper.Size size) {
        this.picture = picture;
        this.size = size;
        getImage().setUrl(ImageHelper.getUrl(this.picture.resource, this.size));
        isDefault = false;
    }

    public void set(ResourceD picture, ImageHelper.Size size) {
        this.picture = new TokenizedResourceD();
        this.picture.resource = picture;
        this.size = size;
        getImage().setUrl(ImageHelper.getUrl(this.picture.resource, size));
        isDefault = false;
    }

    public TokenizedResourceD get() {
        return picture;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setLink(String href, String target) {
        if (href != null) {
            getImageLink().getElement().appendChild(getImage().getElement());
            getImageLink().setHref(href);
            if (target != null)
                getImageLink().setTarget(target);
            getImageLink().setVisible(true);
        } else {
            getInner().add(getImage());
            getImageLink().setVisible(false);
        }
    }

    public void setSize(int width, int height, boolean horizontalCenter, boolean verticalCenter) {
        this.width = width;
        this.height = height;
        this.horizontalCenter = horizontalCenter;
        this.verticalCenter = verticalCenter;

        getInner().setWidth(width + "px");
        getInner().setHeight(height + "px");
        getOuter().setWidth((width + 2 * getBorderWidth()) + "px");
        getOuter().setHeight((height + 2 * getBorderWidth()) + "px");
        if (isAttached())
            updateSize();
    }

    public void setNativeHeight(int maxWidth) {
        this.maxWidth = maxWidth;
        width = -1;
        height = -1;
        if (isAttached())
            updateSize();
    }

    private void updateSize() {
        if (width >= 0 && height >= 0) {
            if (horizontalCenter || verticalCenter) {
                if (getImage().getOffsetWidth() > getImage().getOffsetHeight()) {
                    getImage().setWidth("auto");
                    getImage().setHeight(height + "px");
                    if (getImage().getOffsetWidth() > 0 && getImage().getOffsetHeight() > 0) {
                        if (horizontalCenter) {
                            setImagePos((width - getImage().getOffsetWidth()) / 2, 0);
                        } else {
                            setImagePos(0, 0);
                        }
                    }
                } else {
                    getImage().setWidth(width + "px");
                    getImage().setHeight("auto");
                    if (getImage().getOffsetWidth() > 0 && getImage().getOffsetHeight() > 0) {
                        if (verticalCenter) {
                            setImagePos(0, (height - getImage().getOffsetHeight()) / 2);
                        } else {
                            setImagePos(0, 0);
                        }
                    }
                }
            } else {
                getImage().setWidth(width + "px");
                getImage().setHeight(height + "px");
                setImagePos(0, 0);
            }
        } else {
            if (maxWidth > 0) {
                getImage().getElement().getStyle().setProperty("maxWidth", maxWidth + "px");
                getImage().setWidth("auto");
                getImage().setHeight("auto");
                getInner().setWidth(getImage().getOffsetWidth() + "px");
                getInner().setHeight(getImage().getOffsetHeight() + "px");
                getOuter().setWidth((getImage().getOffsetWidth() + 2 * getBorderWidth()) + "px");
                getOuter().setHeight((getImage().getOffsetHeight() + 2 * getBorderWidth()) + "px");
            } else {
                getImage().setWidth("auto");
                getImage().setHeight("auto");
                getInner().setWidth(getImage().getOffsetWidth() + "px");
                getInner().setHeight(getImage().getOffsetHeight() + "px");
                getOuter().setWidth((getImage().getOffsetWidth() + 2 * getBorderWidth()) + "px");
                getOuter().setHeight((getImage().getOffsetHeight() + 2 * getBorderWidth()) + "px");
            }
            setImagePos(0, 0);
        }
    }

    protected void setImagePos(int left, int top) {
        getImage().getElement().getStyle().setMarginLeft(left, Style.Unit.PX);
        getImage().getElement().getStyle().setMarginTop(top, Style.Unit.PX);
    }

    public void setLicenseVisibility(boolean visible) {
        licenseButtonAllowed = visible;
    }
}