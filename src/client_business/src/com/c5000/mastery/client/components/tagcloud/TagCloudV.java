package com.c5000.mastery.client.components.tagcloud;

import com.c5000.mastery.shared.data.base.TagD;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import java.util.List;

public class TagCloudV extends Composite {
    interface ThisUiBinder extends UiBinder<Widget, TagCloudV> {}
    private static ThisUiBinder uiBinder = GWT.create(ThisUiBinder.class);

    @UiField HTMLPanel outer;

    public TagCloudV() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public void set(List<TagD> tags, int minFontSizePt, int maxFontSizePt) {
        outer.clear();
        int maxWeight = Integer.MIN_VALUE;
        int minWeight = Integer.MAX_VALUE;
        for(TagD tag : tags) {
            minWeight = Math.min(minWeight, tag.weight);
            maxWeight = Math.max(maxWeight, tag.weight);
        }
        for(TagD tag : tags) {
            Anchor a = new Anchor();
            a.setText(tag.label);
            a.setHref(tag.url);
            double weight = (tag.weight - minWeight) / (float)(maxWeight - minWeight);
            if(tags.size() == 1 || minWeight == maxWeight)
                weight = 0.5;
            double fontSize = minFontSizePt + weight * (maxFontSizePt - minFontSizePt);
            a.getElement().getStyle().setFontSize(fontSize, Style.Unit.PT);
            InlineHTML span = new InlineHTML();
            span.setText(" ");
            span.getElement().insertFirst(a.getElement());
            outer.add(span);
        }
    }

}