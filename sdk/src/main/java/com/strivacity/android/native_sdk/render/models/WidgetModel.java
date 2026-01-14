package com.strivacity.android.native_sdk.render.models;

import com.strivacity.android.native_sdk.util.JSON;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.stream.Collectors;

@Data
@FieldNameConstants
public abstract class WidgetModel {

    private final String id;

    public static WidgetModel fromJson(JSON json) {
        String type = json.string("type");
        switch (type) {
            case "static":
                return new StaticWidgetModel(json);
            case "input":
                return new InputWidgetModel(json);
            case "password":
                return new PasswordWidgetModel(json);
            case "checkbox":
                return new CheckboxWidgetModel(json);
            case "submit":
                return new SubmitWidgetModel(json);
            case "select":
                return new SelectWidgetModel(json);
            case "multiSelect":
                return new MultiSelectWidgetModel(json);
            case "passcode":
                return new PasscodeWidgetModel(json);
            case "phone":
                return new PhoneWidgetModel(json);
            case "date":
                return new DateWidgetModel(json);
            case "close":
                return new CloseWidgetModel(json);
            default:
                throw new RuntimeException("Unknown widget type " + type);
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    public static class StaticWidgetModel extends WidgetModel {

        private final String value;
        private final Render render;

        @Data
        @FieldNameConstants
        public static class Render {

            private final String type;
        }

        public StaticWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.value = json.string(Fields.value);
            JSON render = json.object(CheckboxWidgetModel.Fields.render);
            this.render = render == null ? null : new Render(render.string(Render.Fields.type));
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    public static class SubmitWidgetModel extends WidgetModel {

        private final String label;
        private final Render render;

        @Data
        @FieldNameConstants
        public static class Render {

            private final String type;
            private final String textColor;
            private final String bgColor;
            private final SubmitWidgetHint hint;
        }

        @Data
        @FieldNameConstants
        public static class SubmitWidgetHint {

            private final String icon;
            private final String variant;

            SubmitWidgetHint(JSON hint) {
                this.icon = hint.string(SubmitWidgetHint.Fields.icon);
                this.variant = hint.string(SubmitWidgetHint.Fields.variant);
            }
        }

        public SubmitWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(Fields.label);
            JSON render = json.object(SubmitWidgetModel.Fields.render);

            this.render =
                render == null
                    ? null
                    : new Render(
                        render.string(Render.Fields.type),
                        render.string(Render.Fields.textColor),
                        render.string(Render.Fields.bgColor),
                        render.object(Render.Fields.hint) == null
                            ? null
                            : new SubmitWidgetHint(render.object(Render.Fields.hint))
                    );
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    public static class CloseWidgetModel extends WidgetModel {

        private final String label;
        private final Render render;

        @Data
        @FieldNameConstants
        public static class Render {

            private final String type;
            private final String textColor;
            private final String bgColor;
            private final CloseWidgetHint hint;
        }

        @Data
        @FieldNameConstants
        public static class CloseWidgetHint {

            private final String icon;
            private final String variant;

            CloseWidgetHint(JSON hint) {
                this.icon = hint.string(CloseWidgetHint.Fields.icon);
                this.variant = hint.string(CloseWidgetHint.Fields.variant);
            }
        }

        public CloseWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(Fields.label);
            JSON render = json.object(SubmitWidgetModel.Fields.render);

            this.render =
                render == null
                    ? null
                    : new Render(
                        render.string(Render.Fields.type),
                        render.string(Render.Fields.textColor),
                        render.string(Render.Fields.bgColor),
                        render.object(Render.Fields.hint) == null
                            ? null
                            : new CloseWidgetHint(render.object(Render.Fields.hint))
                    );
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    public static class InputWidgetModel extends WidgetModel {

        private final String label;
        private final String value;
        private final boolean readonly;
        private final String autocomplete;
        private final String inputmode;
        private final Validator validator;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final Integer minLength;
            private final Integer maxLength;
            private final String regexp;
            private final boolean required;
        }

        public InputWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(Fields.label);
            this.value = json.string(Fields.value);
            this.readonly = json.bool(Fields.readonly);
            this.autocomplete = json.string(Fields.autocomplete);
            this.inputmode = json.string(Fields.inputmode);

            JSON validator = json.object(Fields.validator);
            this.validator =
                validator == null
                    ? null
                    : new Validator(
                        validator.integer(Validator.Fields.minLength),
                        validator.integer(Validator.Fields.maxLength),
                        validator.string(Validator.Fields.regexp),
                        validator.bool(Validator.Fields.required)
                    );
        }

        public InputWidgetModel(String label, String value, boolean isRequired) {
            super(WidgetModel.Fields.id);
            this.label = label;
            this.value = value;
            this.readonly = false;
            this.autocomplete = null;
            this.inputmode = null;
            this.validator = new Validator(null, null, null, isRequired);
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    public static class PasswordWidgetModel extends WidgetModel {

        private final String label;
        private final boolean qualityIndicator;
        private final Validator validator;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final Integer minLength;
            private final Integer maxNumericCharacterSequences;
            private final Integer maxRepeatedCharacters;
            private final List<String> mustContain;
        }

        public PasswordWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(Fields.label);
            this.qualityIndicator = json.bool(Fields.qualityIndicator);

            JSON validator = json.object(Fields.validator);
            this.validator =
                validator == null
                    ? null
                    : new Validator(
                        validator.integer(Validator.Fields.minLength),
                        validator.integer(Validator.Fields.maxNumericCharacterSequences),
                        validator.integer(Validator.Fields.maxRepeatedCharacters),
                        validator.stringList(Validator.Fields.mustContain)
                    );
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    public static class CheckboxWidgetModel extends WidgetModel {

        private final String label;
        private final boolean readonly;
        private final boolean value;
        private final Validator validator;
        private final Render render;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final boolean required;
        }

        @Data
        @FieldNameConstants
        public static class Render {

            private final String type;
            private final String labelType;
        }

        public CheckboxWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(Fields.label);
            this.readonly = json.bool(Fields.readonly);
            this.value = json.bool(Fields.value);

            JSON validator = json.object(Fields.validator);
            this.validator = validator == null ? null : new Validator(validator.bool(Validator.Fields.required));

            JSON render = json.object(Fields.render);
            this.render =
                render == null
                    ? null
                    : new Render(render.string(Render.Fields.type), render.string(Render.Fields.labelType));
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    @ToString(callSuper = true)
    public static class SelectWidgetModel extends WidgetModel {

        private final String label;
        private final String value;
        private final boolean readonly;

        private final Render render;
        private final List<Option> options;
        private final Validator validator;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final boolean required;
        }

        @Data
        @FieldNameConstants
        public static class Render {

            private final String type;
        }

        @Data
        @FieldNameConstants
        @RequiredArgsConstructor
        public static class Option {

            private final String type;
            private final String label;
            private final String value;
            private final List<Option> options;

            Option(JSON option) {
                this.type = option.string(Fields.type);
                this.label = option.string(Fields.label);
                this.value = option.string(Fields.value);
                if (option.getJsonObject().has(Fields.options)) {
                    this.options = option.list(Fields.options).stream().map(Option::new).collect(Collectors.toList());
                } else {
                    this.options = List.of();
                }
            }
        }

        public SelectWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(Fields.label);
            this.readonly = json.bool(Fields.readonly);
            this.value = json.string(Fields.value);

            JSON validator = json.object(Fields.validator);
            this.validator = validator == null ? null : new Validator(validator.bool(Validator.Fields.required));

            JSON render = json.object(Fields.render);
            this.render = render == null ? null : new Render(render.string(Render.Fields.type));

            if (json.getJsonObject().has(Option.Fields.options)) {
                this.options = json.list(Option.Fields.options).stream().map(Option::new).collect(Collectors.toList());
            } else {
                this.options = List.of();
            }
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    @ToString(callSuper = true)
    public static class MultiSelectWidgetModel extends WidgetModel {

        private final String label;
        private final String value;
        private final boolean readonly;

        private final List<Option> options;
        private final Validator validator;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final Integer minSelectable;
            private final Integer maxSelectable;
        }

        @Data
        @FieldNameConstants
        @RequiredArgsConstructor
        public static class Option {

            private final String type;
            private final String label;
            private final String value;
            private final List<Option> options;

            Option(JSON option) {
                this.type = option.string(Fields.type);
                this.label = option.string(Fields.label);
                this.value = option.string(Fields.value);
                if (option.getJsonObject().has(Fields.options)) {
                    this.options = option.list(Fields.options).stream().map(Option::new).collect(Collectors.toList());
                } else {
                    this.options = List.of();
                }
            }
        }

        public MultiSelectWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(MultiSelectWidgetModel.Fields.label);
            this.readonly = json.bool(MultiSelectWidgetModel.Fields.readonly);
            this.value = json.string(MultiSelectWidgetModel.Fields.value);

            JSON validator = json.object(MultiSelectWidgetModel.Fields.validator);
            this.validator =
                validator == null
                    ? null
                    : new MultiSelectWidgetModel.Validator(
                        validator.integer(Validator.Fields.minSelectable),
                        validator.integer(Validator.Fields.maxSelectable)
                    );

            if (json.getJsonObject().has(MultiSelectWidgetModel.Option.Fields.options)) {
                this.options =
                    json
                        .list(MultiSelectWidgetModel.Option.Fields.options)
                        .stream()
                        .map(MultiSelectWidgetModel.Option::new)
                        .collect(Collectors.toList());
            } else {
                this.options = List.of();
            }
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    @ToString(callSuper = true)
    public static class PasscodeWidgetModel extends WidgetModel {

        private final String label;
        private final Validator validator;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final Integer length;
        }

        public PasscodeWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(PasscodeWidgetModel.Fields.label);

            JSON validator = json.object(PasscodeWidgetModel.Fields.validator);
            this.validator =
                validator == null
                    ? null
                    : new PasscodeWidgetModel.Validator(validator.integer(Validator.Fields.length));
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    @ToString(callSuper = true)
    public static class PhoneWidgetModel extends WidgetModel {

        private final String label;
        private final String value;
        private final boolean readonly;

        private final PhoneWidgetModel.Validator validator;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final boolean required;
        }

        public PhoneWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(PhoneWidgetModel.Fields.label);
            this.readonly = json.bool(PhoneWidgetModel.Fields.readonly);
            this.value = json.string(PhoneWidgetModel.Fields.value);

            JSON validator = json.object(PhoneWidgetModel.Fields.validator);
            this.validator =
                validator == null ? null : new PhoneWidgetModel.Validator(validator.bool(Validator.Fields.required));
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @FieldNameConstants
    @ToString(callSuper = true)
    public static class DateWidgetModel extends WidgetModel {

        private final String label;
        private final String value;
        private final String placeholder;
        private final boolean readonly;

        private final DateWidgetModel.Validator validator;
        private final DateWidgetModel.Render render;

        @Data
        @FieldNameConstants
        public static class Validator {

            private final boolean required;
            private final String notBefore;
            private final String notAfter;
        }

        @Data
        @FieldNameConstants
        public static class Render {

            private final String type;
        }

        public DateWidgetModel(JSON json) {
            super(json.string(WidgetModel.Fields.id));
            this.label = json.string(DateWidgetModel.Fields.label);
            this.readonly = json.bool(DateWidgetModel.Fields.readonly);
            this.value = json.string(DateWidgetModel.Fields.value);
            this.placeholder = json.string(DateWidgetModel.Fields.placeholder);

            JSON validator = json.object(DateWidgetModel.Fields.validator);
            this.validator =
                validator == null
                    ? null
                    : new DateWidgetModel.Validator(
                        validator.bool(Validator.Fields.required),
                        validator.string(Validator.Fields.notBefore),
                        validator.string(Validator.Fields.notAfter)
                    );

            JSON render = json.object(DateWidgetModel.Fields.render);
            this.render = new DateWidgetModel.Render(render.string(Render.Fields.type));
        }
    }
}
