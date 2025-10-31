package com.strivacity.android.demo.render;

import android.content.Context;

import com.strivacity.android.demo.render.widgets.CheckboxWidget;
import com.strivacity.android.demo.render.widgets.CloseWidget;
import com.strivacity.android.demo.render.widgets.InputWidget;
import com.strivacity.android.demo.render.widgets.MultiSelectWidget;
import com.strivacity.android.demo.render.widgets.PasscodeWidget;
import com.strivacity.android.demo.render.widgets.PasswordWidget;
import com.strivacity.android.demo.render.widgets.PhoneWidget;
import com.strivacity.android.demo.render.widgets.StaticWidget;
import com.strivacity.android.demo.render.widgets.SubmitWidget;
import com.strivacity.android.demo.render.widgets.select.simple.DropdownWidget;
import com.strivacity.android.demo.render.widgets.select.simple.RadioWidget;
import com.strivacity.android.native_sdk.render.models.BrandingModel;
import com.strivacity.android.native_sdk.render.models.WidgetModel;
import com.strivacity.android.native_sdk.render.widgets.select.simple.SelectWidget;

public class ViewFactory extends com.strivacity.android.native_sdk.render.ViewFactory {

    public ViewFactory(Context context) {
        super(context);
    }

    @Override
    protected StaticWidget getStaticView(
        WidgetModel.StaticWidgetModel staticWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new StaticWidget(getContext(), staticWidgetModel);
    }

    @Override
    protected InputWidget getInputView(
        WidgetModel.InputWidgetModel inputWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new InputWidget(getContext(), inputWidgetModel);
    }

    @Override
    protected PasswordWidget getPasswordView(
        WidgetModel.PasswordWidgetModel passwordWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new PasswordWidget(getContext(), passwordWidgetModel);
    }

    @Override
    protected CheckboxWidget getCheckboxView(
        WidgetModel.CheckboxWidgetModel checkboxWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new CheckboxWidget(getContext(), checkboxWidgetModel);
    }

    @Override
    protected SubmitWidget getButtonView(
        WidgetModel.SubmitWidgetModel submitWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new SubmitWidget(getContext(), submitWidgetModel);
    }

    @Override
    protected CloseWidget getCloseView(
        WidgetModel.CloseWidgetModel closeWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new CloseWidget(getContext(), closeWidgetModel);
    }

    @Override
    protected SelectWidget getSelectView(
        WidgetModel.SelectWidgetModel selectWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        switch (selectWidgetModel.getRender().getType()) {
            case "radio":
                return new RadioWidget(getContext(), selectWidgetModel);
            case "dropdown":
                return new DropdownWidget(getContext(), selectWidgetModel);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    protected MultiSelectWidget getMultiSelectView(
        WidgetModel.MultiSelectWidgetModel multiselectWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new MultiSelectWidget(getContext(), multiselectWidgetModel);
    }

    @Override
    protected PhoneWidget getPhoneView(
        WidgetModel.PhoneWidgetModel phoneWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new PhoneWidget(getContext(), phoneWidgetModel);
    }

    @Override
    protected PasscodeWidget getPasscodeView(
        WidgetModel.PasscodeWidgetModel passcodeWidgetModel,
        BrandingModel brandingModel,
        String screenId,
        String formId
    ) {
        return new PasscodeWidget(getContext(), passcodeWidgetModel);
    }
}
