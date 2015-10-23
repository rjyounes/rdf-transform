package org.ld4l.rdftransform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public abstract class RdfDataTransformer {

    private static final Logger LOGGER = 
            LogManager.getLogger(RdfDataTransformer.class);  
    
    private final File outputFile;
    
    protected Model model;
    protected Model assertions;
    protected Model retractions;
    
    public RdfDataTransformer(File inputFile, File outputFile) {
        this.outputFile = outputFile;
        
        model = ModelFactory.createDefaultModel();
        try {
            String canonicalPath = inputFile.getCanonicalPath();
            System.out.println("Reading model from file " + canonicalPath);
            model.read(canonicalPath);            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        assertions = ModelFactory.createDefaultModel();
        retractions = ModelFactory.createDefaultModel();
    }

    public abstract void transform();
    
//    protected Model readModelFromFile(File file) {
//        return readModelFromFile(file.toString());
//    }
//    
//    protected Model readModelFromFile(String filename) {
//        //return RDFDataMgr.loadModel(filename);
//        System.out.println("Reading file " + filename);
//        Model model = ModelFactory.createDefaultModel() ; 
//        model.read(filename);
//        return model;
//    }
    
    protected void writeNewModel() {
        applyModelChanges();
        writeModelToFile();       
    }
    
    private void applyModelChanges() {
        model.remove(retractions);
        model.add(assertions);
    }

    private void writeModelToFile() {
        
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(outputFile, false);
            RDFDataMgr.write(outStream, model, RDFFormat.RDFXML);
                    
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }          
    }
    
}
