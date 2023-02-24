package com.c5000.mastery.client.views.skill;

import com.c5000.mastery.client.SimpleAsyncCallback;
import com.c5000.mastery.client.components.picture.PictureV;
import com.c5000.mastery.client.events.MasteryEvents;
import com.c5000.mastery.client.events.PictureSelectedE;
import com.c5000.mastery.client.events.SelectedSkillChangedE;
import com.c5000.mastery.shared.ImageHelper;
import com.c5000.mastery.shared.Sanitizer;
import com.c5000.mastery.shared.data.base.SkillCreationParamsD;
import com.c5000.mastery.shared.data.base.SkillDescriptionD;
import com.c5000.mastery.shared.data.base.TokenizedResourceD;
import com.c5000.mastery.shared.services.IMasteryS;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;

public class NewSkillV extends Composite implements MasteryEvents.Listener {
    interface ThisUiBinder extends UiBinder<Widget, NewSkillV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField Image picture;
    @UiField PictureV pictureLarge;
    @UiField Label title;
    @UiField TextArea description;
    @UiField TextBox pictureSearch;
    @UiField HorizontalPanel skillPictures1;
    @UiField HorizontalPanel skillPictures2;
    @UiField Label noPicturesFound;
    @UiField Label loading;

    private Timer pictureSearchChangedTimer;
    private boolean isWikipediaDescription;

    public NewSkillV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    protected void onAttach() {
        super.onAttach();
        MasteryEvents.subscribeWidget(this);
        pictureLarge.setSize(110, 110, true, true);
    }

    public TokenizedResourceD getPicture() {
        return pictureLarge.get();
    }

    @UiHandler("pictureSearch")
    void pictureSearchKeyUp(KeyUpEvent event) {
        if (pictureSearchChangedTimer != null) {
            pictureSearchChangedTimer.cancel();
            pictureSearchChangedTimer = null;
        }
        pictureSearchChangedTimer = new Timer() {
            @Override
            public void run() {
                pictureSearchChangedTimer = null;
                onPictureSearchChanged();
            }
        };
        pictureSearchChangedTimer.schedule(250);
    }

    private void onPictureSearchChanged() {
        skillPictures1.clear();
        skillPictures2.clear();
        noPicturesFound.setVisible(false);
        loading.setVisible(true);

        IMasteryS.Instance.get().suggestSkillPictures(pictureSearch.getText(), new SimpleAsyncCallback<ArrayList<TokenizedResourceD>>() {

            @Override
            public void onSuccess(ArrayList<TokenizedResourceD> result) {
                skillPictures1.clear();
                skillPictures2.clear();
                loading.setVisible(false);
                if (result.size() > 0) {
                    for (int i = 0; i < result.size() && i < 10; i++) {
                        TokenizedResourceD picture = result.get(i);
                        SmallPictureV v = new SmallPictureV(picture);
                        if (i % 2 == 0) {
                            skillPictures1.add(v);
                        } else {
                            skillPictures2.add(v);
                        }
                        if (pictureLarge.isDefault()) {
                            MasteryEvents.dispatch(new PictureSelectedE(picture));
                        }
                    }
                } else {
                    noPicturesFound.setVisible(true);
                }
            }
        });
    }

    public void setTitle(String title, boolean searchForPictures) {
        this.title.setText(title);
        if (searchForPictures) {
            pictureSearch.setText(title);
            onPictureSearchChanged();

            if (description.getText().isEmpty()) {
                description.setText("Searching Wikipedia...");
                getDescriptionFromWikipedia(title);
                description.setEnabled(false);
            }
        }
    }

    public SkillCreationParamsD getParams() {

        SkillCreationParamsD params = new SkillCreationParamsD();
        params.title = title.getText();
        params.description = getDescription();
        params.pictureToken = pictureLarge.get().token;

        return params;
    }

    private SkillDescriptionD getDescription() {
        SkillDescriptionD result = new SkillDescriptionD();
        result.description = Sanitizer.skillDescription(description.getText());
        result.isWikipedia = isWikipediaDescription;
        return result;
    }

    @UiHandler("description")
    void descriptionKeyUp(KeyUpEvent event) {
        String text = description.getText();
        if (text.length() > Sanitizer.MAX_ACTIVITY_TEXT_LENGTH) {
            description.setText(text.substring(0, Sanitizer.MAX_ACTIVITY_TEXT_LENGTH));
        }
        description.setText(description.getText().replace("\r", "").replace("\n", " "));
    }

    @UiHandler("description")
    void descriptionKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == 13) {
            event.preventDefault();
        }
    }

    @UiHandler("description")
    void descriptionChanged(ChangeEvent event) {
        capitalizeDescription();
        isWikipediaDescription = false;
    }

    private void capitalizeDescription() {
        String clean = Sanitizer.capitalize(description.getText());
        if (!description.getText().equals(clean)) {
            description.setText(clean);
        }
    }

    @Override
    public void onEvent(MasteryEvents.MasteryEvent event) {
        if (event instanceof PictureSelectedE) {
            pictureLarge.set(((PictureSelectedE) event).picture, ImageHelper.Size.MEDIUM);
            picture.setUrl(ImageHelper.getUrl(pictureLarge.get().resource, ImageHelper.Size.SMALL));
            MasteryEvents.dispatch(new SelectedSkillChangedE(true));
        }
    }

    private native void getDescriptionFromWikipedia(String title) /*-{
        var _this = this;
        $wnd.getWikipediaSummary(title, function (summary) {
            _this.@com.c5000.mastery.client.views.skill.NewSkillV::onWikipediaResult(Ljava/lang/String;)(summary.replace(/\[\d+\]/g));
        });
    }-*/;

    private void onWikipediaResult(String summary) {
        if (summary.length() > 50) {
            String first = getFirst3Sentences(summary);
            if (first != null) {
                description.setText(first);
                isWikipediaDescription = true;
            } else {
                description.setText("");
            }
        } else {
            description.setText("");
        }
        description.setEnabled(true);
    }

    private String getFirst3Sentences(String summary) {
        String desc = Sanitizer.skillDescription(summary.substring(0, Sanitizer.MAX_SKILL_DESCRIPTION_LENGTH));
        if (desc == null)
            return null;
        int period = desc.indexOf(".");
        if (period != -1) {
            period = desc.indexOf(".", period + 1);
            if (period != -1) {
                period = desc.indexOf(".", period + 1);
                if (period != -1) {
                    desc = desc.substring(0, period + 1);
                }
            }
        }
        return desc;
    }
}