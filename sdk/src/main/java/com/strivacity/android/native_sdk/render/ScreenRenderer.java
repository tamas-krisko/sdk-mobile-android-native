package com.strivacity.android.native_sdk.render;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.strivacity.android.native_sdk.render.models.BrandingModel;
import com.strivacity.android.native_sdk.render.models.FormModel;
import com.strivacity.android.native_sdk.render.models.LayoutModel;
import com.strivacity.android.native_sdk.render.models.WidgetModel;
import com.strivacity.android.native_sdk.render.widgets.EditableWidget;
import com.strivacity.android.native_sdk.render.widgets.LayoutWidget;
import com.strivacity.android.native_sdk.render.widgets.Widget;
import com.strivacity.android.native_sdk.util.HttpClient;
import com.strivacity.android.native_sdk.util.JSON;
import com.strivacity.android.native_sdk.util.Logging;

import org.json.JSONObject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScreenRenderer {

    private final ViewFactory viewFactory;
    private final ViewGroup parentLayout;
    private final Logging logging;
    private final Consumer<Form> sendFormAction;
    private final Consumer<Uri> finalizeAction;
    private final Runnable closeFlowAction;

    private BrandingModel brandingModel;
    private Map<String, Form> forms;
    private LayoutWidget layout;

    /**
     * Screen ID for the last set of forms that were rendered
     */
    private String lastScreenId;

    @Getter
    private Uri fallbackUrl;

    public void showScreen(HttpClient.HttpResponse httpResponse) {
        if (httpResponse.getResponseCode() == 200) {
            try {
                JSON json = new JSON(new JSONObject(httpResponse.getBody()));
                fallbackUrl = Uri.parse(json.string("hostedUrl"));

                if (!json.isNull("finalizeUrl")) {
                    finalizeAction.accept(Uri.parse(json.string("finalizeUrl")));
                } else {
                    showScreen(json);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException();
        }
    }

    private void showScreen(JSON json) {
        if (json.isNull("layout") && json.isNull("messages")) {
            throw new RuntimeException();
        }

        viewFactory.getContext().getMainExecutor().execute(() -> setEnabled(parentLayout, true));

        if (!json.isNull("branding")) {
            brandingModel = new BrandingModel(json.object("branding"));
        }

        if (!json.isNull("layout") && !json.isNull("forms")) {
            LayoutModel.SingleLayoutModel layout = new LayoutModel.SingleLayoutModel(json.object("layout"));
            List<FormModel> forms = json.list("forms").stream().map(FormModel::new).collect(Collectors.toList());

            render(parentLayout, forms, layout, json.string("screen"));
        }

        forms.forEach((key, value) ->
            value
                .getWidgets()
                .values()
                .forEach(widget -> {
                    if (widget instanceof EditableWidget) {
                        viewFactory
                            .getContext()
                            .getMainExecutor()
                            .execute(() -> {
                                ((EditableWidget) widget).clearError();
                            });
                    }
                })
        );

        if (!json.isNull("messages")) {
            showErrorMessages(json.object("messages"));
        }
    }

    private void showErrorMessages(JSON messages) {
        logging.info(String.format("Updating screen `%s` with messages", lastScreenId));
        messages
            .keys()
            .forEach(formId -> {
                JSON formErrors = messages.object(formId);

                if ("global".equalsIgnoreCase(formId)) {
                    if ("error".equals(formErrors.string("type"))) {
                        viewFactory
                            .getContext()
                            .getMainExecutor()
                            .execute(() ->
                                Toast
                                    .makeText(
                                        viewFactory.getContext().getApplicationContext(),
                                        formErrors.string("text"),
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            );
                        return;
                    } else {
                        throw new RuntimeException();
                    }
                }

                formErrors
                    .keys()
                    .forEach(widgetId -> {
                        JSON widgetError = formErrors.object(widgetId);
                        if ("error".equals(widgetError.string("type"))) {
                            EditableWidget editable = (EditableWidget) forms.get(formId).getWidgets().get(widgetId);

                            viewFactory
                                .getContext()
                                .getMainExecutor()
                                .execute(() -> {
                                    editable.showError(widgetError.string("text"));
                                });
                        } else {
                            throw new RuntimeException();
                        }
                    });
            });

        // Focus on the first error field in the layout

        for (Map.Entry<String, Form> form : forms.entrySet()) {
            for (WidgetModel widgetModel : form.getValue().getModel().getWidgets()) {
                Widget widget = form.getValue().getWidgets().get(widgetModel.getId());
                if (widget instanceof EditableWidget && !((EditableWidget) widget).isValid()) {
                    viewFactory.getContext().getMainExecutor().execute(() -> widget.getView().requestFocus());
                    break;
                }
            }
        }
    }

    private void render(
        ViewGroup parentLayout,
        List<FormModel> formModels,
        LayoutModel.SingleLayoutModel singleLayoutModel,
        String screenId
    ) {
        logging.info(String.format("Displaying screen `%s`", screenId));
        forms =
            formModels
                .stream()
                .map(formModel -> new Form(formModel, viewFactory, brandingModel, screenId))
                .collect(Collectors.toMap(Form::getId, Function.identity()));

        forms.forEach((formId, form) ->
            form.setOnClickListeners(
                v -> {
                    setEnabled(parentLayout, false);
                    sendFormAction.accept(form);
                },
                v -> closeFlowAction.run()
            )
        );

        layout = viewFactory.layoutWidget(forms, brandingModel, singleLayoutModel);

        viewFactory
            .getContext()
            .getMainExecutor()
            .execute(() -> {
                parentLayout.removeAllViews();
                layout.render(parentLayout);
            });

        lastScreenId = screenId;
    }

    public static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int idx = 0; idx < group.getChildCount(); idx++) {
                setEnabled(group.getChildAt(idx), enabled);
            }
        }
    }

    public void clear() {
        viewFactory.getContext().getMainExecutor().execute(parentLayout::removeAllViews);
    }
}
