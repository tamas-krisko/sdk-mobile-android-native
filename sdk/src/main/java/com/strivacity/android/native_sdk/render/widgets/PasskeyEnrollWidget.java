package com.strivacity.android.native_sdk.render.widgets;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.strivacity.android.native_sdk.render.models.WidgetModel;
import com.strivacity.android.native_sdk.util.JSON;

import lombok.Getter;

public class PasskeyEnrollWidget extends Widget {

    @Getter
    private final JSON enrollOptions;

    public PasskeyEnrollWidget(Context context, WidgetModel.PasskeyEnrollWidgetModel widgetModel) {
        super(context);
        this.enrollOptions = widgetModel.getEnrollOptions();
        switch (widgetModel.getRender().getType()) {
            case "button":
                setView(new Button(context));
                break;
            case "link":
                setView(new TextView(context));
                break;
            default:
                throw new RuntimeException("Unknown render type " + widgetModel.getRender());
        }

        this.<TextView>typedView().setText(widgetModel.getLabel());
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.<TextView>typedView().setOnClickListener(onClickListener);
    }
}
