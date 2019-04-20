package com.toufuchew.cardocr.idcard.ocr;

public interface CardOCR {

    String getIDString();

    String getValidDateString();

    boolean checkCardID();
}
