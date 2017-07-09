/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.medmax.potholedetector.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Max Medina
 */
public class CSVHelper {
    
    private BufferedWriter bwritter;
    private FileWriter fwritter;
    private boolean isOpen = false;
    private File currentFile;

    public CSVHelper() {

    }

    public void open(String filepath, boolean append) throws IOException {
        fwritter = new FileWriter(filepath, append);
        bwritter = new BufferedWriter(fwritter);
        isOpen = true;
    }

    public void open(File directory, String filename, boolean append) throws IOException {
        if(!directory.exists()){
            directory.mkdir();
        }

        File file = new File(directory, filename);
        fwritter = new FileWriter(file, append);
        bwritter = new BufferedWriter(fwritter);
        isOpen = true;
        currentFile = file;
    }

    public void close() throws IOException{
        if(bwritter != null) {
            bwritter.close();
        }
        
        if(fwritter != null) {
            fwritter.close();
        }
        currentFile = null;
        isOpen = false;
    }
    
    public void setHeader(String[] headers) throws FileNotFoundException, IOException {
        if(bwritter == null) {
            throw new FileNotFoundException();
        }
        
        for(int i= 0; i< headers.length; i++) {
            String header = headers[i];
            
            if(i < headers.length-1) {
                bwritter.write(String.format("%s,", header));
                continue;
            } 
            
            bwritter.write(String.format("%s\n", header));
        }
    }
    
    public void write(String line) throws FileNotFoundException, IOException {
        if(bwritter == null) {
            throw new FileNotFoundException();
        }
        
        bwritter.write(String.format("%s\n", line));
    }
    
    public void write(String[] values) throws FileNotFoundException, IOException {
        if (bwritter == null) {
            throw new FileNotFoundException();
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i];

            if (i < values.length - 1) {
                bwritter.write(String.format("%s,", value));
                continue;
            }
            bwritter.write(String.format("%s\n", value));
        }
    }

    public boolean isOpen(){
        return isOpen;
    }

    public String getcurrentFileName(){
        return currentFile.getName();
    }
}
