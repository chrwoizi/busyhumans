package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.shared.data.base.LicenseTypes;
import com.c5000.mastery.shared.data.base.ResourceD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;

public class LicenseV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, LicenseV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField FocusPanel outer;
    @UiField Anchor authorUrl;
    @UiField Anchor licenseUrl;

    private ResourceD resource;

    public LicenseV(ResourceD resource) {
        this.resource = resource;
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        authorUrl.setText(resource.authorName);
        authorUrl.setHref(resource.authorUrl);
        authorUrl.setTarget("_blank");
        licenseUrl.setText(getLicenseText(resource.license));
        licenseUrl.setHref(getLicenseUrl(resource.license));
        licenseUrl.setTarget("_blank");
    }

    @UiHandler("outer")
    void outerMouseOut(MouseOutEvent event) {
        removeFromParent();
    }

    private String getLicenseText(int licenseType) {
        switch (licenseType) {
            case LicenseTypes.NONE:
                return "None";
            case LicenseTypes.CC_BY_30:
                return "CC BY";
            case LicenseTypes.CC_BY_SA_30:
                return "CC BY-SA";
            case LicenseTypes.CC_BY_ND_30:
                return "CC BY-ND";
            default:
                return "Unknown";
        }
    }

    private String getLicenseUrl(int licenseType) {
        switch (licenseType) {
            case LicenseTypes.NONE:
                return "";
            case LicenseTypes.CC_BY_30:
                return "http://creativecommons.org/licenses/by/3.0/";
            case LicenseTypes.CC_BY_SA_30:
                return "http://creativecommons.org/licenses/by-sa/3.0/";
            case LicenseTypes.CC_BY_ND_30:
                return "http://creativecommons.org/licenses/by-nd/3.0/";
            default:
                return "";
        }
    }

}