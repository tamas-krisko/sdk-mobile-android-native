package com.strivacity.android.native_sdk.render;

import android.view.View;

import com.strivacity.android.native_sdk.render.models.BrandingModel;
import com.strivacity.android.native_sdk.render.models.FormModel;
import com.strivacity.android.native_sdk.render.models.WidgetModel;
import com.strivacity.android.native_sdk.render.widgets.CloseWidget;
import com.strivacity.android.native_sdk.render.widgets.EditableWidget;
import com.strivacity.android.native_sdk.render.widgets.SubmitWidget;
import com.strivacity.android.native_sdk.render.widgets.Widget;
import com.strivacity.android.native_sdk.util.JSON;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Form {

    private final FormModel model;
    private final Map<String, Widget> widgets;

    public Form(FormModel model, ViewFactory viewFactory, BrandingModel brandingModel, String screenId) {
        this.model = model;
        this.widgets =
            model
                .getWidgets()
                .stream()
                .collect(
                    Collectors.toMap(
                        WidgetModel::getId,
                        widgetModel -> viewFactory.widget(widgetModel, brandingModel, screenId, widgetModel.getId())
                    )
                );
    }

    public String getId() {
        return model.getId();
    }

    public void setOnClickListeners(View.OnClickListener onClickListener, View.OnClickListener onCloseClickListener) {
        getWidgets()
            .values()
            .stream()
            .filter(value -> value instanceof SubmitWidget)
            .forEach(value -> ((SubmitWidget) value).setOnClickListener(onClickListener));

        getWidgets()
            .values()
            .stream()
            .filter(value -> value instanceof CloseWidget)
            .forEach(value -> ((CloseWidget) value).setOnClickListener(onCloseClickListener));
    }

    public JSONObject requestBody() {
        JSONObject requestBody = new JSONObject();

        getWidgets()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() instanceof EditableWidget)
            .forEach(entry -> {
                try {
                    EditableWidget editable = (EditableWidget) entry.getValue();
                    if (!editable.isReadonly()) {
                        Object value = editable.getValue();
                        if (value != null && !"".equals(value)) {
                            JSON.put(requestBody, entry.getKey(), value);
                        }
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });

        return requestBody;
    }
}
