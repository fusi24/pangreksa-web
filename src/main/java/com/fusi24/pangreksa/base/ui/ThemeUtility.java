package com.fusi24.pangreksa.base.ui;

/**
 * Project-local replacement for {@code com.vaadin.flow.theme.lumo.LumoUtility}.
 *
 * <p>Aura (the default theme in Vaadin 25) does not ship a Java utility class equivalent to {@code LumoUtility}.
 * The CSS class strings exposed here (e.g. {@code "p-m"}, {@code "flex-col"}) are still rendered by the
 * Lumo CSS that Vaadin 25 bundles transitively, so visual output is unchanged. The point of this class
 * is to remove the {@code com.vaadin.flow.theme.lumo} Java import surface from view code so the project
 * has a single, project-controlled place to evolve toward Aura-native styling later (project CSS file or
 * Aura design tokens).</p>
 *
 * <p>Constants mirror Lumo 25.1 verbatim where they are used by views in this module. If a constant is
 * missing, add it here and let the build catch the gap. Do <strong>not</strong> reintroduce
 * {@code import com.vaadin.flow.theme.lumo.LumoUtility} into view code.</p>
 */
public final class ThemeUtility {

    private ThemeUtility() {}

    public static final class AlignItems {
        public static final String BASELINE = "items-baseline";
        public static final String CENTER = "items-center";
        public static final String END = "items-end";
        public static final String START = "items-start";
        public static final String STRETCH = "items-stretch";

        public static final class Breakpoint {
            public static final class Medium {
                public static final String BASELINE = "md:items-baseline";
                public static final String CENTER = "md:items-center";
                public static final String END = "md:items-end";
                public static final String START = "md:items-start";
                public static final String STRETCH = "md:items-stretch";
                private Medium() {}
            }
            private Breakpoint() {}
        }
        private AlignItems() {}
    }

    public static final class Background {
        public static final String BASE = "bg-base";
        public static final String TRANSPARENT = "bg-transparent";
        public static final String CONTRAST = "bg-contrast";
        public static final String CONTRAST_5 = "bg-contrast-5";
        public static final String CONTRAST_10 = "bg-contrast-10";
        public static final String CONTRAST_20 = "bg-contrast-20";
        public static final String CONTRAST_30 = "bg-contrast-30";
        public static final String CONTRAST_40 = "bg-contrast-40";
        public static final String CONTRAST_50 = "bg-contrast-50";
        public static final String CONTRAST_60 = "bg-contrast-60";
        public static final String CONTRAST_70 = "bg-contrast-70";
        public static final String CONTRAST_80 = "bg-contrast-80";
        public static final String CONTRAST_90 = "bg-contrast-90";
        public static final String PRIMARY = "bg-primary";
        public static final String PRIMARY_10 = "bg-primary-10";
        public static final String PRIMARY_50 = "bg-primary-50";
        public static final String ERROR = "bg-error";
        public static final String ERROR_10 = "bg-error-10";
        public static final String ERROR_50 = "bg-error-50";
        public static final String WARNING = "bg-warning";
        public static final String WARNING_10 = "bg-warning-10";
        public static final String SUCCESS = "bg-success";
        public static final String SUCCESS_10 = "bg-success-10";
        public static final String SUCCESS_50 = "bg-success-50";
        private Background() {}
    }

    public static final class Border {
        public static final String NONE = "border-0";
        public static final String ALL = "border";
        public static final String BOTTOM = "border-b";
        public static final String TOP = "border-t";
        public static final String LEFT = "border-l";
        public static final String RIGHT = "border-r";
        public static final String DASHED = "border-dashed";
        public static final String DOTTED = "border-dotted";
        private Border() {}
    }

    public static final class BorderColor {
        public static final String CONTRAST = "border-contrast";
        public static final String CONTRAST_10 = "border-contrast-10";
        public static final String CONTRAST_20 = "border-contrast-20";
        public static final String CONTRAST_30 = "border-contrast-30";
        public static final String CONTRAST_40 = "border-contrast-40";
        public static final String CONTRAST_50 = "border-contrast-50";
        public static final String PRIMARY = "border-primary";
        public static final String ERROR = "border-error";
        public static final String SUCCESS = "border-success";
        public static final String WARNING = "border-warning";
        private BorderColor() {}
    }

    public static final class BorderRadius {
        public static final String NONE = "rounded-none";
        public static final String SMALL = "rounded-s";
        public static final String MEDIUM = "rounded-m";
        public static final String LARGE = "rounded-l";
        public static final String FULL = "rounded-full";
        private BorderRadius() {}
    }

    public static final class BoxShadow {
        public static final String NONE = "shadow-none";
        public static final String XSMALL = "shadow-xs";
        public static final String SMALL = "shadow-s";
        public static final String MEDIUM = "shadow-m";
        public static final String LARGE = "shadow-l";
        public static final String XLARGE = "shadow-xl";
        private BoxShadow() {}
    }

    public static final class BoxSizing {
        public static final String BORDER = "box-border";
        public static final String CONTENT = "box-content";
        private BoxSizing() {}
    }

    public static final class Display {
        public static final String BLOCK = "block";
        public static final String FLEX = "flex";
        public static final String GRID = "grid";
        public static final String HIDDEN = "hidden";
        public static final String INLINE = "inline";
        public static final String INLINE_BLOCK = "inline-block";
        public static final String INLINE_FLEX = "inline-flex";
        public static final String INLINE_GRID = "inline-grid";
        private Display() {}
    }

    public static final class Flex {
        public static final String ONE = "flex-1";
        public static final String AUTO = "flex-auto";
        public static final String NONE = "flex-none";
        public static final String GROW = "flex-grow";
        public static final String GROW_NONE = "flex-grow-0";
        public static final String SHRINK = "flex-shrink";
        public static final String SHRINK_NONE = "flex-shrink-0";
        private Flex() {}
    }

    public static final class FlexDirection {
        public static final String ROW = "flex-row";
        public static final String ROW_REVERSE = "flex-row-reverse";
        public static final String COLUMN = "flex-col";
        public static final String COLUMN_REVERSE = "flex-col-reverse";

        public static final class Breakpoint {
            public static final class Medium {
                public static final String ROW = "md:flex-row";
                public static final String COLUMN = "md:flex-col";
                private Medium() {}
            }
            private Breakpoint() {}
        }
        private FlexDirection() {}
    }

    public static final class FontSize {
        public static final String XXSMALL = "text-2xs";
        public static final String XSMALL = "text-xs";
        public static final String SMALL = "text-s";
        public static final String MEDIUM = "text-m";
        public static final String LARGE = "text-l";
        public static final String XLARGE = "text-xl";
        public static final String XXLARGE = "text-2xl";
        public static final String XXXLARGE = "text-3xl";
        private FontSize() {}
    }

    public static final class FontWeight {
        public static final String THIN = "font-thin";
        public static final String EXTRALIGHT = "font-extralight";
        public static final String LIGHT = "font-light";
        public static final String NORMAL = "font-normal";
        public static final String MEDIUM = "font-medium";
        public static final String SEMIBOLD = "font-semibold";
        public static final String BOLD = "font-bold";
        public static final String EXTRABOLD = "font-extrabold";
        public static final String BLACK = "font-black";
        private FontWeight() {}
    }

    public static final class Gap {
        public static final String XSMALL = "gap-xs";
        public static final String SMALL = "gap-s";
        public static final String MEDIUM = "gap-m";
        public static final String LARGE = "gap-l";
        public static final String XLARGE = "gap-xl";
        private Gap() {}
    }

    public static final class Height {
        public static final String NONE = "h-0";
        public static final String XSMALL = "h-xs";
        public static final String SMALL = "h-s";
        public static final String MEDIUM = "h-m";
        public static final String LARGE = "h-l";
        public static final String XLARGE = "h-xl";
        public static final String AUTO = "h-auto";
        public static final String FULL = "h-full";
        public static final String SCREEN = "h-screen";
        private Height() {}
    }

    public static final class JustifyContent {
        public static final String AROUND = "justify-around";
        public static final String BETWEEN = "justify-between";
        public static final String CENTER = "justify-center";
        public static final String END = "justify-end";
        public static final String EVENLY = "justify-evenly";
        public static final String START = "justify-start";
        private JustifyContent() {}
    }

    public static final class LineHeight {
        public static final String NONE = "leading-none";
        public static final String XSMALL = "leading-xs";
        public static final String SMALL = "leading-s";
        public static final String MEDIUM = "leading-m";
        private LineHeight() {}
    }

    public static final class Margin {
        public static final String NONE = "m-0";
        public static final String XSMALL = "m-xs";
        public static final String SMALL = "m-s";
        public static final String MEDIUM = "m-m";
        public static final String LARGE = "m-l";
        public static final String XLARGE = "m-xl";
        public static final String AUTO = "m-auto";

        public static final class Bottom {
            public static final String NONE = "mb-0";
            public static final String XSMALL = "mb-xs";
            public static final String SMALL = "mb-s";
            public static final String MEDIUM = "mb-m";
            public static final String LARGE = "mb-l";
            public static final String XLARGE = "mb-xl";
            private Bottom() {}
        }

        public static final class Top {
            public static final String NONE = "mt-0";
            public static final String XSMALL = "mt-xs";
            public static final String SMALL = "mt-s";
            public static final String MEDIUM = "mt-m";
            public static final String LARGE = "mt-l";
            public static final String XLARGE = "mt-xl";
            private Top() {}
        }

        public static final class Left {
            public static final String NONE = "ml-0";
            public static final String XSMALL = "ml-xs";
            public static final String SMALL = "ml-s";
            public static final String MEDIUM = "ml-m";
            public static final String LARGE = "ml-l";
            public static final String XLARGE = "ml-xl";
            private Left() {}
        }

        public static final class Right {
            public static final String NONE = "mr-0";
            public static final String XSMALL = "mr-xs";
            public static final String SMALL = "mr-s";
            public static final String MEDIUM = "mr-m";
            public static final String LARGE = "mr-l";
            public static final String XLARGE = "mr-xl";
            private Right() {}
        }

        public static final class Vertical {
            public static final String NONE = "my-0";
            public static final String XSMALL = "my-xs";
            public static final String SMALL = "my-s";
            public static final String MEDIUM = "my-m";
            public static final String LARGE = "my-l";
            public static final String XLARGE = "my-xl";
            private Vertical() {}
        }

        public static final class Horizontal {
            public static final String NONE = "mx-0";
            public static final String XSMALL = "mx-xs";
            public static final String SMALL = "mx-s";
            public static final String MEDIUM = "mx-m";
            public static final String LARGE = "mx-l";
            public static final String XLARGE = "mx-xl";
            private Horizontal() {}
        }

        private Margin() {}
    }

    public static final class Overflow {
        public static final String AUTO = "overflow-auto";
        public static final String HIDDEN = "overflow-hidden";
        public static final String SCROLL = "overflow-scroll";
        private Overflow() {}
    }

    public static final class Padding {
        public static final String NONE = "p-0";
        public static final String XSMALL = "p-xs";
        public static final String SMALL = "p-s";
        public static final String MEDIUM = "p-m";
        public static final String LARGE = "p-l";
        public static final String XLARGE = "p-xl";

        public static final class Bottom {
            public static final String NONE = "pb-0";
            public static final String XSMALL = "pb-xs";
            public static final String SMALL = "pb-s";
            public static final String MEDIUM = "pb-m";
            public static final String LARGE = "pb-l";
            public static final String XLARGE = "pb-xl";
            private Bottom() {}
        }

        public static final class Top {
            public static final String NONE = "pt-0";
            public static final String XSMALL = "pt-xs";
            public static final String SMALL = "pt-s";
            public static final String MEDIUM = "pt-m";
            public static final String LARGE = "pt-l";
            public static final String XLARGE = "pt-xl";
            private Top() {}
        }

        public static final class Left {
            public static final String NONE = "pl-0";
            public static final String XSMALL = "pl-xs";
            public static final String SMALL = "pl-s";
            public static final String MEDIUM = "pl-m";
            public static final String LARGE = "pl-l";
            public static final String XLARGE = "pl-xl";
            private Left() {}
        }

        public static final class Right {
            public static final String NONE = "pr-0";
            public static final String XSMALL = "pr-xs";
            public static final String SMALL = "pr-s";
            public static final String MEDIUM = "pr-m";
            public static final String LARGE = "pr-l";
            public static final String XLARGE = "pr-xl";
            private Right() {}
        }

        public static final class Vertical {
            public static final String NONE = "py-0";
            public static final String XSMALL = "py-xs";
            public static final String SMALL = "py-s";
            public static final String MEDIUM = "py-m";
            public static final String LARGE = "py-l";
            public static final String XLARGE = "py-xl";
            private Vertical() {}
        }

        public static final class Horizontal {
            public static final String NONE = "px-0";
            public static final String XSMALL = "px-xs";
            public static final String SMALL = "px-s";
            public static final String MEDIUM = "px-m";
            public static final String LARGE = "px-l";
            public static final String XLARGE = "px-xl";
            private Horizontal() {}
        }

        private Padding() {}
    }

    public static final class Position {
        public static final String ABSOLUTE = "absolute";
        public static final String FIXED = "fixed";
        public static final String RELATIVE = "relative";
        public static final String STATIC = "static";
        public static final String STICKY = "sticky";
        private Position() {}
    }

    public static final class TextColor {
        public static final String HEADER = "text-header";
        public static final String BODY = "text-body";
        public static final String SECONDARY = "text-secondary";
        public static final String TERTIARY = "text-tertiary";
        public static final String DISABLED = "text-disabled";
        public static final String PRIMARY = "text-primary";
        public static final String PRIMARY_CONTRAST = "text-primary-contrast";
        public static final String ERROR = "text-error";
        public static final String ERROR_CONTRAST = "text-error-contrast";
        public static final String WARNING = "text-warning";
        public static final String WARNING_CONTRAST = "text-warning-contrast";
        public static final String SUCCESS = "text-success";
        public static final String SUCCESS_CONTRAST = "text-success-contrast";
        private TextColor() {}
    }

    public static final class Width {
        public static final String XSMALL = "w-xs";
        public static final String SMALL = "w-s";
        public static final String MEDIUM = "w-m";
        public static final String LARGE = "w-l";
        public static final String XLARGE = "w-xl";
        public static final String AUTO = "w-auto";
        public static final String FULL = "w-full";
        private Width() {}
    }
}
