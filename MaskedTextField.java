/*
 * Copyright (C) 2017 Gustavo Fragoso
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

/**
 * This approach has been inspired on https://github.com/vas7n/VAMaskField solution.
 * We added new masks, fixed bugs and improved performance.
 * Now this component works much closer to JFormattedTextfield.
 * @author gbfragoso
 */
public class MaskedTextField extends TextField{
    
    // Available properties
    private StringProperty mask;
    private StringProperty placeholder;
    private StringProperty plainText;
    
    // Available masks
    private static final char MASK_ANYTHING = '*';
    private static final char MASK_CHARACTER = '?';
    private static final char MASK_CHAR_OR_NUM = 'A';
    private static final char MASK_HEXADECIMAL = 'H';
    private static final char MASK_LOWER_CHARACTER = 'L';
    private static final char MASK_NUMBER = '#';
    private static final char MASK_UPPER_CHARACTER = 'U';
    
    private String defaultText;
    private String actualText;
    private int maskLength;
    
    public MaskedTextField(){
        this.mask = new SimpleStringProperty(this, "mask", "");
        this.placeholder = new SimpleStringProperty(this,"placeholder","_");
        this.plainText = new SimpleStringProperty(this,"plaintext","");
        start();
    }
    
    public MaskedTextField(String mask){
        this.mask = new SimpleStringProperty(this, "mask", mask);
        this.placeholder = new SimpleStringProperty(this,"placeholder","_");
        this.plainText = new SimpleStringProperty(this,"plaintext","");
        start();
    }
    
    public MaskedTextField(String mask, char placeHolder){
        this.mask = new SimpleStringProperty(this, "mask", mask);
        this.placeholder = new SimpleStringProperty(this,"placeholder",String.valueOf(placeHolder));
        this.plainText = new SimpleStringProperty(this,"plaintext","");
        start();
    }
    
    private void start(){
        maskLength = mask.get().length();
        defaultText = mask.get().replaceAll("[#\\?HULA\\*]", placeholder.get());
        actualText = defaultText;
        setText(defaultText);
        
        // When MaskedTextField gains focus caret goes to first placeholder position and avoid default selection
        focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            Platform.runLater(() -> {
                int pos = getText().indexOf(placeholder.get());
                selectRange(pos, pos);
                positionCaret(pos);
            });
        });
    }
    
    // *******************************************************
    // Property's value getters
    // *******************************************************
    public final String getMask(){
        return mask.get();
    }

    public final String getPlaceholder(){
        return placeholder.get();
    }

    public final String getPlainText(){
        return plainText.get();
    }

    // *******************************************************
    // Property's value setters
    // *******************************************************
    public final void setMask(String m){
        mask.set(m);
        maskLength = m.length();
        defaultText = m.replaceAll("[#\\?HULA\\*]", placeholder.get());
        updateEditorText();
    }
    
    public final void setPlaceholder(String holder){
        placeholder.set(holder);
        defaultText = mask.get().replaceAll("[#\\?HULA\\*]", holder);
        updateEditorText();
    }
    
    public final void setPlainText(String text){
        plainText.set(text);
        updateEditorText();
    }
    
    // *******************************************************
    // Property getters
    // *******************************************************
    public StringProperty maskProperty(){
        return mask;
    }
    
    public StringProperty placeholderProperty(){
        return placeholder;
    }
    
    public StringProperty plainTextProperty(){
        return plainText;
    }
    
    // *******************************************************
    // Methods
    // *******************************************************
    private boolean isCorrect(char m, char c){
        switch(m){
            case MASK_ANYTHING: return true;
            case MASK_CHARACTER: return Character.isLetter(c);
            case MASK_NUMBER: return Character.isDigit(c);
            case MASK_CHAR_OR_NUM: return Character.isLetterOrDigit(c);
            case MASK_HEXADECIMAL: return Pattern.matches("[0-9a-fA-F]", String.valueOf(c));
            case MASK_LOWER_CHARACTER: return Character.isLetter(c);
            case MASK_UPPER_CHARACTER: return Character.isLetter(c);
        }
        return false;
    }
    
    // Count diffences between mask and actual text to see where plain text starts/end
    private int maskPositionToPlainTextPosition(int pos){
        int count = 0;
        char [] atext = actualText.toCharArray();
        char [] mtext = defaultText.toCharArray();
        
        for (int i = 0; i < maskLength && i < pos; i++){
            if (atext[i] != mtext[i]){
                count++;
            }
        }
        
        return count;
    }
    
    private void updateEditorText(){
        
        String newPlainText = getPlainText();
        
        int plainCharCounter = 0;
        int lastPlainCharInMask = 0;
        int plainTextSize = newPlainText.length();
        
        char holder = placeholder.get().charAt(0);
        char[] newText = defaultText.toCharArray(); 
        char[] textMask = mask.get().toCharArray();
        char[] textPlain = newPlainText.toCharArray();
        
        for(int i = 0; i < maskLength; i++){
            // If we has free slots and plainText to add
            if(newText[i] == holder && plainTextSize > plainCharCounter){
                // Verifing if character entered are valid
                if(isCorrect(textMask[i], textPlain[plainCharCounter])){
                    newText[i] = textPlain[plainCharCounter];
                    
                    // When MASK_LOWER|UPPER_CHARACTER is set apply lower|upper to the char
                    if(textMask[i] == MASK_LOWER_CHARACTER){
                        newText[i] = Character.toLowerCase(textPlain[plainCharCounter]);
                    }
                    if(textMask[i] == MASK_UPPER_CHARACTER){
                        newText[i] = Character.toUpperCase(textPlain[plainCharCounter]);
                    }
                    
                    lastPlainCharInMask = i;
                }else{
                    // Remove wrong character from textplain
                    newPlainText = newPlainText.substring(0, plainCharCounter) + newPlainText.substring(plainCharCounter+1);
                }
                plainCharCounter++;
            }
        }
        
        // Setting new text with mask
        actualText = String.valueOf(newText);
        setText(actualText);
        
        // Positioning caret
        int caretPosition = (plainTextSize > 0 ? lastPlainCharInMask + 1 : actualText.indexOf(holder));
        selectRange(caretPosition, caretPosition);
        
        // Limiting plain text to number of free slots
        if (plainTextSize > plainCharCounter)
            newPlainText = newPlainText.substring(0, plainCharCounter);
        
        // If the statement above comes "true" or wrong characteres are entered, ajust the plain text
        if (!newPlainText.equals(getPlainText())){
            setPlainText(newPlainText);
        }
    }
    
    
    // *******************************************************
    // Overrides
    // *******************************************************
    
    @Override
    public void replaceText(int start, int end, String Text){
        
        int plainStart = maskPositionToPlainTextPosition(start);
        int plainEnd = maskPositionToPlainTextPosition(end);
    
        String oldPlainText = getPlainText();
        String rightText = oldPlainText;
        String leftText = "";
        
        // When user delete text
        if (oldPlainText.length() > plainStart){
            rightText = oldPlainText.substring(0, plainStart);
        }
        
        // When user insert text
        if (oldPlainText.length() > plainEnd){
            leftText = oldPlainText.substring(plainEnd);
        }
        
        // Building new plain text
        setPlainText(rightText + Text + leftText);
    }
    
    @Override
    public void clear(){
        setPlainText("");
    }
}
