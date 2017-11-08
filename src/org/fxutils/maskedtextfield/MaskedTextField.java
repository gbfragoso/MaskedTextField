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

package org.fxutils.maskedtextfield;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;
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
    private static final char MASK_ESCAPE = '\'';
    private static final char MASK_NUMBER = '#';
    private static final char MASK_CHARACTER = '?';
    private static final char MASK_HEXADECIMAL = 'H';
    private static final char MASK_UPPER_CHARACTER = 'U';
    private static final char MASK_LOWER_CHARACTER = 'L';
    private static final char MASK_CHAR_OR_NUM = 'A';
    private static final char MASK_ANYTHING = '*';
    
    private ArrayList<Mask> semanticMask = new ArrayList<>();
    private int maskLength;
    private int semanticMaskLength;
    
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
        buildSemanticMask();
        updateSemanticMask();
    }
    
    public final void setPlaceholder(String holder){
        placeholder.set(holder);
        resetSemanticMask();
        updateSemanticMask();
    }
    
    public final void setPlainText(String text){
        plainText.set(text);
        updateSemanticMask();
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
    
    /**
     * Configuration method
     */
    private void start(){
        maskLength = getMask().length();
        buildSemanticMask();
        setText(allSemanticValuesToString());
        
        // When MaskedTextField gains focus caret goes to first placeholder position
        focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            Platform.runLater(() -> {
                int pos = firstPlaceholderPosition();
                selectRange(pos, pos);
                positionCaret(pos);
            });
        });
    }
    
    /**
     * Take user mask and convert it into a Semantic Mask, when each value receives
     * the state of literal or non-literal.
     */
    private void buildSemanticMask(){
        String newMask = getMask();
        for (int i = 0; i < maskLength; i++){
            char c = newMask.charAt(i);
            
            // If the actual char is MASK_ESCAPE look the next char as literal
            if(c == MASK_ESCAPE){
                semanticMask.add(new Mask(newMask.charAt(i+1), newMask.charAt(i+1), true, false));
                i++;
            }else{
                if(isLiteral(c)){
                    semanticMask.add(new Mask(c, c, true, false));
                }else{
                    semanticMask.add(new Mask(getPlaceholder().charAt(0), c, false, true));
                }
            }
        }
        semanticMaskLength = semanticMask.size();
    }
    
    /**
     * Returns to the initial semanticMask's state, when all non-literal values 
     * are equals to placeholder char and isPlaceholder is true.
     */
    private void resetSemanticMask(){
        char p = getPlaceholder().charAt(0);
        semanticMask.stream()
            .filter(e -> !e.isLiteral())
            .forEach(e -> {
                e.setValue(p);
                e.setPlaceholder(true);
            });
    }
    
    /**
     * Updates all semanticMask's values according to plainText and set the
     * editor text.
     */
    public void updateSemanticMask(){
        resetSemanticMask();
        
        String newPlainText = getPlainText();
        
        int plainCharCounter = 0;
        int plainTextSize = newPlainText.length();

        char[] textPlain = newPlainText.toCharArray();
        
        for(int i = 0; i < semanticMaskLength; i++){
            Mask m = semanticMask.get(i);
            
            // If we has free slots and plainText to add
            if(m.isPlaceholder() && plainTextSize > plainCharCounter){
                // Verifing if character entered are valid
                if(isCorrect(m.getMask(), textPlain[plainCharCounter])){

                    // Change value of mask and turn off isPlaceholder
                    m.setValue(textPlain[plainCharCounter]);
                    m.setPlaceholder(false);
                    
                    // When MASK_LOWER|UPPER_CHARACTER is set apply lower|upper to the char
                    if(m.getMask() == MASK_LOWER_CHARACTER){
                        m.setValue(Character.toLowerCase(textPlain[plainCharCounter]));
                    }else if(m.getMask() == MASK_UPPER_CHARACTER){
                        m.setValue(Character.toUpperCase(textPlain[plainCharCounter]));
                    }

                }else{
                    // Remove wrong character from textplain
                    newPlainText = newPlainText.substring(0, plainCharCounter) + newPlainText.substring(plainCharCounter+1);
                }
                plainCharCounter++;
            }
        }
        
        // Setting new text with mask
        setText(allSemanticValuesToString());
        
        // Limiting plain text to number of free slots
        if (plainTextSize > plainCharCounter)
            newPlainText = newPlainText.substring(0, plainCharCounter);
        
        // If the statement above comes "true" or wrong characteres are entered, ajust the plain text
        if (!newPlainText.equals(getPlainText())){
            setPlainText(newPlainText);
        }    
    }
    
    /**
     * Get all the semanticMask's values and convert it into an string.
     * @return String Concatenation of all values of semanticMask
     */
    private String allSemanticValuesToString(){
        return semanticMask.stream().map(e -> String.valueOf(e.getValue())).collect(Collectors.joining());
    }
    
    /**
     * If a given char is literal, that is isn't a mask, return true
     * else false.
     * @param c character for verification
     * @return boolean
     */
    private boolean isLiteral(char c){
        return (c != MASK_ANYTHING &&
                c != MASK_CHARACTER &&
                c != MASK_ESCAPE &&
                c != MASK_NUMBER && 
                c != MASK_CHAR_OR_NUM &&
                c != MASK_HEXADECIMAL &&
                c != MASK_LOWER_CHARACTER &&
                c != MASK_UPPER_CHARACTER);
    }
    
    /**
     * For an given char and mask, returns if char is according with the mask
     * @param m An valid mask value
     * @param value Any char
     * @return boolean Return true if the given char is according with the mask
     */
    public boolean isCorrect(char m, char value){
        switch(m){
            case MASK_ANYTHING: return true;
            case MASK_CHARACTER: return Character.isLetter(value);
            case MASK_NUMBER: return Character.isDigit(value);
            case MASK_CHAR_OR_NUM: return Character.isLetterOrDigit(value);
            case MASK_HEXADECIMAL: return Pattern.matches("[0-9a-fA-F]", String.valueOf(value));
            case MASK_LOWER_CHARACTER: return Character.isLetter(value);
            case MASK_UPPER_CHARACTER: return Character.isLetter(value);
        }
        return false;
    }
    
    /**
     * Browse semanticMask and give the position of first mask with isPlaceholder = true.
     * Even if plainText has a placeholder on it.
     * @return int first placeholder on mask
     */
    public int firstPlaceholderPosition(){
        for(int i = 0; i < semanticMaskLength; i++){
            if(semanticMask.get(i).isPlaceholder()){
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Given a position in mask convert it into plainText position
     * @param pos Position in mask
     * @return converted position
     */
    private int maskPositionToPlaintextPosition(int pos){
        int count = 0;
        
        for (int i = 0; i < semanticMaskLength && i < pos; i++){
            Mask m = semanticMask.get(i);
            if (!(m.isPlaceholder() || m.isLiteral())){
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Given a plain text position return the maskPosition
     * @param pos 
     */
    private int plaintextPositionToMaskPosition(int pos){
        int countLiterals = 0, countNonLiterals = 0;
        
        for (int i = 0; i < semanticMaskLength && countNonLiterals < pos; i++){
            Mask m = semanticMask.get(i);
            if (m.isLiteral()){
                countLiterals++;
            }else {
                countNonLiterals++;
            }
        }
        
        return countLiterals + countNonLiterals;
    }
    
    // *******************************************************
    // Overrides
    // *******************************************************
    
    /**
     * Main method to insert text on mask.
     * The left side of newString only exist if user insert text on middle, is empty on most cases
     * @param start Edition's start range
     * @param end Edition's end
     * @param newText Text to be inserted/replaced
     */
    @Override
    public void replaceText(int start, int end, String newText){
        
        int plainStart = maskPositionToPlaintextPosition(start);
        
        String oldPlainText = getPlainText();
        String newString = oldPlainText.substring(0, plainStart) + newText + oldPlainText.substring(plainStart, oldPlainText.length());
        setPlainText(newString);
        
        int newPos = plaintextPositionToMaskPosition(newString.lastIndexOf(newText) + newText.length());
        selectRange(newPos, newPos);
    }
    
    /**
     * Enables the delete/insert text at selected position
     * @param string 
     */
    @Override
    public void replaceSelection(String string){
        IndexRange range = getSelection();
        if(string.equals("")){
            deleteText(range.getStart(), range.getEnd());
        }else{
            replaceText(range.getStart(), range.getEnd(), string);
        }
    }
    
    /**
     * Delete text char by char left to right (backspace) and right to left (delete)
     * @param start Delete start
     * @param end Delete end
     */
    @Override
    public void deleteText(int start, int end){
        
        int plainStart = maskPositionToPlaintextPosition(start);
        int plainEnd = maskPositionToPlaintextPosition(end);
        
        StringBuilder newString = new StringBuilder(getPlainText());
        newString.delete(plainStart, plainEnd);
        setPlainText(newString.toString());

        selectRange(start, start);
    }
    
    @Override
    public void clear(){
        setPlainText("");
    }
}
