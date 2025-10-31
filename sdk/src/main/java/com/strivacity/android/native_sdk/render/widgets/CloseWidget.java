package com.strivacity.android.native_sdk.render.widgets;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.strivacity.android.native_sdk.render.models.WidgetModel;

public class CloseWidget extends Widget {

    public CloseWidget(Context context, WidgetModel.CloseWidgetModel widgetModel) {
        super(context);
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
