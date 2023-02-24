package com.c5000.mastery.client.pages;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.auth.Auth;
import com.c5000.mastery.client.auth.CredentialHelper;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.components.upload.FileUploadV;
import com.c5000.mastery.client.events.FileUploadCompleteE;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.shared.Config;
import com.c5000.mastery.shared.FileParts;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.PublicRecaptchaConfig;
import com.c5000.mastery.shared.data.base.AnonRegisterResultD;
import com.c5000.mastery.shared.data.base.ResourceD;
import com.c5000.mastery.shared.data.base.TokenizedResourceD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

public class AnonRegisterP extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, AnonRegisterP> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel form;
    @UiField TextBox email;
    @UiField Label emailError;
    @UiField PasswordTextBox password1;
    @UiField PasswordTextBox password2;
    @UiField Label passwordError;
    @UiField TextBox name;
    @UiField Label nameError;
    @UiField FileUploadV pictureUpload;
    @UiField Label pictureError;
    @UiField PictureV picture;
    @UiField Button register;

    private TokenizedResourceD pictureResource;

    public AnonRegisterP() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        form.setVisible(true);
        createRecaptcha();
        picture.setSize(150, 150, true, true);
        pictureUpload.setTitle("Upload");
        pictureUpload.setSize(80, 22);
        pictureUpload.setQuery("?register=true");
        email.setFocus(true);
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof FileUploadCompleteE) {
            FileUploadCompleteE e = (FileUploadCompleteE) event;
            if (e.sender == pictureUpload) {
                if (e.fileId != null) {
                    pictureResource = createDbImageResource("mastery/res/dynamic/" + e.fileId, e.fileId);
                    picture.set(pictureResource, ImageHelper.Size.MEDIUM);
                }
            }
        }
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

    @UiHandler("register")
    void registerClicked(ClickEvent event) {

        final String emailVal = email.getText();
        final String passwordVal1 = password1.getText();
        String passwordVal2 = password2.getText();
        String nameVal = name.getText();

        String error;

        error = validateEmail(emailVal);
        if (error != null) {
            emailError.setText(error);
            return;
        }
        emailError.setText("");

        if (!passwordVal1.equals(passwordVal2)) {
            passwordError.setText("Both passwords must be equal.");
            return;
        }

        error = validatePassword(passwordVal1);
        if (error != null) {
            passwordError.setText(error);
            return;
        }
        passwordError.setText("");

        error = validateName(nameVal);
        if (error != null) {
            nameError.setText(error);
            return;
        }
        nameError.setText("");

        String pictureToken = null;
        if (pictureResource == null) {
            if (!Config.ENABLE_PICTURES) {
                pictureResource = new TokenizedResourceD();
                pictureResource.token = "default-skill";
                pictureResource.resource = new ResourceD();
                pictureResource.resource.resource = "static/default-skill.png";
                pictureResource.resource.small = pictureResource.resource.resource;
                pictureResource.resource.medium = pictureResource.resource.resource;
                pictureResource.resource.large = pictureResource.resource.resource;
                pictureResource.resource.authorName = "busyhumans.com";
                pictureResource.resource.authorUrl = "http://busyhumans.com";
                pictureResource.resource.license = 41;
            }
            else {
                pictureError.setText("Please upload a picture.");
                return;
            }
        }
        pictureToken = pictureResource.token;
        pictureError.setText("");

        String challenge = Config.ENABLE_CAPTCHAS ? getRecaptchaChallenge() : "disabled";
        String response = Config.ENABLE_CAPTCHAS ? getRecaptchaResponse() : "disabled";

        String passwordHash = CredentialHelper.hashAnonPassword(emailVal, passwordVal1);

        IMasteryS.Instance.get().authWithNewAnon(challenge, response, emailVal, passwordHash, nameVal, pictureToken, new SimpleAsyncCallback<AnonRegisterResultD>() {
            @Override
            public void onSuccess(AnonRegisterResultD result) {
                if (result.registrationError != null) {
                    Window.alert(result.registrationError);
                    if(Config.ENABLE_CAPTCHAS) {
                        destroyRecaptcha();
                        createRecaptcha();
                    }
                } else {
                    Auth.anon.email = emailVal;
                    Auth.anon.password = passwordVal1;
                    Auth.setMasteryAuth(result);
                }
            }
        });
    }

    private String validateEmail(String email) {
        if (!email.matches(Config.EMAIL_REGEX))
            return "The email address seems to be invalid.";
        return null;
    }

    private String validatePassword(String password) {
        if (password.length() < Config.ANON_PASSWORD_LENGTH_MIN)
            return "The password must consist of at least " + Config.ANON_PASSWORD_LENGTH_MIN + " characters.";
        if (password.length() > Config.ANON_PASSWORD_LENGTH_MAX)
            return "The password must not consist of more than " + Config.ANON_PASSWORD_LENGTH_MAX + " characters.";
        return null;
    }

    private String validateName(String name) {
        if (name.trim().length() < Config.ANON_NAME_LENGTH_MIN)
            return "The name must consist of at least " + Config.ANON_NAME_LENGTH_MIN + " characters.";
        if (name.trim().length() > Config.ANON_NAME_LENGTH_MAX)
            return "The name must not consist of more than " + Config.ANON_NAME_LENGTH_MAX + " characters.";
        return null;
    }

    private void createRecaptcha() {
        if(Config.ENABLE_CAPTCHAS) {
            createRecaptcha(PublicRecaptchaConfig.PUBLIC_KEY, "recaptcha");
        }
        else {
            DOM.getElementById("recaptcha").getStyle().setDisplay(Style.Display.NONE);
            DOM.getElementById("recaptcha-label").getStyle().setDisplay(Style.Display.NONE);
        }
    }

    private static native void createRecaptcha(String publicKey, String elementId) /*-{
        Recaptcha = $wnd.Recaptcha;
        Recaptcha.create(publicKey,
                elementId,
                {
                    theme:"clean",
                    callback:undefined //Recaptcha.focus_response_field
                }
        );
    }-*/;

    private static native String getRecaptchaChallenge() /*-{
        return Recaptcha.get_challenge();
    }-*/;

    private static native String getRecaptchaResponse() /*-{
        return Recaptcha.get_response();
    }-*/;

    private static native String destroyRecaptcha() /*-{
        return Recaptcha.destroy();
    }-*/;

}