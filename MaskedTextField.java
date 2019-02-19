package org.casadeguara.componentes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;

/**
 * This approach has been inspired on https://github.com/vas7n/VAMaskField solution. We added new
 * masks, fixed bugs and improved performance. Now this component works much closer to
 * JFormattedTextfield.
 * 
 * @author gbfragoso
 * @version 2.0
 */public class MaskedTextField extends TextField {

    // Available properties
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

    private int maskLength;
    private char placeholder;
    private String mask;
    private StringBuilder plainTextBuilder;
    
    private List<MaskCharacter> semanticMask;

    public MaskedTextField() {
        this("", '_');
    }

    public MaskedTextField(String mask) {
        this(mask, '_');
    }

    public MaskedTextField(String mask, char placeholder) {
        this.mask = mask;
        this.placeholder = placeholder;
        this.plainText = new SimpleStringProperty(this, "plaintext", "");
        this.plainTextBuilder = new StringBuilder();
        this.semanticMask = new ArrayList<>();
        
        init();
    }
    
    private void init() {
        buildSemanticMask();
        updateSemanticMask("");
        
        // When MaskedTextField gains focus caret goes to first placeholder position
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                Platform.runLater(() -> {
                    int pos = firstPlaceholderPosition();
                    selectRange(pos, pos);
                    positionCaret(pos);
                });
            }
        });
    }

    // *******************************************************
    // PlainText Property
    // *******************************************************
    public String getPlainText() {
        return plainText.get();
    }
    
    public void setPlainText(String text) {
        setPlainText(text, true);
    }
    
    private void setPlainText(String text, boolean update) {
        String newText = (text != null) ? text : "";
        plainText.set(newText);
        
        if(update) {
            updateSemanticMask(newText);
        }
    }
    
    public StringProperty plainTextProperty() {
        return plainText;
    }

    // *******************************************************
    // Getters and Setters
    // *******************************************************
    
    /**
     * Returns the formatting mask.
     * @return Mask dictating legal character values.
     */
    public String getMask() {
        return this.mask;
    }
    
    /**
     * Set input mask, rebuild component and update view.
     * @param mask Mask dictating legal character values.
     */
    public final void setMask(String mask) {
        this.mask = mask;
        buildSemanticMask();
        updateSemanticMask("");
    }
    
    /**
     * Returns the character to use in place of characters that are not present in the value, ie the user must fill them in.
     * @return Character used when formatting if the value does not completely fill the mask
     */
    public char getPlaceholder() {
        return this.placeholder;
    }
    
    /**
     * Set placeholder character.
     * @param placeholder Characters that user must fill.
     */
    public void setPlaceholder(char placeholder) {
        this.placeholder = placeholder;
    }

    // *******************************************************
    // Semantic Mask Methods
    // *******************************************************

    /**
     * Build internal mask from input mask using AbstractFactory to add the right MaskCharacter.
     */
    private void buildSemanticMask() {
        char[] newMask = getMask().toCharArray();
        int i = 0;
        int length = newMask.length;
        
        semanticMask.clear();
        
        MaskFactory factory = new MaskFactory();
        while(i < length) {
            char maskValue = newMask[i];

            // If the actual char is MASK_ESCAPE look the next char as literal
            if (maskValue == MASK_ESCAPE) {
                semanticMask.add(factory.createMask(maskValue, newMask[i + 1]));
                i++;
            } else {
                char value = isLiteral(maskValue) ? maskValue : placeholder;
                semanticMask.add(factory.createMask(maskValue, value));
            }
            
            i++;
        }
        
        maskLength = semanticMask.size();
    }

    /**
     * Returns to the initial semanticMask's state when all non-literal values
     * are equals the placeholder character.
     */
    private void resetSemanticMask() {
        semanticMask.stream().forEach(m-> m.setValue(placeholder));
    }

    /**
     * Updates all semanticMask's values according to plainText and set the editor text.
     */
    public void updateSemanticMask(String newText) {
        resetSemanticMask();
        stringToValue(newText);
        setText(valuesToString());
    }
    
    // *******************************************************
    // Methods
    // *******************************************************

    /**
     * Given a position in mask convert it into plainText position
     * 
     * @param pos Position in mask
     * @return converted position
     */
    private int convertToPlainTextPosition(int pos) {
        int count = 0;

        for (int i = 0; i < maskLength && i < pos; i++) {
            MaskCharacter m = semanticMask.get(i);
            if (!(m.getValue() == placeholder || m.isLiteral())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Given a plain text position return the maskPosition
     * 
     * @param pos Position in mask
     * @return converted position
     */
    private int convertToMaskPosition(int pos) {
        int countLiterals = 0;
        int countNonLiterals = 0;

        for (int i = 0; i < maskLength && countNonLiterals < pos; i++) {
            if (semanticMask.get(i).isLiteral()) {
                countLiterals++;
            } else {
                countNonLiterals++;
            }
        }

        return countLiterals + countNonLiterals;
    }
    
    /**
     * Return true if a given char isn't a mask.
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
     * Fetch first mask with placeholder on value.
     * 
     * @return int Position of first placeholder on mask
     */
    public int firstPlaceholderPosition() {
        for (int i = 0; i < maskLength; i++) {
            if (semanticMask.get(i).getValue() == placeholder) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Insert values on semantic mask.
     * @param text Plain text to be inserted
     */
    private void stringToValue(String text) {
        StringBuilder inputText = new StringBuilder(text);
        StringBuilder validText = new StringBuilder();

        int maskPosition = 0;
        int textLength = text.length();
        int textPosition = 0;
        
        while (textPosition < textLength) {
            if (maskPosition < maskLength) {
                MaskCharacter maskCharacter = semanticMask.get(maskPosition);
                
                if (!maskCharacter.isLiteral()) {
                    char ch = inputText.charAt(textPosition);
                    
                    if(maskCharacter.accept(ch)) {
                        maskCharacter.setValue(ch);
                        validText.append(maskCharacter.getValue());
                        maskPosition++;
                    } 

                    textPosition++;
                } else {
                    maskPosition++;
                }
            } else {
                break;
            }
        }
        
        setPlainText(validText.toString(), false);
    }
    
    /**
     * Get all the semanticMask's values and convert it into an string.
     * 
     * @return String Concatenation of all values of semanticMask
     */
    private String valuesToString() {
        StringBuilder value = new StringBuilder();
        semanticMask.stream().forEach(character -> value.append(character.getValue()));
        return value.toString();
    }

    // *******************************************************
    // Overrides
    // *******************************************************

    /**
     * Main method to insert text on mask. The left side of newString only exist if user insert text
     * on middle, is empty on most cases
     * 
     * @param start Edition's start range
     * @param end Edition's end
     * @param newText Text to be inserted/replaced
     */
    @Override
    public void replaceText(int start, int end, String newText) {
        int position = convertToPlainTextPosition(start);
        
        String newString = plainTextBuilder.insert(position, newText).toString();
        updateSemanticMask(newString);

        int newCaretPosition = convertToMaskPosition(newString.lastIndexOf(newText) + newText.length());
        selectRange(newCaretPosition, newCaretPosition);
    }

    /**
     * Enables the delete/insert text at selected position
     * 
     * @param string
     */
    @Override
    public void replaceSelection(String string) {
        IndexRange range = getSelection();
        if (string.equals("")) {
            deleteText(range.getStart(), range.getEnd());
        } else {
            replaceText(range.getStart(), range.getEnd(), string);
        }
    }

    /**
     * Delete text char by char left to right (backspace) and right to left (delete)
     * 
     * @param start Delete start
     * @param end Delete end
     */
    @Override
    public void deleteText(int start, int end) {

        int plainStart = convertToPlainTextPosition(start);
        int plainEnd = convertToPlainTextPosition(end);
        
        plainTextBuilder.delete(plainStart, plainEnd);
        updateSemanticMask(plainTextBuilder.toString());

        selectRange(start, start);
    }

    @Override
    public void clear() {
        setPlainText("");
    }
    
    // *******************************************************
    // Private Classes
    // *******************************************************
    private abstract class MaskCharacter {
        
        private char value;
        
        public MaskCharacter(char value) {
            this.value = value;
        }
        
        public char getValue() {
            return this.value;
        }
        
        public void setValue(char value) {
            this.value = value;
        }
        
        public boolean isLiteral() {
            return false;
        }
        
        abstract boolean accept(char value);
    }
    
    private class MaskFactory {
        
        public MaskCharacter createMask(char mask, char value) {
            switch (mask) {
                case MASK_ANYTHING:
                    return new AnythingCharacter(value);
                case MASK_CHARACTER:
                    return new LetterCharacter(value);
                case MASK_NUMBER:
                    return new NumericCharacter(value);
                case MASK_CHAR_OR_NUM:
                    return new AlphaNumericCharacter(value);
                case MASK_HEXADECIMAL:
                    return new HexCharacter(value);
                case MASK_LOWER_CHARACTER:
                    return new LowerCaseCharacter(value);
                case MASK_UPPER_CHARACTER:
                    return new UpperCaseCharacter(value);
                default:
                    return new LiteralCharacter(value);
            }

        } 
    }
    
    private class AnythingCharacter extends MaskCharacter {
        
        public AnythingCharacter(char value) {
            super(value);
        }

        public boolean accept(char value) {
            return true;
        }
    }
    
    private class AlphaNumericCharacter extends MaskCharacter {
        
        public AlphaNumericCharacter(char value) {
            super(value);
        }

        public boolean accept(char value) {
            return Character.isLetterOrDigit(value);
        }
    }
    
    private class LiteralCharacter extends MaskCharacter {

        public LiteralCharacter(char value) {
            super(value);
        }
        
        @Override
        public boolean isLiteral() {
            return true;
        }
        
        @Override
        public void setValue(char value) {
            // Literal can't be changed
        }

        public boolean accept(char value) {
            return false;
        }
    }
    
    private class LetterCharacter extends MaskCharacter {

        public LetterCharacter(char value) {
            super(value);
        }

        public boolean accept(char value) {
            return Character.isLetter(value);
        }

    }
    
    private class LowerCaseCharacter extends MaskCharacter {
        
        public LowerCaseCharacter(char value) {
            super(value);
        }

        @Override
        public char getValue() {
            return Character.toLowerCase(super.getValue());
        }
               
        public boolean accept(char value) {
            return Character.isLetter(value);
        }
    }
    
    private class UpperCaseCharacter extends MaskCharacter {
        
        public UpperCaseCharacter(char value) {
            super(value);
        }

        @Override
        public char getValue() {
            return Character.toUpperCase(super.getValue());
        }
        
        public boolean accept(char value) {
            return Character.isLetter(value);
        }
    }
    
    private class NumericCharacter extends MaskCharacter {

        public NumericCharacter(char value) {
            super(value);
        }

        @Override
        public boolean accept(char value) {
            return Character.isDigit(value);
        }
    
    }
    
    private class HexCharacter extends MaskCharacter {
        
        public HexCharacter(char value) {
            super(value);
        }

        @Override
        public boolean accept(char value) {
            return Pattern.matches("[0-9a-fA-F]", String.valueOf(value));
        }
    }
}
