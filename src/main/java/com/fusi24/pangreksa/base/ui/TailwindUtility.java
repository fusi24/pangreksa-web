package com.fusi24.pangreksa.base.ui;

/**
 * Tailwind CSS v4 utility-class constants for use with Vaadin Flow's {@code addClassName()} /
 * {@code addClassNames()} API.
 *
 * <p>Replaces the old {@code ThemeUtility} (Lumo-named class constants) with standard Tailwind v4
 * class names. The project's Vite build processes {@code tailwindcss/utilities} via
 * {@code @tailwindcss/vite}, so these classes are generated and included in the output bundle.</p>
 *
 * <p><strong>Semantic colour classes</strong> ({@link TextColor}, {@link Background},
 * {@link BorderColor}) are still Lumo-named strings — they are provided globally by
 * {@code @vaadin/vaadin-lumo-styles} which is bundled by Vaadin and does not need Tailwind
 * to generate them.</p>
 *
 * <p><strong>Component-size width/height</strong> ({@link Width} XS–XL, {@link Height} XS–XL)
 * also remain as Lumo-named strings for the same reason.</p>
 */
public final class TailwindUtility {

    private TailwindUtility() {}

    // ── Layout ────────────────────────────────────────────────────────────────────

    public static final class AlignItems {
        public static final String BASELINE = "items-baseline";
        public static final String CENTER    = "items-center";
        public static final String END       = "items-end";
        public static final String START     = "items-start";
        public static final String STRETCH   = "items-stretch";

        public static final class Breakpoint {
            public static final class Medium {
                public static final String BASELINE = "md:items-baseline";
                public static final String CENTER   = "md:items-center";
                public static final String END      = "md:items-end";
                public static final String START    = "md:items-start";
                public static final String STRETCH  = "md:items-stretch";
                private Medium() {}
            }
            private Breakpoint() {}
        }
        private AlignItems() {}
    }

    public static final class Display {
        public static final String BLOCK       = "block";
        public static final String FLEX        = "flex";
        public static final String GRID        = "grid";
        public static final String HIDDEN      = "hidden";
        public static final String INLINE      = "inline";
        public static final String INLINE_BLOCK = "inline-block";
        public static final String INLINE_FLEX  = "inline-flex";
        public static final String INLINE_GRID  = "inline-grid";
        private Display() {}
    }

    public static final class Flex {
        public static final String ONE        = "flex-1";
        public static final String AUTO       = "flex-auto";
        public static final String NONE       = "flex-none";
        /** Tailwind v4: {@code grow} (replaces deprecated {@code flex-grow}) */
        public static final String GROW       = "grow";
        /** Tailwind v4: {@code grow-0} */
        public static final String GROW_NONE  = "grow-0";
        /** Tailwind v4: {@code shrink} (replaces deprecated {@code flex-shrink}) */
        public static final String SHRINK     = "shrink";
        /** Tailwind v4: {@code shrink-0} */
        public static final String SHRINK_NONE = "shrink-0";
        private Flex() {}
    }

    public static final class FlexDirection {
        public static final String ROW            = "flex-row";
        public static final String ROW_REVERSE    = "flex-row-reverse";
        public static final String COLUMN         = "flex-col";
        public static final String COLUMN_REVERSE = "flex-col-reverse";

        public static final class Breakpoint {
            public static final class Medium {
                public static final String ROW    = "md:flex-row";
                public static final String COLUMN = "md:flex-col";
                private Medium() {}
            }
            private Breakpoint() {}
        }
        private FlexDirection() {}
    }

    public static final class JustifyContent {
        public static final String AROUND  = "justify-around";
        public static final String BETWEEN = "justify-between";
        public static final String CENTER  = "justify-center";
        public static final String END     = "justify-end";
        public static final String EVENLY  = "justify-evenly";
        public static final String START   = "justify-start";
        private JustifyContent() {}
    }

    public static final class Overflow {
        public static final String AUTO   = "overflow-auto";
        public static final String HIDDEN = "overflow-hidden";
        public static final String SCROLL = "overflow-scroll";
        private Overflow() {}
    }

    public static final class Position {
        public static final String ABSOLUTE = "absolute";
        public static final String FIXED    = "fixed";
        public static final String RELATIVE = "relative";
        public static final String STATIC   = "static";
        public static final String STICKY   = "sticky";
        private Position() {}
    }

    // ── Spacing ───────────────────────────────────────────────────────────────────
    // Scale calibrated to match this project's compact Lumo token overrides:
    //   xs=4px (p-1), s=6px (p-1.5), m=10px (p-2.5), l=14px (p-3.5), xl=32px (p-8)

    public static final class Gap {
        public static final String XSMALL = "gap-1";
        public static final String SMALL  = "gap-1.5";
        public static final String MEDIUM = "gap-2.5";
        public static final String LARGE  = "gap-3.5";
        public static final String XLARGE = "gap-8";
        private Gap() {}
    }

    public static final class Margin {
        public static final String NONE   = "m-0";
        public static final String XSMALL = "m-1";
        public static final String SMALL  = "m-1.5";
        public static final String MEDIUM = "m-2.5";
        public static final String LARGE  = "m-3.5";
        public static final String XLARGE = "m-8";
        public static final String AUTO   = "m-auto";

        public static final class Bottom {
            public static final String NONE   = "mb-0";
            public static final String XSMALL = "mb-1";
            public static final String SMALL  = "mb-1.5";
            public static final String MEDIUM = "mb-2.5";
            public static final String LARGE  = "mb-3.5";
            public static final String XLARGE = "mb-8";
            private Bottom() {}
        }
        public static final class Top {
            public static final String NONE   = "mt-0";
            public static final String XSMALL = "mt-1";
            public static final String SMALL  = "mt-1.5";
            public static final String MEDIUM = "mt-2.5";
            public static final String LARGE  = "mt-3.5";
            public static final String XLARGE = "mt-8";
            private Top() {}
        }
        public static final class Left {
            public static final String NONE   = "ml-0";
            public static final String XSMALL = "ml-1";
            public static final String SMALL  = "ml-1.5";
            public static final String MEDIUM = "ml-2.5";
            public static final String LARGE  = "ml-3.5";
            public static final String XLARGE = "ml-8";
            private Left() {}
        }
        public static final class Right {
            public static final String NONE   = "mr-0";
            public static final String XSMALL = "mr-1";
            public static final String SMALL  = "mr-1.5";
            public static final String MEDIUM = "mr-2.5";
            public static final String LARGE  = "mr-3.5";
            public static final String XLARGE = "mr-8";
            private Right() {}
        }
        public static final class Vertical {
            public static final String NONE   = "my-0";
            public static final String XSMALL = "my-1";
            public static final String SMALL  = "my-1.5";
            public static final String MEDIUM = "my-2.5";
            public static final String LARGE  = "my-3.5";
            public static final String XLARGE = "my-8";
            private Vertical() {}
        }
        public static final class Horizontal {
            public static final String NONE   = "mx-0";
            public static final String XSMALL = "mx-1";
            public static final String SMALL  = "mx-1.5";
            public static final String MEDIUM = "mx-2.5";
            public static final String LARGE  = "mx-3.5";
            public static final String XLARGE = "mx-8";
            private Horizontal() {}
        }
        private Margin() {}
    }

    public static final class Padding {
        public static final String NONE   = "p-0";
        public static final String XSMALL = "p-1";
        public static final String SMALL  = "p-1.5";
        public static final String MEDIUM = "p-2.5";
        public static final String LARGE  = "p-3.5";
        public static final String XLARGE = "p-8";

        public static final class Bottom {
            public static final String NONE   = "pb-0";
            public static final String XSMALL = "pb-1";
            public static final String SMALL  = "pb-1.5";
            public static final String MEDIUM = "pb-2.5";
            public static final String LARGE  = "pb-3.5";
            public static final String XLARGE = "pb-8";
            private Bottom() {}
        }
        public static final class Top {
            public static final String NONE   = "pt-0";
            public static final String XSMALL = "pt-1";
            public static final String SMALL  = "pt-1.5";
            public static final String MEDIUM = "pt-2.5";
            public static final String LARGE  = "pt-3.5";
            public static final String XLARGE = "pt-8";
            private Top() {}
        }
        public static final class Left {
            public static final String NONE   = "pl-0";
            public static final String XSMALL = "pl-1";
            public static final String SMALL  = "pl-1.5";
            public static final String MEDIUM = "pl-2.5";
            public static final String LARGE  = "pl-3.5";
            public static final String XLARGE = "pl-8";
            private Left() {}
        }
        public static final class Right {
            public static final String NONE   = "pr-0";
            public static final String XSMALL = "pr-1";
            public static final String SMALL  = "pr-1.5";
            public static final String MEDIUM = "pr-2.5";
            public static final String LARGE  = "pr-3.5";
            public static final String XLARGE = "pr-8";
            private Right() {}
        }
        public static final class Vertical {
            public static final String NONE   = "py-0";
            public static final String XSMALL = "py-1";
            public static final String SMALL  = "py-1.5";
            public static final String MEDIUM = "py-2.5";
            public static final String LARGE  = "py-3.5";
            public static final String XLARGE = "py-8";
            private Vertical() {}
        }
        public static final class Horizontal {
            public static final String NONE   = "px-0";
            public static final String XSMALL = "px-1";
            public static final String SMALL  = "px-1.5";
            public static final String MEDIUM = "px-2.5";
            public static final String LARGE  = "px-3.5";
            public static final String XLARGE = "px-8";
            private Horizontal() {}
        }
        private Padding() {}
    }

    // ── Typography ────────────────────────────────────────────────────────────────

    public static final class FontSize {
        /** No Tailwind equivalent smaller than xs; mapped to {@code text-xs} (0.75 rem). */
        public static final String XXSMALL  = "text-xs";
        public static final String XSMALL   = "text-xs";
        public static final String SMALL    = "text-sm";
        public static final String MEDIUM   = "text-base";
        public static final String LARGE    = "text-lg";
        public static final String XLARGE   = "text-xl";
        public static final String XXLARGE  = "text-2xl";
        public static final String XXXLARGE = "text-3xl";
        private FontSize() {}
    }

    public static final class FontWeight {
        public static final String THIN       = "font-thin";
        public static final String EXTRALIGHT = "font-extralight";
        public static final String LIGHT      = "font-light";
        public static final String NORMAL     = "font-normal";
        public static final String MEDIUM     = "font-medium";
        public static final String SEMIBOLD   = "font-semibold";
        public static final String BOLD       = "font-bold";
        public static final String EXTRABOLD  = "font-extrabold";
        public static final String BLACK      = "font-black";
        private FontWeight() {}
    }

    public static final class LineHeight {
        public static final String NONE   = "leading-none";
        public static final String XSMALL = "leading-tight";
        public static final String SMALL  = "leading-snug";
        public static final String MEDIUM = "leading-normal";
        private LineHeight() {}
    }

    // ── Colour (Lumo semantic classes — provided by @vaadin/vaadin-lumo-styles) ──
    // These are NOT generated by Tailwind; they are global Lumo utility classes.

    public static final class TextColor {
        public static final String HEADER           = "text-header";
        public static final String BODY             = "text-body";
        public static final String SECONDARY        = "text-secondary";
        public static final String TERTIARY         = "text-tertiary";
        public static final String DISABLED         = "text-disabled";
        public static final String PRIMARY          = "text-primary";
        public static final String PRIMARY_CONTRAST = "text-primary-contrast";
        public static final String ERROR            = "text-error";
        public static final String ERROR_CONTRAST   = "text-error-contrast";
        public static final String WARNING          = "text-warning";
        public static final String WARNING_CONTRAST = "text-warning-contrast";
        public static final String SUCCESS          = "text-success";
        public static final String SUCCESS_CONTRAST = "text-success-contrast";
        private TextColor() {}
    }

    public static final class Background {
        public static final String BASE        = "bg-base";
        public static final String TRANSPARENT = "bg-transparent";
        public static final String CONTRAST    = "bg-contrast";
        public static final String CONTRAST_5  = "bg-contrast-5";
        public static final String CONTRAST_10 = "bg-contrast-10";
        public static final String CONTRAST_20 = "bg-contrast-20";
        public static final String CONTRAST_30 = "bg-contrast-30";
        public static final String CONTRAST_40 = "bg-contrast-40";
        public static final String CONTRAST_50 = "bg-contrast-50";
        public static final String CONTRAST_60 = "bg-contrast-60";
        public static final String CONTRAST_70 = "bg-contrast-70";
        public static final String CONTRAST_80 = "bg-contrast-80";
        public static final String CONTRAST_90 = "bg-contrast-90";
        public static final String PRIMARY     = "bg-primary";
        public static final String PRIMARY_10  = "bg-primary-10";
        public static final String PRIMARY_50  = "bg-primary-50";
        public static final String ERROR       = "bg-error";
        public static final String ERROR_10    = "bg-error-10";
        public static final String ERROR_50    = "bg-error-50";
        public static final String WARNING     = "bg-warning";
        public static final String WARNING_10  = "bg-warning-10";
        public static final String SUCCESS     = "bg-success";
        public static final String SUCCESS_10  = "bg-success-10";
        public static final String SUCCESS_50  = "bg-success-50";
        private Background() {}
    }

    public static final class Border {
        public static final String NONE   = "border-0";
        public static final String ALL    = "border";
        public static final String BOTTOM = "border-b";
        public static final String TOP    = "border-t";
        public static final String LEFT   = "border-l";
        public static final String RIGHT  = "border-r";
        public static final String DASHED = "border-dashed";
        public static final String DOTTED = "border-dotted";
        private Border() {}
    }

    public static final class BorderColor {
        public static final String CONTRAST    = "border-contrast";
        public static final String CONTRAST_10 = "border-contrast-10";
        public static final String CONTRAST_20 = "border-contrast-20";
        public static final String CONTRAST_30 = "border-contrast-30";
        public static final String CONTRAST_40 = "border-contrast-40";
        public static final String CONTRAST_50 = "border-contrast-50";
        public static final String PRIMARY     = "border-primary";
        public static final String ERROR       = "border-error";
        public static final String SUCCESS     = "border-success";
        public static final String WARNING     = "border-warning";
        private BorderColor() {}
    }

    // ── Decoration ────────────────────────────────────────────────────────────────

    public static final class BorderRadius {
        public static final String NONE   = "rounded-none";
        public static final String SMALL  = "rounded-sm";
        public static final String MEDIUM = "rounded-md";
        public static final String LARGE  = "rounded-lg";
        public static final String FULL   = "rounded-full";
        private BorderRadius() {}
    }

    public static final class BoxShadow {
        public static final String NONE   = "shadow-none";
        public static final String XSMALL = "shadow-xs";
        public static final String SMALL  = "shadow-sm";
        public static final String MEDIUM = "shadow-md";
        public static final String LARGE  = "shadow-lg";
        public static final String XLARGE = "shadow-xl";
        private BoxShadow() {}
    }

    public static final class BoxSizing {
        public static final String BORDER  = "box-border";
        public static final String CONTENT = "box-content";
        private BoxSizing() {}
    }

    // ── Sizing ────────────────────────────────────────────────────────────────────

    public static final class Width {
        /** Component-size classes — provided by {@code @vaadin/vaadin-lumo-styles}, not Tailwind. */
        public static final String XSMALL = "w-xs";
        public static final String SMALL  = "w-s";
        public static final String MEDIUM = "w-m";
        public static final String LARGE  = "w-l";
        public static final String XLARGE = "w-xl";
        public static final String AUTO   = "w-auto";
        public static final String FULL   = "w-full";
        private Width() {}
    }

    public static final class Height {
        public static final String NONE   = "h-0";
        /** Component-size classes — provided by {@code @vaadin/vaadin-lumo-styles}, not Tailwind. */
        public static final String XSMALL = "h-xs";
        public static final String SMALL  = "h-s";
        public static final String MEDIUM = "h-m";
        public static final String LARGE  = "h-l";
        public static final String XLARGE = "h-xl";
        public static final String AUTO   = "h-auto";
        public static final String FULL   = "h-full";
        public static final String SCREEN = "h-screen";
        private Height() {}
    }
}
