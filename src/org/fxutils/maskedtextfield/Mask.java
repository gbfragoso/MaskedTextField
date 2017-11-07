/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fxutils.maskedtextfield;

/**
 *
 * @author gbfragoso
 */
public class Mask {
    
    private final char mask;
    private final boolean literal;
    private char value;
    private boolean placeholder;
    
    public Mask (char mask, boolean literal, boolean placeholder) {
        this.mask = mask;
        this.literal = literal;
        this.placeholder = placeholder;
    }
    
    public Mask (char value, char mask, boolean literal, boolean placeholder) {
        this.value = value;
        this.mask = mask;
        this.literal = literal;
        this.placeholder = placeholder;
    }
    
    public char getMask(){
        return this.mask;
    }
    
    public char getValue(){
        return this.value;
    }
    
    public void setValue(char value){
        this.value = value;
    }
    
    public boolean isLiteral(){
        return this.literal;
    }
    
    public boolean isPlaceholder(){
        return this.placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }
}
