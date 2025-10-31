package com.strivacity.android.native_sdk.render;

import android.content.Context;

import com.strivacity.android.native_sdk.render.models.BrandingModel;
import com.strivacity.android.native_sdk.render.models.LayoutModel;
import com.strivacity.android.native_sdk.render.models.WidgetModel;
import com.strivacity.android.native_sdk.render.widgets.CheckboxWidget;
import com.strivacity.android.native_sdk.render.widgets.CloseWidget;
import com.strivacity.android.native_sdk.render.widgets.DateWidget;
import com.strivacity.android.native_sdk.render.widgets.InputWidget;
import com.strivacity.android.native_sdk.render.widgets.LayoutWidget;
import com.strivacity.android.native_sdk.render.widgets.MultiSelectWidget;
import com.strivacity.android.native_sdk.render.widgets.PasscodeWidget;
import com.strivacity.android.native_sdk.render.widgets.PasswordWidget;
import com.strivacity.android.native_sdk.render.widgets.PhoneWidget;
import com.strivacity.android.native_sdk.render.widgets.StaticWidget;
import com.strivacity.android.native_sdk.render.widgets.SubmitWidget;
import com.strivacity.android.native_sdk.render.widgets.Widget;
import com.strivacity.android.native_sdk.render.widgets.select.simple.DropdownWidget;
import com.strivacity.android.native_sdk.render.widgets.select.simple.RadioWidget;
import com.strivacity.android.native_sdk.render.widgets.select.simple.SelectWidget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ViewFactory {

    private final Context context;

    public final Widget widget(WidgetModel widgetModel, BrandingModel brandingModel, String screenId, String formId) {
        if (widgetModel instanceof WidgetModel.StaticWidgetModel) {
            return getStaticView((WidgetModel.StaticWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.InputWidgetModel) {
            return getInputView((WidgetModel.InputWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.PasswordWidgetModel) {
            return getPasswordView((WidgetModel.PasswordWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.CheckboxWidgetModel) {
            return getCheckboxView((WidgetModel.CheckboxWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.SubmitWidgetModel) {
            return getButtonView((WidgetModel.SubmitWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.SelectWidgetModel) {
            return getSelectView((WidgetModel.SelectWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.MultiSelectWidgetModel) {
            return getMultiSelectView(
                (WidgetModel.MultiSelectWidgetModel) widgetModel,
                brandingModel,
                screenId,
                formId
            );
        } else if (widgetModel instanceof WidgetModel.PasscodeWidgetModel) {
            return getPasscodeView((WidgetModel.PasscodeWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.PhoneWidgetModel) {
            return getPhoneView((WidgetModel.PhoneWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.DateWidgetModel) {
            return getDateView((WidgetModel.DateWidgetModel) widgetModel, brandingModel, screenId, formId);
        } else if (widgetModel instanceof WidgetModel.CloseWidgetModel) {
            return getCloseView((WidgetModel.CloseWidgetModel) widgetModel, brandingModel, screenId, formId);
        }

        throw new RuntimeException();
    }

    public LayoutWidget layoutWidget(
        Map<String, Form> forms,
        BrandingModel brandingModel,
        LayoutModel.SingleLayoutModel singleLayoutModel
    ) {
        return new LayoutWidget(this, brandingModel, forms, singleLayoutModel);
    }

    protected StaticWidget getStaticView(
        WidgetModel.StaticWidgetModel staticWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new StaticWidget(context, staticWidgetModel);
    }

    protected InputWidget getInputView(
        WidgetModel.InputWidgetModel inputWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new InputWidget(context, inputWidgetModel);
    }

    protected PasswordWidget getPasswordView(
        WidgetModel.PasswordWidgetModel passwordWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new PasswordWidget(context, passwordWidgetModel);
    }

    protected CheckboxWidget getCheckboxView(
        WidgetModel.CheckboxWidgetModel checkboxWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new CheckboxWidget(context, checkboxWidgetModel);
    }

    protected SubmitWidget getButtonView(
        WidgetModel.SubmitWidgetModel submitWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new SubmitWidget(context, submitWidgetModel);
    }

    protected SelectWidget getSelectView(
        WidgetModel.SelectWidgetModel selectWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        switch (selectWidgetModel.getRender().getType()) {
            case "radio":
                return new RadioWidget(context, selectWidgetModel);
            case "dropdown":
                return new DropdownWidget(context, selectWidgetModel);
            default:
                throw new RuntimeException();
        }
    }

    protected MultiSelectWidget getMultiSelectView(
        WidgetModel.MultiSelectWidgetModel multiSelectWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new MultiSelectWidget(context, multiSelectWidgetModel);
    }

    protected PasscodeWidget getPasscodeView(
        WidgetModel.PasscodeWidgetModel passcodeWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new PasscodeWidget(context, passcodeWidgetModel);
    }

    protected PhoneWidget getPhoneView(
        WidgetModel.PhoneWidgetModel phoneWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new PhoneWidget(context, phoneWidgetModel);
    }

    protected DateWidget getDateView(
        WidgetModel.DateWidgetModel dateWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new DateWidget(context, dateWidgetModel);
    }

    protected CloseWidget getCloseView(
        WidgetModel.CloseWidgetModel closeWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new CloseWidget(context, closeWidgetModel);
    }
}
