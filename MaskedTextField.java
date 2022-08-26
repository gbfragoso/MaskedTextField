import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;

/**
 * This component receives a mask that dictate the valid input for this field.
 * @author gbfragoso
 * @version 2.1
 */
public class MaskedTextField extends TextField {

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
    private StringProperty mask;
    private StringProperty plainText;
    private StringBuilder plainTextBuilder;
    
    private List<MaskCharacter> semanticMask;

    public MaskedTextField() {
        this("", '_');
    }

    public MaskedTextField(String mask) {
        this(mask, '_');
    }

    public MaskedTextField(String mask, char placeholder) {
        this.mask = new SimpleStringProperty(this, "mask", mask);
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
        
        // Add a listener to the plain text property so that binding will properly update the formatting as well
        this.plainTextProperty().addListener((observable, oldValue, newValue) ->
        {
            this.updateSemanticMask(newValue);
        });
    }

    // *******************************************************
    // Properties
    // *******************************************************
    public String getPlainText() {
        return plainText.get();
    }
    
    public void setPlainText(String text) {
        setPlainTextWithUpdate(text);
    }
    
    public StringProperty plainTextProperty() {
        return this.plainText;
    }

    public String getMask() {
        return mask.get();
    }
    
    /**
     * Set input mask, rebuild internal mask and update view.
     * @param mask Mask dictating legal character values.
     */
    public void setMask(String mask) {
        this.mask.set(mask);
        buildSemanticMask();
        updateSemanticMask("");
    }

    public StringProperty maskProperty() {
        return this.mask;
    }

    // *******************************************************
    // Getters and Setters
    // *******************************************************
    
    /**
     * Returns the character to use in place of characters that are not present in the value, ie the user must fill them in.
     */
    public char getPlaceholder() {
        return this.placeholder;
    }
    
    /**
     * Set placeholder character.
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

    private void resetSemanticMask() {
        semanticMask.stream().forEach(maskCharacter-> maskCharacter.setValue(placeholder));
    }

    private void updateSemanticMask(String newText) {
        resetSemanticMask();
        stringToValue(newText);
        setText(valuesToString());
    }
    
    // *******************************************************
    // Private Methods
    // *******************************************************

    /**
     * Given a position in mask convert it into plainText position
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
     * Given a position in plain text convert it into mask position
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
     * Return the position of first mask with placeholder on value.
     */
    private int firstPlaceholderPosition() {
        for (int i = 0; i < maskLength; i++) {
            if (semanticMask.get(i).getValue() == placeholder) {
                return i;
            }
        }
        return -1;
    }
    
    private void setPlainTextWithUpdate(String text) {
        String newText = (text != null) ? text : "";
        setPlainTextWithoutUpdate(newText);
        updateSemanticMask(newText);        
    }
    
    private void setPlainTextWithoutUpdate(String text) {
        plainTextBuilder = new StringBuilder(text);
        plainText.set(text);        
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
        
        setPlainTextWithoutUpdate(validText.toString());
    }
    
    /**
     * Get all the semanticMask's values and convert it into an string.
     */
    private String valuesToString() {
        StringBuilder value = new StringBuilder();
        semanticMask.stream().forEach(character -> value.append(character.getValue()));
        return value.toString();
    }

    // *******************************************************
    // Overrides
    // *******************************************************

    @Override
    public void replaceText(int start, int end, String newText) {
        int position = convertToPlainTextPosition(start);
        int endPosition = convertToPlainTextPosition(end);

        String newString = null;
        if (start != end) {
            newString = plainTextBuilder.replace(position, endPosition, newText).toString();
        } else {
            newString = plainTextBuilder.insert(position, newText).toString();
        }
        updateSemanticMask(newString);
        
        int newCaretPosition = convertToMaskPosition(position + newText.length());
        selectRange(newCaretPosition, newCaretPosition);
    }

    @Override
    public void replaceSelection(String string) {
        IndexRange range = getSelection();
        if (string.equals("")) {
            deleteText(range.getStart(), range.getEnd());
        } else {
            replaceText(range.getStart(), range.getEnd(), string);
        }
    }

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
