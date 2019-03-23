package com.toufuchew.cardocr.idcard.cv.imgutils;


import org.opencv.core.Mat;

/**
 * Created by chenqiu on 3/11/19.
 */
public class CardFonts {

    /**
     * The sign as the type of digits on credit card,
     * <p>their differences mainly are Digit`1` , Digit `6` and Digit `9`</p>
     */
    public enum FontType {
        BLACK_FONT, // means Typographic font
        LIGHT_FONT, // means Bump font
        UNKNOWN,
    }

    protected Mat fonts;

    private FontType type;

    public CardFonts(){
        fonts = null;
        type = FontType.UNKNOWN;
    }

    public CardFonts(Mat fonts, FontType type) {
        this.fonts = fonts;
        this.type = type;
    }

    public FontType getType() {
        return type;
    }

    public void setType(FontType type) {
        this.type = type;
    }

    public void setFonts(Mat fonts) {
        this.fonts = fonts;
    }

    public Mat getFonts() {
        return fonts;
    }

    public static String fontTypeToString(FontType type) {
        if (type == FontType.LIGHT_FONT) {
            return "FontType.LIGHT_FONT";
        }
        else if (type == FontType.BLACK_FONT) {
            return "FontType.BLACK_FONT";
        }
        else if (type == FontType.UNKNOWN) {
            return "FontType.UNKNOWN";
        }
        return "";
    }
}
